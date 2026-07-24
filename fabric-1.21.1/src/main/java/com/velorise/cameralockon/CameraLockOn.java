package com.velorise.cameralockon;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;

/** Fabric 1.21.1 client entry point. */
public final class CameraLockOn implements ClientModInitializer {
    public static final String MODID = "camera_lockon";

    @Override
    public void onInitializeClient() {
        CameraLockOnConfig.initialize(
                PlatformPaths.configDirectory().resolve("camera_lockon-client.json")
        );

        registerKeyMappings();

        // Cycle mode must consume Tab before the normal Smart switch handler.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            TargetCycleController.clientTick();
            LockOnController.clientTick();
        });

        HudRenderCallback.EVENT.register((graphics, tickCounter) ->
                LockOnHudRenderer.render(graphics)
        );

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            PoseStack poseStack = context.matrixStack();
            if (poseStack != null) {
                LockOnController.renderReticle(
                        poseStack,
                        context.camera(),
                        context.tickCounter().getGameTimeDeltaPartialTick(true)
                );
            }
        });

        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level.isClientSide() && entity instanceof LivingEntity living) {
                LockOnController.onEntityAttacked(living);
            }
            return InteractionResult.PASS;
        });
    }

    private static void registerKeyMappings() {
        KeyMapping[] mappings = {
                LockOnController.LOCK_ON_KEY,
                LockOnController.SWITCH_TARGET_KEY,
                LockOnController.SWITCH_TARGET_PREVIOUS_KEY,
                LockOnController.PIN_TARGET_TYPE_KEY,
                LockOnController.OPEN_CONFIG_KEY,
                LockOnController.TOGGLE_AUTO_LOCK_KEY,
                LockOnController.TOGGLE_SMART_LOCK_KEY,
                LockOnController.TOGGLE_HOSTILE_ONLY_KEY,
                LockOnController.TOGGLE_HUD_KEY,
                LockOnController.CYCLE_TARGET_PRIORITY_KEY,
                LockOnController.CYCLE_SWITCH_MODE_KEY,
                LockOnController.CLEAR_PIN_KEY
        };
        for (KeyMapping mapping : mappings) {
            KeyBindingHelper.registerKeyBinding(mapping);
        }
    }
}
