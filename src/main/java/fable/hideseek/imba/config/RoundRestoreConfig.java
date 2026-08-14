package fable.hideseek.imba.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persistent snapshots restored whenever an IMBA round ends. */
public final class RoundRestoreConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("imba_round_restore.json");
    public static final int MAX_LAYER_BLOCKS = 100_000;
    private static Data data = new Data();

    private RoundRestoreConfig() {}

    public static void load() {
        data = new Data();
        if (!Files.exists(PATH)) { save(); return; }
        try {
            Data loaded = GSON.fromJson(Files.readString(PATH), Data.class);
            if (loaded != null) data = loaded;
            if (data.blocks == null) data.blocks = new ArrayList<>();
            if (data.layers == null) data.layers = new ArrayList<>();
            for (Layer layer : data.layers) {
                if (layer.blocks == null) layer.blocks = new ArrayList<>();
                if (layer.name == null || layer.name.isBlank()) layer.name = "Слой";
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("[IMBA] Не удалось загрузить восстановление раунда: " + e.getMessage());
        }
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Path tmp = PATH.resolveSibling(PATH.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(data));
            Files.move(tmp, PATH, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("[IMBA] Не удалось сохранить восстановление раунда: " + e.getMessage());
        }
    }

    public static int saveSingle(ServerWorld world, BlockPos pos) {
        Snapshot snapshot = capture(world, pos);
        if (snapshot == null) return -1;
        data.blocks.removeIf(old -> old.samePosition(snapshot));
        data.blocks.add(snapshot);
        save();
        return data.blocks.size() - 1;
    }

    public static boolean removeSingle(ServerWorld world, BlockPos pos) {
        String id = world.getRegistryKey().getValue().toString();
        boolean removed = data.blocks.removeIf(s -> s.world.equals(id)
                && s.x == pos.getX() && s.y == pos.getY() && s.z == pos.getZ());
        if (removed) save();
        return removed;
    }

    public static boolean deleteSingle(int index) {
        if (index < 0 || index >= data.blocks.size()) return false;
        data.blocks.remove(index);
        save();
        return true;
    }

    public static boolean recaptureSingle(MinecraftServer server, int index) {
        if (server == null || index < 0 || index >= data.blocks.size()) return false;
        Snapshot old = data.blocks.get(index);
        ServerWorld world = world(server, old.world);
        if (world == null) return false;
        Snapshot replacement = capture(world, new BlockPos(old.x, old.y, old.z));
        if (replacement == null) return false;
        data.blocks.set(index, replacement);
        save();
        return true;
    }

    public static boolean restoreSingle(MinecraftServer server, int index) {
        return server != null && index >= 0 && index < data.blocks.size() && restore(server, data.blocks.get(index));
    }

    public static List<SingleMeta> singleMetadata() {
        List<SingleMeta> result = new ArrayList<>();
        for (int i = 0; i < data.blocks.size(); i++) {
            Snapshot s = data.blocks.get(i);
            int items = 0;
            if (s.inventory != null) for (String value : s.inventory) if (value != null && !value.isEmpty()) items++;
            result.add(new SingleMeta(i, s.world, s.x, s.y, s.z, s.block, items));
        }
        return result;
    }

    public static LayerMeta saveLayer(ServerWorld world, BlockPos a, BlockPos b, int replaceIndex, String name) {
        int minX = Math.min(a.getX(), b.getX()), maxX = Math.max(a.getX(), b.getX());
        int minY = Math.min(a.getY(), b.getY()), maxY = Math.max(a.getY(), b.getY());
        int minZ = Math.min(a.getZ(), b.getZ()), maxZ = Math.max(a.getZ(), b.getZ());
        long volume = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        if (volume <= 0 || volume > MAX_LAYER_BLOCKS) return null;

        List<Snapshot> snapshots = new ArrayList<>((int) volume);
        for (BlockPos pos : BlockPos.iterate(minX, minY, minZ, maxX, maxY, maxZ)) {
            Snapshot snapshot = capture(world, pos.toImmutable());
            if (snapshot != null) snapshots.add(snapshot);
        }

        String clean = cleanLayerName(name, replaceIndex >= 0 ? replaceIndex + 1 : data.layers.size() + 1);
        boolean enabled = replaceIndex >= 0 && replaceIndex < data.layers.size()
                ? data.layers.get(replaceIndex).enabled : true;
        Layer layer = new Layer(clean, enabled, snapshots);
        int index;
        if (replaceIndex >= 0 && replaceIndex < data.layers.size()) {
            data.layers.set(replaceIndex, layer);
            index = replaceIndex;
        } else {
            data.layers.add(layer);
            index = data.layers.size() - 1;
        }
        save();
        return meta(index, layer);
    }

    public static boolean setLayerEnabled(int index, boolean enabled) {
        if (index < 0 || index >= data.layers.size()) return false;
        data.layers.get(index).enabled = enabled;
        save();
        return true;
    }

    public static boolean renameLayer(int index, String name) {
        if (index < 0 || index >= data.layers.size()) return false;
        data.layers.get(index).name = cleanLayerName(name, index + 1);
        save();
        return true;
    }

    public static boolean deleteLayer(int index) {
        if (index < 0 || index >= data.layers.size()) return false;
        data.layers.remove(index);
        save();
        return true;
    }

    public static boolean restoreLayer(MinecraftServer server, int index) {
        if (server == null || index < 0 || index >= data.layers.size()) return false;
        Layer layer = data.layers.get(index);
        if (layer.blocks == null || layer.blocks.isEmpty()) return false;
        boolean restored = false;
        for (Snapshot snapshot : layer.blocks) restored |= restore(server, snapshot);
        return restored;
    }

    public static List<LayerMeta> layerMetadata() {
        List<LayerMeta> result = new ArrayList<>();
        for (int i = 0; i < data.layers.size(); i++) result.add(meta(i, data.layers.get(i)));
        return result;
    }

    /** Central round/location restoration entry point used by round-end and manual commands. */
    public static RestoreResult restoreEnabled(MinecraftServer server) {
        if (server == null) return new RestoreResult(0, 0, 0);
        int restoredPoints = 0;
        int restoredLayers = 0;
        int restoredBlocks = 0;

        for (Snapshot snapshot : List.copyOf(data.blocks)) {
            if (restore(server, snapshot)) { restoredPoints++; restoredBlocks++; }
        }

        for (Layer layer : List.copyOf(data.layers)) {
            if (!layer.enabled || layer.blocks == null || layer.blocks.isEmpty()) continue;
            int layerBlocks = 0;
            for (Snapshot snapshot : layer.blocks) if (restore(server, snapshot)) layerBlocks++;
            if (layerBlocks > 0) { restoredLayers++; restoredBlocks += layerBlocks; }
        }
        return new RestoreResult(restoredPoints, restoredLayers, restoredBlocks);
    }

    /** Compatibility wrapper for existing round-end hooks. */
    public static void restoreAll(MinecraftServer server) { restoreEnabled(server); }

    private static LayerMeta meta(int index, Layer layer) {
        if (layer.blocks == null || layer.blocks.isEmpty()) {
            return new LayerMeta(index, layer.name, layer.enabled, 0, "", 0, 0, 0, 0, 0, 0);
        }
        Snapshot first = layer.blocks.get(0);
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (Snapshot s : layer.blocks) {
            minX = Math.min(minX, s.x); minY = Math.min(minY, s.y); minZ = Math.min(minZ, s.z);
            maxX = Math.max(maxX, s.x); maxY = Math.max(maxY, s.y); maxZ = Math.max(maxZ, s.z);
        }
        return new LayerMeta(index, layer.name, layer.enabled, layer.blocks.size(), first.world,
                minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static String cleanLayerName(String name, int fallbackNumber) {
        String clean = name == null ? "" : name.strip().replaceAll("\\s+", " ");
        if (clean.isEmpty()) clean = "Слой " + fallbackNumber;
        return clean.length() > 64 ? clean.substring(0, 64) : clean;
    }

    private static Snapshot capture(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Identifier blockId = Registries.BLOCK.getId(state.getBlock());
        if (blockId == null) return null;
        Map<String, String> properties = new LinkedHashMap<>();
        for (Property<?> property : state.getProperties()) properties.put(property.getName(), propertyName(state, property));

        List<String> inventory = null;
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof Inventory inv) {
            inventory = new ArrayList<>(inv.size());
            for (int i = 0; i < inv.size(); i++) {
                ItemStack stack = inv.getStack(i);
                inventory.add(stack.isEmpty() ? "" : stack.writeNbt(new NbtCompound()).asString());
            }
        }
        return new Snapshot(world.getRegistryKey().getValue().toString(), pos.getX(), pos.getY(), pos.getZ(),
                blockId.toString(), properties, inventory);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String propertyName(BlockState state, Property property) { return property.name(state.get(property)); }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState applyProperty(BlockState state, String name, String value) {
        for (Property property : state.getProperties()) {
            if (!property.getName().equals(name)) continue;
            var parsed = property.parse(value);
            if (parsed.isPresent()) return state.with(property, (Comparable) parsed.get());
        }
        return state;
    }

    private static ServerWorld world(MinecraftServer server, String id) {
        try {
            Identifier worldId = new Identifier(id);
            RegistryKey<net.minecraft.world.World> worldKey = RegistryKey.of(RegistryKeys.WORLD, worldId);
            return server.getWorld(worldKey);
        } catch (RuntimeException ignored) { return null; }
    }

    private static boolean restore(MinecraftServer server, Snapshot snapshot) {
        try {
            ServerWorld world = world(server, snapshot.world);
            Identifier blockId = new Identifier(snapshot.block);
            if (world == null || !Registries.BLOCK.containsId(blockId)) return false;
            Block block = Registries.BLOCK.get(blockId);
            BlockState state = block.getDefaultState();
            if (snapshot.properties != null) {
                for (var entry : snapshot.properties.entrySet()) state = applyProperty(state, entry.getKey(), entry.getValue());
            }
            BlockPos pos = new BlockPos(snapshot.x, snapshot.y, snapshot.z);
            world.setBlockState(pos, state, Block.NOTIFY_ALL);
            if (snapshot.inventory != null && world.getBlockEntity(pos) instanceof Inventory inv) {
                inv.clear();
                for (int i = 0; i < Math.min(inv.size(), snapshot.inventory.size()); i++) {
                    String snbt = snapshot.inventory.get(i);
                    if (snbt == null || snbt.isEmpty()) continue;
                    NbtCompound nbt = StringNbtReader.parse(snbt);
                    inv.setStack(i, ItemStack.fromNbt(nbt));
                }
                BlockEntity blockEntity = world.getBlockEntity(pos);
                if (blockEntity != null) blockEntity.markDirty();
            }
            return true;
        } catch (Exception ignored) { return false; }
    }

    public record SingleMeta(int index, String world, int x, int y, int z, String block, int inventoryItems) {}
    public record LayerMeta(int index, String name, boolean enabled, int blockCount, String world,
                            int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {}
    public record RestoreResult(int points, int layers, int blocks) {
        public boolean anythingRestored() { return points > 0 || layers > 0; }
    }

    private static final class Data {
        List<Snapshot> blocks = new ArrayList<>();
        List<Layer> layers = new ArrayList<>();
    }

    private static final class Layer {
        String name;
        boolean enabled;
        List<Snapshot> blocks;
        Layer(String name, boolean enabled, List<Snapshot> blocks) {
            this.name = name; this.enabled = enabled; this.blocks = blocks;
        }
    }

    private static final class Snapshot {
        String world;
        int x, y, z;
        String block;
        Map<String, String> properties;
        List<String> inventory;
        Snapshot(String world, int x, int y, int z, String block, Map<String, String> properties, List<String> inventory) {
            this.world = world; this.x = x; this.y = y; this.z = z; this.block = block;
            this.properties = properties; this.inventory = inventory;
        }
        boolean samePosition(Snapshot other) {
            return world.equals(other.world) && x == other.x && y == other.y && z == other.z;
        }
    }
}
