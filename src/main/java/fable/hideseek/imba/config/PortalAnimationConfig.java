package fable.hideseek.imba.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class PortalAnimationConfig {
    public static final int DEFAULT_FREEZE_TICKS = 10;
    private static final int MAX_FREEZE_TICKS = 20 * 30;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("imba_portal_animation.json");

    private static int freezeTicks = DEFAULT_FREEZE_TICKS;

    private PortalAnimationConfig() {
    }

    public static synchronized void load() {
        freezeTicks = DEFAULT_FREEZE_TICKS;
        if (!Files.exists(PATH)) {
            save();
            return;
        }
        try {
            Data data = GSON.fromJson(Files.readString(PATH), Data.class);
            if (data != null) {
                freezeTicks = clamp(data.freezeTicks);
            }
        } catch (IOException | RuntimeException exception) {
            System.err.println("[IMBA] Не удалось загрузить задержку анимации портала: " + exception.getMessage());
        }
    }

    public static synchronized int freezeTicks() {
        return freezeTicks;
    }

    public static synchronized void setFreezeTicks(int ticks) {
        freezeTicks = clamp(ticks);
        save();
    }

    public static synchronized void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Path tmp = PATH.resolveSibling(PATH.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(new Data(freezeTicks)));
            Files.move(tmp, PATH, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            System.err.println("[IMBA] Не удалось сохранить задержку анимации портала: " + exception.getMessage());
        }
    }

    private static int clamp(int ticks) {
        return Math.max(0, Math.min(MAX_FREEZE_TICKS, ticks));
    }

    private static final class Data {
        int freezeTicks = DEFAULT_FREEZE_TICKS;

        Data() {
        }

        Data(int freezeTicks) {
            this.freezeTicks = freezeTicks;
        }
    }
}
