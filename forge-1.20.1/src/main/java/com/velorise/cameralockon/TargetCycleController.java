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

            LivingEntity next = findNextTarget(player, level, current);
            if (next != null && next != current) {
                LockOnController.requestLock(next, LockOnController.LockReason.SWITCH, true);
            }
        }
    }

    /** Applies the Retarget Rule (Any / Same Type First / Same Type Only) on top of the clockwise search. */
    private static LivingEntity findNextTarget(
            LocalPlayer player,
            ClientLevel level,
            LivingEntity current
    ) {
        CameraLockOnConfig.RetargetMode mode =
                CameraLockOnConfig.RetargetMode.fromConfig(CameraLockOnConfig.RETARGET_MODE.get());
        if (mode == CameraLockOnConfig.RetargetMode.ANY) {
            return findNextClockwiseTarget(player, level, current, null);
        }

        LivingEntity sameType = findNextClockwiseTarget(player, level, current, current.getType());
        if (sameType != null) {
            return sameType;
        }

        return mode == CameraLockOnConfig.RetargetMode.SAME_TYPE_FIRST
                ? findNextClockwiseTarget(player, level, current, null)
                : null;
    }

    private static LivingEntity findNextClockwiseTarget(
            LocalPlayer player,
            ClientLevel level,
            LivingEntity current,
            EntityType<?> requiredType
    ) {
        double range = CameraLockOnConfig.LOCK_ON_RANGE.get();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        double currentAngle = horizontalAngle(player, current);

        LivingEntity bestForward = null;
        LivingEntity wrapTarget = null;
        double bestForwardDelta = Double.MAX_VALUE;
        double smallestAbsoluteAngle = Double.MAX_VALUE;

        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)
                    || living == player
                    || living == current
                    || (requiredType != null && living.getType() != requiredType)
                    || !TargetingRules.isEligible(
                            player,
                            living,
                            true,
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

            double angle = horizontalAngle(player, living);
            double clockwiseDelta = positiveDegrees(angle - currentAngle);
            if (clockwiseDelta > MINIMUM_ANGLE_STEP && clockwiseDelta < bestForwardDelta) {
                bestForwardDelta = clockwiseDelta;
                bestForward = living;
            }

            double normalizedAngle = positiveDegrees(angle);
            if (normalizedAngle < smallestAbsoluteAngle) {
                smallestAbsoluteAngle = normalizedAngle;
                wrapTarget = living;
            }
        }

        return bestForward != null ? bestForward : wrapTarget;
    }

    private static double horizontalAngle(LocalPlayer player, LivingEntity target) {
        double deltaX = target.getBoundingBox().getCenter().x - player.getX();
        double deltaZ = target.getBoundingBox().getCenter().z - player.getZ();
        return positiveDegrees(Math.toDegrees(Math.atan2(-deltaX, deltaZ)));
    }

    private static double positiveDegrees(double degrees) {
        return Mth.positiveModulo(degrees, 360.0D);
    }
}