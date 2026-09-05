package fable.hideseek.imba.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ReturnTimingConfig {
    public static final int PRE_FADE_DEFAULT_TICKS = 40;
    public static final int PRE_TELEPORT_DEFAULT_TICKS = 40;
    private static final int MAX_TICKS = 20 * 60;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("imba_return_timing.json");

    private static int preFadeTicks = PRE_FADE_DEFAULT_TICKS;
    private static int preTeleportTicks = PRE_TELEPORT_DEFAULT_TICKS;

    private ReturnTimingConfig() {
    }

    public static synchronized void load() {
        preFadeTicks = PRE_FADE_DEFAULT_TICKS;
        preTeleportTicks = PRE_TELEPORT_DEFAULT_TICKS;
        if (!Files.exists(PATH)) {
            save();
            return;
        }
        try {
            Data data = GSON.fromJson(Files.readString(PATH), Data.class);
            if (data != null) {
                preFadeTicks = clamp(data.preFadeTicks);
                preTeleportTicks = clamp(data.preTeleportTicks);
            }
        } catch (IOException | RuntimeException exception) {
            System.err.println("[IMBA] Не удалось загрузить настройки возврата: " + exception.getMessage());
        }
    }

    public static synchronized int preFadeTicks() {
        return preFadeTicks;
    }

    public static synchronized int preTeleportTicks() {
        return preTeleportTicks;
    }

    public static synchronized void set(int preFade, int preTeleport) {
        preFadeTicks = clamp(preFade);
        preTeleportTicks = clamp(preTeleport);
        save();
    }

    public static synchronized void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Path tmp = PATH.resolveSibling(PATH.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(new Data(preFadeTicks, preTeleportTicks)));
            Files.move(tmp, PATH, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            System.err.println("[IMBA] Не удалось сохранить настройки возврата: " + exception.getMessage());
        }
    }

    private static int clamp(int ticks) {
        return Math.max(0, Math.min(MAX_TICKS, ticks));
    }

    private static final class Data {
        int preFadeTicks = PRE_FADE_DEFAULT_TICKS;
        int preTeleportTicks = PRE_TELEPORT_DEFAULT_TICKS;

        Data() {
        }

        Data(int preFadeTicks, int preTeleportTicks) {
            this.preFadeTicks = preFadeTicks;
            this.preTeleportTicks = preTeleportTicks;
        }
    }
}
