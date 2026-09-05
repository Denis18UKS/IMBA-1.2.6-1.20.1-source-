package fable.hideseek.imba.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fable.hideseek.imba.ImbaMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.EmptyBlockView;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public final class MaskHitboxConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("imba_nonfull_hitboxes.json");
    private static final Map<String, Bounds> CUSTOM = new HashMap<>();

    private MaskHitboxConfig() {}

    public static void load() {
        CUSTOM.clear();
        if (!Files.exists(PATH)) {
            save();
            return;
        }
        boolean migrated = false;
        try {
            Data data = GSON.fromJson(Files.readString(PATH), Data.class);
            if (data != null && data.hitboxes != null) {
                for (var entry : data.hitboxes.entrySet()) {
                    Identifier id = Identifier.tryParse(entry.getKey());
                    if (id == null || !Registries.BLOCK.containsId(id) || entry.getValue() == null) continue;
                    Bounds sanitized = entry.getValue().sanitized();
                    Bounds normalized = normalizeSpecial(id, sanitized);
                    if (!sameBounds(sanitized, normalized)) migrated = true;
                    CUSTOM.put(id.toString(), normalized);
                }
            }
            if (migrated) save();
        } catch (IOException | RuntimeException e) {
            System.err.println("[IMBA] Не удалось загрузить хитбоксы масок: " + e.getMessage());
        }
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Path tmp = PATH.resolveSibling(PATH.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(new Data(new HashMap<>(CUSTOM))));
            Files.move(tmp, PATH, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("[IMBA] Не удалось сохранить хитбоксы масок: " + e.getMessage());
        }
    }

    public static Bounds boundsFor(Block block) {
        if (block == null) return Bounds.FULL.copy();
        Identifier id = Registries.BLOCK.getId(block);
        if (id != null && CUSTOM.containsKey(id.toString())) {
            return normalizeSpecial(id, CUSTOM.get(id.toString()).copy());
        }
        return defaultBounds(block);
    }

    public static Bounds boundsFor(Identifier id) {
        if (id == null || !Registries.BLOCK.containsId(id)) return Bounds.FULL.copy();
        return boundsFor(Registries.BLOCK.get(id));
    }

    public static boolean hasCustom(Block block) {
        Identifier id = block == null ? null : Registries.BLOCK.getId(block);
        return id != null && CUSTOM.containsKey(id.toString());
    }

    public static void set(Identifier id, Bounds bounds) {
        if (id == null || !Registries.BLOCK.containsId(id) || bounds == null) return;
        CUSTOM.put(id.toString(), normalizeSpecial(id, bounds.sanitized()));
        save();
    }

    public static void reset(Identifier id) {
        if (id == null) return;
        CUSTOM.remove(id.toString());
        save();
    }

    public static Map<String, Bounds> snapshot() {
        Map<String, Bounds> out = new HashMap<>();
        CUSTOM.forEach((k, v) -> out.put(k, v.copy()));
        return out;
    }

    public static Bounds defaultBounds(Block block) {
        if (block == null) return Bounds.FULL.copy();
        if (block == ImbaMod.HANGING_LANTERN) {
            return Bounds.FULL.copy();
        }
        if (block == ImbaMod.STONRCUTTER_LEZVIE) {
            return new Bounds(0,0,0,16,8,16);
        }
        BlockState state = block.getDefaultState();
        var boxes = state.getCollisionShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN).getBoundingBoxes();
        if (boxes.isEmpty()) return Bounds.FULL.copy();
        double minX = 1, minY = 1, minZ = 1, maxX = 0, maxY = 0, maxZ = 0;
        for (Box b : boxes) {
            minX = Math.min(minX, b.minX);
            minY = Math.min(minY, b.minY);
            minZ = Math.min(minZ, b.minZ);
            maxX = Math.max(maxX, b.maxX);
            maxY = Math.max(maxY, b.maxY);
            maxZ = Math.max(maxZ, b.maxZ);
        }
        return new Bounds((float) (minX * 16), (float) (minY * 16), (float) (minZ * 16),
                (float) (maxX * 16), (float) (maxY * 16), (float) (maxZ * 16)).sanitized();
    }

    private static Bounds normalizeSpecial(Identifier id, Bounds bounds) {
        if (id == null || bounds == null) return bounds;
        Identifier stonecutter = Registries.BLOCK.getId(ImbaMod.STONRCUTTER_LEZVIE);
        if (id.equals(stonecutter) && isLegacyVerticalStonecutter(bounds)) {
            return new Bounds(0,0,0,16,8,16);
        }
        return bounds;
    }

    private static boolean isLegacyVerticalStonecutter(Bounds bounds) {
        if (bounds == null) return false;
        float sizeX = bounds.maxX - bounds.minX;
        float sizeY = bounds.maxY - bounds.minY;
        float sizeZ = bounds.maxZ - bounds.minZ;
        return sizeY > 8.0F && Math.min(sizeX, sizeZ) <= 4.0F;
    }

    private static boolean sameBounds(Bounds a, Bounds b) {
        return Float.compare(a.minX, b.minX) == 0 && Float.compare(a.minY, b.minY) == 0
                && Float.compare(a.minZ, b.minZ) == 0 && Float.compare(a.maxX, b.maxX) == 0
                && Float.compare(a.maxY, b.maxY) == 0 && Float.compare(a.maxZ, b.maxZ) == 0;
    }

    public static final class Bounds {
        public static final Bounds FULL = new Bounds(0, 0, 0, 16, 16, 16);
        public float minX, minY, minZ, maxX, maxY, maxZ;

        public Bounds() {}

        public Bounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        public Bounds sanitized() {
            float a = clamp(minX), b = clamp(minY), c = clamp(minZ);
            float d = clamp(maxX), e = clamp(maxY), f = clamp(maxZ);
            if (d <= a) d = Math.min(16, a + 1);
            if (e <= b) e = Math.min(16, b + 1);
            if (f <= c) f = Math.min(16, c + 1);
            if (d <= a) a = Math.max(0, d - 1);
            if (e <= b) b = Math.max(0, e - 1);
            if (f <= c) c = Math.max(0, f - 1);
            return new Bounds(a, b, c, d, e, f);
        }

        public Bounds copy() {
            return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
        }

        public Box worldBox(double anchorX, double anchorY, double anchorZ) {
            return new Box(anchorX - .5D + minX / 16.0D, anchorY + minY / 16.0D, anchorZ - .5D + minZ / 16.0D,
                    anchorX - .5D + maxX / 16.0D, anchorY + maxY / 16.0D, anchorZ - .5D + maxZ / 16.0D);
        }

        public float width() {
            return Math.max((maxX - minX) / 16.0F, (maxZ - minZ) / 16.0F);
        }

        public float height() {
            return (maxY - minY) / 16.0F;
        }

        private static float clamp(float v) {
            return Float.isFinite(v) ? Math.max(0, Math.min(16, v)) : 0;
        }
    }

    private record Data(Map<String, Bounds> hitboxes) {}
}
