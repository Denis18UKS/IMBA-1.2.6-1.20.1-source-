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

/** Small square, fixed-orientation, depth-tested location holograms. */
public final class LocationHologramRenderer {
    private static final float BASE_SIZE = 0.78F;

    private LocationHologramRenderer() {}

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null || client.player == null || ctx.matrixStack() == null || ctx.consumers() == null) return;
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
                int intensity = Math.max(48, Math.min(255, 48 + Math.round((207.0F * projector.light()) / 15.0F)));
                Identifier texture = ClientLocationPhotos.texture(projector.location());

                VertexConsumer vertices = ctx.consumers().getBuffer(RenderLayer.getEntityTranslucentEmissive(texture));
                quad(vertices, matrices, -half, -half, half, half, 0.0F, intensity, false);
                quad(vertices, matrices, -half, -half, half, half, -0.002F, intensity, true);

                String title = PanelData.locationName(projector.location());
                if (title != null && !title.isBlank()) {
                    matrices.push();
                    matrices.translate(0.0D, -half - 0.12F, 0.012F);
                    float textScale = 0.0135F * Math.max(0.85F, scale);
                    matrices.scale(textScale, -textScale, textScale);
                    float width = client.textRenderer.getWidth(title);
                    client.textRenderer.draw(title, -width / 2.0F, 0.0F, 0xFFFFFFFF, false,
                            matrices.peek().getPositionMatrix(), ctx.consumers(), TextRenderer.TextLayerType.NORMAL,
                            0xB0000000, LightmapTextureManager.MAX_LIGHT_COORDINATE);
                    matrices.pop();
                }
                matrices.pop();
            }
        });
    }

    private static void quad(VertexConsumer vertices, MatrixStack matrices,
                             float left, float bottom, float right, float top, float z, int intensity, boolean reverse) {
        MatrixStack.Entry entry = matrices.peek();
        if (!reverse) {
            vertex(vertices, entry, left, bottom, z, 0, 1, intensity, 1);
            vertex(vertices, entry, right, bottom, z, 1, 1, intensity, 1);
            vertex(vertices, entry, right, top, z, 1, 0, intensity, 1);
            vertex(vertices, entry, left, top, z, 0, 0, intensity, 1);
        } else {
            vertex(vertices, entry, right, bottom, z, 0, 1, intensity, -1);
            vertex(vertices, entry, left, bottom, z, 1, 1, intensity, -1);
            vertex(vertices, entry, left, top, z, 1, 0, intensity, -1);
            vertex(vertices, entry, right, top, z, 0, 0, intensity, -1);
        }
    }

    private static void vertex(VertexConsumer vertices, MatrixStack.Entry entry,
                               float x, float y, float z, float u, float v, int intensity, int normalZ) {
        vertices.vertex(entry.getPositionMatrix(), x, y, z)
                .color(intensity, intensity, intensity, 255)
                .texture(u, v)
                .overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(entry.getNormalMatrix(), 0, 0, normalZ)
                .next();
    }
}
