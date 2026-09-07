package com.velorise.cameralockon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/** Automates bow release/recharge and the full crossbow charge/fire cycle while use is held. */
public final class AutoBowReleaseController {
    private static int actionCooldownTicks;

    private AutoBowReleaseController() {}

    public static void tick(Minecraft minecraft, LocalPlayer player) {
        if (actionCooldownTicks > 0) actionCooldownTicks--;
        if (minecraft.gameMode == null || minecraft.screen != null) return;
        if (!isUsePhysicallyHeld(minecraft)) return;

        InteractionHand hand = findRangedHand(player);
        if (hand == null) return;
        ItemStack stack = player.getItemInHand(hand);

        if (stack.getItem() instanceof BowItem) {
            tickBow(minecraft, player, hand, stack);
        } else if (stack.getItem() instanceof CrossbowItem) {
            tickCrossbow(minecraft, player, hand, stack);
        }
    }

    private static void tickBow(Minecraft minecraft, LocalPlayer player,
                                InteractionHand hand, ItemStack stack) {
        // Recharge and release are separate features. Recharge may begin a new
        // draw while the physical Use key remains held, even if auto-release is off.
        if (!player.isUsingItem()) {
            if (CameraLockOnConfig.AUTO_RECHARGE_BOW.get() && actionCooldownTicks == 0) {
                minecraft.gameMode.useItem(player, hand);
                actionCooldownTicks = 2;
            }
            return;
        }
        if (!CameraLockOnConfig.AUTO_RELEASE_BOW.get()) return;
        if (!(player.getUseItem().getItem() instanceof BowItem) || actionCooldownTicks > 0) return;

        float charge = BowItem.getPowerForTime(player.getTicksUsingItem());
        if (charge + 1.0E-4F < CameraLockOnConfig.AUTO_RELEASE_BOW_CHARGE.get()) return;

        // Default behavior: release immediately at the configured charge threshold.
        // Target visibility, hit prediction and path checks must never hold a fully charged bow.
        minecraft.gameMode.releaseUsingItem(player);
        actionCooldownTicks = 4;
    }

    private static void tickCrossbow(Minecraft minecraft, LocalPlayer player,
                                     InteractionHand hand, ItemStack stack) {
        if (!false /* auto crossbow cycle disabled */ || actionCooldownTicks > 0) return;

        if (!CrossbowItem.isCharged(stack)) {
            if (player.isUsingItem()) {
                // Vanilla expects the player to release after charging. Do that automatically
                // while the physical button remains held so the next synthetic use can fire.
                if (player.getUseItemRemainingTicks() <= 3) {
                    minecraft.gameMode.releaseUsingItem(player);
                    actionCooldownTicks = 2;
                }
            } else {
                minecraft.gameMode.useItem(player, hand);
                actionCooldownTicks = 2;
            }
            return;
        }

        if (player.isUsingItem()) {
            minecraft.gameMode.releaseUsingItem(player);
            actionCooldownTicks = 2;
            return;
        }

        LivingEntity target = getAssistedTarget(player);
        if (target != null) {
            Vec3 baseAim = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
            Vec3 aimPoint = ProjectileAimCalculator.resolveForCamera(player, target, baseAim);
            if (!ProjectileAimCalculator.hasClearProjectilePath(player, target, aimPoint)) return;
            if (!isAligned(player, aimPoint)) return;
        }

        // Charged crossbows fire on a fresh use press. Calling useItem here provides that
        // press even though the user has never released the physical right mouse button.
        minecraft.gameMode.useItem(player, hand);
        actionCooldownTicks = 5;
    }

    private static LivingEntity getAssistedTarget(LocalPlayer player) {
        if (!LockOnController.isActive()) return null;
        if (!ProjectileAimCalculator.isCameraAssistActive(player)) return null;
        LivingEntity target = LockOnController.getLockedTarget();
        return target != null && target.isAlive() && !target.isRemoved() ? target : null;
    }

    private static boolean isUsePhysicallyHeld(Minecraft minecraft) {
        if (minecraft.options.keyUse.isDown()) return true;
        long window = minecraft.getWindow().handle();
        return GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
    }

    private static InteractionHand findRangedHand(LocalPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof BowItem || main.getItem() instanceof CrossbowItem) return InteractionHand.MAIN_HAND;
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof BowItem || off.getItem() instanceof CrossbowItem) return InteractionHand.OFF_HAND;
        return null;
    }

    private static boolean isAligned(LocalPlayer player, Vec3 aimPoint) {
        Vec3 toAim = aimPoint.subtract(player.getEyePosition());
        if (toAim.lengthSqr() < 1.0E-8D) return false;
        double dot = Mth.clamp(player.getViewVector(1.0F).normalize().dot(toAim.normalize()), -1.0D, 1.0D);
        double errorDegrees = Math.toDegrees(Math.acos(dot));
        return errorDegrees <= CameraLockOnConfig.AUTO_RELEASE_AIM_TOLERANCE.get();
    }
}
