package fable.hideseek.imba.game;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HologramTextSnowStreetContractTest {
    private static String read(String rel) throws Exception {
        return Files.readString(Path.of(rel));
    }

    @Test
    void dedicatedTextToolEditsOnlyTextScaleAndManualLineBreak() throws Exception {
        String extension = read("src/main/java/fable/hideseek/imba/ImbaExtension.java");
        String clientExtension = read("src/main/java/fable/hideseek/imba/ImbaClientExtension.java");
        String config = read("src/main/java/fable/hideseek/imba/config/HologramConfig.java");
        String networking = read("src/main/java/fable/hideseek/imba/net/HologramNetworking.java");
        String clientData = read("src/main/java/fable/hideseek/imba/client/HologramClientData.java");
        String renderer = read("src/main/java/fable/hideseek/imba/client/LocationHologramRenderer.java");

        Path library = Path.of("src/main/java/fable/hideseek/imba/client/HologramTextLibraryScreen.java");
        Path settings = Path.of("src/main/java/fable/hideseek/imba/client/HologramTextSettingsScreen.java");
        assertTrue(Files.exists(library));
        assertTrue(Files.exists(settings));

        String librarySource = Files.readString(library);
        String settingsSource = Files.readString(settings);

        assertTrue(extension.contains("HOLOGRAM_TEXT_TOOL"));
        assertTrue(extension.contains("hologram_text_tool"));
        assertTrue(clientExtension.contains("new HologramTextLibraryScreen()"));
        assertTrue(librarySource.contains("TextFieldWidget"));
        assertTrue(librarySource.contains("PanelData.locationName"));
        assertTrue(settingsSource.contains("changeTextScale"));
        assertTrue(settingsSource.contains("changeTitleBreak"));

        assertTrue(config.contains("textScale"));
        assertTrue(config.contains("titleBreak"));
        assertTrue(networking.contains("writeFloat(p.textScale)"));
        assertTrue(networking.contains("writeInt(p.titleBreak)"));
        assertTrue(clientData.contains("float textScale"));
        assertTrue(clientData.contains("int titleBreak"));
        assertTrue(renderer.contains("projector.textScale()"));
        assertTrue(renderer.contains("projector.titleBreak()"));
        assertTrue(renderer.contains("splitTitle"));
    }

    @Test
    void onlySnowVillageStreetCanEverRenderAsNight() throws Exception {
        String client = read("src/main/java/fable/hideseek/imba/client/LocalNightClientData.java");
        String server = read("src/main/java/fable/hideseek/imba/net/LocalNightNetworking.java");
        assertTrue(client.contains("Улица снежной деревни"));
        assertTrue(client.contains("isSnowVillageStreet"));
        assertTrue(client.contains("PanelData.locationName(nearest)"));
        assertTrue(server.contains("isSnowVillageStreet"));
        assertTrue(server.contains("GameConfig.getLocationName(location)"));
    }

    @Test
    void preservesGlowberryTextureFromAttachedV24Jar() throws Exception {
        byte[] bytes = Files.readAllBytes(Path.of("src/main/resources/assets/imba/textures/block/glowberry.png"));
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) hex.append(String.format("%02x", b));
        assertEquals("ff8bccc5eed9bd54ab936f5d9bbf0fef3d878071cc2adfa17ff49c0b36386244", hex.toString());
    }
}
