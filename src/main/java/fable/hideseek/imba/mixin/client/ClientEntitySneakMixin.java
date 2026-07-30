package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.client.ClientMaskCollision;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class ClientEntitySneakMixin {
    @Inject(method = "adjustMovementForSneaking", at = @At("HEAD"), cancellable = true)
    private void imba$walkOnMaskWhileSneaking(Vec3d movement, net.minecraft.entity.MovementType type,
            CallbackInfoReturnable<Vec3d> cir) {
        Entity self = (Entity) (Object) this;
        if (self.getWorld().isClient && self.isSneaking()
                && ClientMaskCollision.canKeepSneakMovement(movement)) {
            cir.setReturnValue(movement);
        }
    }
}
