package fable.hideseek.imba.client;

import fable.hideseek.imba.mask.MaskType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EmptyBlockView;

public final class ClientPlayerLadderHelper {
    private static final Identifier IMBA_LADDER_ID = new Identifier("imba", "ladder");

    private ClientPlayerLadderHelper() {
    }

    public static boolean isTouchingPlayerLadder(LivingEntity entity) {
        return findTouching(entity) != null;
    }

    public static boolean isMovingTowardPlayerLadder(LivingEntity entity) {
        AbstractClientPlayerEntity ladder = findTouching(entity);
        if (ladder == null) {
            return false;
        }
        Vec3d velocity = entity.getVelocity();
        double dx = ladder.getX() - entity.getX();
        double dz = ladder.getZ() - entity.getZ();
        return velocity.x * dx + velocity.z * dz > 1.0E-4D;
    }

    private static AbstractClientPlayerEntity findTouching(LivingEntity entity) {
        if (entity.getWorld() == null) {
            return null;
        }
        Box entityBox = entity.getBoundingBox();
        Box search = entityBox.expand(0.20D, 0.20D, 0.20D);
        for (AbstractClientPlayerEntity player : entity.getWorld().getEntitiesByClass(
                AbstractClientPlayerEntity.class, search,
                other -> other != entity && other.isAlive() && isPlayerLadder(other))) {
            if (entityBox.intersects(ladderPlane(player).expand(0.075D, 0.04D, 0.075D))) {
                return player;
            }
        }
        return null;
    }

    private static Box ladderPlane(AbstractClientPlayerEntity player) {
        float rotation = ClientMaskData.ROTATIONS.getOrDefault(player.getUuid(), 0.0F);
        Direction facing = Direction.fromHorizontal(Math.floorMod(Math.round(rotation / 90.0F), 4));
        if (facing == null) {
            facing = Direction.NORTH;
        }
        BlockState ladderState = Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, facing);
        Box local = ladderState.getCollisionShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN).getBoundingBox();
        Vec3d anchor = ClientMaskData.getStatueAnchor(player.getUuid());
        double x = anchor == null ? player.getX() : anchor.x;
        double y = anchor == null ? player.getY() : anchor.y;
        double z = anchor == null ? player.getZ() : anchor.z;
        return local.offset(x - 0.5D, y, z - 0.5D);
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
        return block == Blocks.LADDER
                || block != null && IMBA_LADDER_ID.equals(Registries.BLOCK.getId(block));
    }
}
