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

public final class LocationHologramRenderer {
    private static final float BASE_SIZE = .78F;

    private LocationHologramRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null || client.player == null || ctx.matrixStack() == null || ctx.consumers() == null) {
                return;
            }
            String worldId = client.world.getRegistryKey().getValue().toString();
            var camera = client.gameRenderer.getCamera().getPos();
            for (var p : HologramClientData.snapshot()) {
                if (!worldId.equals(p.world())) {
                    continue;
                }
                MatrixStack m = ctx.matrixStack();
                m.push();
                m.translate(p.x() - camera.x, p.y() - camera.y, p.z() - camera.z);
                m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-p.yaw()));

                float scale = Math.max(.20F, p.scale());
                float size = BASE_SIZE * scale;
                float half = size / 2.0F;
                Identifier texture = ClientLocationPhotos.hologramTexture(p.location(), p.light());
                VertexConsumer photo = ctx.consumers().getBuffer(RenderLayer.getEntityCutoutNoCull(texture));
                quad(photo, m, -half, -half, half, half, 0.0F, false);
                quad(photo, m, -half, -half, half, half, 0.0F, true);

                String title = PanelData.locationName(p.location());
                if (title != null && !title.isBlank()) {
                    m.push();
                    m.translate(0.0F, -half - 0.115F, 0.012F);
                    float ts = .0145F * Math.max(.88F, scale);
                    m.scale(ts, -ts, ts);
                    float w = client.textRenderer.getWidth(title);
                    int background = p.textBackground() ? 0x66000000 : 0;
                    client.textRenderer.draw(
                            title,
                            -w / 2.0F,
                            0.0F,
                            0xFFFFFFFF,
                            false,
                            m.peek().getPositionMatrix(),
                            ctx.consumers(),
                            TextRenderer.TextLayerType.NORMAL,
                            background,
                            LightmapTextureManager.MAX_LIGHT_COORDINATE);
                    m.pop();
                }
                m.pop();
            }
        });
    }

    private static void quad(VertexConsumer v, MatrixStack m, float l, float b, float r, float t, float z, boolean reverse) {
        var e = m.peek();
        if (!reverse) {
            vertex(v, e, l, b, z, 0, 1, 1);
            vertex(v, e, r, b, z, 1, 1, 1);
            vertex(v, e, r, t, z, 1, 0, 1);
            vertex(v, e, l, t, z, 0, 0, 1);
        } else {
            vertex(v, e, r, b, z, 0, 1, -1);
            vertex(v, e, l, b, z, 1, 1, -1);
            vertex(v, e, l, t, z, 1, 0, -1);
            vertex(v, e, r, t, z, 0, 0, -1);
        }
    }

    private static void vertex(VertexConsumer v, MatrixStack.Entry e, float x, float y, float z, float u, float vv, int nz) {
        v.vertex(e.getPositionMatrix(), x, y, z)
                .color(255, 255, 255, 255)
                .texture(u, vv)
                .overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(e.getNormalMatrix(), 0, 0, nz)
                .next();
    }
}
