package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.client.ClientMaskData;
import fable.hideseek.imba.mask.MaskHitbox;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.EntityShapeContext;
import net.minecraft.util.shape.ShapeContext;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DoorBlock.class)
public abstract class ClientDoorCollisionMixin {
    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void imba$allowOneBlockMaskThroughOpenDoorClient(
            BlockState state, BlockView world, BlockPos pos, ShapeContext context,
            CallbackInfoReturnable<VoxelShape> cir) {
        if (!state.get(DoorBlock.OPEN) || !(context instanceof EntityShapeContext entityContext)) {
            return;
        }
        Entity entity = entityContext.getEntity();
        if (!(entity instanceof PlayerEntity player)
                || !ClientMaskData.hasMask(player.getUuid())) {
            return;
        }
        var dimensions = MaskHitbox.getDimensions(
                ClientMaskData.TYPES.get(player.getUuid()),
                ClientMaskData.ITEMS.get(player.getUuid()));
        if (dimensions.width >= 0.90F && dimensions.height <= 1.05F) {
            cir.setReturnValue(VoxelShapes.empty());
        }
    }
}
