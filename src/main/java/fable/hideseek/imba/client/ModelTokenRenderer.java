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
                    if (mode == ModelTransformationMode.GUI) {
                        /*
                         * renderBlockAsEntity already renders around the item
                         * matrix origin. Scaling around (0.5,0.5,0.5) shifted
                         * the token toward a slot corner. Normalize every block
                         * token and give the animated portal extra margin.
                         */
                        float guiScale = block == Blocks.NETHER_PORTAL ? 0.34F : 0.55F;
                        matrices.scale(guiScale, guiScale, guiScale);
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
