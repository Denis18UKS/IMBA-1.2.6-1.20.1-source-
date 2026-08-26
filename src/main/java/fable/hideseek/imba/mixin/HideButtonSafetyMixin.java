package fable.hideseek.imba.mixin;

import fable.hideseek.imba.item.HideButtonHandler;
import fable.hideseek.imba.mask.MaskState;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes sure geometry recalculation cannot leave the server-side player a few
 * pixels away from the anchor that is about to be sent to clients.
 */
@Mixin(HideButtonHandler.class)
public abstract class HideButtonSafetyMixin {
    @Inject(
            method = "handle",
            at = @At(
                    value = "INVOKE",
                    target = "Lfable/hideseek/imba/net/MaskNetworking;refresh(Lnet/minecraft/server/network/ServerPlayerEntity;)V",
                    shift = At.Shift.BEFORE))
    private static void imba$finalizeAnchorBeforeSync(ServerPlayerEntity player, CallbackInfo ci) {
        MaskState state = MaskState.get(player.getUuid());
        if (!state.statue) return;

        player.setPosition(state.anchorX, state.anchorY, state.anchorZ);
        player.setVelocity(0.0D, 0.0D, 0.0D);
        player.fallDistance = 0.0F;
    }
}
