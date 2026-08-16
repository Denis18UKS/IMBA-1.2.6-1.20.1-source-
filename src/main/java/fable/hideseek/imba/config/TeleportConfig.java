package fable.hideseek.imba.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fable.hideseek.imba.game.GameConfig;
import fable.hideseek.imba.game.RoundDefinition;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.Vec3d;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Persistent coordinates edited by the in-game teleport setup tools. */
public final class TeleportConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("imba_teleports.json");
    private static final List<ExtraSeekerPoint> EXTRA_SEEKER_PREPARE = new ArrayList<>();

    private TeleportConfig() {}

    public static void load() {
        EXTRA_SEEKER_PREPARE.clear();
        if (!Files.exists(PATH)) { save(); return; }
        try {
            Data data = GSON.fromJson(Files.readString(PATH), Data.class);
            if (data == null) return;
            if (data.lobby != null) {
                GameConfig.LOBBY_POS = data.lobby.toVec3d();
                if (data.lobby.yaw != null) GameConfig.LOBBY_YAW = data.lobby.yaw;
                if (data.lobby.pitch != null) GameConfig.LOBBY_PITCH = data.lobby.pitch;
            }
            if (data.rounds != null) {
                for (int i = 0; i < Math.min(data.rounds.size(), GameConfig.ROUNDS.size()); i++) {
                    RoundPoints points = data.rounds.get(i); RoundDefinition round = GameConfig.ROUNDS.get(i);
                    if (points.hider != null && !isLegacyPlaceholder(points.hider, i, false)) {
                        round.hiderPos=points.hider.toVec3d(); if(points.hider.yaw!=null)round.hiderYaw=points.hider.yaw;if(points.hider.pitch!=null)round.hiderPitch=points.hider.pitch;
                    }
                    if (points.seeker != null && !isLegacyPlaceholder(points.seeker, i, true)) {
                        round.seekerPos=points.seeker.toVec3d(); if(points.seeker.yaw!=null)round.seekerYaw=points.seeker.yaw;if(points.seeker.pitch!=null)round.seekerPitch=points.seeker.pitch;
                    }
                }
            }
            if (data.extraSeekerPrepare != null) {
                for (Point p : data.extraSeekerPrepare) if (p != null) EXTRA_SEEKER_PREPARE.add(new ExtraSeekerPoint(p.toVec3d(), p.yaw==null?GameConfig.LOBBY_YAW:p.yaw, p.pitch==null?GameConfig.LOBBY_PITCH:p.pitch));
            }
            save();
        } catch (IOException | RuntimeException e) { System.err.println("[IMBA] Не удалось загрузить координаты телепортации: " + e.getMessage()); }
    }

    public static void save() {
        List<RoundPoints> rounds=new ArrayList<>(); for(RoundDefinition r:GameConfig.ROUNDS)rounds.add(new RoundPoints(Point.from(r.hiderPos,r.hiderYaw,r.hiderPitch),Point.from(r.seekerPos,r.seekerYaw,r.seekerPitch)));
        List<Point> extras=EXTRA_SEEKER_PREPARE.stream().map(p->p==null?null:Point.from(p.pos,p.yaw,p.pitch)).toList();
        Data data=new Data(Point.from(GameConfig.LOBBY_POS,GameConfig.LOBBY_YAW,GameConfig.LOBBY_PITCH),rounds,extras);
        try{Files.createDirectories(PATH.getParent());Path tmp=PATH.resolveSibling(PATH.getFileName()+".tmp");Files.writeString(tmp,GSON.toJson(data));Files.move(tmp,PATH,StandardCopyOption.REPLACE_EXISTING);}catch(IOException e){System.err.println("[IMBA] Не удалось сохранить координаты телепортации: "+e.getMessage());}
    }

    public static ExtraSeekerPoint getExtraSeekerPrepare(int zeroBasedExtraIndex){return zeroBasedExtraIndex>=0&&zeroBasedExtraIndex<EXTRA_SEEKER_PREPARE.size()?EXTRA_SEEKER_PREPARE.get(zeroBasedExtraIndex):null;}
    public static void setExtraSeekerPrepare(int zeroBasedExtraIndex,Vec3d pos,float yaw,float pitch){while(EXTRA_SEEKER_PREPARE.size()<=zeroBasedExtraIndex)EXTRA_SEEKER_PREPARE.add(null);EXTRA_SEEKER_PREPARE.set(zeroBasedExtraIndex,new ExtraSeekerPoint(pos,yaw,pitch));while(!EXTRA_SEEKER_PREPARE.isEmpty()&&EXTRA_SEEKER_PREPARE.get(EXTRA_SEEKER_PREPARE.size()-1)==null)EXTRA_SEEKER_PREPARE.remove(EXTRA_SEEKER_PREPARE.size()-1);}
    public static List<ExtraSeekerPoint> extraSeekerPrepareSnapshot(){return java.util.Collections.unmodifiableList(new ArrayList<>(EXTRA_SEEKER_PREPARE));}
    public record ExtraSeekerPoint(Vec3d pos,float yaw,float pitch){}

    private record Data(Point lobby,List<RoundPoints> rounds,List<Point> extraSeekerPrepare){}
    private record RoundPoints(Point hider,Point seeker){}
    private static boolean isLegacyPlaceholder(Point p,int i,boolean seeker){double base=100.5D+i*20.0D,expectedX=seeker?base+8.0D:base;return Math.abs(p.x-expectedX)<.001D&&Math.abs(p.z-base)<.001D;}
    private record Point(double x,double y,double z,Float yaw,Float pitch){static Point from(Vec3d v,float yaw,float pitch){return new Point(v.x,v.y,v.z,yaw,pitch);}Vec3d toVec3d(){return new Vec3d(x,y,z);}}
}
