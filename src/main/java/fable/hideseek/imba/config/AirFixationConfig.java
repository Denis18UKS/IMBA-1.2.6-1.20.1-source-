package fable.hideseek.imba.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import fable.hideseek.imba.mask.MaskState;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AirFixationConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("imba_air_fixation.json");
    private static final Type MAP_TYPE = new TypeToken<LinkedHashMap<String, Boolean>>() {}.getType();
    private static final Map<String, Boolean> OVERRIDES = new LinkedHashMap<>();
    private AirFixationConfig() {}

    public static void load() {
        OVERRIDES.clear();
        if (!Files.exists(PATH)) { save(); return; }
        try { Map<String, Boolean> loaded = GSON.fromJson(Files.readString(PATH), MAP_TYPE); if (loaded != null) OVERRIDES.putAll(loaded); }
        catch (IOException | RuntimeException e) { System.err.println("[IMBA] Не удалось загрузить правила фиксации в воздухе: " + e.getMessage()); }
    }
    public static void save() {
        try { Files.createDirectories(PATH.getParent()); Path tmp=PATH.resolveSibling(PATH.getFileName()+".tmp"); Files.writeString(tmp,GSON.toJson(OVERRIDES)); Files.move(tmp,PATH,StandardCopyOption.REPLACE_EXISTING); }
        catch(IOException e){System.err.println("[IMBA] Не удалось сохранить правила фиксации в воздухе: "+e.getMessage());}
    }
    public static boolean isAllowed(MaskState state){ if(state==null)return false; String key=keyFor(state); if(key==null)return false; Boolean override=OVERRIDES.get(key); return override!=null?override:defaultAllowed(key); }
    public static boolean defaultAllowed(String key){return key!=null&&key.startsWith("item:");}
    public static void set(String key,boolean allowed){if(key==null||key.isBlank())return;if(allowed==defaultAllowed(key))OVERRIDES.remove(key);else OVERRIDES.put(key,allowed);save();}
    public static Map<String,Boolean> overridesSnapshot(){return Map.copyOf(OVERRIDES);}
    public static String blockKey(Identifier id){return id==null?null:"block:"+id;}
    public static String itemKey(Identifier id){return id==null?null:"item:"+id;}
    public static String keyFor(MaskState state){if(state.block!=null)return blockKey(Registries.BLOCK.getId(state.block));if(state.item!=null)return itemKey(Registries.ITEM.getId(state.item));return "type:"+state.type.name().toLowerCase(java.util.Locale.ROOT);}
}
