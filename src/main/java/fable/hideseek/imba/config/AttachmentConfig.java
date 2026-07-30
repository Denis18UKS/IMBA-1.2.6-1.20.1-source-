package fable.hideseek.imba.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class AttachmentConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("imba_attachments.json");
    public static Data DATA = new Data();

    private AttachmentConfig() {}

    public static void load() {
        if (!Files.exists(PATH)) { save(); return; }
        try {
            DATA = GSON.fromJson(Files.readString(PATH), Data.class);
            if (DATA == null) DATA = new Data();
            if (DATA.itemOffsets == null) DATA.itemOffsets = new HashMap<>();
            DATA.itemOffsets.putIfAbsent("imba:potion_2d", new Offset(0.0, 0.15, 0.0));
        } catch (IOException | RuntimeException e) { DATA = new Data(); }
    }

    public static void save() {
        try { Files.createDirectories(PATH.getParent()); Files.writeString(PATH, GSON.toJson(DATA)); }
        catch (IOException ignored) {}
    }

    public static Vec3d offsetFor(Identifier id) {
        Offset o = DATA.itemOffsets.get(id.toString());
        return o == null ? Vec3d.ZERO : new Vec3d(o.x, o.y, o.z);
    }
    public static void setOffset(Identifier id, Vec3d value) {
        DATA.itemOffsets.put(id.toString(), new Offset(value.x, value.y, value.z));
        save();
    }

    public static final class Data {
        public Map<String, Offset> itemOffsets = new HashMap<>();
        public Data() {
            itemOffsets.put("imba:potion_2d", new Offset(0.0, 0.15, 0.0));
        }
    }
    public static final class Offset { public double x,y,z; public Offset(){} public Offset(double x,double y,double z){this.x=x;this.y=y;this.z=z;} }
}
