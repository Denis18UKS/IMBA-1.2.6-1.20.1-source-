package fable.hideseek.imba.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PortalConfig {

    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().create();

    private static final Path OVERWORLD_PATH =
            FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("imba_portal.json");

    private static final Path NETHER_PATH =
            FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("imba_portal_in_overworld.json");

    public static Data OVERWORLD = createNetherTarget();
    public static Data NETHER = createOverworldTarget();

    private PortalConfig() {
    }

    public static void load() {
        OVERWORLD = loadConfig(OVERWORLD_PATH, createNetherTarget());
        NETHER = loadConfig(NETHER_PATH, createOverworldTarget());
    }

    public static void save() {
        saveConfig(OVERWORLD_PATH, OVERWORLD);
        saveConfig(NETHER_PATH, NETHER);
    }

    public static Data get(boolean fromNether) {
        return fromNether ? NETHER : OVERWORLD;
    }

    public static RegistryKey<World> worldKey(boolean fromNether) {
        Data data = get(fromNether);
        return RegistryKey.of(
                RegistryKeys.WORLD,
                new Identifier(data.world));
    }

    public static Vec3d targetPos(boolean fromNether) {
        Data data = get(fromNether);
        return new Vec3d(data.x, data.y, data.z);
    }

    private static Data loadConfig(Path path, Data defaults) {
        if (!Files.exists(path)) {
            saveConfig(path, defaults);
            return defaults;
        }

        try {
            Data data = GSON.fromJson(Files.readString(path), Data.class);
            return data != null ? data : defaults;
        } catch (IOException | RuntimeException exception) {
            return defaults;
        }
    }

    private static void saveConfig(Path path, Data data) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(data));
        } catch (IOException ignored) {
        }
    }

    private static Data createNetherTarget() {
        Data data = new Data();
        data.world = "minecraft:the_nether";
        return data;
    }

    private static Data createOverworldTarget() {
        Data data = new Data();
        data.world = "minecraft:overworld";
        return data;
    }

    public static final class Data {
        public boolean disableNether = false;
        public String world = "minecraft:the_nether";
        public double x = 0.5D;
        public double y = 80.0D;
        public double z = 0.5D;
        public float yaw = 0.0F;
        public float pitch = 0.0F;
        public int portalTicks = 80;
    }
}
