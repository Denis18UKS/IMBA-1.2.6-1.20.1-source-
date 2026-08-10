package fable.hideseek.imba.mixin;

import fable.hideseek.imba.mask.MaskHitbox;
import fable.hideseek.imba.mask.MaskState;
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
public abstract class DoorCollisionMixin {
    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void imba$allowOneBlockMaskThroughOpenDoor(
            BlockState state, BlockView world, BlockPos pos, ShapeContext context,
            CallbackInfoReturnable<VoxelShape> cir) {
        if (!state.get(DoorBlock.OPEN) || !(context instanceof EntityShapeContext entityContext)) {
            return;
        }
        Entity entity = entityContext.getEntity();
        if (!(entity instanceof PlayerEntity player) || player.getWorld().isClient
                || !MaskState.hasMask(player.getUuid())) {
            return;
        }
        MaskState mask = MaskState.get(player.getUuid());
        var dimensions = MaskHitbox.getDimensions(mask.type, mask.item);
        if (dimensions.width >= 0.90F && dimensions.height <= 1.05F) {
            cir.setReturnValue(VoxelShapes.empty());
        }
    }
}
