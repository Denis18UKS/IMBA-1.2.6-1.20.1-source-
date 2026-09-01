package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.client.ClientMaskData;
import fable.hideseek.imba.client.MaskLightHelper;
import fable.hideseek.imba.client.MaskRenderHelper;
import fable.hideseek.imba.mask.MaskType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void renderLateMasks(
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
        if (client.player == null || client.world == null || camera == null) {
            return;
        }

        VertexConsumerProvider.Immediate vertexConsumers = client.getBufferBuilders().getEntityVertexConsumers();
        boolean rendered = renderPortalMasksLate(client, matrices, tickDelta, camera, vertexConsumers);

        if (client.options.getPerspective().isFirstPerson()) {
            var uuid = client.player.getUuid();
            if (ClientMaskData.hasMask(uuid)
                    && ClientMaskData.TYPES.get(uuid) != MaskType.PORTAL) {
                renderMaskAtPlayer(client.player, client, matrices, tickDelta, camera, vertexConsumers);
                rendered = true;
            }
        }

        if (rendered) {
            vertexConsumers.draw();
        }
    }

    private static boolean renderPortalMasksLate(
            MinecraftClient client,
            MatrixStack matrices,
            float tickDelta,
            Camera camera,
            VertexConsumerProvider.Immediate vertexConsumers) {
        boolean rendered = false;
        for (var player : client.world.getPlayers()) {
            var uuid = player.getUuid();
            if (!ClientMaskData.hasMask(uuid)
                    || ClientMaskData.TYPES.get(uuid) != MaskType.PORTAL) {
                continue;
            }
            renderMaskAtPlayer(player, client, matrices, tickDelta, camera, vertexConsumers);
            rendered = true;
        }
        return rendered;
    }

    private static void renderMaskAtPlayer(
            AbstractClientPlayerEntity player,
            MinecraftClient client,
            MatrixStack matrices,
            float tickDelta,
            Camera camera,
            VertexConsumerProvider.Immediate vertexConsumers) {
        var uuid = player.getUuid();

        double playerX = player.prevX + (player.getX() - player.prevX) * tickDelta;
        double playerY = player.prevY + (player.getY() - player.prevY) * tickDelta;
        double playerZ = player.prevZ + (player.getZ() - player.prevZ) * tickDelta;

        double cameraX = camera.getPos().x;
        double cameraY = camera.getPos().y;
        double cameraZ = camera.getPos().z;

        matrices.push();
        matrices.translate(playerX - cameraX, playerY - cameraY, playerZ - cameraZ);
        int maskLight = MaskLightHelper.resolve(uuid, client.world, playerX, playerY, playerZ);
        Vec3d anchor = ClientMaskData.getStatueAnchor(uuid);
        double renderX = anchor == null ? playerX : anchor.x;
        double renderY = anchor == null ? playerY : anchor.y;
        double renderZ = anchor == null ? playerZ : anchor.z;
        BlockPos renderPos = BlockPos.ofFloored(renderX, renderY + 0.5D, renderZ);
        MaskRenderHelper.renderMask(
                uuid, matrices, vertexConsumers, maskLight, client.world, renderPos);
        matrices.pop();
    }
}
