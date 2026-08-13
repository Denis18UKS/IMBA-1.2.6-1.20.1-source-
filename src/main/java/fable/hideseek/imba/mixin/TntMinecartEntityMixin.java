package fable.hideseek.imba.mixin;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * TNT minecarts use their own explosion methods instead of TntEntity.explode().
 * Consume the minecart when it detonates, but never create an explosion.
 */
@Mixin(TntMinecartEntity.class)
public abstract class TntMinecartEntityMixin {

    @Inject(method = "explode(D)V", at = @At("HEAD"), cancellable = true)
    private void imba$disableTntMinecartExplosion(double power, CallbackInfo ci) {
        ((TntMinecartEntity) (Object) this).discard();
        ci.cancel();
    }

    @Inject(method = "explode(Lnet/minecraft/entity/damage/DamageSource;D)V", at = @At("HEAD"), cancellable = true)
    private void imba$disableTntMinecartDamageExplosion(DamageSource damageSource, double power, CallbackInfo ci) {
        ((TntMinecartEntity) (Object) this).discard();
        ci.cancel();
    }
}
