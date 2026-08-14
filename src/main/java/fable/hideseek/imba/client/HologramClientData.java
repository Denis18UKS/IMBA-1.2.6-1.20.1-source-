package fable.hideseek.imba.client;
import java.util.ArrayList;import java.util.List;
public final class HologramClientData {
    public record Projector(int id,int location,String world,double x,double y,double z,float yaw,float scale,int light){}
    private static final List<Projector> VALUES=new ArrayList<>(); private HologramClientData(){}
    public static void set(List<Projector> next){VALUES.clear();VALUES.addAll(next);}
    public static List<Projector> snapshot(){return List.copyOf(VALUES);}
}
