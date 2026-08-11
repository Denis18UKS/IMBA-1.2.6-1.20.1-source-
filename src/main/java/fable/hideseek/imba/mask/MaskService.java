package fable.hideseek.imba.mask;

import fable.hideseek.imba.ImbaMod;
import fable.hideseek.imba.net.MaskNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EmptyBlockView;

public final class MaskService {

    private MaskService() {
    }

    public static void applyBlockMask(ServerPlayerEntity player, Block block) {
        applyMask(player, resolveBlockType(block), block, null);
    }

    public static void applyItemMask(ServerPlayerEntity player, Item item) {
        applyMask(player, resolveItemType(item), null, item);
    }

    public static void resetMask(ServerPlayerEntity player) {
        /*
         * Reset the mask state BEFORE recalculating dimensions so the
         * PlayerEntityMixin stops returning mask dimensions immediately.
         */
        MaskState.disableStatue(player.getUuid());
        MaskState.reset(player.getUuid());

        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        player.removeStatusEffect(StatusEffects.INVISIBILITY);
        player.setNoGravity(false);
        player.setVelocity(Vec3d.ZERO);
        player.fallDistance = 0.0f;

        /*
         * setSneaking(false) only clears the sneaking input flag. Minecraft
         * keeps the actual tracked EntityPose separately, and that stale pose
         * can keep the physical bounding box in the mask/crouching state until
         * the player presses Shift once. Force the vanilla standing state now
         * and recalculate the real player bounding box immediately.
         */
        player.setSneaking(false);
        player.setSwimming(false);
        player.setPose(EntityPose.STANDING);
        player.calculateDimensions();

        /*
         * Keep the same world position, but make the server send a position
         * refresh together with the pose/dimension change. Recalculate once
         * more after the teleport so movement collision uses the new box in
         * the exact final position during this same tick.
         */
        player.requestTeleport(x, y, z);
        player.setPose(EntityPose.STANDING);
        player.calculateDimensions();

        MaskNetworking.sendMaskReset(player);
        MaskNetworking.sendStatueSync(player, false);
    }

    private static void applyMask(ServerPlayerEntity player, MaskType type, Block block, Item item) {
        MaskState state = MaskState.get(player.getUuid());

        state.type = type;
        state.block = block;
        state.item = item;

        state.rotation = 0.0f;
        state.rotationX = 0.0f;
        state.rotationZ = 0.0f;

        state.doorOpen = false;
        state.buttonPressed = false;
        state.buttonTicks = 0;

        state.sculkStepCount = 0;
        state.wallClimbing = true;
        state.wallAttached = false;
        state.attachedToFrame = false;
        state.attachmentFacing = Direction.NORTH;
        state.frameRotationStep = 0;

        MaskState.disableStatue(player.getUuid());

        snapToSingleBlock(player);
        player.calculateDimensions();

        MaskNetworking.sendMaskUpdate(player, type, block, item);
        MaskNetworking.sendStatueSync(player, false);
    }

    private static void snapToSingleBlock(ServerPlayerEntity player) {
        double x = Math.floor(player.getX()) + 0.5D;
        double y = Math.floor(player.getY());
        double z = Math.floor(player.getZ()) + 0.5D;
        player.requestTeleport(x, y, z);
        player.setVelocity(Vec3d.ZERO);
        player.fallDistance = 0.0F;
    }

    public static MaskType resolveBlockType(Block block) {
        if (block instanceof DoorBlock) {
            return MaskType.DOOR;
        }
        if (block == Blocks.NETHER_PORTAL) {
            return MaskType.PORTAL;
        }
        if (block instanceof LadderBlock) {
            return MaskType.LADDER_REVERSED;
        }
        if (block instanceof ButtonBlock) {
            return MaskType.BUTTON;
        }
        if (block == Blocks.SCULK_VEIN) {
            return MaskType.SCULK_VEIN;
        }
        if (block == Blocks.LANTERN || block == ImbaMod.HANGING_LANTERN) {
            return MaskType.LANTERN;
        }
        if (block == Blocks.ATTACHED_PUMPKIN_STEM) {
            return MaskType.STEM;
        }

        return MaskType.BLOCK;
    }

    public static MaskType resolveItemType(Item item) {
        if (item == Items.APPLE) {
            return MaskType.WALL_CLIMB;
        }
        return MaskType.ITEM;
    }

    public static boolean isSpecialPotion(Item item) {
        return item == ImbaMod.POTION_2D;
    }

    public static boolean hasPhysicalCollision(MaskState state) {
        return state != null && hasPhysicalCollision(state.type, state.block);
    }

    public static boolean hasPhysicalCollision(MaskType type, Block block) {
        // Player doors are handled by EntityPlayerDoorCollisionMixin and its
        // client counterpart. Returning false here prevents the old seeker-only
        // manual push solver from fighting vanilla movement collision.
        if (type == MaskType.DOOR) {
            return false;
        }
        if (type != MaskType.BLOCK || block == null) {
            return false;
        }
        if (block == ImbaMod.GRASS || block == ImbaMod.GLOWBERRIES
                || block == ImbaMod.STONRCUTTER_BLOCK || block == ImbaMod.STONRCUTTER_LEZVIE) {
            return false;
        }
        BlockState state = block.getDefaultState();
        return Block.isShapeFullCube(state.getCollisionShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN));
    }

    public static boolean supportsWallClimbing(MaskState state) {
        if (state == null) {
            return false;
        }
        return state.type == MaskType.WALL_CLIMB
                || state.block == ImbaMod.GLOWBERRIES;
    }
}
