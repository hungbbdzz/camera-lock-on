package com.velorise.cameralockon;

import net.minecraft.world.entity.LivingEntity;

/**
 * Applies a vanilla model silhouette only on the local client. No glowing state
 * is sent to the server and the entity does not emit light.
 */
public final class TargetOutlineController {
    private static LivingEntity outlinedTarget;
    private static boolean targetWasAlreadyGlowing;

    private TargetOutlineController() {
    }

    public static void tick() {
        LivingEntity desired = CameraLockOnConfig.TARGET_OUTLINE.get()
                && LockOnController.isActive()
                ? LockOnController.getLockedTarget()
                : null;

        if (desired == null || desired.isRemoved() || !desired.isAlive()) {
            clearCurrent();
            return;
        }

        if (outlinedTarget != desired) {
            clearCurrent();
            outlinedTarget = desired;
            targetWasAlreadyGlowing = desired.isCurrentlyGlowing();
        }

        // This changes only the client-side entity copy. It is never synced.
        if (!outlinedTarget.isCurrentlyGlowing()) {
            outlinedTarget.setGlowingTag(true);
        }
    }

    public static void clearCurrent() {
        if (outlinedTarget != null && !outlinedTarget.isRemoved() && !targetWasAlreadyGlowing) {
            outlinedTarget.setGlowingTag(false);
        }
        outlinedTarget = null;
        targetWasAlreadyGlowing = false;
    }
}
