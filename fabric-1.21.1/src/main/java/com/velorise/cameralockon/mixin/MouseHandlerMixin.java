package com.velorise.cameralockon.mixin;

import com.velorise.cameralockon.LockOnController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures the configured mouse lock-on key before vanilla's Pick Block
 * key mapping can consume the same middle-mouse press.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onPress", at = @At("HEAD"))
    private void cameraLockOn$captureLockMouseButton(
            long windowPointer,
            int button,
            int action,
            int modifiers,
            CallbackInfo callbackInfo
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (action != GLFW.GLFW_PRESS
                || minecraft.screen != null
                || windowPointer != minecraft.getWindow().getWindow()) {
            return;
        }

        if (LockOnController.LOCK_ON_KEY.matchesMouse(button)) {
            LockOnController.queueRawLockKeyPress();
        }
    }
}
