package fable.hideseek.imba.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class PortalMaskAnimationClock {
    private static final Identifier PORTAL_TEXTURE = new Identifier("minecraft", "textures/block/nether_portal.png");
    private static final int FRAME_COUNT = 32;
    private static final int FULL_BRIGHT = 15728880;

    private PortalMaskAnimationClock() {
    }

    public static int frameIndex() {
        MinecraftClient client = MinecraftClient.getInstance();
        long tick = client.world == null ? 0L : client.world.getTime();
        int freezeTicks = Math.max(0, PortalAnimationClientData.freezeTicks);
        int holdTicks = freezeTicks;
        int cycleTicks = FRAME_COUNT + holdTicks;
        int cycleTick = (int) Math.floorMod(tick, Math.max(1, cycleTicks));
        return Math.min(FRAME_COUNT - 1, cycleTick);
    }

    public static void renderPortalMaskFrame(MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        int frame = frameIndex();
        float v0 = frame / (float) FRAME_COUNT;
        float v1 = (frame + 1) / (float) FRAME_COUNT;
        VertexConsumer vertices = consumers.getBuffer(RenderLayer.getEntityTranslucent(PORTAL_TEXTURE));
        Matrix4f position = matrices.peek().getPositionMatrix();
        Matrix3f normal = matrices.peek().getNormalMatrix();
        int packedLight = Math.max(light, FULL_BRIGHT);
        float zFront = 0.501F;
        float zBack = 0.499F;

        vertex(vertices, position, normal, 0F, 0F, zFront, 0F, v1, packedLight, 0F, 0F, 1F);
        vertex(vertices, position, normal, 1F, 0F, zFront, 1F, v1, packedLight, 0F, 0F, 1F);
        vertex(vertices, position, normal, 1F, 1F, zFront, 1F, v0, packedLight, 0F, 0F, 1F);
        vertex(vertices, position, normal, 0F, 1F, zFront, 0F, v0, packedLight, 0F, 0F, 1F);

        vertex(vertices, position, normal, 1F, 0F, zBack, 1F, v1, packedLight, 0F, 0F, -1F);
        vertex(vertices, position, normal, 0F, 0F, zBack, 0F, v1, packedLight, 0F, 0F, -1F);
        vertex(vertices, position, normal, 0F, 1F, zBack, 0F, v0, packedLight, 0F, 0F, -1F);
        vertex(vertices, position, normal, 1F, 1F, zBack, 1F, v0, packedLight, 0F, 0F, -1F);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f position, Matrix3f normal,
                               float x, float y, float z, float u, float v, int light,
                               float nx, float ny, float nz) {
        vertices.vertex(position, x, y, z)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(normal, nx, ny, nz)
                .next();
    }
}
