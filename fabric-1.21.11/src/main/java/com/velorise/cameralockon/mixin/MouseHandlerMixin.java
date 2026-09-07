package com.velorise.cameralockon.mixin;

import com.velorise.cameralockon.LockOnController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
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
    @Inject(method = "onButton", at = @At("HEAD"))
    private void cameraLockOn(
            long windowPointer,
            MouseButtonInfo buttonInfo,
            int action,
            CallbackInfo callbackInfo
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (action != GLFW.GLFW_PRESS
                || minecraft.screen != null
                || windowPointer != minecraft.getWindow().handle()) {
            return;
        }

        MouseButtonEvent event = new MouseButtonEvent(0, 0, buttonInfo);
        if (LockOnController.LOCK_ON_KEY.matchesMouse(event)) {
            LockOnController.queueRawLockKeyPress();
        }
    }
}