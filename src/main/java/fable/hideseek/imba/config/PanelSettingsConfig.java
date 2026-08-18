package fable.hideseek.imba.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Server-owned editable layout for the 3x3 in-world settings panel. */
public final class PanelSettingsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("imba_panel_settings.json");

    private static String timerLabel = "Таймер";
    private static String heartsLabel = "Сердца";
    private static float timerTitleScale = 1.30F, heartsTitleScale = 1.30F;
    private static float timerValueScale = 1.60F, heartsValueScale = 1.60F, arrowScale = 1.45F;
    private static int timerX = -38, heartsX = 38, titleY = -52, upArrowY = -26, valueY = 0, downArrowY = 28;

    private static Hitbox timerUp = new Hitbox(-38, -26, 34, 20);
    private static Hitbox timerDown = new Hitbox(-38, 28, 34, 20);
    private static Hitbox heartsUp = new Hitbox(38, -26, 34, 20);
    private static Hitbox heartsDown = new Hitbox(38, 28, 34, 20);

    private PanelSettingsConfig() {}

    public enum Action { TIMER_UP, TIMER_DOWN, HEARTS_UP, HEARTS_DOWN }

    public static void load() {
        resetDefaults(false);
        if (!Files.exists(PATH)) { save(); return; }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(PATH)).getAsJsonObject();
            timerLabel = sanitizeLabel(readString(root, "timerLabel", "Таймер"), "Таймер");
            heartsLabel = sanitizeLabel(readString(root, "heartsLabel", "Сердца"), "Сердца");
            timerTitleScale = readFloat(root, "timerTitleScale", 1.30F, .40F, 3F);
            heartsTitleScale = readFloat(root, "heartsTitleScale", 1.30F, .40F, 3F);
            timerValueScale = readFloat(root, "timerValueScale", 1.60F, .40F, 3F);
            heartsValueScale = readFloat(root, "heartsValueScale", 1.60F, .40F, 3F);
            arrowScale = readFloat(root, "arrowScale", 1.45F, .40F, 3F);
            timerX = readInt(root, "timerX", -38, -120, 120); heartsX = readInt(root, "heartsX", 38, -120, 120);
            titleY = readInt(root, "titleY", -52, -100, 100); upArrowY = readInt(root, "upArrowY", -26, -100, 100);
            valueY = readInt(root, "valueY", 0, -100, 100); downArrowY = readInt(root, "downArrowY", 28, -100, 100);
            timerUp = readHitbox(root, "timerUpHitbox", new Hitbox(-38, -26, 34, 20));
            timerDown = readHitbox(root, "timerDownHitbox", new Hitbox(-38, 28, 34, 20));
            heartsUp = readHitbox(root, "heartsUpHitbox", new Hitbox(38, -26, 34, 20));
            heartsDown = readHitbox(root, "heartsDownHitbox", new Hitbox(38, 28, 34, 20));
        } catch (IOException | RuntimeException e) {
            System.err.println("[IMBA] Не удалось загрузить настройки панели: " + e.getMessage());
        }
    }

    public static String timerLabel(){return timerLabel;} public static String heartsLabel(){return heartsLabel;}
    public static float timerTitleScale(){return timerTitleScale;} public static float heartsTitleScale(){return heartsTitleScale;}
    public static float timerValueScale(){return timerValueScale;} public static float heartsValueScale(){return heartsValueScale;} public static float arrowScale(){return arrowScale;}
    public static int timerX(){return timerX;} public static int heartsX(){return heartsX;} public static int titleY(){return titleY;} public static int upArrowY(){return upArrowY;} public static int valueY(){return valueY;} public static int downArrowY(){return downArrowY;}
    public static Hitbox timerUp(){return timerUp.copy();} public static Hitbox timerDown(){return timerDown.copy();} public static Hitbox heartsUp(){return heartsUp.copy();} public static Hitbox heartsDown(){return heartsDown.copy();}

    public static void setLayout(String newTimerLabel, String newHeartsLabel,
                                 float newTimerTitleScale, float newHeartsTitleScale,
                                 float newTimerValueScale, float newHeartsValueScale,
                                 float newArrowScale, int newTimerX, int newHeartsX,
                                 int newTitleY, int newUpArrowY, int newValueY, int newDownArrowY) {
        timerLabel=sanitizeLabel(newTimerLabel,"Таймер"); heartsLabel=sanitizeLabel(newHeartsLabel,"Сердца");
        timerTitleScale=clamp(newTimerTitleScale,.40F,3F); heartsTitleScale=clamp(newHeartsTitleScale,.40F,3F);
        timerValueScale=clamp(newTimerValueScale,.40F,3F); heartsValueScale=clamp(newHeartsValueScale,.40F,3F); arrowScale=clamp(newArrowScale,.40F,3F);
        timerX=clamp(newTimerX,-120,120); heartsX=clamp(newHeartsX,-120,120); titleY=clamp(newTitleY,-100,100); upArrowY=clamp(newUpArrowY,-100,100); valueY=clamp(newValueY,-100,100); downArrowY=clamp(newDownArrowY,-100,100);
        save();
    }

    public static void setHitboxes(Hitbox newTimerUp, Hitbox newTimerDown, Hitbox newHeartsUp, Hitbox newHeartsDown) {
        timerUp = sanitizeHitbox(newTimerUp); timerDown = sanitizeHitbox(newTimerDown); heartsUp = sanitizeHitbox(newHeartsUp); heartsDown = sanitizeHitbox(newHeartsDown); save();
    }

    public static Action actionAt(float x, float y) {
        if (timerUp.contains(x,y)) return Action.TIMER_UP;
        if (timerDown.contains(x,y)) return Action.TIMER_DOWN;
        if (heartsUp.contains(x,y)) return Action.HEARTS_UP;
        if (heartsDown.contains(x,y)) return Action.HEARTS_DOWN;
        return null;
    }

    public static void resetDefaults(){resetDefaults(true);} private static void resetDefaults(boolean save){timerLabel="Таймер";heartsLabel="Сердца";timerTitleScale=1.30F;heartsTitleScale=1.30F;timerValueScale=1.60F;heartsValueScale=1.60F;arrowScale=1.45F;timerX=-38;heartsX=38;titleY=-52;upArrowY=-26;valueY=0;downArrowY=28;timerUp=new Hitbox(-38,-26,34,20);timerDown=new Hitbox(-38,28,34,20);heartsUp=new Hitbox(38,-26,34,20);heartsDown=new Hitbox(38,28,34,20);if(save)save();}

    public static void save(){try{Files.createDirectories(PATH.getParent());Path tmp=PATH.resolveSibling(PATH.getFileName()+".tmp");Files.writeString(tmp,GSON.toJson(new Data(timerLabel,heartsLabel,timerTitleScale,heartsTitleScale,timerValueScale,heartsValueScale,arrowScale,timerX,heartsX,titleY,upArrowY,valueY,downArrowY,timerUp,timerDown,heartsUp,heartsDown)));Files.move(tmp,PATH,StandardCopyOption.REPLACE_EXISTING);}catch(IOException e){System.err.println("[IMBA] Не удалось сохранить настройки панели: "+e.getMessage());}}

    private static Hitbox readHitbox(JsonObject root,String key,Hitbox fallback){try{if(root.has(key)&&root.get(key).isJsonObject()){Hitbox h=GSON.fromJson(root.get(key),Hitbox.class);return sanitizeHitbox(h);}}catch(RuntimeException ignored){}return fallback;}
    private static Hitbox sanitizeHitbox(Hitbox h){if(h==null)return new Hitbox(0,0,30,20);return new Hitbox(clamp(h.x,-70,70),clamp(h.y,-70,70),clamp(h.width,4,90),clamp(h.height,4,70));}
    private static String sanitizeLabel(String v,String f){String c=v==null?"":v.replaceAll("§.","").trim().replaceAll("\\s+"," ");if(c.isEmpty())c=f;return c.length()>24?c.substring(0,24):c;}
    private static String readString(JsonObject r,String k,String f){return r.has(k)&&!r.get(k).isJsonNull()?r.get(k).getAsString():f;}
    private static float readFloat(JsonObject r,String k,float f,float min,float max){try{return r.has(k)?clamp(r.get(k).getAsFloat(),min,max):f;}catch(RuntimeException e){return f;}}
    private static int readInt(JsonObject r,String k,int f,int min,int max){try{return r.has(k)?clamp(r.get(k).getAsInt(),min,max):f;}catch(RuntimeException e){return f;}}
    private static float clamp(float v,float min,float max){if(!Float.isFinite(v))return min;return Math.max(min,Math.min(max,v));} private static int clamp(int v,int min,int max){return Math.max(min,Math.min(max,v));}

    public static final class Hitbox { public float x,y,width,height; public Hitbox(){} public Hitbox(float x,float y,float width,float height){this.x=x;this.y=y;this.width=width;this.height=height;} public boolean contains(float px,float py){return px>=x-width/2F&&px<=x+width/2F&&py>=y-height/2F&&py<=y+height/2F;} public Hitbox copy(){return new Hitbox(x,y,width,height);} }
    private record Data(String timerLabel,String heartsLabel,float timerTitleScale,float heartsTitleScale,float timerValueScale,float heartsValueScale,float arrowScale,int timerX,int heartsX,int titleY,int upArrowY,int valueY,int downArrowY,Hitbox timerUpHitbox,Hitbox timerDownHitbox,Hitbox heartsUpHitbox,Hitbox heartsDownHitbox){}
}
