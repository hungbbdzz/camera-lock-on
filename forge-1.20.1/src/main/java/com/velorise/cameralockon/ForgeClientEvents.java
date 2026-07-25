package com.velorise.cameralockon;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Thin Forge adapters. All gameplay and rendering logic remains loader-independent. */
public final class ForgeClientEvents {
    private ForgeClientEvents() {
    }

    @Mod.EventBusSubscriber(
            modid = CameraLockOn.MODID,
            bus = Mod.EventBusSubscriber.Bus.MOD,
            value = Dist.CLIENT
    )
    public static final class ModBus {
        private ModBus() {
        }

        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
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
                    LockOnController.CLEAR_PIN_KEY,
                    LockOnController.CYCLE_CAMERA_POSITION_KEY,
                    LockOnController.TOGGLE_AUTO_RELEASE_BOW_KEY,
                    LockOnController.TOGGLE_AUTO_RECHARGE_BOW_KEY,
                    LockOnController.CYCLE_PROJECTILE_ASSIST_KEY,
                    LockOnController.CAMERA_X_DECREASE_KEY,
                    LockOnController.CAMERA_X_INCREASE_KEY,
                    LockOnController.CAMERA_Y_DECREASE_KEY,
                    LockOnController.CAMERA_Y_INCREASE_KEY,
                    LockOnController.CAMERA_Z_DECREASE_KEY,
                    LockOnController.CAMERA_Z_INCREASE_KEY
            };
            for (KeyMapping mapping : mappings) {
                event.register(mapping);
            }
        }
    }

    @Mod.EventBusSubscriber(
            modid = CameraLockOn.MODID,
            bus = Mod.EventBusSubscriber.Bus.FORGE,
            value = Dist.CLIENT
    )
    public static final class GameBus {
        private GameBus() {
        }

        @SubscribeEvent
        public static void clientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }

            // Cycle mode consumes Tab before Smart mode's generic switch handler.
            TargetCycleController.clientTick();
            LockOnController.clientTick();
            // Steering is handled once by RenderTickEvent.START so Camera#setup sees
            // the updated rotation in the same frame without a second smoothing step.
            TargetOutlineController.tick();
        }

        /**
         * Forge 1.20.1 builds the active camera before GameRenderer#renderLevel.
         * Run steering once at render START so player yaw/pitch are available to
         * Camera#setup in the same frame, without the double-update oscillation.
         */
        @SubscribeEvent
        public static void renderTick(TickEvent.RenderTickEvent event) {
            if (event.phase != TickEvent.Phase.START) {
                return;
            }

            ThirdPersonCameraController.captureMouseLook();
            LockOnController.updateCameraAngles();
        }

        @SubscribeEvent
        public static void attackInput(InputEvent.InteractionKeyMappingTriggered event) {
            if (!event.isAttack()) {
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.hitResult instanceof EntityHitResult hit
                    && hit.getEntity() instanceof LivingEntity living) {
                LockOnController.onEntityAttacked(living);
            }
        }


        /**
         * Apply the already-smoothed third-person rotation at Forge's final
         * camera-angle stage. CameraPositionMixin only computes the offset and
         * desired visual angles, so rotation is written exactly once.
         */
        @SubscribeEvent
        public static void computeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
            if (!ThirdPersonCameraController.shouldOverrideCameraRotation()) {
                return;
            }
            event.setYaw(ThirdPersonCameraController.getVisualYaw());
            event.setPitch(ThirdPersonCameraController.getVisualPitch());
        }

        @SubscribeEvent
        public static void renderLevel(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                return;
            }

            PoseStack poseStack = event.getPoseStack();
            ThirdPersonAimRayRenderer.render(poseStack, event.getCamera(), event.getPartialTick());
            ProjectileTrajectoryRenderer.render(poseStack, event.getCamera(), event.getPartialTick());
            LockOnController.renderReticle(
                    poseStack,
                    event.getCamera(),
                    event.getPartialTick()
            );
        }

        @SubscribeEvent
        public static void renderHud(RenderGuiEvent.Post event) {
            LockOnHudRenderer.render(event.getGuiGraphics());
        }
    }
}
