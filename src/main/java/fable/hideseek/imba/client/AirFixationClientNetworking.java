package fable.hideseek.imba.client;

import fable.hideseek.imba.net.AirFixationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AirFixationClientNetworking {
    private AirFixationClientNetworking(){}
    public static void register(){ClientPlayNetworking.registerGlobalReceiver(AirFixationNetworking.SYNC,(client,handler,buf,sender)->{int count=Math.max(0,Math.min(buf.readVarInt(),100000));Map<String,Boolean> values=new LinkedHashMap<>();for(int i=0;i<count;i++)values.put(buf.readString(320),buf.readBoolean());client.execute(()->{if(client.currentScreen instanceof AirFixationConfigScreen screen)screen.applyServerState(values);});});}
}
