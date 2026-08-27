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

    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, "Method not found: " + signature);
        int next = source.indexOf("\n    private static ", start + signature.length());
        return next < 0 ? source.substring(start) : source.substring(start, next);
    }

    @Test
    void returnBlackoutStartsBeforeLobbyTransitionAndEndsAfterSpread() throws Exception {
        String source = read("src/main/java/fable/hideseek/imba/game/GameManager.java");
        String beginReturn = methodBody(source, "private static void beginReturn");
        String finishReturn = methodBody(source, "private static void finishReturn");

        assertTrue(beginReturn.contains("MaskNetworking.broadcastReturnBlackout(server, true);"),
                "Return phase must enable the full-screen blackout before lobby transition");

        int teleport = finishReturn.indexOf("teleportToLobby(player);");
        int spread = finishReturn.indexOf("spreadplayers -131.49 148.72 2 5 under -29 false @a");
        int blackoutOff = finishReturn.indexOf("MaskNetworking.broadcastReturnBlackout(server, false);");

        assertTrue(teleport >= 0, "finishReturn must teleport players to the lobby first");
        assertTrue(spread > teleport, "spreadplayers must run after the lobby teleport");
        assertTrue(blackoutOff > spread, "blackout must be removed only after spreadplayers finishes");
    }

    @Test
    void blackoutHasServerPacketAndClientReceiver() throws Exception {
        String serverNetworking = read("src/main/java/fable/hideseek/imba/net/MaskNetworking.java");
        String clientNetworking = read("src/main/java/fable/hideseek/imba/net/MaskClientNetworking.java");

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
