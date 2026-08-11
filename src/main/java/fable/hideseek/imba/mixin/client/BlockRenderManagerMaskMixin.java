package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.client.VanillaMaskRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Routes ladder player-mask block rendering through the same world-aware
 * BlockModelRenderer path used by a real placed ladder.
 */
@Mixin(BlockRenderManager.class)
public abstract class BlockRenderManagerMaskMixin {

    @Shadow
    public abstract void renderBlock(
            BlockState state,
            net.minecraft.util.math.BlockPos pos,
            net.minecraft.world.BlockRenderView world,
            MatrixStack matrices,
            VertexConsumer vertexConsumer,
            boolean cull,
            Random random);

    @Inject(method = "renderBlockAsEntity", at = @At("HEAD"), cancellable = true)
    private void imba$renderMaskBlockWithWorldLighting(
            BlockState state,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay,
            CallbackInfo ci) {
        VanillaMaskRenderContext.Context context = VanillaMaskRenderContext.get();
        if (context == null || context.mode() != VanillaMaskRenderContext.Mode.WORLD_BLOCK) {
            return;
        }

        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayers.getBlockLayer(state));
        renderBlock(
                state,
                context.pos(),
                context.world(),
                matrices,
                vertices,
                false,
                Random.create(state.getRenderingSeed(context.pos())));
        ci.cancel();
    }
}
