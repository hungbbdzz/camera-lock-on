package com.velorise.cameralockon.mixin;

import com.velorise.cameralockon.ThirdPersonAimResolver;
import com.velorise.cameralockon.ThirdPersonCameraController;
import net.minecraft.client.Camera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraPositionMixin {
    @Shadow protected abstract void setPosition(double x, double y, double z);
    @Shadow public abstract Vec3 getPosition();

    @Inject(method = "setup", at = @At("RETURN"))
    private void cameraLockOn$applyThirdPersonOffset(
            BlockGetter level,
            Entity entity,
            boolean detached,
            boolean mirrored,
            float partialTick,
            CallbackInfo ci
    ) {
        if (!detached || mirrored || !(entity instanceof LocalPlayer player)) {
            return;
        }

        Vec3 adjusted = ThirdPersonCameraController.apply(
                entity, getPosition(), partialTick);
        setPosition(adjusted.x, adjusted.y, adjusted.z);
        ThirdPersonAimResolver.alignFreeAim((Camera) (Object) this, player);
    }
}
