package com.velorise.cameralockon.mixin;

import com.velorise.cameralockon.TargetOutlineController;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forces the locked target to render with a client-side glowing silhouette
 * using the configured Reticle Color, without modifying synced entity flags.
 */
@Mixin(Entity.class)
public abstract class EntityOutlineMixin {
    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void cameraLockOn$enableLockedTargetOutline(CallbackInfoReturnable<Boolean> cir) {
        if (TargetOutlineController.shouldOutline((Entity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void cameraLockOn$applyLockedTargetOutlineColor(CallbackInfoReturnable<Integer> cir) {
        if (TargetOutlineController.shouldOutline((Entity) (Object) this)) {
            cir.setReturnValue(TargetOutlineController.getOutlineColor());
        }
    }
}
