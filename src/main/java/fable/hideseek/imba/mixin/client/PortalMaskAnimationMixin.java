package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.client.MaskRenderHelper;
import fable.hideseek.imba.client.PortalMaskAnimationClock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MaskRenderHelper.class)
public abstract class PortalMaskAnimationMixin {
    @Redirect(
            method = "renderMask",
            at = @At(
                    value = "INVOKE",
                    target = "Lfable/hideseek/imba/client/MaskRenderHelper;renderBlock(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/block/BlockState;)V"),
            remap = false)
    private static void imba$renderPortalMaskFrame(MatrixStack matrices, VertexConsumerProvider consumers,
                                                    int light, BlockState state) {
        if (state.isOf(Blocks.NETHER_PORTAL)) {
            PortalMaskAnimationClock.renderPortalMaskFrame(matrices, consumers, light);
            return;
        }
        matrices.push();
        MinecraftClient.getInstance().getBlockRenderManager()
                .renderBlockAsEntity(state, matrices, consumers, light, OverlayTexture.DEFAULT_UV);
        matrices.pop();
    }
}
