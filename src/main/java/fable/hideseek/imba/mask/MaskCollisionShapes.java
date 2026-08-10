package fable.hideseek.imba.mask;

import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.EmptyBlockView;

import java.util.List;

/** Shared server/client collision geometry for masks which behave as solid blocks. */
public final class MaskCollisionShapes {
    private MaskCollisionShapes() {
    }

    public static List<Box> create(MaskState state) {
        if (state == null) {
            return List.of();
        }
        return create(state.type, state.block, state.rotation, state.doorOpen,
                state.anchorX, state.anchorY, state.anchorZ);
    }

    public static List<Box> create(MaskType type, net.minecraft.block.Block block,
            float rotation, boolean doorOpen, double x, double y, double z) {
        if (type == MaskType.DOOR && block instanceof DoorBlock door) {
            // Keep the visual/player hitbox unchanged. Only seeker world-collision
            // becomes a full two-block barrier while the player-door is closed,
            // preventing sprint-speed tunnelling into the wall behind it.
            if (!doorOpen) {
                return List.of(new Box(x - 0.5D, y, z - 0.5D,
                        x + 0.5D, y + 2.0D, z + 0.5D));
            }
            Direction facing = Direction.fromHorizontal(Math.floorMod(Math.round(rotation / 90.0F), 4));
            BlockState lower = door.getDefaultState()
                    .with(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                    .with(DoorBlock.FACING, facing)
                    .with(DoorBlock.OPEN, true)
                    .with(DoorBlock.HINGE, DoorHinge.LEFT);
            BlockState upper = lower.with(DoorBlock.HALF, DoubleBlockHalf.UPPER);
            return List.of(toWorldBox(lower, x, y, z), toWorldBox(upper, x, y + 1.0D, z));
        }

        if (!MaskService.hasPhysicalCollision(type, block)) {
            return List.of();
        }
        return List.of(new Box(x - 0.5D, y, z - 0.5D, x + 0.5D, y + 1.0D, z + 0.5D));
    }

    private static Box toWorldBox(BlockState state, double x, double y, double z) {
        Box local = state.getCollisionShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN).getBoundingBox();
        return local.offset(x - 0.5D, y, z - 0.5D);
    }
}
