package fable.hideseek.imba.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Optional per-block fine tuning for statue auto-position.
 *
 * This is deliberately separate from MaskBlockConfig. The normal positioning
 * remains untouched unless a block has an explicit entry here.
 *
 * Values are stored in Minecraft model pixels: 16 px = 1 block.
 * This config never changes MaskHitbox/EntityDimensions.
 */
public final class MaskAutoPositionConfig {
    public static final int MAX_ABS_PIXELS = 64;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("imba_mask_autoposition.json");

    public static Data DATA = new Data();

    private MaskAutoPositionConfig() {
    }

    public static void load() {
        if (!Files.exists(PATH)) {
            save();
            return;
        }

        try {
            DATA = GSON.fromJson(Files.readString(PATH), Data.class);
            if (DATA == null) {
                DATA = new Data();
            }
            if (DATA.offsets == null) {
                DATA.offsets = new HashMap<>();
            }
            DATA.offsets.entrySet().removeIf(entry ->
                    Identifier.tryParse(entry.getKey()) == null || entry.getValue() == null);
        } catch (IOException | RuntimeException ignored) {
            DATA = new Data();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(DATA));
        } catch (IOException ignored) {
        }
    }

    public static Offset offsetFor(Block block) {
        if (block == null) {
            return Offset.ZERO;
        }

        Identifier id = Registries.BLOCK.getId(block);
        if (id == null) {
            return Offset.ZERO;
        }

        Offset value = DATA.offsets.get(id.toString());
        return value == null ? Offset.ZERO : value.copy();
    }

    public static void setOffset(Identifier id, int xPixels, int yPixels, int zPixels) {
        if (id == null) {
            return;
        }

        int x = clamp(xPixels);
        int y = clamp(yPixels);
        int z = clamp(zPixels);

        if (x == 0 && y == 0 && z == 0) {
            DATA.offsets.remove(id.toString());
        } else {
            DATA.offsets.put(id.toString(), new Offset(x, y, z));
        }
        save();
    }

    public static Map<String, Offset> snapshot() {
        Map<String, Offset> copy = new HashMap<>();
        DATA.offsets.forEach((id, offset) -> copy.put(id, offset.copy()));
        return copy;
    }

    private static int clamp(int value) {
        return Math.max(-MAX_ABS_PIXELS, Math.min(MAX_ABS_PIXELS, value));
    }

    public static final class Data {
        public Map<String, Offset> offsets = new HashMap<>();
    }

    public static final class Offset {
        public static final Offset ZERO = new Offset(0, 0, 0);

        public int xPixels;
        public int yPixels;
        public int zPixels;

        public Offset() {
        }

        public Offset(int xPixels, int yPixels, int zPixels) {
            this.xPixels = xPixels;
            this.yPixels = yPixels;
            this.zPixels = zPixels;
        }

        public boolean isZero() {
            return xPixels == 0 && yPixels == 0 && zPixels == 0;
        }

        public Offset copy() {
            return new Offset(xPixels, yPixels, zPixels);
        }
    }
}
