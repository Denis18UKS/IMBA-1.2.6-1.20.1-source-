package fable.hideseek.imba.client;
import fable.hideseek.imba.net.HologramNetworking;import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;import java.util.*;
public final class HologramClientNetworking {private HologramClientNetworking(){}
 public static void register(){ClientPlayNetworking.registerGlobalReceiver(HologramNetworking.SYNC,(client,handler,buf,sender)->{int n=Math.max(0,Math.min(buf.readVarInt(),4096));List<HologramClientData.Projector> list=new ArrayList<>();for(int i=0;i<n;i++)list.add(new HologramClientData.Projector(buf.readInt(),buf.readVarInt(),buf.readString(128),buf.readDouble(),buf.readDouble(),buf.readDouble(),buf.readFloat(),buf.readFloat(),buf.readByte(),buf.readBoolean()));client.execute(()->{HologramClientData.set(list);if(client.currentScreen instanceof HologramProjectorScreen s)s.applyServerState(list);});});}
}
