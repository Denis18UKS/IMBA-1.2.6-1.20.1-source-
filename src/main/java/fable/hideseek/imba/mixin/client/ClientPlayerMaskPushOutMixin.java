package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.client.ClientMaskData;
import fable.hideseek.imba.mask.MaskService;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents vanilla neighbour-block push-out from fighting the mask owner's narrow movement box. */
@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerMaskPushOutMixin {
    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void imba$skipPushOutForMaskedOwner(double x, double z, CallbackInfo ci) {
        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
        if (!ClientMaskData.hasMask(self.getUuid())) {
            return;
        }
        Item item = ClientMaskData.ITEMS.get(self.getUuid());
        if (MaskService.isSpecialPotion(item)) {
            return;
        }
        ci.cancel();
    }
}
