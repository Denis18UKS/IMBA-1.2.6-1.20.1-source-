package fable.hideseek.imba;

import fable.hideseek.imba.client.*;
import fable.hideseek.imba.mask.MaskType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.TypedActionResult;

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
        PanelHitboxClientNetworking.register();
        MessageSettingsClientNetworking.register();
        MaskHitboxClientNetworking.register();
        OverlayBarrierClientNetworking.register();
        LocationHologramRenderer.register();

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClient) return TypedActionResult.pass(player.getStackInHand(hand));
            var stack = player.getStackInHand(hand);
            var client = net.minecraft.client.MinecraftClient.getInstance();

            if (stack.isOf(ImbaExtension.BLOCK_RULES_TOOL)) {
                client.setScreen(new BlockRulesScreen());
                return TypedActionResult.success(stack);
            }
            if (stack.isOf(ImbaExtension.BLOCK_RESTORE_TOOL)) {
                client.setScreen(new BlockRestoreScreen());
                return TypedActionResult.success(stack);
            }
            if (stack.isOf(ImbaExtension.STRUCTURE_LAYER_TOOL)) {
                client.setScreen(new StructureLayerScreen());
                return TypedActionResult.success(stack);
            }
            if (stack.isOf(ImbaExtension.HOLOGRAM_PROJECTOR_TOOL)) {
                client.setScreen(new HologramProjectorScreen());
                return TypedActionResult.success(stack);
            }
            if (stack.isOf(ImbaExtension.PANEL_SETTINGS_TOOL)) {
                client.setScreen(new GameSettingsScreen());
                return TypedActionResult.success(stack);
            }
            if (stack.isOf(ImbaExtension.AIR_FIXATION_TOOL)) {
                client.setScreen(new AirFixationConfigScreen());
                return TypedActionResult.success(stack);
            }
            if (stack.isOf(ImbaExtension.MESSAGE_SETTINGS_TOOL)) {
                client.setScreen(new MessageSettingsScreen());
                return TypedActionResult.success(stack);
            }
            if (stack.isOf(ImbaExtension.MASK_HITBOX_TOOL)) {
                client.setScreen(new MaskHitboxScreen());
                return TypedActionResult.success(stack);
            }
            // OVERLAY_BARRIER_TOOL intentionally has no GUI: right-click an
            // existing block to toggle its additional collision cell.
            return TypedActionResult.pass(stack);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                resetHints();
                return;
            }
            var uuid = client.player.getUuid();
            if (!ClientGameState.isHider() || !ClientMaskData.hasMask(uuid) || KeyBindings.rotateKey == null) {
                if (!lastMaskSignature.isEmpty()) client.player.sendMessage(Text.empty(), true);
                resetHints();
                return;
            }

            MaskType type = ClientMaskData.TYPES.get(uuid);
            String block = ClientMaskData.BLOCKS.get(uuid) == null
                    ? "" : Registries.BLOCK.getId(ClientMaskData.BLOCKS.get(uuid)).toString();
            String item = ClientMaskData.ITEMS.get(uuid) == null
                    ? "" : Registries.ITEM.getId(ClientMaskData.ITEMS.get(uuid)).toString();
            String signature = String.valueOf(type) + '|' + block + '|' + item;
            boolean canClimb = type == MaskType.WALL_CLIMB
                    || ClientMaskData.BLOCKS.get(uuid) == ImbaMod.GLOWBERRIES
                    || ClientMaskData.BLOCKS.get(uuid) == ImbaMod.HANGING_LANTERN;
            boolean enabled = !canClimb || WallClimbClientNetworking.isEnabled(uuid);

            if (!signature.equals(lastMaskSignature) || (canClimb && lastClimbEnabled && !enabled)) {
                hintRemaining = HINT_DURATION_TICKS;
                hintRefresh = 0;
            }
            lastMaskSignature = signature;
            lastClimbEnabled = enabled;
            if (hintRemaining <= 0) return;
            if (hintRefresh-- <= 0) {
                client.player.sendMessage(Text.literal(buildHint(canClimb, enabled)), true);
                hintRefresh = HINT_REFRESH_TICKS;
            }
            hintRemaining--;
            if (hintRemaining == 0) client.player.sendMessage(Text.empty(), true);
        });
    }

    private static String buildHint(boolean canClimb, boolean enabled) {
        StringBuilder hint = new StringBuilder("Нажмите ")
                .append(KeyBindings.rotateKey.getBoundKeyLocalizedText().getString())
                .append(", чтобы крутиться");
        if (canClimb && !enabled && KeyBindings.wallClimbKey != null) {
            hint.append("  •  ")
                    .append(KeyBindings.wallClimbKey.getBoundKeyLocalizedText().getString())
                    .append(" — включить ползание");
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
