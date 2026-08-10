package fable.hideseek.imba.util;

import fable.hideseek.imba.mask.MaskState;
import fable.hideseek.imba.mask.MaskType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EmptyBlockView;

import java.util.List;

public final class PlayerLadderHelper {
    private static final Identifier IMBA_LADDER_ID = new Identifier("imba", "ladder");

    private PlayerLadderHelper() {
    }

    public static boolean isTouchingPlayerLadder(LivingEntity entity) {
        return findTouching(entity) != null;
    }

    public static boolean isMovingTowardPlayerLadder(LivingEntity entity) {
        PlayerEntity ladder = findTouching(entity);
        if (ladder == null) {
            return false;
        }
        Vec3d velocity = entity.getVelocity();
        double dx = ladder.getX() - entity.getX();
        double dz = ladder.getZ() - entity.getZ();
        return velocity.x * dx + velocity.z * dz > 1.0E-4D;
    }

    private static PlayerEntity findTouching(LivingEntity entity) {
        Box entityBox = entity.getBoundingBox();
        Box searchBox = entityBox.expand(0.20D, 0.20D, 0.20D);
        List<PlayerEntity> ladderPlayers = entity.getWorld().getEntitiesByClass(
                PlayerEntity.class, searchBox,
                other -> other.isAlive() && other != entity && isPlayerLadder(other));

        for (PlayerEntity ladderPlayer : ladderPlayers) {
            MaskState state = MaskState.get(ladderPlayer.getUuid());
            if (entityBox.intersects(ladderPlane(state).expand(0.075D, 0.04D, 0.075D))) {
                return ladderPlayer;
            }
        }
        return null;
    }

    private static Box ladderPlane(MaskState state) {
        Direction facing = Direction.fromHorizontal(Math.floorMod(Math.round(state.rotation / 90.0F), 4));
        if (facing == null) {
            facing = Direction.NORTH;
        }
        BlockState ladderState = Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, facing);
        Box local = ladderState.getCollisionShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN).getBoundingBox();
        return local.offset(state.anchorX - 0.5D, state.anchorY, state.anchorZ - 0.5D);
    }

    public static boolean isPlayerLadder(PlayerEntity player) {
        if (!MaskState.isStatue(player.getUuid())) {
            return false;
        }
        MaskState state = MaskState.get(player.getUuid());
        if (state.type == MaskType.LADDER_REVERSED) {
            return true;
        }
        if (state.type != MaskType.BLOCK || state.block == null) {
            return false;
        }
        return state.block == Blocks.LADDER
                || IMBA_LADDER_ID.equals(Registries.BLOCK.getId(state.block));
    }
}
