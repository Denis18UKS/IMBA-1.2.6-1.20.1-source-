package fable.hideseek.imba.item;

import fable.hideseek.imba.ImbaMod;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Creates the teleport setup item. Configuration itself is handled by the
 * client GUI and TeleportToolNetworking; the old click-a-block behaviour was
 * removed so using the tool never accidentally overwrites a point.
 */
public final class TeleportToolHandler {
    private static final Map<UUID, String> LEGACY_MODES = new HashMap<>();

    private TeleportToolHandler() {
    }

    public static void register() {
        // Kept as an explicit registration hook for binary/source compatibility.
        // The tool now opens TeleportSetupScreen and saves through networking.
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

    /** Legacy command compatibility; the GUI no longer depends on this mode. */
    public static void setMode(ServerPlayerEntity player, String mode) {
        if (player != null) {
            LEGACY_MODES.put(player.getUuid(), mode);
        }
    }
}
