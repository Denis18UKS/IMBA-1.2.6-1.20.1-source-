package fable.hideseek.imba.net;

import fable.hideseek.imba.config.HologramConfig;
import fable.hideseek.imba.game.GameConfig;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class HologramNetworking {
    public static final Identifier REQUEST=new Identifier("imba","hologram_request"), SAVE=new Identifier("imba","hologram_save"), DELETE=new Identifier("imba","hologram_delete"), SYNC=new Identifier("imba","hologram_sync");
    private HologramNetworking() {}
    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST,(server,player,handler,buf,sender)->server.execute(()->{if(player.hasPermissionLevel(2))sendSync(player);}));
        ServerPlayNetworking.registerGlobalReceiver(SAVE,(server,player,handler,buf,sender)->{
            int id=buf.readInt(), location=buf.readVarInt(); String world=buf.readString(128);
            double x=buf.readDouble(), y=buf.readDouble(), z=buf.readDouble(); float yaw=buf.readFloat(), scale=buf.readFloat();
            int light=buf.readByte(); boolean textBackground=buf.readBoolean(); float contrast=buf.readFloat();
            float textScale=buf.readFloat(); int titleBreak=buf.readInt();
            server.execute(()->{
                if(!player.hasPermissionLevel(2)||location<0||location>=GameConfig.ROUNDS.size()||Identifier.tryParse(world)==null
                        ||!finite(x,y,z,yaw,scale,contrast,textScale)||titleBreak<0||titleBreak>256)return;
                HologramConfig.saveProjector(id,location,world,x,y,z,yaw,scale,light,textBackground,contrast,textScale,titleBreak);
                broadcast(server); player.sendMessage(Text.literal("§aГолограмма сохранена: §f"+GameConfig.getLocationName(location)),true);
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(DELETE,(server,player,handler,buf,sender)->{int id=buf.readInt();server.execute(()->{if(player.hasPermissionLevel(2)&&HologramConfig.delete(id))broadcast(server);});});
    }
    private static boolean finite(double x,double y,double z,float yaw,float scale,float contrast,float textScale){return Double.isFinite(x)&&Double.isFinite(y)&&Double.isFinite(z)&&Float.isFinite(yaw)&&Float.isFinite(scale)&&Float.isFinite(contrast)&&Float.isFinite(textScale)&&Math.abs(x)<=3e7&&Math.abs(y)<=2048&&Math.abs(z)<=3e7&&scale>0&&scale<=8&&contrast>=.5f&&contrast<=2.0f&&textScale>=.5f&&textScale<=3.0f;}
    public static void broadcast(MinecraftServer server){if(server==null)return;for(ServerPlayerEntity p:PlayerLookup.all(server))sendSync(p);}
    public static void sendSync(ServerPlayerEntity player){var list=HologramConfig.snapshot();PacketByteBuf out=PacketByteBufs.create();out.writeVarInt(list.size());for(var p:list){out.writeInt(p.id);out.writeVarInt(p.location);out.writeString(p.world,128);out.writeDouble(p.x);out.writeDouble(p.y);out.writeDouble(p.z);out.writeFloat(p.yaw);out.writeFloat(p.scale);out.writeByte(p.light);out.writeBoolean(p.textBackground);out.writeFloat(p.contrast);out.writeFloat(p.textScale);out.writeInt(p.titleBreak);}ServerPlayNetworking.send(player,SYNC,out);}
}
