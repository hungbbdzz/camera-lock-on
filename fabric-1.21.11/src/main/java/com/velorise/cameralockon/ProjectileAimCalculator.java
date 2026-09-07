package com.velorise.cameralockon;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Resolves a stable future intercept point for supported ranged weapons. */
public final class ProjectileAimCalculator {
    private static final double ARROW_GRAVITY = 0.05D;
    private static final double AIR_DRAG = 0.99D;
    private static final double MIN_SOLVER_SPEED = 0.25D;
    private static Vec3 smoothedVelocity = Vec3.ZERO;
    private static Vec3 smoothedAimPoint;
    private static int trackedTargetId = Integer.MIN_VALUE;
    private static Vec3 lastResolvedAimPoint;
    private static int lastResolvedTargetId = Integer.MIN_VALUE;

    private static Vec3 lastSmartAimPoint;
    private static Vec3 lastSmartTargetPosition;
    private static int lastSmartTargetId = Integer.MIN_VALUE;
    private static Vec3 cachedSmartAimPoint;
    private static Vec3 cachedSmartTargetPosition;
    private static int cachedSmartTargetId = Integer.MIN_VALUE;
    private static long cachedSmartGameTime = Long.MIN_VALUE;
    private static boolean cachedSmartMultipart;

    private ProjectileAimCalculator() {}

    public static boolean isSupportedWeapon(LocalPlayer player) {
        ItemStack stack = player.getUseItem();
        if (stack.isEmpty()) stack = player.getMainHandItem();
        return stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem
                || ProjectileWeaponStore.contains(stack.getItem());
    }

    public static boolean isCameraAssistActive(LocalPlayer player) {
        return CameraLockOnConfig.ProjectileAssistMode.fromConfig(CameraLockOnConfig.PROJECTILE_ASSIST_MODE.get())
                == CameraLockOnConfig.ProjectileAssistMode.CAMERA
                && isPredictionActive(player);
    }

    /** True only when the currently charged shot has a physically valid low-arc solution. */
    public static boolean canCurrentShotReach(LocalPlayer player, LivingEntity target, Vec3 baseAimPoint) {
        if (!isPredictionActive(player)) return false;
        baseAimPoint = selectBestAimPoint(player, target, baseAimPoint);
        baseAimPoint = AdaptiveAimCalibration.apply(baseAimPoint, player.getEyePosition());
        double speed = projectileSpeed(player);
        if (speed < MIN_SOLVER_SPEED) return false;
        Vec3 velocity = measuredRelativeVelocity(player, target);
        return solveReachableIntercept(player.getEyePosition(), baseAimPoint, velocity, speed) != null;
    }

    public static Vec3 resolveForCamera(LocalPlayer player, LivingEntity target, Vec3 baseAimPoint) {
        if (CameraLockOnConfig.ProjectileAssistMode.fromConfig(CameraLockOnConfig.PROJECTILE_ASSIST_MODE.get())
                != CameraLockOnConfig.ProjectileAssistMode.CAMERA) {
            return baseAimPoint;
        }
        return resolve(player, target, baseAimPoint);
    }

    public static Vec3 resolveForReticle(LocalPlayer player, LivingEntity target, Vec3 baseAimPoint) {
        return resolve(player, target, baseAimPoint);
    }

    private static Vec3 resolve(LocalPlayer player, LivingEntity target, Vec3 baseAimPoint) {
        baseAimPoint = selectBestAimPoint(player, target, baseAimPoint);
        baseAimPoint = AdaptiveAimCalibration.apply(baseAimPoint, player.getEyePosition());
        lastResolvedAimPoint = baseAimPoint;
        lastResolvedTargetId = target.getId();
        CameraLockOnConfig.ProjectileAssistMode mode = CameraLockOnConfig.ProjectileAssistMode
                .fromConfig(CameraLockOnConfig.PROJECTILE_ASSIST_MODE.get());
        if (mode == CameraLockOnConfig.ProjectileAssistMode.OFF || !isPredictionActive(player)) {
            resetIfTargetChanged(target);
            smoothedAimPoint = baseAimPoint;
            return baseAimPoint;
        }

        resetIfTargetChanged(target);
        Vec3 measuredVelocity = new Vec3(target.getX() - target.xo, target.getY() - target.yo, target.getZ() - target.zo);
        double smoothing = CameraLockOnConfig.PROJECTILE_MOTION_SMOOTHING.get();
        smoothedVelocity = smoothedVelocity.scale(smoothing).add(measuredVelocity.scale(1.0D - smoothing));

        double speed = projectileSpeed(player);
        Vec3 relativeVelocity = CameraLockOnConfig.PROJECTILE_COMPENSATE_PLAYER_MOVEMENT.get()
                ? smoothedVelocity.subtract(player.getDeltaMovement()) : smoothedVelocity;

        Vec3 eye = player.getEyePosition();
        boolean usingBow = player.isUsingItem() && player.getUseItem().getItem() instanceof BowItem;

        /*
         * Bow Aim Reference:
         *
         * FULL_CHARGE and RELEASE_THRESHOLD solve a stable destination and only use draw progress
         * to control when the camera transitions toward it. CURRENT_CHARGE deliberately resolves
         * the live shot power for players who prefer partial-charge shooting.
         */
        double referenceSpeed = speed;
        double transitionProgress = 1.0D;
        if (usingBow) {
            CameraLockOnConfig.BowAimReference aimReference = CameraLockOnConfig.BowAimReference
                    .fromConfig(CameraLockOnConfig.BOW_AIM_REFERENCE.get());
            double earlyPrediction = CameraLockOnConfig.PROJECTILE_EARLY_PREDICTION.get();
            switch (aimReference) {
                case FULL_CHARGE -> {
                    referenceSpeed = 3.0D;
                    transitionProgress = earlyLeadProgress(bowChargeRatio(player), earlyPrediction);
                }
                case RELEASE_THRESHOLD -> {
                    double releasePower = Mth.clamp(CameraLockOnConfig.AUTO_RELEASE_BOW_CHARGE.get(), 0.25D, 1.0D);
                    referenceSpeed = releasePower * 3.0D;
                    double releaseDrawRatio = drawRatioForBowPower(releasePower);
                    double normalizedToRelease = Mth.clamp(bowChargeRatio(player) / releaseDrawRatio, 0.0D, 1.0D);
                    transitionProgress = earlyLeadProgress(normalizedToRelease, earlyPrediction);
                }
                case CURRENT_CHARGE -> {
                    // Intentionally follows the current shot power. This mode may produce large
                    // vertical movement at low charge and is therefore never the default.
                    referenceSpeed = speed;
                    transitionProgress = 1.0D;
                }
            }
        }

        ReachableSolution reference = solveReachableIntercept(
                eye,
                baseAimPoint,
                relativeVelocity,
                referenceSpeed
        );
        if (reference == null) return settleOnBase(baseAimPoint);

        double stability = motionStability(measuredVelocity, smoothedVelocity);
        double configuredStrength = CameraLockOnConfig.PROJECTILE_PREDICTION_STRENGTH.get() * stability;

        Vec3 referenceAim = reference.targetPoint;
        if (CameraLockOnConfig.PROJECTILE_COMPENSATE_DROP.get()) {
            referenceAim = referenceAim.add(0.0D, reference.dropCompensation, 0.0D);
        }

        double aimStrength = Mth.clamp(configuredStrength * transitionProgress, 0.0D, 1.0D);
        Vec3 desired = baseAimPoint.lerp(referenceAim, aimStrength);

        if (smoothedAimPoint == null) smoothedAimPoint = baseAimPoint;
        smoothedAimPoint = smoothedAimPoint.lerp(desired, 0.28D);
        return smoothedAimPoint;
    }

    /** Returns true when the selected damage point has a block-clear ballistic path. */
    public static boolean hasClearProjectilePath(LocalPlayer player, LivingEntity target, Vec3 requestedAimPoint) {
        Vec3 selected = selectBestAimPoint(player, target, requestedAimPoint);
        double speed = selectionSpeed(player);
        if (speed < MIN_SOLVER_SPEED) return isBlockRayClear(player, selected);
        Vec3 velocity = measuredRelativeVelocity(player, target);
        ReachableSolution solution = solveReachableIntercept(player.getEyePosition(), selected, velocity, speed);
        return solution != null && isTrajectoryBlockClear(player, solution, speed);
    }

    /**
     * Selects an exposed point from the target's real damage boxes. Normal entities use a 5x5
     * camera-facing sample grid. Multipart entities use the individual part boxes and never fall
     * back to the large parent bounding box, which may contain empty non-damageable space.
     */
    public static Vec3 selectBestAimPoint(LocalPlayer player, LivingEntity target, Vec3 preferred) {
        if (!CameraLockOnConfig.SMART_PROJECTILE_HITBOX.get() || target == null) return preferred;

        long gameTime = player.level().getGameTime();
        Vec3 targetPosition = target.position();
        if (cachedSmartTargetId == target.getId() && cachedSmartAimPoint != null) {
            long age = gameTime - cachedSmartGameTime;
            long maximumAge = cachedSmartMultipart ? 1L : 2L;
            if (age >= 0L && age <= maximumAge) {
                Vec3 translated = cachedSmartAimPoint.add(targetPosition.subtract(cachedSmartTargetPosition));
                return translated;
            }
        }

        List<MultipartDamageBoxResolver.DamageBox> damageBoxes = MultipartDamageBoxResolver.resolve(target);
        if (damageBoxes.isEmpty()) return preferred;
        boolean multipart = damageBoxes.stream().anyMatch(MultipartDamageBoxResolver.DamageBox::multipart);
        CameraLockOnConfig.MultipartAimMode multipartAimMode = CameraLockOnConfig.MultipartAimMode
                .fromConfig(CameraLockOnConfig.MULTIPART_AIM_MODE.get());
        if (multipart && multipartAimMode == CameraLockOnConfig.MultipartAimMode.OFF) {
            rememberSmartAim(target, preferred, gameTime, false);
            return preferred;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 stablePrevious = null;
        if (lastSmartTargetId == target.getId() && lastSmartAimPoint != null && lastSmartTargetPosition != null) {
            stablePrevious = lastSmartAimPoint.add(targetPosition.subtract(lastSmartTargetPosition));
        }

        double speed = selectionSpeed(player);
        Vec3 relativeVelocity = measuredRelativeVelocity(player, target);
        if (multipart && multipartAimMode == CameraLockOnConfig.MultipartAimMode.STABLE_BODY) {
            Vec3 stableBody = selectStableMultipartBodyAim(player, target, preferred, eye, stablePrevious,
                    relativeVelocity, speed, damageBoxes, gameTime);
            rememberSmartAim(target, stableBody, gameTime, true);
            return stableBody;
        }
        List<HitboxCandidate> candidates = new ArrayList<>();
        int boxIndex = 0;
        for (MultipartDamageBoxResolver.DamageBox damageBox : damageBoxes) {
            AABB box = damageBox.box();
            double[] verticalSamples = multipart
                    ? new double[] {0.22D, 0.52D, 0.80D}
                    : new double[] {0.12D, 0.30D, 0.52D, 0.74D, 0.90D};
            double[] lateralSamples = multipart
                    ? new double[] {-0.62D, 0.0D, 0.62D}
                    : new double[] {-0.78D, -0.38D, 0.0D, 0.38D, 0.78D};

            for (int row = 0; row < verticalSamples.length; row++) {
                for (int column = 0; column < lateralSamples.length; column++) {
                    Vec3 point = cameraFacingPoint(box, eye, verticalSamples[row], lateralSamples[column]);
                    ReachableSolution solution = speed >= MIN_SOLVER_SPEED
                            ? solveReachableIntercept(eye, point, relativeVelocity, speed)
                            : null;
                    boolean directClear = isBlockRayClear(player, point);
                    boolean trajectoryClear = solution != null
                            ? isTrajectoryBlockClear(player, solution, speed)
                            : directClear;
                    candidates.add(new HitboxCandidate(boxIndex, row, column,
                            verticalSamples.length, lateralSamples.length,
                            damageBox, point, directClear, trajectoryClear));
                }
            }
            boxIndex++;
        }

        HitboxCandidate best = null;
        HitboxCandidate previousCandidate = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        double previousScore = Double.NEGATIVE_INFINITY;

        for (HitboxCandidate candidate : candidates) {
            if (!candidate.trajectoryClear) continue;
            double exposure = localExposure(candidate, candidates);
            double verticalCenter = 1.0D - Math.min(1.0D, Math.abs(candidate.verticalFraction() - 0.55D) / 0.55D);
            double lateralCenter = 1.0D - Math.min(1.0D, Math.abs(candidate.lateralFraction()));
            double boxDiagonal = Math.max(0.25D, boxDiagonal(candidate.damageBox.box()));
            double preferredPenalty = candidate.point.distanceTo(preferred) / boxDiagonal;
            double stabilityPenalty = stablePrevious == null ? 0.0D
                    : candidate.point.distanceTo(stablePrevious) / boxDiagonal;

            double score = candidate.damageBox.priority() * 1.35D
                    + exposure * 2.65D
                    + verticalCenter * 0.70D
                    + lateralCenter * 0.40D
                    + (candidate.directClear ? 0.35D : 0.0D)
                    - preferredPenalty * 0.30D
                    - stabilityPenalty * 0.85D;
            candidate.score = score;

            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
            if (stablePrevious != null) {
                double previousDistance = candidate.point.distanceToSqr(stablePrevious);
                if (previousCandidate == null
                        || previousDistance < previousCandidate.point.distanceToSqr(stablePrevious)) {
                    previousCandidate = candidate;
                    previousScore = score;
                }
            }
        }

        // Hysteresis: retain the previous exposed region unless the replacement is meaningfully better.
        if (previousCandidate != null && best != null && previousScore >= bestScore - 0.55D) {
            best = previousCandidate;
        }

        Vec3 selected = best != null ? best.point : fallbackDamagePoint(damageBoxes, preferred, eye);
        rememberSmartAim(target, selected, gameTime, multipart);
        return selected;
    }


    private static Vec3 selectStableMultipartBodyAim(LocalPlayer player, LivingEntity target, Vec3 preferred,
                                                     Vec3 eye, Vec3 stablePrevious, Vec3 relativeVelocity,
                                                     double speed, List<MultipartDamageBoxResolver.DamageBox> damageBoxes,
                                                     long gameTime) {
        MultipartDamageBoxResolver.DamageBox anchor = chooseStableMultipartBodyBox(target, damageBoxes);
        if (anchor == null) {
            return fallbackDamagePoint(damageBoxes, preferred, eye);
        }

        List<HitboxCandidate> candidates = new ArrayList<>();
        double[] verticalSamples = new double[] {0.34D, 0.54D, 0.72D};
        double[] lateralSamples = new double[] {-0.26D, 0.0D, 0.26D};
        for (int row = 0; row < verticalSamples.length; row++) {
            for (int column = 0; column < lateralSamples.length; column++) {
                Vec3 point = cameraFacingPoint(anchor.box(), eye, verticalSamples[row], lateralSamples[column]);
                ReachableSolution solution = speed >= MIN_SOLVER_SPEED
                        ? solveReachableIntercept(eye, point, relativeVelocity, speed)
                        : null;
                boolean directClear = isBlockRayClear(player, point);
                boolean trajectoryClear = solution != null
                        ? isTrajectoryBlockClear(player, solution, speed)
                        : directClear;
                candidates.add(new HitboxCandidate(0, row, column,
                        verticalSamples.length, lateralSamples.length,
                        anchor, point, directClear, trajectoryClear));
            }
        }

        HitboxCandidate best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (HitboxCandidate candidate : candidates) {
            if (!candidate.trajectoryClear) continue;
            double exposure = localExposure(candidate, candidates);
            double verticalCenter = 1.0D - Math.min(1.0D, Math.abs(candidate.verticalFraction() - 0.56D) / 0.56D);
            double lateralCenter = 1.0D - Math.min(1.0D, Math.abs(candidate.lateralFraction()) / 0.30D);
            double boxDiagonal = Math.max(0.25D, boxDiagonal(candidate.damageBox.box()));
            double preferredPenalty = candidate.point.distanceTo(preferred) / boxDiagonal;
            double stabilityPenalty = stablePrevious == null ? 0.0D
                    : candidate.point.distanceTo(stablePrevious) / boxDiagonal;
            double score = exposure * 2.6D
                    + verticalCenter * 1.15D
                    + lateralCenter * 1.85D
                    + (candidate.directClear ? 0.35D : 0.0D)
                    - preferredPenalty * 0.20D
                    - stabilityPenalty * 0.55D;
            candidate.score = score;
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        if (best != null) {
            return best.point;
        }

        Vec3 anchorPoint = cameraFacingPoint(anchor.box(), eye, 0.56D, 0.0D);
        if (isBlockRayClear(player, anchorPoint)) {
            return anchorPoint;
        }
        return fallbackDamagePoint(damageBoxes, preferred, eye);
    }

    private static MultipartDamageBoxResolver.DamageBox chooseStableMultipartBodyBox(LivingEntity target,
                                                                                      List<MultipartDamageBoxResolver.DamageBox> damageBoxes) {
        Vec3 center = target.getBoundingBox().getCenter();
        MultipartDamageBoxResolver.DamageBox best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (MultipartDamageBoxResolver.DamageBox damageBox : damageBoxes) {
            AABB box = damageBox.box();
            Vec3 boxCenter = box.getCenter();
            double volume = Math.max(0.001D, (box.maxX - box.minX) * (box.maxY - box.minY) * (box.maxZ - box.minZ));
            String label = damageBox.label();
            double score = Math.log1p(volume) * 1.2D
                    - boxCenter.distanceTo(center) * 0.65D
                    + stableMultipartLabelWeight(label);
            if (score > bestScore) {
                bestScore = score;
                best = damageBox;
            }
        }
        return best;
    }

    private static double stableMultipartLabelWeight(String label) {
        if (label == null) return 0.0D;
        String key = label.toLowerCase(java.util.Locale.ROOT);
        if (key.contains("body") || key.contains("torso") || key.contains("chest") || key.contains("core")) return 5.0D;
        if (key.contains("head") || key.contains("neck") || key.contains("jaw") || key.contains("mouth")) return -1.5D;
        if (key.contains("wing") || key.contains("tail") || key.contains("arm") || key.contains("leg") || key.contains("claw")) return -2.0D;
        return 0.0D;
    }

    private static double selectionSpeed(LocalPlayer player) {
        ItemStack useStack = player.getUseItem();
        ItemStack heldStack = player.getMainHandItem();
        if (useStack.getItem() instanceof BowItem || heldStack.getItem() instanceof BowItem) {
            // A stable full-charge path prevents the selected body region from changing while charging.
            return 3.0D;
        }
        return projectileSpeed(player);
    }

    private static Vec3 cameraFacingPoint(AABB box, Vec3 eye, double verticalFraction, double lateralFraction) {
        Vec3 center = box.getCenter();
        Vec3 horizontal = new Vec3(center.x - eye.x, 0.0D, center.z - eye.z);
        Vec3 side = horizontal.lengthSqr() < 1.0E-8D
                ? new Vec3(1.0D, 0.0D, 0.0D)
                : new Vec3(-horizontal.z, 0.0D, horizontal.x).normalize();

        double halfX = Math.max(0.01D, (box.maxX - box.minX) * 0.5D);
        double halfZ = Math.max(0.01D, (box.maxZ - box.minZ) * 0.5D);
        double sideLimitX = Math.abs(side.x) < 1.0E-6D ? Double.POSITIVE_INFINITY : halfX / Math.abs(side.x);
        double sideLimitZ = Math.abs(side.z) < 1.0E-6D ? Double.POSITIVE_INFINITY : halfZ / Math.abs(side.z);
        double sideLimit = Math.min(sideLimitX, sideLimitZ) * 0.82D;

        double y = Mth.clamp(box.minY + (box.maxY - box.minY) * verticalFraction,
                box.minY + 0.01D, box.maxY - 0.01D);
        Vec3 interior = new Vec3(center.x, y, center.z).add(side.scale(sideLimit * lateralFraction));
        Optional<Vec3> surface = box.clip(eye, interior);
        if (surface.isEmpty()) return interior;

        Vec3 inward = interior.subtract(eye);
        if (inward.lengthSqr() < 1.0E-8D) return interior;
        double inset = Math.min(0.08D, Math.min(box.maxX - box.minX,
                Math.min(box.maxY - box.minY, box.maxZ - box.minZ)) * 0.18D);
        Vec3 point = surface.get().add(inward.normalize().scale(Math.max(0.01D, inset)));
        return new Vec3(
                Mth.clamp(point.x, box.minX + 0.005D, box.maxX - 0.005D),
                Mth.clamp(point.y, box.minY + 0.005D, box.maxY - 0.005D),
                Mth.clamp(point.z, box.minZ + 0.005D, box.maxZ - 0.005D)
        );
    }

    private static double localExposure(HitboxCandidate candidate, List<HitboxCandidate> candidates) {
        int visible = 0;
        int total = 0;
        for (HitboxCandidate other : candidates) {
            if (other.boxIndex != candidate.boxIndex) continue;
            if (Math.abs(other.row - candidate.row) > 1 || Math.abs(other.column - candidate.column) > 1) continue;
            total++;
            if (other.trajectoryClear) visible++;
        }
        return total == 0 ? 0.0D : (double) visible / (double) total;
    }

    private static boolean isTrajectoryBlockClear(LocalPlayer player, ReachableSolution solution, double speed) {
        Vec3 eye = player.getEyePosition();
        Vec3 compensatedPoint = solution.targetPoint.add(0.0D, solution.dropCompensation, 0.0D);
        Vec3 direction = compensatedPoint.subtract(eye);
        if (direction.lengthSqr() < 1.0E-8D) return true;

        Vec3 velocity = direction.normalize().scale(speed);
        if (CameraLockOnConfig.PROJECTILE_COMPENSATE_PLAYER_MOVEMENT.get()) {
            velocity = velocity.add(player.getDeltaMovement());
        }

        Vec3 position = eye;
        int steps = Mth.clamp((int) Math.ceil(solution.flightTime) + 2, 1, 160);
        for (int step = 0; step < steps; step++) {
            Vec3 next = position.add(velocity);
            HitResult hit = player.level().clip(new ClipContext(
                    position, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.MISS) {
                // A collision almost at the predicted target is normally the terrain directly behind it.
                return hit.getLocation().distanceToSqr(solution.targetPoint) <= 0.25D;
            }
            position = next;
            velocity = velocity.scale(AIR_DRAG).add(0.0D, -ARROW_GRAVITY, 0.0D);
        }
        return true;
    }

    private static Vec3 fallbackDamagePoint(List<MultipartDamageBoxResolver.DamageBox> damageBoxes,
                                            Vec3 preferred, Vec3 eye) {
        Vec3 best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (MultipartDamageBoxResolver.DamageBox damageBox : damageBoxes) {
            AABB box = damageBox.box();
            Vec3 clamped = new Vec3(
                    Mth.clamp(preferred.x, box.minX + 0.01D, box.maxX - 0.01D),
                    Mth.clamp(preferred.y, box.minY + 0.01D, box.maxY - 0.01D),
                    Mth.clamp(preferred.z, box.minZ + 0.01D, box.maxZ - 0.01D)
            );
            Vec3 point = cameraFacingPoint(box, eye,
                    Mth.clamp((clamped.y - box.minY) / Math.max(0.01D, box.maxY - box.minY), 0.1D, 0.9D),
                    0.0D);
            double score = damageBox.priority() * 1.5D - point.distanceTo(preferred) * 0.25D;
            if (score > bestScore) {
                bestScore = score;
                best = point;
            }
        }
        return best != null ? best : preferred;
    }

    private static double boxDiagonal(AABB box) {
        double x = box.maxX - box.minX;
        double y = box.maxY - box.minY;
        double z = box.maxZ - box.minZ;
        return Math.sqrt(x * x + y * y + z * z);
    }

    private static void rememberSmartAim(LivingEntity target, Vec3 selected, long gameTime, boolean multipart) {
        lastSmartTargetId = target.getId();
        lastSmartAimPoint = selected;
        lastSmartTargetPosition = target.position();
        cachedSmartTargetId = target.getId();
        cachedSmartAimPoint = selected;
        cachedSmartTargetPosition = target.position();
        cachedSmartGameTime = gameTime;
        cachedSmartMultipart = multipart;
    }

    private static boolean isBlockRayClear(LocalPlayer player, Vec3 point) {
        HitResult hit = player.level().clip(new ClipContext(
                player.getEyePosition(), point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) return true;
        return hit.getLocation().distanceToSqr(point) <= 0.16D;
    }

    private static ReachableSolution solveReachableIntercept(Vec3 eye, Vec3 baseAimPoint, Vec3 relativeVelocity, double speed) {
        double maxTicks = CameraLockOnConfig.PROJECTILE_MAX_FLIGHT_TIME.get() * 20.0D;
        double time = Mth.clamp(eye.distanceTo(baseAimPoint) / Math.max(speed, MIN_SOLVER_SPEED), 0.0D, maxTicks);
        Vec3 targetPoint = baseAimPoint;

        for (int i = 0; i < 6; i++) {
            targetPoint = baseAimPoint.add(relativeVelocity.scale(time));
            BallisticCheck check = checkBallisticReachability(eye, targetPoint, speed);
            if (!check.reachable) return null;

            double averageSpeed = effectiveAverageSpeed(speed, time);
            double nextTime = Mth.clamp(check.horizontalDistance / Math.max(0.05D, averageSpeed * check.horizontalCosine), 0.0D, maxTicks);
            if (nextTime >= maxTicks - 1.0E-4D) return null;
            time = time * 0.45D + nextTime * 0.55D;
        }

        BallisticCheck finalCheck = checkBallisticReachability(eye, targetPoint, speed);
        if (!finalCheck.reachable || time <= 0.0D || time > maxTicks) return null;

        // Retain the existing discrete-gravity compensation while reachability is determined analytically.
        double drop = 0.5D * ARROW_GRAVITY * time * time;
        return new ReachableSolution(targetPoint, time, drop, finalCheck.requiredMinimumSpeed);
    }

    private static BallisticCheck checkBallisticReachability(Vec3 eye, Vec3 targetPoint, double speed) {
        Vec3 delta = targetPoint.subtract(eye);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        double vertical = delta.y;
        if (horizontal < 1.0E-4D) {
            return new BallisticCheck(Math.abs(vertical) <= speed * 20.0D, horizontal, 1.0D, 0.0D);
        }

        double speedSq = speed * speed;
        double discriminant = speedSq * speedSq
                - ARROW_GRAVITY * (ARROW_GRAVITY * horizontal * horizontal + 2.0D * vertical * speedSq);
        if (discriminant < 0.0D) {
            return new BallisticCheck(false, horizontal, 0.0D, minimumSpeedForTarget(horizontal, vertical));
        }

        double tanTheta = (speedSq - Math.sqrt(discriminant)) / (ARROW_GRAVITY * horizontal);
        double horizontalCosine = 1.0D / Math.sqrt(1.0D + tanTheta * tanTheta);
        if (!Double.isFinite(horizontalCosine) || horizontalCosine < 0.08D) {
            return new BallisticCheck(false, horizontal, 0.0D, minimumSpeedForTarget(horizontal, vertical));
        }
        return new BallisticCheck(true, horizontal, horizontalCosine, minimumSpeedForTarget(horizontal, vertical));
    }

    private static double minimumSpeedForTarget(double horizontal, double vertical) {
        double distance = Math.sqrt(horizontal * horizontal + vertical * vertical);
        double value = ARROW_GRAVITY * (vertical + distance);
        return value <= 0.0D ? 0.0D : Math.sqrt(value);
    }

    /**
     * Maps Early Prediction to the real bow draw timeline.
     * 0% starts at 90% charge, 50% at 45%, and 100% at the beginning.
     * The destination is always the same full-charge ballistic aim point.
     */
    private static double earlyLeadProgress(double chargeRatio, double earlyPrediction) {
        double early = Mth.clamp(earlyPrediction, 0.0D, 1.0D);
        double startCharge = 0.90D * (1.0D - early);
        double transitionWindow = Math.max(0.10D, 1.0D - startCharge);
        double normalized = Mth.clamp((chargeRatio - startCharge) / transitionWindow, 0.0D, 1.0D);
        return smoothStep(normalized);
    }

    private static double bowChargeRatio(LocalPlayer player) {
        if (!player.isUsingItem() || !(player.getUseItem().getItem() instanceof BowItem)) return 1.0D;
        return Mth.clamp(player.getTicksUsingItem() / 20.0D, 0.0D, 1.0D);
    }

    /** Inverse of BowItem's vanilla power curve: power = (draw^2 + 2 * draw) / 3. */
    private static double drawRatioForBowPower(double power) {
        double normalizedPower = Mth.clamp(power, 0.0D, 1.0D);
        return Mth.clamp(Math.sqrt(1.0D + 3.0D * normalizedPower) - 1.0D, 0.01D, 1.0D);
    }

    private static double smoothStep(double value) {
        double t = Mth.clamp(value, 0.0D, 1.0D);
        return t * t * (3.0D - 2.0D * t);
    }


    private static Vec3 measuredRelativeVelocity(LocalPlayer player, LivingEntity target) {
        Vec3 measured = new Vec3(target.getX() - target.xo, target.getY() - target.yo, target.getZ() - target.zo);
        return CameraLockOnConfig.PROJECTILE_COMPENSATE_PLAYER_MOVEMENT.get()
                ? measured.subtract(player.getDeltaMovement()) : measured;
    }

    private static Vec3 settleOnBase(Vec3 baseAimPoint) {
        if (smoothedAimPoint == null) smoothedAimPoint = baseAimPoint;
        smoothedAimPoint = smoothedAimPoint.lerp(baseAimPoint, 0.55D);
        return smoothedAimPoint;
    }


    public static Vec3 getLastResolvedAimPoint(LivingEntity target) {
        return target != null && target.getId() == lastResolvedTargetId ? lastResolvedAimPoint : null;
    }

    private static boolean isPredictionActive(LocalPlayer player) {
        ItemStack useStack = player.getUseItem();
        if (player.isUsingItem() && useStack.getItem() instanceof BowItem) return true;
        ItemStack held = player.getMainHandItem();
        return held.getItem() instanceof CrossbowItem;
    }

    public static double projectileSpeedForPreview(LocalPlayer player) {
        return projectileSpeed(player);
    }

    private static double projectileSpeed(LocalPlayer player) {
        ItemStack stack = player.getUseItem();
        if (stack.getItem() instanceof BowItem) {
            int ticks = player.getTicksUsingItem();
            float charge = ticks / 20.0F;
            charge = (charge * charge + charge * 2.0F) / 3.0F;
            return Mth.clamp(charge, 0.0F, 1.0F) * 3.0D;
        }
        return 3.15D;
    }

    private static double effectiveAverageSpeed(double initialSpeed, double ticks) {
        if (ticks <= 0.0D) return initialSpeed;
        double retained = Math.pow(AIR_DRAG, Math.max(1.0D, ticks * 0.5D));
        return Math.max(0.2D, initialSpeed * retained);
    }

    private static double motionStability(Vec3 current, Vec3 smooth) {
        double speed = smooth.length();
        if (speed < 0.015D) return 1.0D;
        if (current.lengthSqr() < 1.0E-6D) return 0.55D;
        double agreement = current.normalize().dot(smooth.normalize());
        return Mth.clamp(0.35D + Math.max(0.0D, agreement) * 0.65D, 0.35D, 1.0D);
    }

    private static void resetIfTargetChanged(LivingEntity target) {
        if (trackedTargetId != target.getId()) {
            trackedTargetId = target.getId();
            smoothedVelocity = Vec3.ZERO;
            smoothedAimPoint = null;
        }
    }

    private static final class HitboxCandidate {
        private final int boxIndex;
        private final int row;
        private final int column;
        private final int rowCount;
        private final int columnCount;
        private final MultipartDamageBoxResolver.DamageBox damageBox;
        private final Vec3 point;
        private final boolean directClear;
        private final boolean trajectoryClear;
        private double score;

        private HitboxCandidate(int boxIndex, int row, int column, int rowCount, int columnCount,
                                MultipartDamageBoxResolver.DamageBox damageBox, Vec3 point,
                                boolean directClear, boolean trajectoryClear) {
            this.boxIndex = boxIndex;
            this.row = row;
            this.column = column;
            this.rowCount = rowCount;
            this.columnCount = columnCount;
            this.damageBox = damageBox;
            this.point = point;
            this.directClear = directClear;
            this.trajectoryClear = trajectoryClear;
        }

        private double verticalFraction() {
            return rowCount <= 1 ? 0.5D : (double) row / (double) (rowCount - 1);
        }

        private double lateralFraction() {
            return columnCount <= 1 ? 0.0D : ((double) column / (double) (columnCount - 1)) * 2.0D - 1.0D;
        }
    }

    private static final class ReachableSolution {
        private final Vec3 targetPoint;
        private final double flightTime;
        private final double dropCompensation;
        private final double requiredMinimumSpeed;

        private ReachableSolution(Vec3 targetPoint, double flightTime, double dropCompensation, double requiredMinimumSpeed) {
            this.targetPoint = targetPoint;
            this.flightTime = flightTime;
            this.dropCompensation = dropCompensation;
            this.requiredMinimumSpeed = requiredMinimumSpeed;
        }
    }

    private static final class BallisticCheck {
        private final boolean reachable;
        private final double horizontalDistance;
        private final double horizontalCosine;
        private final double requiredMinimumSpeed;

        private BallisticCheck(boolean reachable, double horizontalDistance, double horizontalCosine, double requiredMinimumSpeed) {
            this.reachable = reachable;
            this.horizontalDistance = horizontalDistance;
            this.horizontalCosine = horizontalCosine;
            this.requiredMinimumSpeed = requiredMinimumSpeed;
        }
    }
}
