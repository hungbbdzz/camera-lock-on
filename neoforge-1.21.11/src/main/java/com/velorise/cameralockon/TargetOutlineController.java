package com.velorise.cameralockon;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

import java.lang.reflect.Method;

/**
 * Applies a client-only model silhouette to the currently locked target.
 * Works via EntityOutlineMixin (zero flicker + custom reticle color) with a
 * client-side shared-flag fallback. Respects Line-of-Sight STRICT mode.
 */
public final class TargetOutlineController {
    private static final int GLOWING_SHARED_FLAG = 6;
    private static final Method SET_SHARED_FLAG_METHOD = resolveSetSharedFlagMethod();

    private static LivingEntity outlinedTarget;
    private static boolean targetWasAlreadyGlowing;

    private TargetOutlineController() {
    }

    /**
     * Returns true when the given client entity (or multipart sub-entity) should
     * render with a target outline in the current frame.
     */
    public static boolean shouldOutline(Entity entity) {
        if (entity == null
                || !entity.level().isClientSide()
                || !CameraLockOnConfig.TARGET_OUTLINE.get()
                || !LockOnController.canRenderLockedTargetInfo()) {
            return false;
        }

        LivingEntity locked = LockOnController.getLockedTarget();
        if (locked == null || locked.isRemoved() || !locked.isAlive()) {
            return false;
        }

        if (entity == locked) {
            return true;
        }

        // Support vanilla Ender Dragon multipart body/head/wing sub-entities
        if (locked instanceof EnderDragon dragon
                && entity instanceof EnderDragonPart part
                && part.parentMob == dragon) {
            return true;
        }

        return false;
    }

    /**
     * Resolves the 24-bit RGB outline color matching the configured Reticle Color.
     */
    public static int getOutlineColor() {
        String color = CameraLockOnConfig.RETICLE_COLOR.get();
        if ("Red".equalsIgnoreCase(color)) {
            return 0xFF331A;
        }
        if ("Green".equalsIgnoreCase(color)) {
            return 0x33FF33;
        }
        if ("Yellow".equalsIgnoreCase(color)) {
            return 0xFFE61A;
        }
        if ("Purple".equalsIgnoreCase(color)) {
            return 0xCC33FF;
        }
        return 0x1AE6CC; // Default Cyan
    }

    public static void tick() {
        LivingEntity desired = CameraLockOnConfig.TARGET_OUTLINE.get()
                && LockOnController.canRenderLockedTargetInfo()
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

        // Client-side fallback for environments without EntityOutlineMixin refmap
        if (!outlinedTarget.isCurrentlyGlowing()) {
            outlinedTarget.setGlowingTag(true);
            setClientGlowingSharedFlag(outlinedTarget, true);
        }
    }

    public static void clearCurrent() {
        if (outlinedTarget != null && !outlinedTarget.isRemoved() && !targetWasAlreadyGlowing) {
            outlinedTarget.setGlowingTag(false);
            setClientGlowingSharedFlag(outlinedTarget, false);
        }
        outlinedTarget = null;
        targetWasAlreadyGlowing = false;
    }

    private static void setClientGlowingSharedFlag(Entity entity, boolean glowing) {
        if (entity == null || !entity.level().isClientSide() || SET_SHARED_FLAG_METHOD == null) {
            return;
        }
        try {
            SET_SHARED_FLAG_METHOD.invoke(entity, GLOWING_SHARED_FLAG, glowing);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private static Method resolveSetSharedFlagMethod() {
        for (String name : new String[] {"setSharedFlag", "m_20115_"}) {
            try {
                Method method = Entity.class.getDeclaredMethod(name, int.class, boolean.class);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException | RuntimeException ignored) {
            }
        }
        return null;
    }
}
