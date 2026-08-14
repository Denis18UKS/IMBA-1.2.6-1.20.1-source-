package fable.hideseek.imba;

import fable.hideseek.imba.client.*;
import fable.hideseek.imba.mask.MaskType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.text.Text;
import net.minecraft.util.TypedActionResult;

/** Client half of the extra admin/gameplay tools. */
public final class ImbaClientExtension implements ClientModInitializer {
    private int hintTicks;

    @Override
    public void onInitializeClient() {
        BlockRulesClientNetworking.register();
        RoundRestoreClientNetworking.register();
        HologramClientNetworking.register();
        LocationHologramRenderer.register();

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClient) return TypedActionResult.pass(player.getStackInHand(hand));
            var stack = player.getStackInHand(hand);
            var client = net.minecraft.client.MinecraftClient.getInstance();
            if (stack.isOf(ImbaExtension.BLOCK_RULES_TOOL)) {
                client.setScreen(new BlockRulesScreen());
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
            return TypedActionResult.pass(stack);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || --hintTicks > 0) return;
            hintTicks = 20;
            var uuid = client.player.getUuid();
            if (!ClientMaskData.hasMask(uuid) || KeyBindings.rotateKey == null) return;

            String rotate = KeyBindings.rotateKey.getBoundKeyLocalizedText().getString();
            StringBuilder hint = new StringBuilder("Нажмите ").append(rotate).append(", чтобы крутиться");
            MaskType type = ClientMaskData.TYPES.get(uuid);
            boolean canClimb = type == MaskType.WALL_CLIMB
                    || type == MaskType.BLOCK && ClientMaskData.BLOCKS.get(uuid) == ImbaMod.GLOWBERRIES;
            if (canClimb && !WallClimbClientNetworking.isEnabled(uuid) && KeyBindings.wallClimbKey != null) {
                hint.append("  •  ")
                        .append(KeyBindings.wallClimbKey.getBoundKeyLocalizedText().getString())
                        .append(" — включить ползание");
            }
            client.player.sendMessage(Text.literal(hint.toString()), true);
        });
    }
}
