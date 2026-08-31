package fable.hideseek.imba.client;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Controls only the animation clock of the player-portal mask.
 *
 * Geometry is always produced by Minecraft's own minecraft:nether_portal baked
 * block model.  We only remap the portal sprite UVs to a selected frame of the
 * vanilla 32-frame texture sheet so the configured end-of-cycle freeze can be
 * accumulated without replacing the portal with a custom plane/model.
 */
public final class PortalMaskAnimationClock {
    private static final Identifier PORTAL_SPRITE = new Identifier("minecraft", "block/nether_portal");
    private static final Identifier PORTAL_TEXTURE =
            new Identifier("minecraft", "textures/block/nether_portal.png");
    private static final int FRAME_COUNT = 32;

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
        MinecraftClient client = MinecraftClient.getInstance();
        SpriteAtlasTexture atlas = client.getBakedModelManager().getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
        Sprite sprite = atlas.getSprite(PORTAL_SPRITE);
        int frame = frameIndex();

        /*
         * Keep Minecraft's portal model/shape/rotation exactly as-is.  The
         * provider below changes only where that model reads its UVs from:
         * atlas-local portal UV -> the chosen frame in vanilla nether_portal.png.
         */
        VertexConsumerProvider frameProvider = requestedLayer -> {
            VertexConsumer delegate = consumers.getBuffer(RenderLayer.getEntityTranslucent(PORTAL_TEXTURE));
            return new PortalFrameVertexConsumer(delegate, sprite, frame);
        };

        client.getBlockRenderManager().renderBlockAsEntity(
                Blocks.NETHER_PORTAL.getDefaultState(),
                matrices,
                frameProvider,
                light,
                OverlayTexture.DEFAULT_UV);
    }

    private static final class PortalFrameVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float minU;
        private final float minV;
        private final float widthU;
        private final float heightV;
        private final int frame;

        private PortalFrameVertexConsumer(VertexConsumer delegate, Sprite sprite, int frame) {
            this.delegate = delegate;
            this.minU = sprite.getMinU();
            this.minV = sprite.getMinV();
            this.widthU = Math.max(0.000001F, sprite.getMaxU() - minU);
            this.heightV = Math.max(0.000001F, sprite.getMaxV() - minV);
            this.frame = Math.max(0, Math.min(FRAME_COUNT - 1, frame));
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            delegate.color(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            float localU = clamp01((u - minU) / widthU);
            float localV = clamp01((v - minV) / heightV);
            delegate.texture(localU, (frame + localV) / (float) FRAME_COUNT);
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            delegate.overlay(u, v);
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v) {
            delegate.light(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            delegate.normal(x, y, z);
            return this;
        }

        @Override
        public void next() {
            delegate.next();
        }

        @Override
        public void fixedColor(int red, int green, int blue, int alpha) {
            delegate.fixedColor(red, green, blue, alpha);
        }

        @Override
        public void unfixColor() {
            delegate.unfixColor();
        }

        private static float clamp01(float value) {
            return Math.max(0.0F, Math.min(1.0F, value));
        }
    }
}
