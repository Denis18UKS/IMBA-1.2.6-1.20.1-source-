package fable.hideseek.imba.client;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public final class PortalMaskAnimationClock {
    private static final Identifier PORTAL_SPRITE = new Identifier("minecraft", "block/nether_portal");
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

    /**
     * Render exactly the vanilla minecraft:nether_portal baked model.  The old
     * implementation sampled textures/block/nether_portal.png as a raw entity
     * texture, which exposed the vertical animation sheet as stripes and lost
     * the vanilla 4/16-thick portal geometry.
     *
     * The visual pause is applied by temporarily selecting the requested frame
     * in the block-atlas portal sprite while the vanilla block model is drawn.
     * PortalMaskSpriteFrame supplies/restores the atlas frame around this call.
     */
    public static void renderPortalMaskFrame(MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        MinecraftClient client = MinecraftClient.getInstance();
        SpriteAtlasTexture atlas = client.getBakedModelManager().getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
        Sprite sprite = atlas.getSprite(PORTAL_SPRITE);
        int frame = frameIndex();

        PortalMaskSpriteFrame.withFrame(sprite, frame, () ->
                client.getBlockRenderManager().renderBlockAsEntity(
                        Blocks.NETHER_PORTAL.getDefaultState(),
                        matrices,
                        consumers,
                        light,
                        OverlayTexture.DEFAULT_UV));
    }
}
