package com.velorise.cameralockon.mixin;

import com.velorise.cameralockon.LockOnController;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the lock camera immediately before the world camera is prepared. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void cameraLockOn$updateCamera(DeltaTracker deltaTracker, CallbackInfo callbackInfo) {
        LockOnController.updateCameraAngles();
    }
}
