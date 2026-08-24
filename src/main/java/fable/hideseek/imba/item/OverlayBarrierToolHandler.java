package fable.hideseek.imba.item;

import fable.hideseek.imba.ImbaExtension;
import fable.hideseek.imba.config.OverlayBarrierConfig;
import fable.hideseek.imba.net.OverlayBarrierNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

/**
 * Right-click an existing block with the tool to toggle a full barrier collision
 * in the same cell. The real block state is never changed or copied.
 */
public final class OverlayBarrierToolHandler {
    private OverlayBarrierToolHandler() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!player.getStackInHand(hand).isOf(ImbaExtension.OVERLAY_BARRIER_TOOL)) {
                return ActionResult.PASS;
            }
            if (world.isClient) return ActionResult.SUCCESS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.FAIL;
            if (!serverPlayer.hasPermissionLevel(2)) {
                serverPlayer.sendMessage(Text.literal("§cДля накладных барьеров нужны права оператора"), true);
                return ActionResult.FAIL;
            }

            boolean added = OverlayBarrierConfig.toggle(world, hit.getBlockPos());
            OverlayBarrierNetworking.broadcast(serverPlayer.getServer());
            serverPlayer.sendMessage(Text.literal(added
                    ? "§aНакладной барьер установлен поверх блока §f" + hit.getBlockPos().toShortString()
                    : "§eНакладной барьер удалён §f" + hit.getBlockPos().toShortString()), true);
            return ActionResult.SUCCESS;
        });
    }
}
