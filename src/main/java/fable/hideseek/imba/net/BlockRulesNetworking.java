package fable.hideseek.imba.net;

import fable.hideseek.imba.config.BreakRulesConfig;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Set;

public final class BlockRulesNetworking {
    public static final Identifier REQUEST=new Identifier("imba","block_rules_request"),SET=new Identifier("imba","block_rules_set"),SYNC=new Identifier("imba","block_rules_sync");
    private BlockRulesNetworking(){}
    public static void register(){
        ServerPlayNetworking.registerGlobalReceiver(REQUEST,(server,player,handler,buf,sender)->server.execute(()->{if(!player.hasPermissionLevel(2)){player.sendMessage(Text.literal("§cДля настройки исключений нужны права оператора"),true);return;}sendSync(player);}));
        ServerPlayNetworking.registerGlobalReceiver(SET,(server,player,handler,buf,sender)->{int type=buf.readByte();String rawId=buf.readString(256);boolean enabled=buf.readBoolean();server.execute(()->{if(!player.hasPermissionLevel(2))return;Identifier id=Identifier.tryParse(rawId);if(id==null||!Registries.BLOCK.containsId(id)){player.sendMessage(Text.literal("§cНеизвестный блок: §f"+rawId),true);return;}if(type==0)BreakRulesConfig.setInteractiveAllowed(id,enabled);else if(type==1)BreakRulesConfig.setAdventureBreakAllowed(id,enabled);else return;sendSync(player);});});
    }
    public static void sendSync(ServerPlayerEntity player){Set<String> interactive=BreakRulesConfig.interactiveExceptionsSnapshot(),breakable=BreakRulesConfig.adventureBreakSnapshot();PacketByteBuf out=PacketByteBufs.create();out.writeVarInt(interactive.size());for(String id:interactive)out.writeString(id,256);out.writeVarInt(breakable.size());for(String id:breakable)out.writeString(id,256);ServerPlayNetworking.send(player,SYNC,out);}
}
