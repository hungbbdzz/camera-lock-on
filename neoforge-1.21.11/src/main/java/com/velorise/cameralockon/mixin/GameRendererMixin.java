package com.velorise.cameralockon.mixin;

import com.velorise.cameralockon.LockOnController;
import com.velorise.cameralockon.ThirdPersonCameraController;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures free camera mouse input before player aim assistance is applied. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void cameraLockOn$captureThirdPersonLook(
            DeltaTracker deltaTracker,
            CallbackInfo callbackInfo
    ) {
        ThirdPersonCameraController.captureMouseLook();
        LockOnController.updateCameraAngles();
    }
}
