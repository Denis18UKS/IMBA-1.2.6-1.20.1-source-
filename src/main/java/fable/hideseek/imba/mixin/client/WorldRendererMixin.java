package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.client.ClientMaskData;
import fable.hideseek.imba.client.MaskRenderHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void renderFirstPersonMask(
            MatrixStack matrices,
            float tickDelta,
            long limitTime,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightmapTextureManager lightmapTextureManager,
            Matrix4f positionMatrix,
            CallbackInfo ci) {

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        if (camera == null) {
            return;
        }
        if (!client.options.getPerspective().isFirstPerson()) {
            return;
        }

        var uuid = client.player.getUuid();
        if (!ClientMaskData.hasMask(uuid)) {
            return;
        }

        VertexConsumerProvider.Immediate vertexConsumers = client.getBufferBuilders().getEntityVertexConsumers();

        double playerX = client.player.prevX + (client.player.getX() - client.player.prevX) * tickDelta;
        double playerY = client.player.prevY + (client.player.getY() - client.player.prevY) * tickDelta;
        double playerZ = client.player.prevZ + (client.player.getZ() - client.player.prevZ) * tickDelta;

        double cameraX = camera.getPos().x;
        double cameraY = camera.getPos().y;
        double cameraZ = camera.getPos().z;

        matrices.push();
        matrices.translate(playerX - cameraX, playerY - cameraY, playerZ - cameraZ);
        MaskRenderHelper.renderMask(uuid, matrices, vertexConsumers, LightmapTextureManager.MAX_LIGHT_COORDINATE);
        matrices.pop();

        vertexConsumers.draw();
    }
}