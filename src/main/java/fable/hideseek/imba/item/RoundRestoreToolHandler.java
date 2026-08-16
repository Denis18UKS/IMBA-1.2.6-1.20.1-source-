package fable.hideseek.imba.item;

import fable.hideseek.imba.ImbaExtension;
import fable.hideseek.imba.config.RoundRestoreConfig;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

/** World-selection behavior for both round-restoration tools. */
public final class RoundRestoreToolHandler {
    private RoundRestoreToolHandler() {}
    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.isOf(ImbaExtension.BLOCK_RESTORE_TOOL)) {
                if (world.isClient) return ActionResult.SUCCESS;
                if (!(player instanceof ServerPlayerEntity serverPlayer) || !serverPlayer.hasPermissionLevel(2)) return ActionResult.FAIL;
                if (player.isSneaking()) {
                    boolean removed = RoundRestoreConfig.removeSingle((ServerWorld)world, hit.getBlockPos());
                    player.sendMessage(Text.literal(removed ? "§eТочка восстановления удалена" : "§7Эта точка не была сохранена"), true);
                } else {
                    RoundRestoreConfig.saveSingle((ServerWorld)world, hit.getBlockPos());
                    player.sendMessage(Text.literal("§aСохранён блок и его содержимое: §f" + hit.getBlockPos().toShortString()), true);
                }
                return ActionResult.SUCCESS;
            }
            if (stack.isOf(ImbaExtension.STRUCTURE_LAYER_TOOL)) {
                if (world.isClient) return ActionResult.SUCCESS;
                if (!(player instanceof ServerPlayerEntity serverPlayer) || !serverPlayer.hasPermissionLevel(2)) return ActionResult.FAIL;
                NbtCompound nbt = stack.getOrCreateNbt();
                String prefix = player.isSneaking() ? "imba_layer_b" : "imba_layer_a";
                nbt.putString(prefix + "_world", world.getRegistryKey().getValue().toString());
                nbt.putInt(prefix + "_x", hit.getBlockPos().getX()); nbt.putInt(prefix + "_y", hit.getBlockPos().getY()); nbt.putInt(prefix + "_z", hit.getBlockPos().getZ());
                player.sendMessage(Text.literal((player.isSneaking() ? "§bТочка B: §f" : "§aТочка A: §f")
                        + hit.getBlockPos().toShortString() + " §7(ПКМ по воздуху — GUI)"), true);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });
    }
}
