package fable.hideseek.imba.game;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhotoHologramAndLocalNightContractTest {
    private static String read(String rel) throws Exception {
        return Files.readString(Path.of(rel));
    }

    @Test
    void hologramLibraryProvidesSearchSelectionAndLiveScale() throws Exception {
        Path libraryPath = Path.of("src/main/java/fable/hideseek/imba/client/HologramLibraryScreen.java");
        assertTrue(Files.exists(libraryPath), "photo-hologram library screen must exist");
        String library = Files.readString(libraryPath);
        String clientNet = read("src/main/java/fable/hideseek/imba/client/HologramClientNetworking.java");
        String clientInit = read("src/main/java/fable/hideseek/imba/ImbaClientExtension.java");

        assertTrue(library.contains("TextFieldWidget"));
        assertTrue(library.contains("setChangedListener"));
        assertTrue(library.contains("PanelData.locationName"));
        assertTrue(library.contains("HologramNetworking.SAVE"));
        assertTrue(library.contains("changeScale"));
        assertTrue(clientNet.contains("HologramLibraryScreen"));
        assertTrue(clientInit.contains("new HologramLibraryScreen()"));
    }

    @Test
    void hologramEditorCanOpenTheExactLibrarySelection() throws Exception {
        String editor = read("src/main/java/fable/hideseek/imba/client/HologramProjectorScreen.java");
        assertTrue(editor.contains("requestedProjectorId"));
        assertTrue(editor.contains("findProjectorIndex"));
        assertTrue(editor.contains("new HologramLibraryScreen()"));
    }

    @Test
    void localNightUsesDedicatedToolAndNeverChangesGlobalWorldTime() throws Exception {
        Path configPath = Path.of("src/main/java/fable/hideseek/imba/config/LocalNightConfig.java");
        Path serverNetPath = Path.of("src/main/java/fable/hideseek/imba/net/LocalNightNetworking.java");
        Path clientDataPath = Path.of("src/main/java/fable/hideseek/imba/client/LocalNightClientData.java");
        Path screenPath = Path.of("src/main/java/fable/hideseek/imba/client/LocalNightScreen.java");
        Path mixinPath = Path.of("src/main/java/fable/hideseek/imba/mixin/client/LocalNightWorldMixin.java");
        assertTrue(Files.exists(configPath), "local-night config must exist");
        assertTrue(Files.exists(serverNetPath), "local-night networking must exist");
        assertTrue(Files.exists(clientDataPath), "local-night client data must exist");
        assertTrue(Files.exists(screenPath), "local-night tool screen must exist");
        assertTrue(Files.exists(mixinPath), "local-night time mixin must exist");

        String extension = read("src/main/java/fable/hideseek/imba/ImbaExtension.java");
        String clientExtension = read("src/main/java/fable/hideseek/imba/ImbaClientExtension.java");
        String mixins = read("src/main/resources/imba.mixins.json");
        String networking = Files.readString(serverNetPath);
        String clientData = Files.readString(clientDataPath);
        String mixin = Files.readString(mixinPath);

        assertTrue(extension.contains("LOCAL_NIGHT_TOOL"));
        assertTrue(extension.contains("LocalNightConfig.load()"));
        assertTrue(extension.contains("LocalNightNetworking.register()"));
        assertTrue(clientExtension.contains("new LocalNightScreen()"));
        assertTrue(networking.contains("GameConfig.ROUNDS"));
        assertTrue(clientData.contains("nearestLocation"));
        assertTrue(mixin.contains("getTimeOfDay"));
        assertTrue(mixins.contains("client.LocalNightWorldMixin"));
        assertFalse(networking.contains("setTimeOfDay"));
        assertFalse(clientData.contains("setTimeOfDay"));
    }
}
