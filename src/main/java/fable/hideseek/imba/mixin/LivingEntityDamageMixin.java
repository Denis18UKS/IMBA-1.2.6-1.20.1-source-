package fable.hideseek.imba.mixin;

import fable.hideseek.imba.game.GameManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Central protection for players and map mobs while a round is active. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void imba$cancelProtectedDamage(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (GameManager.shouldCancelDamage(self, source)) {
            cir.setReturnValue(false);
        }
    }
}
