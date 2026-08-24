package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.client.ClientMaskData;
import fable.hideseek.imba.config.MaskHitboxConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Client counterpart so ray-picking and local collision use the same edited hitbox. */
@Mixin(Entity.class)
public abstract class ClientConfiguredMaskBoundingBoxMixin {
    @Inject(method = "calculateBoundingBox", at = @At("HEAD"), cancellable = true)
    private void imba$configuredMaskBox(CallbackInfoReturnable<Box> cir) {
        Entity self = (Entity) (Object) this;
        if (!self.getWorld().isClient || !(self instanceof PlayerEntity player)
                || !ClientMaskData.hasMask(player.getUuid()) || !ClientMaskData.isStatue(player.getUuid())) return;

        var block = ClientMaskData.BLOCKS.get(player.getUuid());
        if (block == null) return;
        float rotation = ClientMaskData.ROTATIONS.getOrDefault(player.getUuid(), 0.0F);
        Box box = MaskHitboxConfig.worldBox(block, rotation, player.getX(), player.getY(), player.getZ());
        if (box != null) cir.setReturnValue(box);
    }
}
