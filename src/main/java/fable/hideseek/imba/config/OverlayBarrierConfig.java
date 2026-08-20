package fable.hideseek.imba.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Full-cube collision cells stored separately from block states.
 * They can therefore coexist with any real block without replacing it.
 */
public final class OverlayBarrierConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("imba_overlay_barriers.json");
    private static final Map<String, Set<Long>> CELLS = new HashMap<>();

    private OverlayBarrierConfig() {
    }

    public static void load() {
        CELLS.clear();
        if (!Files.exists(PATH)) {
            save();
            return;
        }
        try {
            Data data = GSON.fromJson(Files.readString(PATH), Data.class);
            if (data != null && data.dimensions != null) {
                data.dimensions.forEach((dimension, positions) -> {
                    Identifier id = Identifier.tryParse(dimension);
                    if (id != null && positions != null) {
                        CELLS.put(id.toString(), new HashSet<>(positions));
                    }
                });
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("[IMBA] Не удалось загрузить накладные барьеры: " + e.getMessage());
        }
    }

    public static boolean toggle(World world, BlockPos pos) {
        if (world == null || pos == null) return false;
        String dimension = dimension(world);
        Set<Long> positions = CELLS.computeIfAbsent(dimension, ignored -> new HashSet<>());
        long packed = pos.asLong();
        boolean added;
        if (positions.remove(packed)) {
            added = false;
            if (positions.isEmpty()) CELLS.remove(dimension);
        } else {
            positions.add(packed);
            added = true;
        }
        save();
        return added;
    }

    public static boolean contains(World world, BlockPos pos) {
        if (world == null || pos == null) return false;
        Set<Long> positions = CELLS.get(dimension(world));
        return positions != null && positions.contains(pos.asLong());
    }

    public static List<VoxelShape> collisionShapes(World world, Box query) {
        if (world == null || query == null) return List.of();
        Set<Long> positions = CELLS.get(dimension(world));
        if (positions == null || positions.isEmpty()) return List.of();
        Box expanded = query.expand(0.001D);
        List<VoxelShape> result = new ArrayList<>();
        for (long packed : positions) {
            BlockPos pos = BlockPos.fromLong(packed);
            Box box = new Box(pos);
            if (box.intersects(expanded)) result.add(VoxelShapes.cuboid(box));
        }
        return result;
    }

    public static Map<String, Set<Long>> snapshot() {
        Map<String, Set<Long>> result = new HashMap<>();
        CELLS.forEach((dimension, positions) -> result.put(dimension, new HashSet<>(positions)));
        return result;
    }

    /** Applies a server snapshot on a client. Does not write a local config file. */
    public static void applyNetworkSnapshot(Map<String, Set<Long>> values) {
        CELLS.clear();
        if (values == null) return;
        values.forEach((dimension, positions) -> {
            Identifier id = Identifier.tryParse(dimension);
            if (id != null && positions != null && !positions.isEmpty()) {
                CELLS.put(id.toString(), new HashSet<>(positions));
            }
        });
    }

    private static String dimension(World world) {
        return world.getRegistryKey().getValue().toString();
    }

    private static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Map<String, Set<Long>> values = snapshot();
            Path tmp = PATH.resolveSibling(PATH.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(new Data(values)));
            Files.move(tmp, PATH, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("[IMBA] Не удалось сохранить накладные барьеры: " + e.getMessage());
        }
    }

    private static final class Data {
        Map<String, Set<Long>> dimensions = new HashMap<>();

        Data() {
        }

        Data(Map<String, Set<Long>> dimensions) {
            this.dimensions = dimensions;
        }
    }
}
