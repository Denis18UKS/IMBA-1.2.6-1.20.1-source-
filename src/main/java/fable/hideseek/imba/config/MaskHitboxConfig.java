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

/** Per-block 3D mask hitbox tuning. Values are model pixels (16 = 1 block). */
public final class MaskHitboxConfig {
    public static final int MIN_COORD = -32;
    public static final int MAX_COORD = 48;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("imba_mask_hitboxes.json");
    private static final Map<String, BoxSpec> BOXES = new HashMap<>();

    public static final class BoxSpec {
        public int minX;
        public int minY;
        public int minZ;
        public int maxX;
        public int maxY;
        public int maxZ;

        public BoxSpec() {}
        public BoxSpec(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }
        public BoxSpec copy() { return new BoxSpec(minX, minY, minZ, maxX, maxY, maxZ); }
    }

    private MaskHitboxConfig() {}

    public static void load() {
        BOXES.clear();
        if (!Files.exists(PATH)) { save(); return; }
        try {
            Data data = GSON.fromJson(Files.readString(PATH), Data.class);
            if (data != null && data.boxes != null) {
                data.boxes.forEach((id, box) -> {
                    Identifier parsed = Identifier.tryParse(id);
                    if (parsed != null && Registries.BLOCK.containsId(parsed) && box != null) {
                        BOXES.put(parsed.toString(), normalizeSpecial(parsed, sanitize(box)));
                    }
                });
            }
            // Older builds could persist the placed blade outline (a thin vertical
            // plate) as the disguise hitbox. Re-save after normalization so an old
            // config cannot restore that geometry on the next server start.
            save();
        } catch (IOException | RuntimeException e) {
            System.err.println("[IMBA] Не удалось загрузить хитбоксы масок: " + e.getMessage());
        }
    }

    public static boolean hasCustom(Block block) {
        Identifier id = block == null ? null : Registries.BLOCK.getId(block);
        return id != null && BOXES.containsKey(id.toString());
    }

    public static BoxSpec custom(Block block) {
        Identifier id = block == null ? null : Registries.BLOCK.getId(block);
        if (id == null) return null;
        BoxSpec value = BOXES.get(id.toString());
        return value == null ? null : value.copy();
    }

    public static BoxSpec effective(Block block) {
        BoxSpec custom = custom(block);
        if (custom != null) {
            Identifier id = Registries.BLOCK.getId(block);
            return normalizeSpecial(id, custom);
        }
        return defaultFor(block);
    }

    public static BoxSpec defaultFor(Block block) {
        if (block == null) return new BoxSpec(0, 0, 0, 16, 16, 16);

        // imba:stonrcutter_lezvie is modeled as a horizontal stonecutter/slab
        // disguise. The placed helper blade itself has a thin vertical outline,
        // which must never leak into the player's mask collision.
        if (block == ImbaMod.STONRCUTTER_LEZVIE) {
            return new BoxSpec(0, 0, 0, 16, 8, 16);
        }

        try {
            BlockState state = block.getDefaultState();
            var shape = state.getOutlineShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
            if (!shape.isEmpty()) {
                Box box = shape.getBoundingBox();
                return sanitize(new BoxSpec(
                        toPixelFloor(box.minX), toPixelFloor(box.minY), toPixelFloor(box.minZ),
                        toPixelCeil(box.maxX), toPixelCeil(box.maxY), toPixelCeil(box.maxZ)));
            }
        } catch (RuntimeException ignored) {}
        return new BoxSpec(0, 0, 0, 16, 16, 16);
    }

    public static void set(Identifier id, BoxSpec box) {
        if (id == null || box == null || !Registries.BLOCK.containsId(id)) return;
        BOXES.put(id.toString(), normalizeSpecial(id, sanitize(box)));
        save();
    }

    public static void clear(Identifier id) {
        if (id == null) return;
        BOXES.remove(id.toString());
        save();
    }

    public static Map<String, BoxSpec> snapshot() {
        Map<String, BoxSpec> result = new HashMap<>();
        BOXES.forEach((id, box) -> result.put(id, box.copy()));
        return result;
    }

    /** Client applies the authoritative server copy without writing a local file. */
    public static void applyNetworkSnapshot(Map<String, BoxSpec> values) {
        BOXES.clear();
        if (values == null) return;
        values.forEach((id, box) -> {
            Identifier parsed = Identifier.tryParse(id);
            if (parsed != null && Registries.BLOCK.containsId(parsed) && box != null) {
                BOXES.put(parsed.toString(), normalizeSpecial(parsed, sanitize(box)));
            }
        });
    }

    /** Returns a world-space configured box centered on mask anchor X/Z and based at anchor Y. */
    public static Box worldBox(Block block, float rotation, double anchorX, double anchorY, double anchorZ) {
        if (block == null) return null;
        if (MaskBlockConfig.isFull(block) && !hasCustom(block) && block != ImbaMod.STONRCUTTER_LEZVIE) return null;
        BoxSpec spec = effective(block);
        Box local = new Box(spec.minX / 16.0D, spec.minY / 16.0D, spec.minZ / 16.0D,
                spec.maxX / 16.0D, spec.maxY / 16.0D, spec.maxZ / 16.0D);
        local = rotateY(local, Math.floorMod(Math.round(rotation / 90.0F), 4));
        return local.offset(anchorX - 0.5D, anchorY, anchorZ - 0.5D);
    }

    private static BoxSpec normalizeSpecial(Identifier id, BoxSpec box) {
        if (id == null || box == null) return box;
        Identifier bladeId = Registries.BLOCK.getId(ImbaMod.STONRCUTTER_LEZVIE);
        if (!id.equals(bladeId)) return box;

        int sizeX = box.maxX - box.minX;
        int sizeY = box.maxY - box.minY;
        int sizeZ = box.maxZ - box.minZ;
        boolean verticalBlade = sizeY > 8 && Math.min(sizeX, sizeZ) <= 4;
        return verticalBlade ? new BoxSpec(0, 0, 0, 16, 8, 16) : box;
    }

    private static Box rotateY(Box box, int steps) {
        Box result = box;
        for (int i = 0; i < steps; i++) {
            result = new Box(1.0D - result.maxZ, result.minY, result.minX,
                    1.0D - result.minZ, result.maxY, result.maxX);
        }
        return result;
    }

    private static BoxSpec sanitize(BoxSpec box) {
        int minX = clamp(box.minX), minY = clamp(box.minY), minZ = clamp(box.minZ);
        int maxX = clamp(box.maxX), maxY = clamp(box.maxY), maxZ = clamp(box.maxZ);
        if (maxX <= minX) maxX = Math.min(MAX_COORD, minX + 1);
        if (maxY <= minY) maxY = Math.min(MAX_COORD, minY + 1);
        if (maxZ <= minZ) maxZ = Math.min(MAX_COORD, minZ + 1);
        if (maxX <= minX) minX = maxX - 1;
        if (maxY <= minY) minY = maxY - 1;
        if (maxZ <= minZ) minZ = maxZ - 1;
        return new BoxSpec(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static int clamp(int value) { return Math.max(MIN_COORD, Math.min(MAX_COORD, value)); }
    private static int toPixelFloor(double value) { return (int) Math.floor(value * 16.0D + 1.0E-6D); }
    private static int toPixelCeil(double value) { return (int) Math.ceil(value * 16.0D - 1.0E-6D); }

    private static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Path tmp = PATH.resolveSibling(PATH.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(new Data(snapshot())));
            Files.move(tmp, PATH, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("[IMBA] Не удалось сохранить хитбоксы масок: " + e.getMessage());
        }
    }

    private static final class Data {
        Map<String, BoxSpec> boxes = new HashMap<>();
        Data() {}
        Data(Map<String, BoxSpec> boxes) { this.boxes = boxes; }
    }
}
