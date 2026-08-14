package fable.hideseek.imba.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persistent snapshots restored whenever an IMBA round ends. */
public final class RoundRestoreConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("imba_round_restore.json");
    public static final int MAX_LAYER_BLOCKS = 100_000;
    private static Data data = new Data();
    private RoundRestoreConfig() {}
    public static void load() {
        data = new Data();
        if (!Files.exists(PATH)) { save(); return; }
        try { Data loaded=GSON.fromJson(Files.readString(PATH),Data.class); if(loaded!=null)data=loaded; if(data.blocks==null)data.blocks=new ArrayList<>(); if(data.layers==null)data.layers=new ArrayList<>(); }
        catch(IOException|RuntimeException e){System.err.println("[IMBA] Не удалось загрузить восстановление раунда: "+e.getMessage());}
    }
    public static void save(){try{Files.createDirectories(PATH.getParent());Path tmp=PATH.resolveSibling(PATH.getFileName()+".tmp");Files.writeString(tmp,GSON.toJson(data));Files.move(tmp,PATH,StandardCopyOption.REPLACE_EXISTING);}catch(IOException e){System.err.println("[IMBA] Не удалось сохранить восстановление раунда: "+e.getMessage());}}
    public static int saveSingle(ServerWorld world,BlockPos pos){Snapshot s=capture(world,pos);if(s==null)return-1;data.blocks.removeIf(o->o.samePosition(s));data.blocks.add(s);save();return data.blocks.size();}
    public static boolean removeSingle(ServerWorld world,BlockPos pos){String id=world.getRegistryKey().getValue().toString();boolean r=data.blocks.removeIf(s->s.world.equals(id)&&s.x==pos.getX()&&s.y==pos.getY()&&s.z==pos.getZ());if(r)save();return r;}
    public static LayerMeta saveLayer(ServerWorld world,BlockPos a,BlockPos b,int replaceIndex,String name){
        int minX=Math.min(a.getX(),b.getX()),maxX=Math.max(a.getX(),b.getX()),minY=Math.min(a.getY(),b.getY()),maxY=Math.max(a.getY(),b.getY()),minZ=Math.min(a.getZ(),b.getZ()),maxZ=Math.max(a.getZ(),b.getZ());
        long volume=(long)(maxX-minX+1)*(maxY-minY+1)*(maxZ-minZ+1);if(volume<=0||volume>MAX_LAYER_BLOCKS)return null;
        List<Snapshot> snapshots=new ArrayList<>((int)volume);for(BlockPos pos:BlockPos.iterate(minX,minY,minZ,maxX,maxY,maxZ)){Snapshot s=capture(world,pos.toImmutable());if(s!=null)snapshots.add(s);}
        String clean=name==null?"":name.strip();if(clean.isEmpty())clean="Слой "+(replaceIndex>=0?replaceIndex+1:data.layers.size()+1);if(clean.length()>64)clean=clean.substring(0,64);
        Layer layer=new Layer(clean,true,snapshots);if(replaceIndex>=0&&replaceIndex<data.layers.size())data.layers.set(replaceIndex,layer);else data.layers.add(layer);save();int index=replaceIndex>=0&&replaceIndex<data.layers.size()?replaceIndex:data.layers.size()-1;return new LayerMeta(index,layer.name,layer.enabled,layer.blocks.size());
    }
    public static boolean setLayerEnabled(int index,boolean enabled){if(index<0||index>=data.layers.size())return false;data.layers.get(index).enabled=enabled;save();return true;}
    public static boolean deleteLayer(int index){if(index<0||index>=data.layers.size())return false;data.layers.remove(index);save();return true;}
    public static List<LayerMeta> layerMetadata(){List<LayerMeta> r=new ArrayList<>();for(int i=0;i<data.layers.size();i++){Layer l=data.layers.get(i);r.add(new LayerMeta(i,l.name,l.enabled,l.blocks==null?0:l.blocks.size()));}return r;}
    public static void restoreAll(MinecraftServer server){if(server==null)return;for(Snapshot s:List.copyOf(data.blocks))restore(server,s);for(Layer l:List.copyOf(data.layers)){if(!l.enabled||l.blocks==null)continue;for(Snapshot s:l.blocks)restore(server,s);}}
    private static Snapshot capture(ServerWorld world,BlockPos pos){BlockState state=world.getBlockState(pos);Identifier blockId=Registries.BLOCK.getId(state.getBlock());if(blockId==null)return null;Map<String,String> props=new LinkedHashMap<>();for(Property<?> p:state.getProperties())props.put(p.getName(),propertyName(state,p));List<String> inventory=null;BlockEntity be=world.getBlockEntity(pos);if(be instanceof Inventory inv){inventory=new ArrayList<>(inv.size());for(int i=0;i<inv.size();i++){ItemStack stack=inv.getStack(i);inventory.add(stack.isEmpty()?"":stack.writeNbt(new NbtCompound()).asString());}}return new Snapshot(world.getRegistryKey().getValue().toString(),pos.getX(),pos.getY(),pos.getZ(),blockId.toString(),props,inventory);}
    @SuppressWarnings({"rawtypes","unchecked"}) private static String propertyName(BlockState state,Property property){return property.name(state.get(property));}
    @SuppressWarnings({"rawtypes","unchecked"}) private static BlockState applyProperty(BlockState state,String name,String value){for(Property property:state.getProperties()){if(!property.getName().equals(name))continue;var parsed=property.parse(value);if(parsed.isPresent())return state.with(property,(Comparable)parsed.get());}return state;}
    private static void restore(MinecraftServer server,Snapshot s){try{Identifier worldId=new Identifier(s.world);RegistryKey<net.minecraft.world.World> worldKey=RegistryKey.of(RegistryKeys.WORLD,worldId);ServerWorld world=server.getWorld(worldKey);Identifier blockId=new Identifier(s.block);if(world==null||!Registries.BLOCK.containsId(blockId))return;Block block=Registries.BLOCK.get(blockId);BlockState state=block.getDefaultState();if(s.properties!=null)for(var e:s.properties.entrySet())state=applyProperty(state,e.getKey(),e.getValue());BlockPos pos=new BlockPos(s.x,s.y,s.z);world.setBlockState(pos,state,Block.NOTIFY_ALL);if(s.inventory!=null&&world.getBlockEntity(pos) instanceof Inventory inv){inv.clear();for(int i=0;i<Math.min(inv.size(),s.inventory.size());i++){String snbt=s.inventory.get(i);if(snbt==null||snbt.isEmpty())continue;NbtCompound nbt=StringNbtReader.parse(snbt);inv.setStack(i,ItemStack.fromNbt(nbt));}if(world.getBlockEntity(pos)!=null)world.getBlockEntity(pos).markDirty();}}catch(Exception ignored){}}
    public record LayerMeta(int index,String name,boolean enabled,int blockCount){}
    private static final class Data{List<Snapshot> blocks=new ArrayList<>();List<Layer> layers=new ArrayList<>();}
    private static final class Layer{String name;boolean enabled;List<Snapshot> blocks;Layer(String n,boolean e,List<Snapshot>b){name=n;enabled=e;blocks=b;}}
    private static final class Snapshot{String world;int x,y,z;String block;Map<String,String>properties;List<String>inventory;Snapshot(String w,int x,int y,int z,String b,Map<String,String>p,List<String>i){world=w;this.x=x;this.y=y;this.z=z;block=b;properties=p;inventory=i;}boolean samePosition(Snapshot o){return world.equals(o.world)&&x==o.x&&y==o.y&&z==o.z;}}
}
