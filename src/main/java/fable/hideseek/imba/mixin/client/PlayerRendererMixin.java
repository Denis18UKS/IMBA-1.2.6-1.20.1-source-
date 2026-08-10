package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.client.ClientMaskData;
import fable.hideseek.imba.client.MaskRenderHelper;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class PlayerRendererMixin {

        @Inject(method = "render", at = @At("HEAD"), cancellable = true)
        private void render(AbstractClientPlayerEntity player,
                        float yaw,
                        float tickDelta,
                        MatrixStack matrices,
                        VertexConsumerProvider consumers,
                        int light,
                        CallbackInfo ci) {
                var uuid = player.getUuid();
                if (!ClientMaskData.hasMask(uuid)) {
                        return;
                }

                ci.cancel();
                int maskLight = WorldRenderer.getLightmapCoordinates(player.getWorld(), player.getBlockPos());
                MaskRenderHelper.renderMask(uuid, matrices, consumers, maskLight);
        }
}
