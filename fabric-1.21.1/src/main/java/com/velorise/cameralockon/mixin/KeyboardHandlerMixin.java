package com.velorise.cameralockon.mixin;

import com.velorise.cameralockon.LockOnController;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures keyboard presses such as Tab (SWITCH_TARGET_KEY) before vanilla's
 * KeyMapping.MAP lookup drops duplicate bindings (e.g. key.playerlist).
 */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void cameraLockOn$onKeyPress(
            long windowPointer,
            int key,
            int scanCode,
            int action,
            int modifiers,
            CallbackInfo ci
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (action != GLFW.GLFW_PRESS
                || minecraft.screen != null
                || windowPointer != minecraft.getWindow().getWindow()) {
            return;
        }

        if (LockOnController.SWITCH_TARGET_KEY.matches(key, scanCode)) {
            LockOnController.queueSwitchTargetKeyPress();
        }
        if (LockOnController.SWITCH_TARGET_PREVIOUS_KEY.matches(key, scanCode)) {
            LockOnController.queueSwitchTargetPreviousKeyPress();
        }
        if (LockOnController.LOCK_ON_KEY.matches(key, scanCode)) {
            LockOnController.queueRawLockKeyPress();
        }
    }
}

