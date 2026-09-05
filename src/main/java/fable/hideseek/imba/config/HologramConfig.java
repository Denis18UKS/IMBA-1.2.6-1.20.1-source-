package fable.hideseek.imba.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Server-owned locations for fixed, depth-tested location-photo holograms. */
public final class HologramConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("imba_location_holograms.json");
    private static Data data = new Data();
    private HologramConfig() {}

    public static void load() {
        data = new Data();
        if (!Files.exists(PATH)) { save(); return; }
        try {
            Data loaded = GSON.fromJson(Files.readString(PATH), Data.class);
            if (loaded != null) data = loaded;
            if (data.projectors == null) data.projectors = new ArrayList<>();
            for (Projector p : data.projectors) {
                if (!Float.isFinite(p.contrast) || p.contrast <= 0) p.contrast = 1.0f;
                if (!Float.isFinite(p.textScale) || p.textScale <= 0) p.textScale = 1.0f;
                p.textScale = Math.max(.50f, Math.min(3.0f, p.textScale));
                p.titleBreak = Math.max(0, Math.min(256, p.titleBreak));
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("[IMBA] Не удалось загрузить голопроекторы: " + e.getMessage());
        }
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Path tmp = PATH.resolveSibling(PATH.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(data));
            Files.move(tmp, PATH, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("[IMBA] Не удалось сохранить голопроекторы: " + e.getMessage());
        }
    }

    public static List<Projector> snapshot() { return data.projectors.stream().map(Projector::copy).toList(); }

    public static Projector saveProjector(int id, int location, String world, double x, double y, double z,
                                          float yaw, float scale, int light, boolean textBackground, float contrast,
                                          float textScale, int titleBreak) {
        int realId = id;
        if (realId < 0) realId = data.projectors.stream().mapToInt(p -> p.id).max().orElse(0) + 1;
        Projector p = new Projector(realId, location, world, x, y, z, yaw,
                Math.max(.20f, Math.min(3.0f, scale)), Math.max(0, Math.min(15, light)), textBackground,
                Math.max(.50f, Math.min(2.0f, contrast)), Math.max(.50f, Math.min(3.0f, textScale)),
                Math.max(0, Math.min(256, titleBreak)));
        final int find = realId;
        int index = -1;
        for (int i = 0; i < data.projectors.size(); i++) if (data.projectors.get(i).id == find) { index = i; break; }
        if (index >= 0) data.projectors.set(index, p); else data.projectors.add(p);
        save();
        return p.copy();
    }

    public static boolean delete(int id) { boolean r = data.projectors.removeIf(p -> p.id == id); if (r) save(); return r; }

    private static final class Data { List<Projector> projectors = new ArrayList<>(); }

    public static final class Projector {
        public int id; public int location; public String world; public double x,y,z; public float yaw,scale;
        public int light; public boolean textBackground; public float contrast; public float textScale; public int titleBreak;
        Projector(int id,int location,String world,double x,double y,double z,float yaw,float scale,int light,
                  boolean textBackground,float contrast,float textScale,int titleBreak) {
            this.id=id;this.location=location;this.world=world;this.x=x;this.y=y;this.z=z;this.yaw=yaw;this.scale=scale;
            this.light=light;this.textBackground=textBackground;this.contrast=Math.max(.50f,Math.min(2.0f,contrast));
            this.textScale=Math.max(.50f,Math.min(3.0f,textScale));this.titleBreak=Math.max(0,Math.min(256,titleBreak));
        }
        Projector copy(){return new Projector(id,location,world,x,y,z,yaw,scale,light,textBackground,contrast,textScale,titleBreak);}
    }
}
