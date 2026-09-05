package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.client.LocalNightClientData;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Per-client visual time override; integrated/dedicated server worlds are never changed. */
@Mixin(World.class)
public abstract class LocalNightWorldMixin {
    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
    private void imba$localTime(CallbackInfoReturnable<Long> cir) {
        if (!((Object) this instanceof ClientWorld)) return;
        cir.setReturnValue(LocalNightClientData.visualTime());
    }
}
