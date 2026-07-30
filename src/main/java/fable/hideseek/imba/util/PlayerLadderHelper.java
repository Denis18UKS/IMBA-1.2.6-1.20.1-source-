package fable.hideseek.imba.util;

import fable.hideseek.imba.mask.MaskState;
import fable.hideseek.imba.mask.MaskType;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

import java.util.List;

public final class PlayerLadderHelper {

    private static final Identifier IMBA_LADDER_ID = new Identifier("imba", "ladder");

    private PlayerLadderHelper() {
    }

    public static boolean isNearPlayerLadder(LivingEntity entity) {
        Box entityBox = entity.getBoundingBox();
        Box searchBox = entityBox.expand(0.55, 0.8, 0.55);

        List<PlayerEntity> ladderPlayers = entity.getWorld().getEntitiesByClass(
                PlayerEntity.class,
                searchBox,
                other -> other.isAlive() && other != entity && isPlayerLadder(other));

        for (PlayerEntity ladderPlayer : ladderPlayers) {
            Box ladderBox = ladderPlayer.getBoundingBox().expand(0.22, 0.35, 0.22);

            if (entityBox.intersects(ladderBox)) {
                return true;
            }

            boolean xzClose = Math.abs(entity.getX() - ladderPlayer.getX()) <= 0.72
                    && Math.abs(entity.getZ() - ladderPlayer.getZ()) <= 0.72;

            boolean yOverlap = entityBox.maxY > ladderBox.minY - 0.20
                    && entityBox.minY < ladderBox.maxY + 0.55;

            if (xzClose && yOverlap) {
                return true;
            }
        }

        return false;
    }

    public static boolean isPlayerLadder(PlayerEntity player) {
        if (!MaskState.isStatue(player.getUuid())) {
            return false;
        }

        var state = MaskState.get(player.getUuid());

        if (state.type == MaskType.LADDER_REVERSED) {
            return true;
        }

        if (state.type != MaskType.BLOCK || state.block == null) {
            return false;
        }

        if (state.block == Blocks.LADDER) {
            return true;
        }

        Identifier blockId = Registries.BLOCK.getId(state.block);
        return IMBA_LADDER_ID.equals(blockId);
    }
}