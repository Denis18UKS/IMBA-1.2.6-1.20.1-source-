package fable.hideseek.imba.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Server-owned editable labels for the in-world settings panel. */
public final class PanelSettingsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("imba_panel_settings.json");
    private static String heartsLabel = "Сердца";

    private PanelSettingsConfig() {}

    public static void load() {
        heartsLabel = "Сердца";
        if (!Files.exists(PATH)) {
            save();
            return;
        }
        try {
            Data data = GSON.fromJson(Files.readString(PATH), Data.class);
            if (data != null) heartsLabel = sanitize(data.heartsLabel);
        } catch (IOException | RuntimeException e) {
            System.err.println("[IMBA] Не удалось загрузить подпись панели: " + e.getMessage());
        }
    }

    public static String heartsLabel() {
        return heartsLabel;
    }

    public static void setHeartsLabel(String value) {
        heartsLabel = sanitize(value);
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Path tmp = PATH.resolveSibling(PATH.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(new Data(heartsLabel)));
            Files.move(tmp, PATH, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("[IMBA] Не удалось сохранить подпись панели: " + e.getMessage());
        }
    }

    private static String sanitize(String value) {
        String clean = value == null ? "" : value.replaceAll("§.", "").trim().replaceAll("\\s+", " ");
        if (clean.isEmpty()) clean = "Сердца";
        return clean.length() > 24 ? clean.substring(0, 24) : clean;
    }

    private record Data(String heartsLabel) {}
}
