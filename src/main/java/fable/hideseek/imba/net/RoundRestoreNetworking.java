package fable.hideseek.imba.net;

import fable.hideseek.imba.ImbaExtension;
import fable.hideseek.imba.config.RoundRestoreConfig;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import java.util.List;

public final class RoundRestoreNetworking {
 public static final Identifier REQUEST=new Identifier("imba","restore_layer_request"),SAVE_LAYER=new Identifier("imba","restore_layer_save"),TOGGLE_LAYER=new Identifier("imba","restore_layer_toggle"),DELETE_LAYER=new Identifier("imba","restore_layer_delete"),SYNC=new Identifier("imba","restore_layer_sync");private RoundRestoreNetworking(){}
 public static void register(){
  ServerPlayNetworking.registerGlobalReceiver(REQUEST,(server,player,handler,buf,sender)->server.execute(()->{if(player.hasPermissionLevel(2))sendSync(player);}));
  ServerPlayNetworking.registerGlobalReceiver(SAVE_LAYER,(server,player,handler,buf,sender)->{int index=buf.readVarInt();String name=buf.readString(64);server.execute(()->{if(!player.hasPermissionLevel(2))return;ItemStack tool=findTool(player);Selection selection=readSelection(tool);if(selection==null){player.sendMessage(Text.literal("§cСначала укажите точки A (ПКМ) и B (Shift+ПКМ) в одном мире"),true);return;}ServerWorld world=server.getWorld(selection.worldKey());if(world==null)return;long volume=selection.volume();if(volume>RoundRestoreConfig.MAX_LAYER_BLOCKS){player.sendMessage(Text.literal("§cСлой слишком большой: §f"+volume+" §cблоков (макс. "+RoundRestoreConfig.MAX_LAYER_BLOCKS+")"),true);return;}var meta=RoundRestoreConfig.saveLayer(world,selection.a(),selection.b(),index,name);if(meta==null){player.sendMessage(Text.literal("§cНе удалось сохранить слой"),true);return;}player.sendMessage(Text.literal("§aСохранён §f"+meta.name()+" §7("+meta.blockCount()+" блоков)"),true);sendSync(player);});});
  ServerPlayNetworking.registerGlobalReceiver(TOGGLE_LAYER,(server,player,handler,buf,sender)->{int index=buf.readVarInt();boolean enabled=buf.readBoolean();server.execute(()->{if(player.hasPermissionLevel(2)&&RoundRestoreConfig.setLayerEnabled(index,enabled))sendSync(player);});});
  ServerPlayNetworking.registerGlobalReceiver(DELETE_LAYER,(server,player,handler,buf,sender)->{int index=buf.readVarInt();server.execute(()->{if(player.hasPermissionLevel(2)&&RoundRestoreConfig.deleteLayer(index))sendSync(player);});});
 }
 private static ItemStack findTool(ServerPlayerEntity player){for(int i=0;i<player.getInventory().size();i++){ItemStack stack=player.getInventory().getStack(i);if(stack.isOf(ImbaExtension.STRUCTURE_LAYER_TOOL))return stack;}return ItemStack.EMPTY;}
 private static Selection readSelection(ItemStack tool){if(tool.isEmpty()||tool.getNbt()==null)return null;NbtCompound nbt=tool.getNbt();if(!nbt.contains("imba_layer_a_world")||!nbt.contains("imba_layer_b_world"))return null;String aWorld=nbt.getString("imba_layer_a_world"),bWorld=nbt.getString("imba_layer_b_world");if(!aWorld.equals(bWorld))return null;Identifier id=Identifier.tryParse(aWorld);if(id==null)return null;var key=net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD,id);BlockPos a=new BlockPos(nbt.getInt("imba_layer_a_x"),nbt.getInt("imba_layer_a_y"),nbt.getInt("imba_layer_a_z")),b=new BlockPos(nbt.getInt("imba_layer_b_x"),nbt.getInt("imba_layer_b_y"),nbt.getInt("imba_layer_b_z"));return new Selection(key,a,b);}
 public static void sendSync(ServerPlayerEntity player){List<RoundRestoreConfig.LayerMeta> layers=RoundRestoreConfig.layerMetadata();PacketByteBuf out=PacketByteBufs.create();out.writeVarInt(layers.size());for(var layer:layers){out.writeString(layer.name(),64);out.writeBoolean(layer.enabled());out.writeVarInt(layer.blockCount());}ServerPlayNetworking.send(player,SYNC,out);}
 private record Selection(net.minecraft.registry.RegistryKey<net.minecraft.world.World> worldKey,BlockPos a,BlockPos b){long volume(){return(long)(Math.abs(a.getX()-b.getX())+1)*(Math.abs(a.getY()-b.getY())+1)*(Math.abs(a.getZ()-b.getZ())+1);}}
}
