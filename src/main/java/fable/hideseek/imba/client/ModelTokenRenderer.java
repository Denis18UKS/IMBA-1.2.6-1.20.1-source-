package fable.hideseek.imba.client;

import fable.hideseek.imba.ImbaMod;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Renders the real target block on model tokens used for blocks that have no
 * vanilla item form, such as the Nether portal.
 */
public final class ModelTokenRenderer {
    private ModelTokenRenderer() {
    }

    public static void register() {
        BuiltinItemRendererRegistry.INSTANCE.register(ImbaMod.MODEL_TOKEN,
                (stack, mode, matrices, consumers, light, overlay) -> {
                    NbtCompound nbt = stack.getNbt();
                    if (nbt == null || !"BLOCK".equalsIgnoreCase(nbt.getString("imba_model_kind"))) {
                        return;
                    }

                    Identifier id;
                    try {
                        id = new Identifier(nbt.getString("imba_model_id"));
                    } catch (RuntimeException exception) {
                        return;
                    }
                    if (!Registries.BLOCK.containsId(id)) {
                        return;
                    }

                    Block block = Registries.BLOCK.get(id);
                    BlockState state = block == Blocks.WATER
                            ? ImbaMod.WATER_MASK.getDefaultState()
                            : block == Blocks.LAVA
                                    ? ImbaMod.LAVA_MASK.getDefaultState()
                                    : block.getDefaultState();

                    matrices.push();
                    /*
                     * Builtin block models can visually fill more than the
                     * nominal 16x16 GUI box. The portal needs a little extra
                     * margin so its animated quad never crosses a hotbar or
                     * inventory slot border.
                     */
                    if (mode == ModelTransformationMode.GUI) {
                        float guiScale = block == Blocks.NETHER_PORTAL ? 0.48F : 1.0F;
                        if (guiScale != 1.0F) {
                            // Scale around the block model's centre. Scaling around
                            // the origin pulled the portal toward a slot corner.
                            matrices.translate(0.5D, 0.5D, 0.5D);
                            matrices.scale(guiScale, guiScale, guiScale);
                            matrices.translate(-0.5D, -0.5D, -0.5D);
                        }
                    }
                    MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(
                            state,
                            matrices,
                            consumers,
                            LightmapTextureManager.MAX_LIGHT_COORDINATE,
                            overlay);
                    matrices.pop();
                });
    }
}
