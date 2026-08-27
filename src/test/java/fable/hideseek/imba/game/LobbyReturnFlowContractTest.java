package fable.hideseek.imba.game;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LobbyReturnFlowContractTest {

    private static String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }

    private static String readOrEmpty(String relativePath) throws IOException {
        Path path = Path.of(relativePath);
        return Files.exists(path) ? Files.readString(path) : "";
    }

    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, "Method not found: " + signature);
        int next = source.indexOf("\n    private static ", start + signature.length());
        return next < 0 ? source.substring(start) : source.substring(start, next);
    }

    @Test
    void returnBlackoutStartsBeforeLobbyTransitionAndEndsAfterSpread() throws Exception {
        String manager = read("src/main/java/fable/hideseek/imba/game/GameManager.java");
        String finishReturn = methodBody(manager, "private static void finishReturn");
        String returnMixin = readOrEmpty("src/main/java/fable/hideseek/imba/mixin/LobbyReturnMixin.java");

        assertTrue(returnMixin.contains("method = \"beginReturn\"")
                        && returnMixin.contains("at = @At(\"HEAD\")")
                        && returnMixin.contains("LobbyReturnNetworking.broadcastReturnBlackout(server, true);"),
                "Return phase must enable full-screen blackout before the lobby transition");

        assertTrue(finishReturn.contains("teleportToLobby(player);"),
                "finishReturn must perform the normal lobby teleport");
        assertTrue(returnMixin.contains("method = \"finishReturn\"")
                        && returnMixin.contains("at = @At(\"TAIL\")"),
                "spreadplayers must run only after finishReturn has completed the lobby teleports");

        int spread = returnMixin.indexOf("spreadplayers -131.49 148.72 2 5 under -29 false @a");
        int blackoutOff = returnMixin.indexOf("LobbyReturnNetworking.broadcastReturnBlackout(server, false);");
        assertTrue(spread >= 0, "Lobby return must execute the configured spreadplayers command");
        assertTrue(blackoutOff > spread, "blackout must be removed only after spreadplayers finishes");
    }

    @Test
    void blackoutHasServerPacketAndClientReceiver() throws Exception {
        String serverNetworking = readOrEmpty(
                "src/main/java/fable/hideseek/imba/net/LobbyReturnNetworking.java");
        String clientNetworking = readOrEmpty(
                "src/main/java/fable/hideseek/imba/net/LobbyReturnClientNetworking.java");

        assertTrue(serverNetworking.contains("return_blackout"),
                "Server networking must define a dedicated return blackout packet");
        assertTrue(serverNetworking.contains("broadcastReturnBlackout"),
                "Server networking must be able to broadcast blackout state");
        assertTrue(clientNetworking.contains("RETURN_BLACKOUT_PACKET"),
                "Client networking must receive the blackout packet");
        assertTrue(clientNetworking.contains("ClientGameState.returnBlackout"),
                "Client receiver must store blackout state");
    }

    @Test
    void blackoutRendersAsOpaqueFullscreenOverlay() throws Exception {
        String state = read("src/main/java/fable/hideseek/imba/client/ClientGameState.java");
        String hud = read("src/main/java/fable/hideseek/imba/mixin/client/InGameHudMixin.java");

        assertTrue(state.contains("returnBlackout"),
                "Client game state must expose return blackout state");
        assertTrue(hud.contains("ClientGameState.returnBlackout"),
                "HUD must render conditionally from return blackout state");
        assertTrue(hud.contains("0xFF000000"),
                "Return blackout must be fully opaque black");
    }
}
