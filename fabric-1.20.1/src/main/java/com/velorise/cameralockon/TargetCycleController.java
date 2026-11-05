package com.velorise.cameralockon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Implements the optional stable clockwise target cycle while preserving the original Smart mode. */
public final class TargetCycleController {
    private static final double WIDE_VIEW_DOT_THRESHOLD = -0.20D;
    private static final double MINIMUM_ANGLE_STEP = 0.25D;

    private TargetCycleController() {
    }

    public static void clientTick() {
        if (ClientFeatureStore.getSwitchTargetMode() != ClientFeatureStore.SwitchTargetMode.CYCLE) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null || minecraft.screen != null) {
            return;
        }

        while (LockOnController.SWITCH_TARGET_KEY.consumeClick()) {
            LivingEntity current = LockOnController.getLockedTarget();
            if (!LockOnController.isActive() || current == null) {
                continue;
            }

            LivingEntity next = findNextTarget(player, level, current, true);
            if (next != null && next != current) {
                LockOnController.requestLock(next, LockOnController.LockReason.SWITCH, true);
            }
        }

        while (LockOnController.SWITCH_TARGET_PREVIOUS_KEY.consumeClick()) {
            LivingEntity current = LockOnController.getLockedTarget();
            if (!LockOnController.isActive() || current == null) {
                continue;
            }

            LivingEntity previous = findNextTarget(player, level, current, false);
            if (previous != null && previous != current) {
                LockOnController.requestLock(previous, LockOnController.LockReason.SWITCH, true);
            }
        }
    }

    public static LivingEntity findPreviousTarget(
            LocalPlayer player,
            ClientLevel level,
            LivingEntity current
    ) {
        return findNextTarget(player, level, current, false);
    }

    /** Applies the Retarget Rule (Any / Same Type First / Same Type Only) on top of the directional search. */
    private static LivingEntity findNextTarget(
            LocalPlayer player,
            ClientLevel level,
            LivingEntity current,
            boolean clockwise
    ) {
        CameraLockOnConfig.RetargetMode mode =
                CameraLockOnConfig.RetargetMode.fromConfig(CameraLockOnConfig.RETARGET_MODE.get());
        if (mode == CameraLockOnConfig.RetargetMode.ANY) {
            return findNextDirectionalTarget(player, level, current, null, clockwise);
        }

        LivingEntity sameType = findNextDirectionalTarget(player, level, current, current.getType(), clockwise);
        if (sameType != null) {
            return sameType;
        }

        return mode == CameraLockOnConfig.RetargetMode.SAME_TYPE_FIRST
                ? findNextDirectionalTarget(player, level, current, null, clockwise)
                : null;
    }

    private static LivingEntity findNextDirectionalTarget(
            LocalPlayer player,
            ClientLevel level,
            LivingEntity current,
            EntityType<?> requiredType,
            boolean clockwise
    ) {
        double range = CameraLockOnConfig.LOCK_ON_RANGE.get();
        Vec3 eye = ThirdPersonAimResolver.targetingOrigin(player);
        Vec3 look = ThirdPersonAimResolver.targetingDirection(player);
        double currentAngle = horizontalAngle(eye, current);

        LivingEntity bestForward = null;
        LivingEntity wrapTarget = null;
        double bestForwardDelta = Double.MAX_VALUE;
        double bestWrapAngle = clockwise ? Double.MAX_VALUE : -Double.MAX_VALUE;

        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)
                    || living == player
                    || living == current
                    || (requiredType != null && living.getType() != requiredType)
                    || !TargetingRules.isEligible(
                            player,
                            living,
                            false,
                            LockOnController.getTemporaryPinnedType(),
                            range
                    )) {
                continue;
            }

            Vec3 direction = living.getBoundingBox().getCenter().subtract(eye);
            double length = direction.length();
            if (length <= 0.0001D || direction.scale(1.0D / length).dot(look) <= WIDE_VIEW_DOT_THRESHOLD) {
                continue;
            }

            if (!player.hasLineOfSight(living)) {
                continue;
            }

            double angle = horizontalAngle(eye, living);
            double directionalDelta = clockwise
                    ? positiveDegrees(angle - currentAngle)
                    : positiveDegrees(currentAngle - angle);
            if (directionalDelta > MINIMUM_ANGLE_STEP && directionalDelta < bestForwardDelta) {
                bestForwardDelta = directionalDelta;
                bestForward = living;
            }

            double normalizedAngle = positiveDegrees(angle);
            if (clockwise ? (normalizedAngle < bestWrapAngle) : (normalizedAngle > bestWrapAngle)) {
                bestWrapAngle = normalizedAngle;
                wrapTarget = living;
            }
        }

        return bestForward != null ? bestForward : wrapTarget;
    }

    private static double horizontalAngle(Vec3 origin, LivingEntity target) {
        double deltaX = target.getBoundingBox().getCenter().x - origin.x;
        double deltaZ = target.getBoundingBox().getCenter().z - origin.z;
        return positiveDegrees(Math.toDegrees(Math.atan2(-deltaX, deltaZ)));
    }

    private static double positiveDegrees(double degrees) {
        return Mth.positiveModulo(degrees, 360.0D);
    }
}