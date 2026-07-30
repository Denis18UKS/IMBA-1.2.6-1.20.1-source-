package fable.hideseek.imba.client;

import fable.hideseek.imba.mask.MaskType;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

/** Client prediction counterpart for climbing a player disguised as a ladder. */
public final class ClientPlayerLadderHelper {
    private static final Identifier IMBA_LADDER_ID = new Identifier("imba", "ladder");

    private ClientPlayerLadderHelper() {
    }

    public static boolean isNearPlayerLadder(LivingEntity entity) {
        if (entity.getWorld() == null) {
            return false;
        }
        Box entityBox = entity.getBoundingBox();
        Box search = entityBox.expand(0.72D, 0.8D, 0.72D);
        for (AbstractClientPlayerEntity player : entity.getWorld().getEntitiesByClass(
                AbstractClientPlayerEntity.class,
                search,
                other -> other != entity && other.isAlive() && isPlayerLadder(other))) {
            Box ladder = player.getBoundingBox().expand(0.30D, 0.40D, 0.30D);
            boolean xzClose = entityBox.maxX > ladder.minX && entityBox.minX < ladder.maxX
                    && entityBox.maxZ > ladder.minZ && entityBox.minZ < ladder.maxZ;
            boolean yOverlap = entityBox.maxY > ladder.minY - 0.25D
                    && entityBox.minY < ladder.maxY + 0.65D;
            if (xzClose && yOverlap) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPlayerLadder(AbstractClientPlayerEntity player) {
        if (!ClientMaskData.isStatue(player.getUuid())) {
            return false;
        }
        MaskType type = ClientMaskData.TYPES.get(player.getUuid());
        if (type == MaskType.LADDER_REVERSED) {
            return true;
        }
        var block = ClientMaskData.BLOCKS.get(player.getUuid());
        if (block == Blocks.LADDER) {
            return true;
        }
        return block != null && IMBA_LADDER_ID.equals(Registries.BLOCK.getId(block));
    }
}
