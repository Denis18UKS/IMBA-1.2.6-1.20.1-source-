package fable.hideseek.imba.client;
import fable.hideseek.imba.config.MaskHitboxConfig;import java.util.HashMap;import java.util.Map;
public final class MaskHitboxClientData{public record Entry(boolean custom,MaskHitboxConfig.Bounds bounds){}private static final Map<String,Entry> DATA=new HashMap<>();public static String selectedId="";private MaskHitboxClientData(){}public static void replace(Map<String,Entry> next){DATA.clear();DATA.putAll(next);}public static Map<String,Entry> snapshot(){return Map.copyOf(DATA);}public static Entry get(String id){return DATA.get(id);}}
