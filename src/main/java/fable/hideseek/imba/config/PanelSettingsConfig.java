package fable.hideseek.imba.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Server-owned editable layout for the 3x3 in-world settings panel. */
public final class PanelSettingsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("imba_panel_settings.json");

    private static String timerLabel = "Таймер";
    private static String heartsLabel = "Сердца";
    private static float timerTitleScale = 1.30F;
    private static float heartsTitleScale = 1.30F;
    private static float timerValueScale = 1.60F;
    private static float heartsValueScale = 1.60F;
    private static float arrowScale = 1.45F;
    private static int timerX = -38;
    private static int heartsX = 38;
    private static int titleY = -52;
    private static int upArrowY = -26;
    private static int valueY = 0;
    private static int downArrowY = 28;

    private PanelSettingsConfig() {}

    public static void load() {
        resetDefaults(false);
        if (!Files.exists(PATH)) {
            save();
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(PATH)).getAsJsonObject();
            timerLabel = sanitizeLabel(readString(root, "timerLabel", "Таймер"), "Таймер");
            heartsLabel = sanitizeLabel(readString(root, "heartsLabel", "Сердца"), "Сердца");
            timerTitleScale = readFloat(root, "timerTitleScale", 1.30F, 0.40F, 3.00F);
            heartsTitleScale = readFloat(root, "heartsTitleScale", 1.30F, 0.40F, 3.00F);
            timerValueScale = readFloat(root, "timerValueScale", 1.60F, 0.40F, 3.00F);
            heartsValueScale = readFloat(root, "heartsValueScale", 1.60F, 0.40F, 3.00F);
            arrowScale = readFloat(root, "arrowScale", 1.45F, 0.40F, 3.00F);
            timerX = readInt(root, "timerX", -38, -120, 120);
            heartsX = readInt(root, "heartsX", 38, -120, 120);
            titleY = readInt(root, "titleY", -52, -100, 100);
            upArrowY = readInt(root, "upArrowY", -26, -100, 100);
            valueY = readInt(root, "valueY", 0, -100, 100);
            downArrowY = readInt(root, "downArrowY", 28, -100, 100);
        } catch (IOException | RuntimeException e) {
            System.err.println("[IMBA] Не удалось загрузить настройки панели: " + e.getMessage());
        }
    }

    public static String timerLabel() { return timerLabel; }
    public static String heartsLabel() { return heartsLabel; }
    public static float timerTitleScale() { return timerTitleScale; }
    public static float heartsTitleScale() { return heartsTitleScale; }
    public static float timerValueScale() { return timerValueScale; }
    public static float heartsValueScale() { return heartsValueScale; }
    public static float arrowScale() { return arrowScale; }
    public static int timerX() { return timerX; }
    public static int heartsX() { return heartsX; }
    public static int titleY() { return titleY; }
    public static int upArrowY() { return upArrowY; }
    public static int valueY() { return valueY; }
    public static int downArrowY() { return downArrowY; }

    public static void setLayout(String newTimerLabel, String newHeartsLabel,
                                 float newTimerTitleScale, float newHeartsTitleScale,
                                 float newTimerValueScale, float newHeartsValueScale,
                                 float newArrowScale, int newTimerX, int newHeartsX,
                                 int newTitleY, int newUpArrowY, int newValueY, int newDownArrowY) {
        timerLabel = sanitizeLabel(newTimerLabel, "Таймер");
        heartsLabel = sanitizeLabel(newHeartsLabel, "Сердца");
        timerTitleScale = clamp(newTimerTitleScale, 0.40F, 3.00F);
        heartsTitleScale = clamp(newHeartsTitleScale, 0.40F, 3.00F);
        timerValueScale = clamp(newTimerValueScale, 0.40F, 3.00F);
        heartsValueScale = clamp(newHeartsValueScale, 0.40F, 3.00F);
        arrowScale = clamp(newArrowScale, 0.40F, 3.00F);
        timerX = clamp(newTimerX, -120, 120);
        heartsX = clamp(newHeartsX, -120, 120);
        titleY = clamp(newTitleY, -100, 100);
        upArrowY = clamp(newUpArrowY, -100, 100);
        valueY = clamp(newValueY, -100, 100);
        downArrowY = clamp(newDownArrowY, -100, 100);
        save();
    }

    public static void resetDefaults() {
        resetDefaults(true);
    }

    private static void resetDefaults(boolean save) {
        timerLabel = "Таймер";
        heartsLabel = "Сердца";
        timerTitleScale = 1.30F;
        heartsTitleScale = 1.30F;
        timerValueScale = 1.60F;
        heartsValueScale = 1.60F;
        arrowScale = 1.45F;
        timerX = -38;
        heartsX = 38;
        titleY = -52;
        upArrowY = -26;
        valueY = 0;
        downArrowY = 28;
        if (save) save();
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Path tmp = PATH.resolveSibling(PATH.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(new Data(timerLabel, heartsLabel,
                    timerTitleScale, heartsTitleScale, timerValueScale, heartsValueScale,
                    arrowScale, timerX, heartsX, titleY, upArrowY, valueY, downArrowY)));
            Files.move(tmp, PATH, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("[IMBA] Не удалось сохранить настройки панели: " + e.getMessage());
        }
    }

    private static String sanitizeLabel(String value, String fallback) {
        String clean = value == null ? "" : value.replaceAll("§.", "").trim().replaceAll("\\s+", " ");
        if (clean.isEmpty()) clean = fallback;
        return clean.length() > 24 ? clean.substring(0, 24) : clean;
    }

    private static String readString(JsonObject root, String key, String fallback) {
        return root.has(key) && !root.get(key).isJsonNull() ? root.get(key).getAsString() : fallback;
    }

    private static float readFloat(JsonObject root, String key, float fallback, float min, float max) {
        try { return root.has(key) ? clamp(root.get(key).getAsFloat(), min, max) : fallback; }
        catch (RuntimeException e) { return fallback; }
    }

    private static int readInt(JsonObject root, String key, int fallback, int min, int max) {
        try { return root.has(key) ? clamp(root.get(key).getAsInt(), min, max) : fallback; }
        catch (RuntimeException e) { return fallback; }
    }

    private static float clamp(float value, float min, float max) {
        if (!Float.isFinite(value)) return min;
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Data(String timerLabel, String heartsLabel,
                        float timerTitleScale, float heartsTitleScale,
                        float timerValueScale, float heartsValueScale,
                        float arrowScale, int timerX, int heartsX,
                        int titleY, int upArrowY, int valueY, int downArrowY) {}
}
