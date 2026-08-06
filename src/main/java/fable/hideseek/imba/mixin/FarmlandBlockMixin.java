package fable.hideseek.imba.mixin;

import fable.hideseek.imba.game.GameManager;
import fable.hideseek.imba.game.GameRoles;
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

/** Participants cannot accidentally turn farmland into dirt during a game. */
@Mixin(FarmlandBlock.class)
public abstract class FarmlandBlockMixin {

    @Inject(method = "onLandedUpon", at = @At("HEAD"), cancellable = true)
    private void imba$preventParticipantTrampling(
            World world,
            BlockState state,
            BlockPos pos,
            Entity entity,
            float fallDistance,
            CallbackInfo ci) {
        if (GameManager.isGameActive()
                && entity instanceof PlayerEntity player
                && GameRoles.isParticipant(player)) {
            ci.cancel();
        }
    }
}
