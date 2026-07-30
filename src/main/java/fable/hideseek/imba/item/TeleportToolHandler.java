package fable.hideseek.imba.item;

import fable.hideseek.imba.game.GameConfig;
import fable.hideseek.imba.ImbaMod;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TeleportToolHandler {

    private static final Map<UUID, String> MODES = new HashMap<>();

    private TeleportToolHandler() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!isTeleportTool(stack))
                return ActionResult.PASS;
            if (world.isClient)
                return ActionResult.SUCCESS;
            if (!(player instanceof ServerPlayerEntity serverPlayer))
                return ActionResult.SUCCESS;

            String mode = MODES.get(serverPlayer.getUuid());
            if (mode == null || mode.isBlank()) {
                serverPlayer.sendMessage(Text.literal("§cСначала выберите точку через /imba_tp_mode"), false);
                return ActionResult.SUCCESS;
            }

            Vec3d pos = new Vec3d(
                    hitResult.getBlockPos().getX() + 0.5,
                    hitResult.getBlockPos().getY() + 1.0,
                    hitResult.getBlockPos().getZ() + 0.5);

            GameConfig.setPoint(mode, pos);
            serverPlayer.sendMessage(Text.literal("§aТочка сохранена: §f"
                    + fable.hideseek.imba.game.GameConfig.getPointDisplayName(mode)
                    + " §7→ §f" + pos), false);
            return ActionResult.SUCCESS;
        });
    }

    public static ItemStack createTool() {
        ItemStack stack = new ItemStack(ImbaMod.TELEPORT_TOOL);
        stack.setCustomName(Text.literal("§eНастройщик телепортов"));

        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putBoolean("imba_tp_tool", true);

        return stack;
    }

    public static boolean isTeleportTool(ItemStack stack) {
        return stack != null && (stack.isOf(ImbaMod.TELEPORT_TOOL)
                || stack.hasNbt() && stack.getNbt().getBoolean("imba_tp_tool"));
    }

    public static void setMode(ServerPlayerEntity player, String mode) {
        MODES.put(player.getUuid(), mode);
    }
}
