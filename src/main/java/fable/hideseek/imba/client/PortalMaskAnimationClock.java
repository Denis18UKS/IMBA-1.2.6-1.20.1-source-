package fable.hideseek.imba.client;

import fable.hideseek.imba.mixin.client.SpriteContentsUploadAccessor;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Adds only an independent clock to the working v10 player-portal renderer.
 *
 * The portal is still rendered as Blocks.NETHER_PORTAL through Minecraft's
 * baked block model and the exact RenderLayer requested by that model. To hold
 * a frame without replacing the portal renderer, the selected vanilla portal
 * frame is copied into a private sprite in the same block atlas and only the
 * generated model UVs are remapped from the vanilla portal sprite slot to that
 * private slot.
 */
public final class PortalMaskAnimationClock {
    private static final Identifier PORTAL_SPRITE = new Identifier("minecraft", "block/nether_portal");
    private static final Identifier PORTAL_BUFFER_SPRITE = new Identifier("imba", "block/portal_animation_buffer");

    private static Sprite lastBufferSprite;
    private static int lastUploadedFrame = -1;

    private PortalMaskAnimationClock() {
    }

    private static int frameIndex(int frameCount) {
        MinecraftClient client = MinecraftClient.getInstance();
        long tick = client.world == null ? 0L : client.world.getTime();
        int freezeTicks = Math.max(0, PortalAnimationClientData.freezeTicks);
        int holdTicks = freezeTicks;
        int cycleTicks = Math.max(1, frameCount + holdTicks);
        int cycleTick = (int) Math.floorMod(tick, cycleTicks);
        return Math.min(Math.max(0, frameCount - 1), cycleTick);
    }

    public static void renderPortalMaskFrame(MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        MinecraftClient client = MinecraftClient.getInstance();

        // With no configured freeze, use the exact working v10 path with no
        // extra atlas work at all.
        if (PortalAnimationClientData.freezeTicks <= 0) {
            renderWorkingV10Portal(client, matrices, consumers, light);
            return;
        }

        try {
            SpriteAtlasTexture atlas = client.getBakedModelManager().getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
            Sprite source = atlas.getSprite(PORTAL_SPRITE);
            Sprite buffer = atlas.getSprite(PORTAL_BUFFER_SPRITE);

            // Never overwrite Minecraft's missing-texture slot if a resource
            // reload somehow did not stitch our private buffer sprite.
            if (!PORTAL_BUFFER_SPRITE.equals(buffer.getContents().getId())) {
                renderWorkingV10Portal(client, matrices, consumers, light);
                return;
            }

            SpriteContents sourceContents = source.getContents();
            NativeImage[] sourceMipmaps =
                    ((SpriteContentsUploadAccessor) (Object) sourceContents).imba$getMipmapLevelsImages();
            if (sourceMipmaps == null || sourceMipmaps.length == 0 || sourceMipmaps[0] == null) {
                renderWorkingV10Portal(client, matrices, consumers, light);
                return;
            }

            int frameWidth = Math.max(1, sourceContents.getWidth());
            int frameHeight = Math.max(1, sourceContents.getHeight());
            NativeImage level0 = sourceMipmaps[0];
            int columns = Math.max(1, level0.getWidth() / frameWidth);
            int rows = Math.max(1, level0.getHeight() / frameHeight);
            int frameCount = Math.max(1, columns * rows);
            int frame = frameIndex(frameCount);

            if (buffer != lastBufferSprite) {
                lastBufferSprite = buffer;
                lastUploadedFrame = -1;
            }

            if (frame != lastUploadedFrame) {
                int skipX = (frame % columns) * frameWidth;
                int skipY = (frame / columns) * frameHeight;
                atlas.bindTexture();
                ((SpriteContentsUploadAccessor) (Object) buffer.getContents()).imba$uploadFrame(
                        buffer.getX(), buffer.getY(), skipX, skipY, sourceMipmaps);
                lastUploadedFrame = frame;
            }

            VertexConsumerProvider frameProvider = requestedLayer -> {
                // Critical: stay on the exact block RenderLayer selected by
                // Minecraft. No raw PNG and no entity-translucent replacement.
                VertexConsumer delegate = consumers.getBuffer(requestedLayer);
                return new PortalAtlasUvConsumer(delegate, source, buffer);
            };

            client.getBlockRenderManager().renderBlockAsEntity(
                    Blocks.NETHER_PORTAL.getDefaultState(),
                    matrices,
                    frameProvider,
                    light,
                    OverlayTexture.DEFAULT_UV);
        } catch (RuntimeException exception) {
            // Rendering must fail safe to the original working v10 portal.
            renderWorkingV10Portal(client, matrices, consumers, light);
        }
    }

    private static void renderWorkingV10Portal(MinecraftClient client, MatrixStack matrices,
                                                VertexConsumerProvider consumers, int light) {
        client.getBlockRenderManager().renderBlockAsEntity(
                Blocks.NETHER_PORTAL.getDefaultState(),
                matrices,
                consumers,
                light,
                OverlayTexture.DEFAULT_UV);
    }

    private static final class PortalAtlasUvConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float sourceMinU;
        private final float sourceMinV;
        private final float sourceWidthU;
        private final float sourceHeightV;
        private final float bufferMinU;
        private final float bufferMinV;
        private final float bufferWidthU;
        private final float bufferHeightV;

        private PortalAtlasUvConsumer(VertexConsumer delegate, Sprite source, Sprite buffer) {
            this.delegate = delegate;
            this.sourceMinU = source.getMinU();
            this.sourceMinV = source.getMinV();
            this.sourceWidthU = Math.max(0.000001F, source.getMaxU() - sourceMinU);
            this.sourceHeightV = Math.max(0.000001F, source.getMaxV() - sourceMinV);
            this.bufferMinU = buffer.getMinU();
            this.bufferMinV = buffer.getMinV();
            this.bufferWidthU = Math.max(0.000001F, buffer.getMaxU() - bufferMinU);
            this.bufferHeightV = Math.max(0.000001F, buffer.getMaxV() - bufferMinV);
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
            float localU = clamp01((u - sourceMinU) / sourceWidthU);
            float localV = clamp01((v - sourceMinV) / sourceHeightV);
            delegate.texture(
                    bufferMinU + localU * bufferWidthU,
                    bufferMinV + localV * bufferHeightV);
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
