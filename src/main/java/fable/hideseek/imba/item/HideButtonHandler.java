package fable.hideseek.imba.item;

import fable.hideseek.imba.config.AttachmentConfig;
import fable.hideseek.imba.config.MaskAutoPositionConfig;
import fable.hideseek.imba.config.MaskBlockConfig;
import fable.hideseek.imba.game.GameMessages;
import fable.hideseek.imba.mask.MaskService;
import fable.hideseek.imba.mask.MaskState;
import fable.hideseek.imba.mask.MaskType;
import fable.hideseek.imba.net.MaskNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

public final class HideButtonHandler {
    private static final int EFFECT_FOREVER = Integer.MAX_VALUE;
    private static final double FULL_BLOCK_EPSILON = 1.0E-7D;
    private static final double SUPPORT_SAMPLE_EPSILON = 0.01D;
    private static final double SUPPORT_XZ_EPSILON = 1.0E-6D;

    private HideButtonHandler() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!(stack.getItem() instanceof HideItem)) {
                return ActionResult.PASS;
            }
            if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
                handle(serverPlayer);
            }
            return ActionResult.SUCCESS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (ModelEquipHandler.isModelItem(stack)) {
                return TypedActionResult.pass(stack);
            }
            if (!(stack.getItem() instanceof HideItem)) {
                return TypedActionResult.pass(stack);
            }
            if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
                handle(serverPlayer);
            }
            return TypedActionResult.success(stack, world.isClient);
        });
    }

    private static void handle(ServerPlayerEntity player) {
        var uuid = player.getUuid();
        MaskState state = MaskState.get(uuid);
        if (state.type == MaskType.NONE) {
            player.sendMessage(Text.literal("§cСначала надень модель"), true);
            return;
        }

        if (state.statue) {
            MaskState.disableStatue(uuid);
            player.setNoGravity(false);
            player.setVelocity(0.0, 0.0, 0.0);
            player.fallDistance = 0.0f;
            player.calculateDimensions();
            player.removeStatusEffect(StatusEffects.INVISIBILITY);
            MaskNetworking.refresh(player);
            GameMessages.send(player, Text.literal("§cВы вышли из маскировки-статуи"));
            return;
        }

        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        /*
         * Capture the real support BEFORE any statue snap. This routine also
         * recovers the support when an older mask snap has already placed the
         * player's feet inside a partial block such as farmland.
         */
        SupportInfo support = findSupport(player);

        state.attachedToFrame = false;
        state.attachmentFacing = Direction.NORTH;

        if (state.type == MaskType.ITEM && state.item != null && MaskService.isSpecialPotion(state.item)) {
            BlockPos brewingSupport = findBrewingStand(player);
            if (brewingSupport != null) {
                Identifier id = Registries.ITEM.getId(state.item);
                Vec3d offset = AttachmentConfig.offsetFor(id);
                x = brewingSupport.getX() + 0.5D + offset.x;
                y = brewingSupport.getY() + 1.0D + offset.y;
                z = brewingSupport.getZ() + 0.5D + offset.z;
            } else {
                x = Math.floor(x) + 0.5D;
                y = Math.floor(y);
                z = Math.floor(z) + 0.5D;
            }
        } else if (state.type == MaskType.BLOCK) {
            if (MaskBlockConfig.isFull(state.block)) {
                x = Math.floor(x) + 0.5D;
                y = snapFullBlockY(y);
                z = Math.floor(z) + 0.5D;
            }
        } else if (shouldCenterOnBlock(state.type)) {
            x = Math.floor(x) + 0.5D;
            y = Math.floor(y);
            z = Math.floor(z) + 0.5D;
        } else if (state.type == MaskType.ITEM || state.type == MaskType.WALL_CLIMB) {
            Attachment attachment = findItemFrameAttachment(player);
            if (attachment != null) {
                x = attachment.pos.x;
                y = attachment.pos.y;
                z = attachment.pos.z;
                state.attachedToFrame = attachment.frame;
                state.attachmentFacing = attachment.facing;
            } else {
                x = Math.floor(x) + 0.5D;
                y = Math.floor(y);
                z = Math.floor(z) + 0.5D;
            }
        }

        /*
         * IMPORTANT: the pair override belongs to ALL block-backed masks, not
         * only MaskType.BLOCK. attached_pumpkin_stem is MaskType.STEM, so the
         * previous implementation never reached the pair config at all.
         *
         * If a pair exists, X/Z remain additive corrections to the normal snap,
         * while Y is rebuilt from the REAL collision-surface top of the support.
         * Example: farmland top is 15/16. Y +1 px therefore becomes exactly the
         * next whole-block level instead of floor(Y) pulling the mask downward.
         */
        if (state.block != null && support != null
                && MaskAutoPositionConfig.hasPair(state.block, support.block)) {
            MaskAutoPositionConfig.Offset autoOffset =
                    MaskAutoPositionConfig.offsetFor(state.block, support.block);
            x += autoOffset.xPixels / 16.0D;
            y = support.surfaceY + autoOffset.yPixels / 16.0D;
            z += autoOffset.zPixels / 16.0D;
        }

        player.requestTeleport(x, y, z);
        player.setVelocity(0.0, 0.0, 0.0);
        player.fallDistance = 0.0f;
        player.setNoGravity(true);
        MaskState.enableStatue(uuid, x, y, z);
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.INVISIBILITY,
                EFFECT_FOREVER,
                0,
                false,
                false,
                false));
        player.calculateDimensions();
        MaskNetworking.refresh(player);
        GameMessages.send(player, Text.literal("§aВы замаскировались"));
    }

    private static SupportInfo findSupport(ServerPlayerEntity player) {
        double x = player.getX();
        double feetY = player.getBoundingBox().minY;
        double z = player.getZ();

        // First inspect the cell containing the feet. This is essential when an
        // earlier floor(Y) snap has already put the player inside farmland/slab.
        BlockPos feetCell = BlockPos.ofFloored(x, feetY + SUPPORT_XZ_EPSILON, z);
        SupportInfo inside = supportAt(player, feetCell, x, z);
        if (inside != null) {
            return inside;
        }

        // Normal case: player is standing exactly on top of a block and the
        // feet cell itself is air, so sample just below the bounding box.
        BlockPos below = BlockPos.ofFloored(x, feetY - SUPPORT_SAMPLE_EPSILON, z);
        SupportInfo belowInfo = supportAt(player, below, x, z);
        if (belowInfo != null) {
            return belowInfo;
        }

        // Small fallback scan for unusually thin/modded supports.
        for (int i = 1; i <= 2; i++) {
            BlockPos candidate = below.down(i);
            SupportInfo info = supportAt(player, candidate, x, z);
            if (info != null) {
                return info;
            }
        }
        return null;
    }

    private static SupportInfo supportAt(ServerPlayerEntity player, BlockPos pos, double worldX, double worldZ) {
        BlockState blockState = player.getWorld().getBlockState(pos);
        if (blockState.isAir()) {
            return null;
        }

        VoxelShape shape = blockState.getCollisionShape(player.getWorld(), pos);
        if (shape.isEmpty()) {
            return null;
        }

        double localX = worldX - pos.getX();
        double localZ = worldZ - pos.getZ();
        double localTop = Double.NEGATIVE_INFINITY;

        for (Box box : shape.getBoundingBoxes()) {
            if (localX >= box.minX - SUPPORT_XZ_EPSILON
                    && localX <= box.maxX + SUPPORT_XZ_EPSILON
                    && localZ >= box.minZ - SUPPORT_XZ_EPSILON
                    && localZ <= box.maxZ + SUPPORT_XZ_EPSILON) {
                localTop = Math.max(localTop, box.maxY);
            }
        }

        if (!Double.isFinite(localTop)) {
            localTop = shape.getMax(Direction.Axis.Y);
        }

        return new SupportInfo(blockState.getBlock(), pos, pos.getY() + localTop);
    }

    private static double snapFullBlockY(double currentY) {
        return Math.ceil(currentY - FULL_BLOCK_EPSILON);
    }

    private record SupportInfo(Block block, BlockPos pos, double surfaceY) {
    }

    private record Attachment(Vec3d pos, Direction facing, boolean frame) {
    }

    private static Attachment findItemFrameAttachment(ServerPlayerEntity player) {
        Vec3d start = player.getCameraPosVec(1.0f);
        Vec3d end = start.add(player.getRotationVec(1.0f).multiply(4.5D));
        Box box = player.getBoundingBox().stretch(end.subtract(start)).expand(1.0D);
        ItemFrameEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ItemFrameEntity frame : player.getWorld().getEntitiesByClass(ItemFrameEntity.class, box, e -> true)) {
            var hit = frame.getBoundingBox().expand(0.15D).raycast(start, end);
            if (hit.isEmpty()) {
                continue;
            }
            double distance = hit.get().squaredDistanceTo(start);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = frame;
            }
        }
        return best == null ? null : new Attachment(best.getPos(), best.getHorizontalFacing(), true);
    }

    private static BlockPos findBrewingStand(ServerPlayerEntity player) {
        BlockPos feet = player.getBlockPos();
        if (player.getWorld().getBlockState(feet).isOf(net.minecraft.block.Blocks.BREWING_STAND)) {
            return feet;
        }
        BlockPos below = feet.down();
        return player.getWorld().getBlockState(below).isOf(net.minecraft.block.Blocks.BREWING_STAND)
                ? below
                : null;
    }

    private static boolean shouldCenterOnBlock(MaskType type) {
        return type == MaskType.BLOCK
                || type == MaskType.DOOR
                || type == MaskType.PORTAL
                || type == MaskType.LADDER_REVERSED
                || type == MaskType.BUTTON
                || type == MaskType.SCULK_VEIN
                || type == MaskType.LANTERN
                || type == MaskType.STEM;
    }
}
