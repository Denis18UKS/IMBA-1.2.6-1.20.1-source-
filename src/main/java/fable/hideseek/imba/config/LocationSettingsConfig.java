package fable.hideseek.imba.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fable.hideseek.imba.game.GameConfig;
import fable.hideseek.imba.game.RoundDefinition;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Persistent names and mask assignments edited in the location gallery. */
public final class LocationSettingsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH =
            FabricLoader.getInstance().getConfigDir().resolve("imba_locations.json");

    private LocationSettingsConfig() {}

    public static void load() {
        if (!Files.exists(PATH)) {
            save();
            return;
        }
        try {
            Data data = GSON.fromJson(Files.readString(PATH), Data.class);
            if (data == null || data.locations == null) {
                save();
                return;
            }
            for (int i = 0; i < Math.min(data.locations.size(), GameConfig.ROUNDS.size()); i++) {
                Location entry = data.locations.get(i);
                if (entry == null || entry.maskId == null || entry.sourceKind == null) {
                    continue;
                }
                try {
                    RoundDefinition.SourceKind kind =
                            RoundDefinition.SourceKind.valueOf(entry.sourceKind);
                    Identifier maskId = new Identifier(entry.maskId);
                    /*
                     * Migrate the old built-in Snow Street assignment. Without
                     * this, an existing imba_locations.json would overwrite the
                     * new hanging-lantern default on every launch.
                     */
                    if (i == 4
                            && kind == RoundDefinition.SourceKind.BLOCK
                            && maskId.equals(new Identifier("minecraft", "lantern"))) {
                        maskId = new Identifier("imba", "hanging_lantern");
                    }
                    GameConfig.setLocationSettings(i, entry.name, kind, maskId);
                } catch (IllegalArgumentException ignored) {
                    // Keep the safe built-in definition for malformed entries.
                }
            }
            save();
        } catch (IOException | RuntimeException e) {
            System.err.println("[IMBA] Не удалось прочитать настройки локаций: " + e.getMessage());
        }
    }

    public static void save() {
        List<Location> locations = new ArrayList<>();
        for (RoundDefinition round : GameConfig.ROUNDS) {
            locations.add(new Location(
                    round.locationName,
                    round.sourceKind.name(),
                    round.maskId.toString()));
        }
        try {
            Files.createDirectories(PATH.getParent());
            Path temporary = PATH.resolveSibling(PATH.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(new Data(locations)));
            Files.move(temporary, PATH, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("[IMBA] Не удалось сохранить настройки локаций: " + e.getMessage());
        }
    }

    private record Data(List<Location> locations) {}
    private record Location(String name, String sourceKind, String maskId) {}
}
