package fable.hideseek.imba.mixin;

import fable.hideseek.imba.game.GameRoles;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hiders and seekers may not sleep or change their respawn point. */
@Mixin(BedBlock.class)
public abstract class BedBlockMixin {

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void imba$preventParticipantRespawnPoint(
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            Hand hand,
            BlockHitResult hit,
            CallbackInfoReturnable<ActionResult> cir) {
        if (!GameRoles.isParticipant(player)) {
            return;
        }
        if (!world.isClient) {
            player.sendMessage(Text.literal("§cУчастникам нельзя устанавливать точку возрождения"), true);
        }
        cir.setReturnValue(ActionResult.SUCCESS);
    }
}
