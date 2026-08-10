package fable.hideseek.imba.mixin;

import net.minecraft.entity.TntEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** TNT may be primed for map effects, but it can never create an explosion. */
@Mixin(TntEntity.class)
public abstract class TntEntityMixin {
    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void imba$disableTntExplosion(CallbackInfo ci) {
        ci.cancel();
    }
}
