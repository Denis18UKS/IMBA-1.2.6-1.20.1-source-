package fable.hideseek.imba.client;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/**
 * Рендер голограммы локации.
 *
 * ВАЖНО: фотография рисуется РОВНО ОДИН раз. Используется no-cull render layer,
 * поэтому один и тот же quad виден с обеих сторон. Второго coplanar quad здесь
 * принципиально нет — это исключает z-fighting/мерцание двух фотографий.
 */
public final class LocationHologramRenderer {
    private static final float BASE_SIZE = 0.78F;

    private LocationHologramRenderer() {}

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null || client.player == null || ctx.matrixStack() == null || ctx.consumers() == null) {
                return;
            }

            String worldId = client.world.getRegistryKey().getValue().toString();
            var camera = client.gameRenderer.getCamera().getPos();

            for (var projector : HologramClientData.snapshot()) {
                if (!worldId.equals(projector.world())) continue;

                MatrixStack matrices = ctx.matrixStack();
                matrices.push();
                matrices.translate(projector.x() - camera.x, projector.y() - camera.y, projector.z() - camera.z);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-projector.yaw()));

                float scale = Math.max(0.20F, projector.scale());
                float size = BASE_SIZE * scale;
                float half = size / 2.0F;

                Identifier texture = ClientLocationPhotos.hologramTexture(projector.location(), projector.light(), projector.contrast());
                VertexConsumer photo = ctx.consumers().getBuffer(RenderLayer.getEntityCutoutNoCull(texture));

                // ЕДИНСТВЕННЫЙ draw-call фотографии. NoCull делает этот quad
                // двусторонним сам по себе. Никакой второй картинки сзади нет.
                drawSingleTwoSidedPhoto(photo, matrices, half);

                // Название остаётся только на лицевой стороне.
                if (isViewerOnFrontSide(projector, camera.x, camera.z)) {
                    drawTitle(client, ctx, matrices, projector, half, scale);
                }

                matrices.pop();
            }
        });
    }

    private static void drawSingleTwoSidedPhoto(VertexConsumer vertices, MatrixStack matrices, float half) {
        MatrixStack.Entry entry = matrices.peek();
        vertex(vertices, entry, -half, -half, 0.0F, 0.0F, 1.0F);
        vertex(vertices, entry,  half, -half, 0.0F, 1.0F, 1.0F);
        vertex(vertices, entry,  half,  half, 0.0F, 1.0F, 0.0F);
        vertex(vertices, entry, -half,  half, 0.0F, 0.0F, 0.0F);
    }

    private static boolean isViewerOnFrontSide(HologramClientData.Projector projector,
                                                double cameraX, double cameraZ) {
        double yawRad = Math.toRadians(projector.yaw());
        double frontX = -Math.sin(yawRad);
        double frontZ = Math.cos(yawRad);
        double toViewerX = cameraX - projector.x();
        double toViewerZ = cameraZ - projector.z();
        return toViewerX * frontX + toViewerZ * frontZ >= 0.0D;
    }

    private static void drawTitle(MinecraftClient client,
                                  net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext ctx,
                                  MatrixStack matrices,
                                  HologramClientData.Projector projector,
                                  float half,
                                  float scale) {
        String title = PanelData.locationName(projector.location());
        if (title == null || title.isBlank()) return;

        matrices.push();
        matrices.translate(0.0F, -half - 0.115F, 0.012F);
        float textScale = 0.0145F * Math.max(0.88F, scale);
        matrices.scale(textScale, -textScale, textScale);

        float width = client.textRenderer.getWidth(title);
        int background = projector.textBackground() ? 0x66000000 : 0;
        client.textRenderer.draw(
                title,
                -width / 2.0F,
                0.0F,
                0xFFFFFFFF,
                false,
                matrices.peek().getPositionMatrix(),
                ctx.consumers(),
                TextRenderer.TextLayerType.NORMAL,
                background,
                LightmapTextureManager.MAX_LIGHT_COORDINATE);
        matrices.pop();
    }

    private static void vertex(VertexConsumer vertices, MatrixStack.Entry entry,
                               float x, float y, float z, float u, float v) {
        vertices.vertex(entry.getPositionMatrix(), x, y, z)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(entry.getNormalMatrix(), 0, 0, 1)
                .next();
    }
}
