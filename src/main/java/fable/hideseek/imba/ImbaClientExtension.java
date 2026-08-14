package fable.hideseek.imba;

import fable.hideseek.imba.client.*;
import fable.hideseek.imba.mask.MaskType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.TypedActionResult;

/** Client half of the extra admin/gameplay tools. */
public final class ImbaClientExtension implements ClientModInitializer {
    private static final int HINT_DURATION_TICKS = 100;
    private static final int HINT_REFRESH_TICKS = 40;

    private int hintRemaining;
    private int hintRefresh;
    private String lastMaskSignature = "";
    private boolean lastClimbEnabled = true;

    @Override
    public void onInitializeClient() {
        AirFixationClientNetworking.register();
        BlockRulesClientNetworking.register();
        RoundRestoreClientNetworking.register();
        HologramClientNetworking.register();
        PanelSettingsClientNetworking.register();
        LocationHologramRenderer.register();

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClient) return TypedActionResult.pass(player.getStackInHand(hand));
            var stack = player.getStackInHand(hand);
            var client = net.minecraft.client.MinecraftClient.getInstance();
            if (stack.isOf(ImbaExtension.BLOCK_RULES_TOOL)) { client.setScreen(new BlockRulesScreen()); return TypedActionResult.success(stack); }
            if (stack.isOf(ImbaExtension.BLOCK_RESTORE_TOOL)) { client.setScreen(new BlockRestoreScreen()); return TypedActionResult.success(stack); }
            if (stack.isOf(ImbaExtension.STRUCTURE_LAYER_TOOL)) { client.setScreen(new StructureLayerScreen()); return TypedActionResult.success(stack); }
            if (stack.isOf(ImbaExtension.HOLOGRAM_PROJECTOR_TOOL)) { client.setScreen(new HologramProjectorScreen()); return TypedActionResult.success(stack); }
            if (stack.isOf(ImbaExtension.PANEL_SETTINGS_TOOL)) { client.setScreen(new GameSettingsScreen()); return TypedActionResult.success(stack); }
            if (stack.isOf(ImbaExtension.AIR_FIXATION_TOOL)) { client.setScreen(new AirFixationConfigScreen()); return TypedActionResult.success(stack); }
            return TypedActionResult.pass(stack);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) { resetHints(); return; }
            var uuid = client.player.getUuid();
            if (!ClientMaskData.hasMask(uuid) || KeyBindings.rotateKey == null) {
                if (!lastMaskSignature.isEmpty()) client.player.sendMessage(Text.empty(), true);
                resetHints();
                return;
            }
            MaskType type = ClientMaskData.TYPES.get(uuid);
            String block = ClientMaskData.BLOCKS.get(uuid) == null ? "" : Registries.BLOCK.getId(ClientMaskData.BLOCKS.get(uuid)).toString();
            String item = ClientMaskData.ITEMS.get(uuid) == null ? "" : Registries.ITEM.getId(ClientMaskData.ITEMS.get(uuid)).toString();
            String signature = String.valueOf(type) + '|' + block + '|' + item;
            boolean canClimb = type == MaskType.WALL_CLIMB || type == MaskType.BLOCK && ClientMaskData.BLOCKS.get(uuid) == ImbaMod.GLOWBERRIES;
            boolean climbEnabled = !canClimb || WallClimbClientNetworking.isEnabled(uuid);
            if (!signature.equals(lastMaskSignature) || (canClimb && lastClimbEnabled && !climbEnabled)) { hintRemaining = HINT_DURATION_TICKS; hintRefresh = 0; }
            lastMaskSignature = signature;
            lastClimbEnabled = climbEnabled;
            if (hintRemaining <= 0) return;
            if (hintRefresh-- <= 0) {
                client.player.sendMessage(Text.literal(buildHint(type, canClimb, climbEnabled)), true);
                hintRefresh = HINT_REFRESH_TICKS;
            }
            hintRemaining--;
            if (hintRemaining == 0) client.player.sendMessage(Text.empty(), true);
        });
    }

    private static String buildHint(MaskType type, boolean canClimb, boolean climbEnabled) {
        String rotate = KeyBindings.rotateKey.getBoundKeyLocalizedText().getString();
        StringBuilder hint = new StringBuilder("Нажмите ").append(rotate).append(", чтобы крутиться");
        if (canClimb && !climbEnabled && KeyBindings.wallClimbKey != null) {
            hint.append("  •  ").append(KeyBindings.wallClimbKey.getBoundKeyLocalizedText().getString()).append(" — включить ползание");
        }
        return hint.toString();
    }

    private void resetHints() {
        hintRemaining = 0;
        hintRefresh = 0;
        lastMaskSignature = "";
        lastClimbEnabled = true;
    }
}
