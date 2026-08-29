package fable.hideseek.imba.game;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FullFixPackContractTest {
    private static String read(String rel) throws Exception { return Files.readString(Path.of(rel)); }
    @Test void gameplayChangesArePresent() throws Exception {
        String fixes=read("src/main/java/fable/hideseek/imba/mixin/GameplayFixesMixin.java");
        assertTrue(fixes.contains("PORTAL_DELAY_TICKS = 75"));
        assertTrue(fixes.contains("state.buttonPressed"));
        assertTrue(fixes.contains("playsound minecraft:entity.generic.hurt player @a ~ ~ ~ 10 1"));
        assertTrue(fixes.contains("kill @e[type=item]"));
    }
    @Test void hologramContrastIsRoundTripped() throws Exception {
        assertTrue(read("src/main/java/fable/hideseek/imba/config/HologramConfig.java").contains("float contrast"));
        assertTrue(read("src/main/java/fable/hideseek/imba/net/HologramNetworking.java").contains("out.writeFloat(p.contrast)"));
        assertTrue(read("src/main/java/fable/hideseek/imba/client/HologramProjectorScreen.java").contains("Контраст:"));
        assertTrue(read("src/main/java/fable/hideseek/imba/client/ClientLocationPhotos.java").contains("brightenContrast"));
    }
    @Test void lobbyTransitionIsFadedAndSpreadIsNormalized() throws Exception {
        String mixin=read("src/main/java/fable/hideseek/imba/mixin/LobbyReturnMixin.java");
        String state=read("src/main/java/fable/hideseek/imba/client/ClientGameState.java");
        assertTrue(mixin.contains("spreadplayers -131.49 148.72 2 5 under -29 false @a"));
        assertTrue(mixin.contains("normalizeLobbySpreadPositions"));
        assertTrue(state.contains("returnBlackoutAlpha")&&state.contains("returnBlackoutTarget"));
    }
    @Test void lobbySpreadKeepsExactRequestedCommand() throws Exception {
        String mixin=read("src/main/java/fable/hideseek/imba/mixin/LobbyReturnMixin.java");
        assertTrue(mixin.contains("spreadplayers -131.49 148.72 2 5 under -29 false @a"));
        assertTrue(mixin.contains("findLobbyFloorY"));
    }
    @Test void portalSelfVisibleThirdPersonAndFixationLeavesSneak() throws Exception {
        String renderer=read("src/main/java/fable/hideseek/imba/mixin/client/PlayerRendererMixin.java");
        assertTrue(renderer.contains("MaskRenderHelper.renderMask"));
        assertTrue(read("src/main/java/fable/hideseek/imba/ImbaClient.java").contains("cp.input.sneaking=false"));
        assertTrue(read("src/main/java/fable/hideseek/imba/item/HideButtonHandler.java").contains("player.setSneaking(false)"));
    }
}
