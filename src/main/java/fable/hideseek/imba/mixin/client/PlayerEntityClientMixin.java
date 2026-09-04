package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.client.ClientMaskData;
import fable.hideseek.imba.client.MaskHitboxClientData;
import fable.hideseek.imba.mask.MaskHitbox;
import fable.hideseek.imba.mask.MaskType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityClientMixin {
    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void clientDimensions(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        PlayerEntity localPlayer = MinecraftClient.getInstance().player;
        if (self == localPlayer) return;

        MaskType type = ClientMaskData.TYPES.get(self.getUuid());
        if (type == null) return;
        Item item = ClientMaskData.ITEMS.get(self.getUuid());
        if(type==MaskType.DOOR){cir.setReturnValue(MaskHitbox.getDimensions(type, item));return;}
        var block=ClientMaskData.BLOCKS.get(self.getUuid());
        if(block!=null){
            var entry=MaskHitboxClientData.get(Registries.BLOCK.getId(block).toString());
            if(entry!=null){
                var b=entry.bounds();
                cir.setReturnValue(EntityDimensions.fixed(Math.max(.05F,b.width()),Math.max(.05F,b.height())));
                return;
            }
        }
        cir.setReturnValue(MaskHitbox.getDimensions(type,item));
    }

    @Inject(method="getActiveEyeHeight",at=@At("HEAD"),cancellable=true)
    private void clientEyeHeight(EntityPose pose,EntityDimensions dimensions,CallbackInfoReturnable<Float> cir){PlayerEntity self=(PlayerEntity)(Object)this;MaskType type=ClientMaskData.TYPES.get(self.getUuid());if(type==null)return;Item item=ClientMaskData.ITEMS.get(self.getUuid());var block=ClientMaskData.BLOCKS.get(self.getUuid());if(block!=null){var entry=MaskHitboxClientData.get(Registries.BLOCK.getId(block).toString());if(entry!=null){float h=Math.max(.05F,entry.bounds().height());cir.setReturnValue(Math.min(h*.85F,Math.max(.05F,h-.05F)));return;}}cir.setReturnValue(MaskHitbox.getEyeHeight(type,item));}
}
