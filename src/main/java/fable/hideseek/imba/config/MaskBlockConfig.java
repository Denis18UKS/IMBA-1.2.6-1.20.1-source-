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
import java.util.HashSet;
import java.util.Set;

/**
 * Manual classification used by statue positioning.
 *
 * This config is authoritative: the real vanilla/modded VoxelShape is NOT
 * allowed to override the value selected in the GUI.
 *
 * A block marked FULL is treated by statue positioning as a full 1x1x1 cube
 * even when the real block is a slab, stair, farmland or any other partial
 * shape. A block marked NON-FULL keeps exact player coordinates instead of
 * receiving full-cube grid snapping.
 *
 * All blocks are FULL by default. Only explicit NON-FULL overrides are stored
 * in nonFullBlocks, keeping the JSON small even with large modpacks.
 *
 * This config intentionally does NOT affect MaskHitbox/EntityDimensions.
 */
public final class MaskBlockConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("imba_mask_blocks.json");

    public static Data DATA = new Data();

    private MaskBlockConfig() {
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
            if (DATA.nonFullBlocks == null) {
                DATA.nonFullBlocks = new HashSet<>();
            }
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

    public static boolean isFull(Block block) {
        if (block == null) {
            return true;
        }
        Identifier id = Registries.BLOCK.getId(block);
        return id == null || !DATA.nonFullBlocks.contains(id.toString());
    }

    public static void setFull(Identifier id, boolean full) {
        if (id == null) {
            return;
        }
        if (full) {
            DATA.nonFullBlocks.remove(id.toString());
        } else {
            DATA.nonFullBlocks.add(id.toString());
        }
        save();
    }

    public static Set<String> nonFullBlocksSnapshot() {
        return Set.copyOf(DATA.nonFullBlocks);
    }

    public static final class Data {
        public Set<String> nonFullBlocks = new HashSet<>();
    }
}
