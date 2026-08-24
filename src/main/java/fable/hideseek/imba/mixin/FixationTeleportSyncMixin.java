package fable.hideseek.imba.mixin;

import fable.hideseek.imba.item.HideButtonHandler;
import fable.hideseek.imba.mask.MaskState;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes fixation position authoritative on the wire before STATUE_SYNC is sent.
 * HideButtonHandler already calculates the final anchor and updates server state;
 * this adds exactly one vanilla teleport packet at that point so the local client
 * reaches the same coordinates before the statue lock packet is processed.
 */
@Mixin(HideButtonHandler.class)
public abstract class FixationTeleportSyncMixin {
    @Inject(
            method = "handle",
            at = @At(
                    value = "INVOKE",
                    target = "Lfable/hideseek/imba/mask/MaskState;enableStatue(Ljava/util/UUID;DDD)V",
                    shift = At.Shift.AFTER),
            remap = false)
    private static void imba$syncFinalAnchorBeforeStatuePacket(ServerPlayerEntity player, CallbackInfo ci) {
        if (player == null) return;
        MaskState state = MaskState.get(player.getUuid());
        player.requestTeleport(state.anchorX, state.anchorY, state.anchorZ);
    }
}
