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
 * Manual classification used only by statue positioning.
 *
 * All blocks are treated as full cubes by default. Blocks added to
 * nonFullBlocks keep the player's exact XYZ when entering statue mode, so no
 * automatic centering can push the player into slabs, stairs, farmland or
 * other partial geometry.
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
