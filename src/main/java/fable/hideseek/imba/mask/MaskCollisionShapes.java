package fable.hideseek.imba.mask;

import fable.hideseek.imba.config.MaskHitboxConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EmptyBlockView;

import java.util.ArrayList;
import java.util.List;

/** Shared server/client collision geometry for static disguises. */
public final class MaskCollisionShapes {
    private MaskCollisionShapes() {}

    public static List<Box> create(MaskState state) {
        return state == null ? List.of() : create(state.type, state.block, state.rotation,
                state.doorOpen, state.anchorX, state.anchorY, state.anchorZ);
    }

    public static List<Box> create(MaskType type, net.minecraft.block.Block block,
            float rotation, boolean doorOpen, double x, double y, double z) {
        if (type == MaskType.DOOR && block instanceof DoorBlock door) {
            Direction facing = Direction.fromHorizontal(Math.floorMod(Math.round(rotation / 90.0F), 4));
            BlockState lower = door.getDefaultState()
                    .with(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                    .with(DoorBlock.FACING, facing)
                    .with(DoorBlock.OPEN, doorOpen)
                    .with(DoorBlock.HINGE, DoorHinge.LEFT);
            BlockState upper = lower.with(DoorBlock.HALF, DoubleBlockHalf.UPPER);
            List<Box> result = new ArrayList<>();
            append(result, lower, x, y, z);
            append(result, upper, x, y + 1.0D, z);
            return result;
        }
        if (!MaskService.hasPhysicalCollision(type, block)) return List.of();

        // Use the hitbox editor geometry whenever it applies. This covers
        // NON-FULL blocks, administrator overrides for any block, and built-in
        // special defaults such as the horizontal stonecutter-blade disguise.
        if (block != null) {
            Box configured = MaskHitboxConfig.worldBox(block, rotation, x, y, z);
            if (configured != null) return List.of(configured);
        }

        BlockState source = block.getDefaultState();
        List<Box> result = new ArrayList<>();
        append(result, source, x, y, z);
        return result;
    }

    public static Vec3d nearestHorizontalSeparation(Box mover, Box obstacle) {
        if (mover == null || obstacle == null || !mover.intersects(obstacle)) return Vec3d.ZERO;
        double toLeft = mover.maxX - obstacle.minX;
        double toRight = obstacle.maxX - mover.minX;
        double toNorth = mover.maxZ - obstacle.minZ;
        double toSouth = obstacle.maxZ - mover.minZ;

        double best = Double.POSITIVE_INFINITY;
        double dx = 0.0D, dz = 0.0D;
        if (toLeft > 0.0D && toLeft < best) { best = toLeft; dx = -toLeft; dz = 0.0D; }
        if (toRight > 0.0D && toRight < best) { best = toRight; dx = toRight; dz = 0.0D; }
        if (toNorth > 0.0D && toNorth < best) { best = toNorth; dx = 0.0D; dz = -toNorth; }
        if (toSouth > 0.0D && toSouth < best) { dx = 0.0D; dz = toSouth; }
        return new Vec3d(dx, 0.0D, dz);
    }

    private static void append(List<Box> target, BlockState state, double x, double y, double z) {
        for (Box local : state.getCollisionShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN).getBoundingBoxes()) {
            target.add(local.offset(x - .5D, y, z - .5D));
        }
    }
}
