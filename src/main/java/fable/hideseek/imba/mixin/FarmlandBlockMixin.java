package fable.hideseek.imba.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Player movement can never turn farmland into dirt on IMBA maps. */
@Mixin(FarmlandBlock.class)
public abstract class FarmlandBlockMixin {

    @Inject(method = "onLandedUpon", at = @At("HEAD"), cancellable = true)
    private void imba$preventPlayerTrampling(
            World world,
            BlockState state,
            BlockPos pos,
            Entity entity,
            float fallDistance,
            CallbackInfo ci) {
        if (entity instanceof PlayerEntity) {
            ci.cancel();
        }
    }
}
