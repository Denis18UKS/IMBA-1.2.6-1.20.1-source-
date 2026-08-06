package fable.hideseek.imba.mixin;

import fable.hideseek.imba.config.ItemRules;
import fable.hideseek.imba.mask.MaskState;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    
    @Shadow public ServerPlayerEntity player;
    
    @Inject(method = "onPlayerAction", at = @At("HEAD"), cancellable = true)
    private void preventDropAndSwap(PlayerActionC2SPacket packet, CallbackInfo ci) {
        if (player == null || player.isCreative()) return;
        
        PlayerActionC2SPacket.Action action = packet.getAction();
        
        // Q - выброс одного предмета
        if (action == PlayerActionC2SPacket.Action.DROP_ITEM) {
            ItemStack mainHand = player.getMainHandStack();
            if (ItemRules.isRestricted(mainHand)) {
                ci.cancel();
                return;
            }
        }
        
        // Ctrl+Q - выброс всей стопки
        if (action == PlayerActionC2SPacket.Action.DROP_ALL_ITEMS) {
            ItemStack mainHand = player.getMainHandStack();
            if (ItemRules.isRestricted(mainHand)) {
                ci.cancel();
                return;
            }
        }
        
        // F - своп в оффхенд
        if (action == PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
            ItemStack mainHand = player.getMainHandStack();
            ItemStack offHand = player.getOffHandStack();
            
            if (ItemRules.isRestricted(mainHand) || ItemRules.isRestricted(offHand)) {
                ci.cancel();
                return;
            }
        }
    }

    @Inject(method = "onPlayerMove", at = @At("HEAD"), cancellable = true)
    private void keepStatueAtAnchor(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        if (player == null || !MaskState.isStatue(player.getUuid()) || !packet.changesPosition()) {
            return;
        }

        MaskState state = MaskState.get(player.getUuid());
        if (packet.changesLook()) {
            player.setYaw(packet.getYaw(player.getYaw()));
            player.setPitch(packet.getPitch(player.getPitch()));
        }
        if (player.getPos().squaredDistanceTo(state.anchorX, state.anchorY, state.anchorZ) > 0.0001D) {
            player.setPosition(state.anchorX, state.anchorY, state.anchorZ);
        }
        player.setVelocity(0.0D, 0.0D, 0.0D);
        player.setNoGravity(true);
        player.fallDistance = 0.0F;
        ci.cancel();
    }
}
