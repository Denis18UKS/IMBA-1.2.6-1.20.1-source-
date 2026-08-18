package fable.hideseek.imba.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** Server-owned click rectangles for the four arrows on the in-world 3x3 settings panel. */
public final class PanelHitboxConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("imba_panel_hitboxes.json");

    public enum Arrow {
        TIMER_UP("Время +", -38, -26),
        TIMER_DOWN("Время -", -38, 28),
        HEARTS_UP("Количество +", 38, -26),
        HEARTS_DOWN("Количество -", 38, 28);

        public final String label;
        public final int defaultX;
        public final int defaultY;

        Arrow(String label, int defaultX, int defaultY) {
            this.label = label;
            this.defaultX = defaultX;
            this.defaultY = defaultY;
        }
    }

    public static final class Rect {
        public int x;
        public int y;
        public int width;
        public int height;

        public Rect() {
        }

        public Rect(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public Rect copy() {
            return new Rect(x, y, width, height);
        }

        public boolean contains(double px, double py) {
            return px >= x - width / 2.0D && px <= x + width / 2.0D
                    && py >= y - height / 2.0D && py <= y + height / 2.0D;
        }
    }

    private static final EnumMap<Arrow, Rect> RECTS = new EnumMap<>(Arrow.class);

    private PanelHitboxConfig() {
    }

    public static void load() {
        resetDefaults(false);
        if (!Files.exists(PATH)) {
            save();
            return;
        }
        try {
            Data data = GSON.fromJson(Files.readString(PATH), Data.class);
            if (data == null || data.rects == null) {
                save();
                return;
            }
            for (Arrow arrow : Arrow.values()) {
                Rect loaded = data.rects.get(arrow.name());
                if (loaded != null) {
                    RECTS.put(arrow, sanitize(loaded));
                }
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("[IMBA] Не удалось загрузить хитбоксы панели: " + e.getMessage());
        }
    }

    public static Rect get(Arrow arrow) {
        Rect rect = RECTS.get(arrow);
        return rect == null ? defaultRect(arrow) : rect.copy();
    }

    public static Map<Arrow, Rect> snapshot() {
        EnumMap<Arrow, Rect> result = new EnumMap<>(Arrow.class);
        for (Arrow arrow : Arrow.values()) {
            result.put(arrow, get(arrow));
        }
        return result;
    }

    public static Arrow hit(double panelX, double panelY) {
        for (Arrow arrow : Arrow.values()) {
            Rect rect = RECTS.get(arrow);
            if (rect != null && rect.contains(panelX, panelY)) {
                return arrow;
            }
        }
        return null;
    }

    public static void set(Arrow arrow, Rect rect) {
        if (arrow == null || rect == null) return;
        RECTS.put(arrow, sanitize(rect));
        save();
    }

    public static void setAll(Map<Arrow, Rect> values) {
        if (values == null) return;
        for (Arrow arrow : Arrow.values()) {
            Rect value = values.get(arrow);
            if (value != null) RECTS.put(arrow, sanitize(value));
        }
        save();
    }

    public static void resetDefaults() {
        resetDefaults(true);
    }

    private static void resetDefaults(boolean save) {
        RECTS.clear();
        for (Arrow arrow : Arrow.values()) {
            RECTS.put(arrow, defaultRect(arrow));
        }
        if (save) save();
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Map<String, Rect> serializable = new HashMap<>();
            for (Arrow arrow : Arrow.values()) {
                serializable.put(arrow.name(), get(arrow));
            }
            Path tmp = PATH.resolveSibling(PATH.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(new Data(serializable)));
            Files.move(tmp, PATH, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("[IMBA] Не удалось сохранить хитбоксы панели: " + e.getMessage());
        }
    }

    private static Rect defaultRect(Arrow arrow) {
        return new Rect(arrow.defaultX, arrow.defaultY, 26, 22);
    }

    private static Rect sanitize(Rect value) {
        int width = clamp(value.width, 6, 60);
        int height = clamp(value.height, 6, 60);
        int x = clamp(value.x, -70, 70);
        int y = clamp(value.y, -70, 70);
        return new Rect(x, y, width, height);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class Data {
        Map<String, Rect> rects = new HashMap<>();

        Data() {
        }

        Data(Map<String, Rect> rects) {
            this.rects = rects;
        }
    }
}
