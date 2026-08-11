package fable.hideseek.imba.mask;

import fable.hideseek.imba.ImbaMod;
import fable.hideseek.imba.net.MaskNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EmptyBlockView;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MaskService {

    private static final int RESET_RECOVERY_PASSES = 2;
    private static final Map<UUID, Integer> RESET_RECOVERY = new HashMap<>();

    private MaskService() {
    }

    public static void applyBlockMask(ServerPlayerEntity player, Block block) {
        applyMask(player, resolveBlockType(block), block, null);
    }

    public static void applyItemMask(ServerPlayerEntity player, Item item) {
        applyMask(player, resolveItemType(item), null, item);
    }

    public static void resetMask(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();

        /*
         * Remove mask state first. From this point getDimensions(STANDING)
         * returns Minecraft's real player dimensions rather than MaskHitbox.
         */
        MaskState.disableStatue(uuid);
        MaskState.reset(uuid);

        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        player.removeStatusEffect(StatusEffects.INVISIBILITY);
        player.setNoGravity(false);
        player.setVelocity(Vec3d.ZERO);
        player.fallDistance = 0.0f;

        /*
         * A manual Shift press fixed the bug because it performs a real pose
         * transition and causes Minecraft to rebuild its cached collision box.
         * Reproduce that transition and explicitly reinstall the vanilla
         * STANDING bounding box. Dimensions are obtained from Minecraft itself;
         * no 0.6x1.8 values are hard-coded here.
         */
        MaskResetGeometry.forceStanding(player);

        /*
         * Keep the same world position and force geometry again after the
         * position refresh. Some ServerPlayer movement state is refreshed by
         * the teleport path, so doing both passes in this order prevents the
         * old mask box from surviving until the next manual crouch.
         */
        player.requestTeleport(x, y, z);
        MaskResetGeometry.forceStanding(player);

        /*
         * Also repeat the physical-box restore on the next couple of server
         * ticks. This protects against a later vanilla/network update in the
         * reset tick restoring cached pre-reset dimensions.
         */
        RESET_RECOVERY.put(uuid, RESET_RECOVERY_PASSES);

        MaskNetworking.sendMaskReset(player);
        MaskNetworking.sendStatueSync(player, false);
    }

    /** Called from PlayerEntityMixin on the logical server. */
    public static void tickResetRecovery(PlayerEntity player) {
        if (player == null || player.getWorld().isClient) {
            return;
        }

        UUID uuid = player.getUuid();
        Integer remaining = RESET_RECOVERY.get(uuid);
        if (remaining == null) {
            return;
        }

        // If another mask was equipped immediately, never overwrite its box.
        if (MaskState.hasMask(uuid)) {
            RESET_RECOVERY.remove(uuid);
            return;
        }

        MaskResetGeometry.forceStanding(player);
        if (remaining <= 1) {
            RESET_RECOVERY.remove(uuid);
        } else {
            RESET_RECOVERY.put(uuid, remaining - 1);
        }
    }

    private static void applyMask(ServerPlayerEntity player, MaskType type, Block block, Item item) {
        UUID uuid = player.getUuid();
        RESET_RECOVERY.remove(uuid);

        MaskState state = MaskState.get(uuid);

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

        MaskState.disableStatue(uuid);

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
