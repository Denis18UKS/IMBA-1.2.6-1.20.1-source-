package fable.hideseek.imba.item;

import fable.hideseek.imba.config.AttachmentConfig;
import fable.hideseek.imba.config.MaskAutoPositionConfig;
import fable.hideseek.imba.config.MaskBlockConfig;
import fable.hideseek.imba.config.MaskHitboxConfig;
import fable.hideseek.imba.game.GameMessages;
import fable.hideseek.imba.mask.MaskHitbox;
import fable.hideseek.imba.mask.MaskService;
import fable.hideseek.imba.mask.MaskState;
import fable.hideseek.imba.mask.MaskType;
import fable.hideseek.imba.net.MaskNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityPose;
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
    private static final double STANDING_SURFACE_TOLERANCE = 0.35D;
    private static final double SUPPORT_FOOTPRINT_OFFSET = 0.27D;

    private HideButtonHandler() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!(stack.getItem() instanceof HideItem)) return ActionResult.PASS;
            if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) handle(serverPlayer);
            return ActionResult.SUCCESS;
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (ModelEquipHandler.isModelItem(stack)) return TypedActionResult.pass(stack);
            if (!(stack.getItem() instanceof HideItem)) return TypedActionResult.pass(stack);
            if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) handle(serverPlayer);
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
            player.setVelocity(0, 0, 0);
            player.fallDistance = 0f;
            player.calculateDimensions();
            player.removeStatusEffect(StatusEffects.INVISIBILITY);
            MaskNetworking.refresh(player);
            GameMessages.send(player, Text.literal("§cВы вышли из маскировки-статуи"));
            return;
        }

        // Keep the exact feet anchor that existed before cancelling sneak. The
        // client may still physically hold Shift, so using floor(Y) as fallback
        // here can move the mask down into a slab/trapdoor/ground block.
        double originalX = player.getX();
        double originalY = player.getY();
        double originalZ = player.getZ();

        player.setSneaking(false);
        player.setPose(EntityPose.STANDING);
        player.calculateDimensions();

        double x = originalX, y = originalY, z = originalZ;
        boolean specialPotion = state.type == MaskType.ITEM
                && state.item != null
                && MaskService.isSpecialPotion(state.item);
        SupportInfo support = findSupport(player, originalX, originalY, originalZ);
        state.attachedToFrame = false;
        state.attachmentFacing = Direction.NORTH;

        if (specialPotion) {
            BlockPos brewing = findBrewingStand(player);
            if (brewing != null) {
                Identifier id = Registries.ITEM.getId(state.item);
                Vec3d off = AttachmentConfig.offsetFor(id);
                x = brewing.getX() + .5D + off.x;
                y = brewing.getY() + 1D + off.y;
                z = brewing.getZ() + .5D + off.z;
            } else {
                x = Math.floor(x) + .5D;
                y = standingSurfaceY(player, support, originalY);
                z = Math.floor(z) + .5D;
            }
        } else if (state.type == MaskType.BLOCK) {
            if (MaskBlockConfig.isFull(state.block)) {
                x = Math.floor(x) + .5D;
                y = standingSurfaceY(player, support, snapFullBlockY(originalY));
                z = Math.floor(z) + .5D;
            }
        } else if (shouldCenterOnBlock(state.type)) {
            x = Math.floor(x) + .5D;
            y = standingSurfaceY(player, support, originalY);
            z = Math.floor(z) + .5D;
        } else if (state.type == MaskType.ITEM || state.type == MaskType.WALL_CLIMB) {
            Attachment attachment = findItemFrameAttachment(player);
            if (attachment != null) {
                x = attachment.pos.x;
                y = attachment.pos.y;
                z = attachment.pos.z;
                state.attachedToFrame = attachment.frame;
                state.attachmentFacing = attachment.facing;
            } else {
                x = Math.floor(x) + .5D;
                y = standingSurfaceY(player, support, originalY);
                z = Math.floor(z) + .5D;
            }
        }

        if (state.block != null && support != null && MaskAutoPositionConfig.hasPair(state.block, support.block)) {
            MaskAutoPositionConfig.Offset offset = MaskAutoPositionConfig.offsetFor(state.block, support.block);
            x += offset.xPixels / 16.0D;
            y = support.surfaceY + offset.yPixels / 16.0D;
            z += offset.zPixels / 16.0D;
        }

        // 2D potion uses only its own dedicated position/offset system above.
        if (!specialPotion) {
            double feetY = player.getBoundingBox().minY;
            boolean inAir = support == null || feetY - support.surfaceY > .30D;
            if (!fable.hideseek.imba.config.AirFixationConfig.isAllowedAt(state, player.getWorld(), x, y, z, inAir)) {
                var rule = fable.hideseek.imba.config.AirFixationConfig.effectiveRule(state);
                player.sendMessage(Text.literal(rule.mode() == fable.hideseek.imba.config.AirFixationConfig.Mode.REQUIRE_BLOCK
                        ? "§cЗдесь нельзя зафиксироваться: требуется блок §f" + rule.requiredBlock()
                        : "§cНельзя зафиксироваться в воздухе"), true);
                return;
            }

            Box configured = state.block == null
                    ? null
                    : MaskHitboxConfig.worldBox(state.block, state.rotation, x, y, z);
            Box finalBox = configured != null
                    ? configured
                    : MaskHitbox.getDimensions(state.type, state.item).getBoxAt(new Vec3d(x, y, z));
            if (!player.getWorld().isSpaceEmpty(player, finalBox)) {
                player.sendMessage(Text.literal("§cВ конечной точке маскировки недостаточно места"), true);
                return;
            }
        }

        player.setPosition(x, y, z);
        // Force an authoritative position packet immediately; this prevents a
        // still-held client Shift from briefly re-applying its crouched position.
        player.requestTeleport(x, y, z);
        player.setVelocity(0, 0, 0);
        player.fallDistance = 0f;
        player.setNoGravity(true);
        MaskState.enableStatue(uuid, x, y, z);
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.INVISIBILITY, EFFECT_FOREVER, 0, false, false, false));
        player.calculateDimensions();
        MaskNetworking.refresh(player);
        GameMessages.send(player, Text.literal("§aВы замаскировались"));
    }

    private static double standingSurfaceY(ServerPlayerEntity player, SupportInfo support, double fallback) {
        if (support == null) return fallback;
        double feetY = player.getBoundingBox().minY;
        return Math.abs(feetY - support.surfaceY) <= STANDING_SURFACE_TOLERANCE
                ? support.surfaceY
                : fallback;
    }

    /**
     * Samples the center and edges of a normal player footprint. Sneaking lets
     * the player's center hang beyond a ledge, so center-only support detection
     * is exactly what made Shift+fixation fall back to floor(Y).
     */
    private static SupportInfo findSupport(ServerPlayerEntity player, double centerX, double feetY, double centerZ) {
        double[] xs = {centerX, centerX - SUPPORT_FOOTPRINT_OFFSET, centerX + SUPPORT_FOOTPRINT_OFFSET};
        double[] zs = {centerZ, centerZ - SUPPORT_FOOTPRINT_OFFSET, centerZ + SUPPORT_FOOTPRINT_OFFSET};
        SupportInfo best = null;
        double bestDistance = Double.MAX_VALUE;

        for (double sampleX : xs) {
            for (double sampleZ : zs) {
                SupportInfo candidate = findSupportAt(player, sampleX, feetY, sampleZ);
                if (candidate == null) continue;
                double distance = Math.abs(feetY - candidate.surfaceY);
                if (distance < bestDistance) {
                    best = candidate;
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    private static SupportInfo findSupportAt(ServerPlayerEntity player, double x, double feetY, double z) {
        BlockPos feet = BlockPos.ofFloored(x, feetY + SUPPORT_XZ_EPSILON, z);
        SupportInfo inside = supportAt(player, feet, x, z);
        if (inside != null) return inside;

        BlockPos below = BlockPos.ofFloored(x, feetY - SUPPORT_SAMPLE_EPSILON, z);
        SupportInfo direct = supportAt(player, below, x, z);
        if (direct != null) return direct;

        for (int i = 1; i <= 2; i++) {
            SupportInfo candidate = supportAt(player, below.down(i), x, z);
            if (candidate != null) return candidate;
        }
        return null;
    }

    private static SupportInfo supportAt(ServerPlayerEntity player, BlockPos pos, double worldX, double worldZ) {
        BlockState state = player.getWorld().getBlockState(pos);
        if (state.isAir()) return null;
        VoxelShape shape = state.getCollisionShape(player.getWorld(), pos);
        if (shape.isEmpty()) return null;
        double localX = worldX - pos.getX();
        double localZ = worldZ - pos.getZ();
        double top = Double.NEGATIVE_INFINITY;
        for (Box box : shape.getBoundingBoxes()) {
            if (localX >= box.minX - SUPPORT_XZ_EPSILON && localX <= box.maxX + SUPPORT_XZ_EPSILON
                    && localZ >= box.minZ - SUPPORT_XZ_EPSILON && localZ <= box.maxZ + SUPPORT_XZ_EPSILON) {
                top = Math.max(top, box.maxY);
            }
        }
        if (!Double.isFinite(top)) return null;
        return new SupportInfo(state.getBlock(), pos, pos.getY() + top);
    }

    private static double snapFullBlockY(double y) {
        return Math.ceil(y - FULL_BLOCK_EPSILON);
    }

    private record SupportInfo(Block block, BlockPos pos, double surfaceY) {}
    private record Attachment(Vec3d pos, Direction facing, boolean frame) {}

    private static Attachment findItemFrameAttachment(ServerPlayerEntity player) {
        Vec3d start = player.getCameraPosVec(1f);
        Vec3d end = start.add(player.getRotationVec(1f).multiply(4.5D));
        Box box = player.getBoundingBox().stretch(end.subtract(start)).expand(1D);
        ItemFrameEntity best = null;
        double distance = Double.MAX_VALUE;
        for (ItemFrameEntity frame : player.getWorld().getEntitiesByClass(ItemFrameEntity.class, box, entity -> true)) {
            var hit = frame.getBoundingBox().expand(.15D).raycast(start, end);
            if (hit.isEmpty()) continue;
            double candidate = hit.get().squaredDistanceTo(start);
            if (candidate < distance) {
                distance = candidate;
                best = frame;
            }
        }
        return best == null ? null : new Attachment(best.getPos(), best.getHorizontalFacing(), true);
    }

    private static BlockPos findBrewingStand(ServerPlayerEntity player) {
        BlockPos feet = player.getBlockPos();
        if (player.getWorld().getBlockState(feet).isOf(net.minecraft.block.Blocks.BREWING_STAND)) return feet;
        BlockPos below = feet.down();
        return player.getWorld().getBlockState(below).isOf(net.minecraft.block.Blocks.BREWING_STAND) ? below : null;
    }

    private static boolean shouldCenterOnBlock(MaskType type) {
        return type == MaskType.BLOCK || type == MaskType.DOOR || type == MaskType.PORTAL
                || type == MaskType.LADDER_REVERSED || type == MaskType.BUTTON
                || type == MaskType.SCULK_VEIN || type == MaskType.LANTERN || type == MaskType.STEM;
    }
}
