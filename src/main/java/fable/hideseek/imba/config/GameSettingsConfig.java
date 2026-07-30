package fable.hideseek.imba.config;
import com.google.gson.*;
import fable.hideseek.imba.game.GameConfig;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.*;
public final class GameSettingsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("imba_game_settings.json");
    private GameSettingsConfig() {}
    public static void load() {
        try {
            if (!Files.exists(PATH)) { save(); return; }
            Data d = GSON.fromJson(Files.readString(PATH), Data.class);
            if (d != null) {
                GameConfig.setRoundSeconds(d.roundSeconds);
                GameConfig.setSeekerHearts(d.seekerHearts);
                GameConfig.setSelectedLocation(d.selectedLocation);
                GameConfig.setShowGameplayMessages(d.showGameplayMessages);
            }
        } catch (IOException | RuntimeException ignored) {}
    }
    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(new Data(
                    GameConfig.ROUND_SECONDS,
                    GameConfig.SEEKER_HEARTS,
                    GameConfig.SELECTED_LOCATION,
                    GameConfig.SHOW_GAMEPLAY_MESSAGES)));
        } catch (IOException ignored) {}
    }
    private record Data(
            int roundSeconds,
            int seekerHearts,
            int selectedLocation,
            boolean showGameplayMessages) {}
}
