package fable.hideseek.imba.mixin;

import fable.hideseek.imba.mask.MaskState;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class StatueTickMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity)(Object)this;

        if (!MaskState.isStatue(player.getUuid())) return;

        player.setVelocity(0, 0, 0);
        player.fallDistance = 0;
        player.setSneaking(false);
    }
}