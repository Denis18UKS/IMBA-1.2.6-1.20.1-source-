package fable.hideseek.imba.mask;

import fable.hideseek.imba.config.MaskBlockConfig;
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
        if (state == null) return List.of();
        MaskHitboxConfig.Bounds custom = state.block != null && !MaskBlockConfig.isFull(state.block)
                && MaskHitboxConfig.hasCustom(state.block) ? MaskHitboxConfig.boundsFor(state.block) : null;
        return create(state.type, state.block, state.rotation, state.doorOpen,
                state.anchorX, state.anchorY, state.anchorZ, custom);
    }

    public static List<Box> create(MaskType type, net.minecraft.block.Block block,
                                   float rotation, boolean doorOpen, double x, double y, double z) {
        return create(type, block, rotation, doorOpen, x, y, z, null);
    }

    public static List<Box> create(MaskType type, net.minecraft.block.Block block,
                                   float rotation, boolean doorOpen, double x, double y, double z,
                                   MaskHitboxConfig.Bounds override) {
        if (type == MaskType.DOOR && block instanceof DoorBlock door) {
            Direction facing = Direction.fromHorizontal(Math.floorMod(Math.round(rotation / 90.0F), 4));
            BlockState lower = door.getDefaultState().with(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                    .with(DoorBlock.FACING, facing).with(DoorBlock.OPEN, doorOpen).with(DoorBlock.HINGE, DoorHinge.LEFT);
            BlockState upper = lower.with(DoorBlock.HALF, DoubleBlockHalf.UPPER);
            List<Box> result = new ArrayList<>(); append(result, lower, x, y, z); append(result, upper, x, y + 1.0D, z); return result;
        }
        if (!MaskService.hasPhysicalCollision(type, block)) return List.of();
        if (override != null) return List.of(override.worldBox(x, y, z));
        BlockState source = block.getDefaultState(); List<Box> result = new ArrayList<>(); append(result, source, x, y, z); return result;
    }

    public static Vec3d nearestHorizontalSeparation(Box mover, Box obstacle) {
        if (mover == null || obstacle == null || !mover.intersects(obstacle)) return Vec3d.ZERO;
        double toLeft=mover.maxX-obstacle.minX,toRight=obstacle.maxX-mover.minX,toNorth=mover.maxZ-obstacle.minZ,toSouth=obstacle.maxZ-mover.minZ;
        double best=Double.POSITIVE_INFINITY,dx=0,dz=0;if(toLeft>0&&toLeft<best){best=toLeft;dx=-toLeft;dz=0;}if(toRight>0&&toRight<best){best=toRight;dx=toRight;dz=0;}if(toNorth>0&&toNorth<best){best=toNorth;dx=0;dz=-toNorth;}if(toSouth>0&&toSouth<best){dx=0;dz=toSouth;}return new Vec3d(dx,0,dz);
    }

    private static void append(List<Box> target, BlockState state, double x, double y, double z) {
        for (Box local : state.getCollisionShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN).getBoundingBoxes()) target.add(local.offset(x - .5D, y, z - .5D));
    }
}
