package fable.hideseek.imba.item;

import fable.hideseek.imba.config.AttachmentConfig;
import fable.hideseek.imba.game.GameMessages;
import fable.hideseek.imba.mask.MaskState;
import fable.hideseek.imba.mask.MaskService;
import fable.hideseek.imba.mask.MaskType;
import fable.hideseek.imba.net.MaskNetworking;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.TypedActionResult;

public final class HideButtonHandler {

    private static final int EFFECT_FOREVER = Integer.MAX_VALUE;

    private HideButtonHandler() {
    }

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);

            // Older builds used hide_button as the icon for blocks without an
            // item form. Let the model handler consume those legacy stacks.
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

        state.attachedToFrame = false;
        state.attachmentFacing = Direction.NORTH;

        if (state.type == MaskType.ITEM && state.item != null && MaskService.isSpecialPotion(state.item)) {
            BlockPos support = findBrewingStand(player);
            if (support != null) {
                Identifier id = Registries.ITEM.getId(state.item);
                Vec3d offset = AttachmentConfig.offsetFor(id);
                x = support.getX() + 0.5D + offset.x;
                y = support.getY() + 1.0D + offset.y;
                z = support.getZ() + 0.5D + offset.z;
            }
        } else if (shouldCenterOnBlock(state.type)) {
            x = Math.floor(x) + 0.5;
            z = Math.floor(z) + 0.5;
        } else if (state.type == MaskType.ITEM || state.type == MaskType.WALL_CLIMB) {
            Attachment attachment = findItemFrameAttachment(player);
            if (attachment != null) {
                x = attachment.pos.x;
                y = attachment.pos.y;
                z = attachment.pos.z;
                state.attachedToFrame = attachment.frame;
                state.attachmentFacing = attachment.facing;
            } else {
                x = Math.floor(x) + 0.5;
                z = Math.floor(z) + 0.5;
            }
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

    private record Attachment(Vec3d pos, Direction facing, boolean frame) {}

    private static Attachment findItemFrameAttachment(ServerPlayerEntity player) {
        Vec3d start = player.getCameraPosVec(1.0f);
        Vec3d end = start.add(player.getRotationVec(1.0f).multiply(4.5D));
        Box box = player.getBoundingBox().stretch(end.subtract(start)).expand(1.0D);
        ItemFrameEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ItemFrameEntity frame : player.getWorld().getEntitiesByClass(ItemFrameEntity.class, box, e -> true)) {
            var hit = frame.getBoundingBox().expand(0.15D).raycast(start, end);
            if (hit.isEmpty()) continue;
            double d = hit.get().squaredDistanceTo(start);
            if (d < bestDistance) {
                bestDistance = d;
                best = frame;
            }
        }
        if (best != null) {
            return new Attachment(best.getPos(), best.getHorizontalFacing(), true);
        }
        return null;
    }

    private static BlockPos findBrewingStand(ServerPlayerEntity player) {
        BlockPos feet = player.getBlockPos();
        if (player.getWorld().getBlockState(feet).isOf(net.minecraft.block.Blocks.BREWING_STAND)) {
            return feet;
        }
        BlockPos below = feet.down();
        return player.getWorld().getBlockState(below).isOf(net.minecraft.block.Blocks.BREWING_STAND)
                ? below : null;
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
