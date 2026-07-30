package fable.hideseek.imba.mixin;

import fable.hideseek.imba.config.ItemRules;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
    
    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void preventDropSelected(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity)(Object)this;
        if (player.isCreative()) return;
        
        ItemStack selected = player.getInventory().getMainHandStack();
        if (ItemRules.isRestricted(selected)) {
            cir.setReturnValue(false);
        }
    }
}