package fable.hideseek.imba.game;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerPortalVisibilityContractTest {
    private static String read(String rel) throws Exception {
        return Files.readString(Path.of(rel));
    }

    @Test
    void portalMaskIsDeferredUntilWorldRenderTailSoPlayersBehindRemainVisible() throws Exception {
        String playerRenderer = read(
                "src/main/java/fable/hideseek/imba/mixin/client/PlayerRendererMixin.java");
        String worldRenderer = read(
                "src/main/java/fable/hideseek/imba/mixin/client/WorldRendererMixin.java");
        String maskHelper = read(
                "src/main/java/fable/hideseek/imba/client/MaskRenderHelper.java");

        assertTrue(playerRenderer.contains("MaskType.PORTAL"));
        assertTrue(playerRenderer.contains("Deferred to WorldRenderer TAIL"));
        assertTrue(worldRenderer.contains("renderPortalMasksLate"));
        assertTrue(worldRenderer.contains("MaskType.PORTAL"));
        assertTrue(worldRenderer.contains("client.world.getPlayers()"));
        assertTrue(worldRenderer.contains("vertexConsumers.draw()"));

        // The visual model stays the already-working vanilla block model path.
        assertTrue(maskHelper.contains("Blocks.NETHER_PORTAL.getDefaultState()"));
        assertTrue(maskHelper.contains(
                "renderBlock(matrices, consumers, light, Blocks.NETHER_PORTAL.getDefaultState())"));
    }
}
