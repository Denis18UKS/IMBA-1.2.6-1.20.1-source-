package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.client.ClientMaskData;
import fable.hideseek.imba.mask.MaskHitbox;
import fable.hideseek.imba.mask.MaskType;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityClientMixin {

    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void clientDimensions(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        MaskType type = ClientMaskData.TYPES.get(self.getUuid());
        if (type == null)
            return;

        Item item = ClientMaskData.ITEMS.get(self.getUuid());
        cir.setReturnValue(MaskHitbox.getDimensions(type, item));
    }

    @Inject(method = "getActiveEyeHeight", at = @At("HEAD"), cancellable = true)
    private void clientEyeHeight(EntityPose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        MaskType type = ClientMaskData.TYPES.get(self.getUuid());
        if (type == null)
            return;

        Item item = ClientMaskData.ITEMS.get(self.getUuid());
        cir.setReturnValue(MaskHitbox.getEyeHeight(type, item));
    }
}