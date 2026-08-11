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
import java.util.Iterator;
import java.util.Map;

/**
 * Optional fine tuning for statue auto-position by exact pair:
 * mask block + support block below the player.
 *
 * Example:
 * minecraft:attached_pumpkin_stem + minecraft:farmland -> Y +2 px.
 *
 * The normal auto-positioning runs first. This config is only an additive
 * correction after that and never changes MaskHitbox/EntityDimensions.
 * Values are Minecraft model pixels: 16 px = 1 block.
 */
public final class MaskAutoPositionConfig {
    public static final int MAX_ABS_PIXELS = 64;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("imba_mask_autoposition_pairs.json");

    public static Data DATA = new Data();

    private MaskAutoPositionConfig() {
    }

    public static void load() {
        if (!Files.exists(PATH)) {
            DATA = new Data();
            save();
            return;
        }

        try {
            DATA = GSON.fromJson(Files.readString(PATH), Data.class);
            if (DATA == null) {
                DATA = new Data();
            }
            if (DATA.pairs == null) {
                DATA.pairs = new HashMap<>();
            }
            sanitize();
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

    public static Offset offsetFor(Block maskBlock, Block supportBlock) {
        if (maskBlock == null || supportBlock == null) {
            return Offset.ZERO;
        }

        Identifier maskId = Registries.BLOCK.getId(maskBlock);
        Identifier supportId = Registries.BLOCK.getId(supportBlock);
        if (maskId == null || supportId == null) {
            return Offset.ZERO;
        }

        Map<String, Offset> supports = DATA.pairs.get(maskId.toString());
        if (supports == null) {
            return Offset.ZERO;
        }

        Offset value = supports.get(supportId.toString());
        return value == null ? Offset.ZERO : value.copy();
    }

    public static Offset offsetFor(Identifier maskId, Identifier supportId) {
        if (maskId == null || supportId == null) {
            return Offset.ZERO;
        }
        Map<String, Offset> supports = DATA.pairs.get(maskId.toString());
        if (supports == null) {
            return Offset.ZERO;
        }
        Offset value = supports.get(supportId.toString());
        return value == null ? Offset.ZERO : value.copy();
    }

    public static void setOffset(Identifier maskId, Identifier supportId,
            int xPixels, int yPixels, int zPixels) {
        if (maskId == null || supportId == null) {
            return;
        }

        int x = clamp(xPixels);
        int y = clamp(yPixels);
        int z = clamp(zPixels);
        String maskKey = maskId.toString();
        String supportKey = supportId.toString();

        if (x == 0 && y == 0 && z == 0) {
            Map<String, Offset> supports = DATA.pairs.get(maskKey);
            if (supports != null) {
                supports.remove(supportKey);
                if (supports.isEmpty()) {
                    DATA.pairs.remove(maskKey);
                }
            }
        } else {
            DATA.pairs.computeIfAbsent(maskKey, ignored -> new HashMap<>())
                    .put(supportKey, new Offset(x, y, z));
        }
        save();
    }

    public static Map<String, Map<String, Offset>> snapshot() {
        Map<String, Map<String, Offset>> result = new HashMap<>();
        DATA.pairs.forEach((maskId, supports) -> {
            Map<String, Offset> supportCopy = new HashMap<>();
            supports.forEach((supportId, offset) -> supportCopy.put(supportId, offset.copy()));
            result.put(maskId, supportCopy);
        });
        return result;
    }

    private static void sanitize() {
        Iterator<Map.Entry<String, Map<String, Offset>>> maskIterator = DATA.pairs.entrySet().iterator();
        while (maskIterator.hasNext()) {
            Map.Entry<String, Map<String, Offset>> maskEntry = maskIterator.next();
            Identifier maskId = Identifier.tryParse(maskEntry.getKey());
            Map<String, Offset> supports = maskEntry.getValue();
            if (maskId == null || !Registries.BLOCK.containsId(maskId) || supports == null) {
                maskIterator.remove();
                continue;
            }

            supports.entrySet().removeIf(entry -> {
                Identifier supportId = Identifier.tryParse(entry.getKey());
                return supportId == null || !Registries.BLOCK.containsId(supportId) || entry.getValue() == null;
            });
            if (supports.isEmpty()) {
                maskIterator.remove();
            }
        }
    }

    private static int clamp(int value) {
        return Math.max(-MAX_ABS_PIXELS, Math.min(MAX_ABS_PIXELS, value));
    }

    public static final class Data {
        public Map<String, Map<String, Offset>> pairs = new HashMap<>();
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
