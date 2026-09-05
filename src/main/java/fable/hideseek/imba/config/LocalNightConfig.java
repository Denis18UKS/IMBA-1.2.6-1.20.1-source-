package fable.hideseek.imba.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fable.hideseek.imba.game.GameConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Server-owned choice of the one map location that should look like night. */
public final class LocalNightConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("imba_local_night.json");
    private static final double DEFAULT_RADIUS = 128.0D;
    private static Data data = new Data(defaultLocation(), DEFAULT_RADIUS);

    private LocalNightConfig() {}

    public static void load() {
        data = new Data(defaultLocation(), DEFAULT_RADIUS);
        if (!Files.exists(PATH)) {
            save();
            return;
        }
        try {
            Data loaded = GSON.fromJson(Files.readString(PATH), Data.class);
            if (loaded != null) data = loaded;
        } catch (IOException | RuntimeException e) {
            System.err.println("[IMBA] Не удалось загрузить локальную ночь: " + e.getMessage());
        }
        data.selectedLocation = sanitizeLocation(data.selectedLocation);
        if (!Double.isFinite(data.radius) || data.radius < 16.0D || data.radius > 512.0D) {
            data.radius = DEFAULT_RADIUS;
        }
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Path tmp = PATH.resolveSibling(PATH.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(data));
            Files.move(tmp, PATH, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("[IMBA] Не удалось сохранить локальную ночь: " + e.getMessage());
        }
    }

    public static int selectedLocation() {
        data.selectedLocation = sanitizeLocation(data.selectedLocation);
        return data.selectedLocation;
    }

    public static double radius() {
        return data.radius;
    }

    public static void setSelectedLocation(int location) {
        data.selectedLocation = sanitizeLocation(location);
        save();
    }

    private static int sanitizeLocation(int location) {
        if (location == -1) return -1;
        return location >= 0 && location < GameConfig.ROUNDS.size() ? location : defaultLocation();
    }

    private static int defaultLocation() {
        return GameConfig.ROUNDS.size() > 4 ? 4 : (GameConfig.ROUNDS.isEmpty() ? -1 : 0);
    }

    private static final class Data {
        int selectedLocation;
        double radius;

        Data(int selectedLocation, double radius) {
            this.selectedLocation = selectedLocation;
            this.radius = radius;
        }
    }
}
