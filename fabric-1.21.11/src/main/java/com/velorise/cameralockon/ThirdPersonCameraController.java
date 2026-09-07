package com.velorise.cameralockon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Runtime controller for the optional third-person shoulder camera. */
public final class ThirdPersonCameraController {
    private static double currentSide;
    private static double currentUp;
    private static double currentBack;
    private static long lastPositionNanos;

    /*
     * Visual camera yaw/pitch are kept separate from the player's real firing
     * rotation. Vanilla mouse input first reaches the player; captureMouseLook
     * extracts that delta before lock/free-aim code drives the player again.
     */
    private static float visualYaw;
    private static float visualPitch;
    /* Final camera rotation. In Converged Lock it looks from the offset camera to the shared lock point. */
    private static float renderedYaw;
    private static float renderedPitch;
    private static float lastDrivenPlayerYaw;
    private static float lastDrivenPlayerPitch;
    private static boolean visualLookInitialized;
    private static boolean playerRotationDriven;

    /* Smooth transition used only when Converged Lock starts steering the camera. */
    private static boolean followStateInitialized;
    private static boolean followTarget;
    private static float followStartYaw;
    private static float followStartPitch;
    private static double followProgress;
    private static long lastFollowNanos;

    private static Vec3 smoothedConvergedAimPoint;
    private static int smoothedConvergedTargetId = Integer.MIN_VALUE;
    private static long lastConvergedAimNanos;
    private static long lastRenderedRotationNanos;

    private ThirdPersonCameraController() {}

    public static void tick() {
        while (LockOnController.CYCLE_CAMERA_POSITION_KEY.consumeClick()) {
            cyclePosition();
        }
    }

    public static void cyclePosition() {
        List<ClientFeatureStore.CameraSlot> enabled = new ArrayList<>();
        for (ClientFeatureStore.CameraSlot slot : ClientFeatureStore.CameraSlot.values()) {
            if (ClientFeatureStore.isCameraSlotEnabledInCycle(slot)) enabled.add(slot);
        }
        if (enabled.isEmpty()) {
            ClientFeatureStore.setCameraSlotEnabledInCycle(
                    ClientFeatureStore.CameraSlot.LEFT_SHOULDER, true);
            enabled.add(ClientFeatureStore.CameraSlot.LEFT_SHOULDER);
        }
        ClientFeatureStore.CameraSlot current = ClientFeatureStore.getActiveCameraSlot();
        int index = enabled.indexOf(current);
        ClientFeatureStore.CameraSlot next = enabled.get(
                (index + 1 + enabled.size()) % enabled.size());
        ClientFeatureStore.setActiveCameraSlot(next);
        Minecraft minecraft = Minecraft.getInstance();
        if (ClientFeatureStore.isCameraPositionMessageEnabled()
                && minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable(
                    "message.camera_lockon.camera_position",
                    next.getDisplayName()
            ), true);
        }
    }

    /** Capture the mouse delta before aim code changes the player's rotation. */
    public static void captureMouseLook() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.getCameraType().isFirstPerson()
                || minecraft.options.getCameraType().isMirrored()
                || (!shouldApply(player) && !hasVisibleOffset())) {
            resetVisualLook();
            return;
        }

        if (!visualLookInitialized) {
            visualYaw = player.getYRot();
            visualPitch = player.getXRot();
            renderedYaw = visualYaw;
            renderedPitch = visualPitch;
            lastDrivenPlayerYaw = player.getYRot();
            lastDrivenPlayerPitch = player.getXRot();
            visualLookInitialized = true;
            playerRotationDriven = false;
            return;
        }

        float referenceYaw = playerRotationDriven
                ? lastDrivenPlayerYaw
                : visualYaw;
        float referencePitch = playerRotationDriven
                ? lastDrivenPlayerPitch
                : visualPitch;
        float yawDelta = Mth.wrapDegrees(player.getYRot() - referenceYaw);
        float pitchDelta = player.getXRot() - referencePitch;

        // Large changes normally mean a teleport/view reset rather than mouse input.
        if (Math.abs(yawDelta) > 120.0F || Math.abs(pitchDelta) > 90.0F) {
            visualYaw = player.getYRot();
            visualPitch = player.getXRot();
        } else {
            visualYaw += yawDelta;
            visualPitch = Mth.clamp(visualPitch + pitchDelta, -90.0F, 90.0F);
        }
        playerRotationDriven = false;
    }

    public static void markPlayerRotationDriven(LocalPlayer player) {
        lastDrivenPlayerYaw = player.getYRot();
        lastDrivenPlayerPitch = player.getXRot();
        playerRotationDriven = true;
    }

    public static Vec3 apply(Entity cameraEntity, Vec3 vanillaPosition, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(cameraEntity instanceof LocalPlayer player)
                || minecraft.options.getCameraType().isFirstPerson()
                || minecraft.options.getCameraType().isMirrored()) {
            resetRuntime();
            return vanillaPosition;
        }

        ensureVisualLook(player);
        boolean active = shouldApply(player);
        ClientFeatureStore.CameraPosition position = ClientFeatureStore.getCameraPosition(
                ClientFeatureStore.getActiveCameraSlot());
        smoothTo(active ? position.horizontalOffset() : 0.0D,
                active ? position.verticalOffset() : 0.0D,
                active ? position.distanceOffset() : 0.0D);

        if (!active && !hasVisibleOffset()) {
            resetFollowState();
            return vanillaPosition;
        }

        /*
         * In Follow Aim, lock-on steering is allowed to rotate the visual camera.
         * In Free Camera, the mouse view remains independent and the dynamic
         * cursor shows the player's actual eye/firing direction instead.
         */
        updateFollowAimCamera(player, active);

        Vec3 pivot = player.getEyePosition(partialTick);
        Vec3 forward = Vec3.directionFromRotation(visualPitch, visualYaw).normalize();
        Vec3 worldUp = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = worldUp.cross(forward);
        if (right.lengthSqr() < 1.0E-8D) {
            double radians = Math.toRadians(visualYaw);
            right = new Vec3(Math.cos(radians), 0.0D, Math.sin(radians));
        } else {
            right = right.normalize();
        }

        double baseDistance = 4.0D;
        Vec3 desired = pivot
                .subtract(forward.scale(baseDistance + currentBack))
                .add(right.scale(currentSide))
                .add(worldUp.scale(currentUp));

        HitResult hit = player.level().clip(new ClipContext(
                pivot,
                desired,
                ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE,
                player
        ));
        if (hit.getType() != HitResult.Type.MISS) {
            Vec3 hitPos = hit.getLocation();
            Vec3 direction = desired.subtract(pivot);
            double length = direction.length();
            if (length > 1.0E-4D) {
                desired = hitPos.subtract(direction.scale(0.12D / length));
            }
        }

        updateRenderedRotation(player, desired);
        return desired;
    }

    public static boolean shouldOverrideCameraRotation() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
                && !minecraft.options.getCameraType().isFirstPerson()
                && !minecraft.options.getCameraType().isMirrored()
                && visualLookInitialized
                && (shouldApply(minecraft.player) || hasVisibleOffset());
    }

    public static float getVisualYaw() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) ensureVisualLook(minecraft.player);
        return renderedYaw;
    }

    public static float getVisualPitch() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) ensureVisualLook(minecraft.player);
        return renderedPitch;
    }

    public static boolean isVisualCameraActive() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
                && !minecraft.options.getCameraType().isFirstPerson()
                && !minecraft.options.getCameraType().isMirrored()
                && (shouldApply(minecraft.player) || hasVisibleOffset());
    }

    /** Runtime aim style after Contextual and hard-lock overrides are resolved. */
    public static boolean isFollowAimActive() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !isVisualCameraActive()) {
            return false;
        }
        return isConvergedLockActive();
    }

    /** True only while a valid lock should share one point between camera, cursor and player ray. */
    public static boolean isConvergedLockActive() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        return player != null
                && isVisualCameraActive()
                && LockOnController.canSteerToLockedTarget()
                && resolveAimStyle(player) == ClientFeatureStore.ThirdPersonAimStyle.CONVERGED_LOCK;
    }

    /** True while Converged Lock is actively steering the visible camera. */
    public static boolean isConvergedCameraSteeringActive() {
        return isConvergedLockActive() && !LockOnController.isTemporaryFreeLookActive();
    }

    public static ClientFeatureStore.ThirdPersonAimStyle resolveAimStyle(LocalPlayer player) {
        // Contextual is always free-look. Lock-on may steer the player's real aim,
        // but it never forces the visual camera. Only Converged Lock does that.
        return ClientFeatureStore.getThirdPersonAimStyle();
    }

    private static void updateFollowAimCamera(LocalPlayer player, boolean active) {
        ClientFeatureStore.ThirdPersonAimStyle style = resolveAimStyle(player);
        boolean desiredFollow = active
                && LockOnController.isActive()
                && style == ClientFeatureStore.ThirdPersonAimStyle.CONVERGED_LOCK
                && !LockOnController.isTemporaryFreeLookActive();

        if (!followStateInitialized) {
            followStateInitialized = true;
            followTarget = desiredFollow;
            followStartYaw = visualYaw;
            followStartPitch = visualPitch;
            followProgress = desiredFollow ? 1.0D : 0.0D;
            lastFollowNanos = System.nanoTime();
        }

        if (desiredFollow != followTarget) {
            followTarget = desiredFollow;
            followStartYaw = visualYaw;
            followStartPitch = visualPitch;
            followProgress = 0.0D;
            lastFollowNanos = System.nanoTime();
        }

        if (!followTarget) {
            return;
        }

        long now = System.nanoTime();
        double dt = Math.min(0.10D, Math.max(0.0D,
                (now - lastFollowNanos) / 1_000_000_000.0D));
        lastFollowNanos = now;

        double speed = Mth.clamp(ClientFeatureStore.getCameraTransitionSpeed(), 0.05D, 1.0D);
        double aimStrength = Mth.clamp(
                CameraLockOnConfig.THIRD_PERSON_AIM_STRENGTH.get(), 0.25D, 2.0D);
        double duration = (0.38D - speed * 0.26D) / Math.sqrt(aimStrength);
        followProgress = Math.min(1.0D, followProgress + dt / Math.max(0.08D, duration));
        float t = smoothStep((float) followProgress);

        visualYaw = lerpDegrees(t, followStartYaw, player.getYRot());
        visualPitch = Mth.lerp(t, followStartPitch, player.getXRot());
        if (followProgress >= 1.0D) {
            visualYaw = player.getYRot();
            visualPitch = player.getXRot();
        }
    }


    public static Vec3 stabilizeConvergedAimPoint(Entity target, Vec3 rawPoint) {
        if (target == null || rawPoint == null || !isConvergedLockActive()) {
            resetConvergedAimSmoothing();
            return rawPoint;
        }

        int targetId = target.getId();
        long now = System.nanoTime();
        if (smoothedConvergedAimPoint == null
                || smoothedConvergedTargetId != targetId
                || smoothedConvergedAimPoint.distanceToSqr(rawPoint) > 16.0D) {
            smoothedConvergedAimPoint = rawPoint;
            smoothedConvergedTargetId = targetId;
            lastConvergedAimNanos = now;
            return rawPoint;
        }

        double dt = Math.min(0.10D, Math.max(0.0D,
                (now - lastConvergedAimNanos) / 1_000_000_000.0D));
        lastConvergedAimNanos = now;

        Minecraft minecraft = Minecraft.getInstance();
        double distance = minecraft.player == null
                ? 8.0D
                : minecraft.player.getEyePosition().distanceTo(rawPoint);
        double aimStrength = Mth.clamp(
                CameraLockOnConfig.THIRD_PERSON_AIM_STRENGTH.get(), 0.25D, 2.0D);
        double tau = (distance < 4.0D ? 0.22D : distance < 8.0D ? 0.14D : 0.09D)
                / Math.sqrt(aimStrength);
        Vec3 delta = rawPoint.subtract(smoothedConvergedAimPoint);
        if (delta.lengthSqr() < 0.0004D) {
            return smoothedConvergedAimPoint;
        }
        double factor = 1.0D - Math.exp(-dt / tau);
        smoothedConvergedAimPoint = smoothedConvergedAimPoint.add(delta.scale(factor));
        return smoothedConvergedAimPoint;
    }

    private static void resetConvergedAimSmoothing() {
        smoothedConvergedAimPoint = null;
        smoothedConvergedTargetId = Integer.MIN_VALUE;
        lastConvergedAimNanos = 0L;
    }

    /**
     * In Converged Lock the shoulder offset remains, but the final camera ray
     * is rotated from the actual offset/collision-adjusted camera position to
     * the same world point used by the player's eye and projectile direction.
     */
    private static void updateRenderedRotation(LocalPlayer player, Vec3 cameraPosition) {
        if (!isConvergedCameraSteeringActive()) {
            renderedYaw = visualYaw;
            renderedPitch = visualPitch;
            lastRenderedRotationNanos = 0L;
            if (!isConvergedLockActive()) {
                resetConvergedAimSmoothing();
            }
            return;
        }

        Vec3 aimPoint = LockOnController.getLastEffectiveAimPoint();
        if (!isUsableAimPoint(aimPoint)) {
            renderedYaw = visualYaw;
            renderedPitch = visualPitch;
            lastRenderedRotationNanos = 0L;
            return;
        }

        Vec3 delta = aimPoint.subtract(cameraPosition);
        if (delta.lengthSqr() < 1.0E-8D) {
            return;
        }
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float targetYaw = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
        float targetPitch = Mth.clamp(
                (float) -Math.toDegrees(Math.atan2(delta.y, horizontal)),
                -89.5F,
                89.5F
        );

        long now = System.nanoTime();
        double dt = lastRenderedRotationNanos == 0L
                ? 1.0D / 60.0D
                : Math.min(0.10D, Math.max(0.0D,
                (now - lastRenderedRotationNanos) / 1_000_000_000.0D));
        lastRenderedRotationNanos = now;

        double distance = cameraPosition.distanceTo(aimPoint);
        double speed = Mth.clamp(ClientFeatureStore.getCameraTransitionSpeed(), 0.05D, 1.0D);
        double aimStrength = Mth.clamp(
                CameraLockOnConfig.THIRD_PERSON_AIM_STRENGTH.get(), 0.25D, 2.0D);
        double tau = (distance < 4.0D ? 0.28D : distance < 8.0D ? 0.19D : 0.12D)
                * (1.20D - speed * 0.45D) / aimStrength;
        float factor = (float) (1.0D - Math.exp(-dt / Math.max(0.05D, tau)));
        float maxDegreesPerSecond = (float) ((distance < 4.0D ? 95.0D : 180.0D)
                * aimStrength);
        float maxStep = (float) (maxDegreesPerSecond * dt);

        float yawStep = Mth.clamp(Mth.wrapDegrees(targetYaw - renderedYaw) * factor,
                -maxStep, maxStep);
        float pitchStep = Mth.clamp((targetPitch - renderedPitch) * factor,
                -maxStep, maxStep);
        renderedYaw += yawStep;
        renderedPitch = Mth.clamp(renderedPitch + pitchStep, -89.5F, 89.5F);
    }

    private static boolean isUsableAimPoint(Vec3 point) {
        return point != null
                && Double.isFinite(point.x)
                && Double.isFinite(point.y)
                && Double.isFinite(point.z)
                && point.lengthSqr() > 1.0E-8D;
    }

    private static float lerpDegrees(float amount, float start, float end) {
        return start + Mth.wrapDegrees(end - start) * amount;
    }

    private static float smoothStep(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private static void ensureVisualLook(LocalPlayer player) {
        if (!visualLookInitialized) {
            visualYaw = player.getYRot();
            visualPitch = player.getXRot();
            renderedYaw = visualYaw;
            renderedPitch = visualPitch;
            lastDrivenPlayerYaw = player.getYRot();
            lastDrivenPlayerPitch = player.getXRot();
            visualLookInitialized = true;
            playerRotationDriven = false;
        }
    }

    private static boolean hasVisibleOffset() {
        return Math.abs(currentSide) > 1.0E-4D
                || Math.abs(currentUp) > 1.0E-4D
                || Math.abs(currentBack) > 1.0E-4D;
    }

    private static boolean shouldApply(LocalPlayer player) {
        return switch (ClientFeatureStore.getThirdPersonCameraMode()) {
            case OFF -> false;
            case ALWAYS -> true;
            case PROJECTILE_ONLY -> ProjectileAimCalculator.isSupportedWeapon(player);
        };
    }

    private static void smoothTo(double side, double up, double back) {
        double speed = Mth.clamp(
                ClientFeatureStore.getCameraTransitionSpeed(), 0.02D, 1.0D);
        double tau = Mth.lerp(speed, 0.42D, 0.07D);
        double factor = positionSmoothingFactor(tau);
        currentSide += (side - currentSide) * factor;
        currentUp += (up - currentUp) * factor;
        currentBack += (back - currentBack) * factor;
        if (Math.abs(currentSide) < 1.0E-4D) currentSide = 0.0D;
        if (Math.abs(currentUp) < 1.0E-4D) currentUp = 0.0D;
        if (Math.abs(currentBack) < 1.0E-4D) currentBack = 0.0D;
    }

    private static double positionSmoothingFactor(double tauSeconds) {
        long now = System.nanoTime();
        double dt = lastPositionNanos == 0L
                ? 1.0D / 60.0D
                : Math.min(0.10D,
                (now - lastPositionNanos) / 1_000_000_000.0D);
        lastPositionNanos = now;
        return 1.0D - Math.exp(-dt / Math.max(0.001D, tauSeconds));
    }

    private static void resetFollowState() {
        followStateInitialized = false;
        followTarget = false;
        followProgress = 0.0D;
        lastFollowNanos = 0L;
    }

    private static void resetVisualLook() {
        visualLookInitialized = false;
        playerRotationDriven = false;
        renderedYaw = 0.0F;
        renderedPitch = 0.0F;
        lastRenderedRotationNanos = 0L;
        resetFollowState();
    }

    private static void resetRuntime() {
        currentSide = currentUp = currentBack = 0.0D;
        lastPositionNanos = 0L;
        resetVisualLook();
    }
}
