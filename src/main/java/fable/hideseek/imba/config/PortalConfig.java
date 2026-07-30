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
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("imba_portal.json");

    public static Data DATA = new Data();

    private PortalConfig() {}

    public static void load() {
        if (!Files.exists(PATH)) {
            save();
            return;
        }
        try {
            DATA = GSON.fromJson(Files.readString(PATH), Data.class);
            if (DATA == null) DATA = new Data();
            DATA.disableNether = true;
        } catch (IOException | RuntimeException e) {
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

    public static RegistryKey<World> worldKey() {
        return RegistryKey.of(RegistryKeys.WORLD, new Identifier(DATA.world));
    }

    public static Vec3d targetPos() {
        return new Vec3d(DATA.x, DATA.y, DATA.z);
    }

    public static final class Data {
        public boolean disableNether = true;
        public String world = "minecraft:the_nether";
        public double x = 0.5;
        public double y = 80.0;
        public double z = 0.5;
        public float yaw = 0.0f;
        public float pitch = 0.0f;
        public int portalTicks = 80;
    }
}
