package fable.hideseek.imba.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class ClientLocationPhotos {
    private static final Map<Integer,Identifier> TEXTURES=new HashMap<>();
    private static final Map<Integer,byte[]> SOURCE_PNGS=new HashMap<>();
    private static final Map<String,Identifier> HOLOGRAM_TEXTURES=new HashMap<>();
    private static final Identifier PLACEHOLDER=new Identifier("minecraft","textures/block/light_gray_concrete.png");
    private ClientLocationPhotos(){}
    public static Identifier texture(int location){return TEXTURES.getOrDefault(location,PLACEHOLDER);}
    public static Identifier hologramTexture(int location,int light){int level=Math.max(0,Math.min(15,light));byte[] png=SOURCE_PNGS.get(location);if(png==null||png.length==0)return texture(location);String key=location+":"+level;Identifier cached=HOLOGRAM_TEXTURES.get(key);if(cached!=null)return cached;try{NativeImage image=NativeImage.read(new ByteArrayInputStream(png));double t=level/15.0D,gamma=1.0D-.48D*t,gain=1.0D+.22D*t;for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){int abgr=image.getColor(x,y);int r=brighten(abgr&255,gamma,gain),g=brighten((abgr>>>8)&255,gamma,gain),b=brighten((abgr>>>16)&255,gamma,gain);image.setColor(x,y,0xFF000000|(b<<16)|(g<<8)|r);}Identifier id=MinecraftClient.getInstance().getTextureManager().registerDynamicTexture("imba_hologram_"+(location+1)+"_light_"+level,new NativeImageBackedTexture(image));HOLOGRAM_TEXTURES.put(key,id);return id;}catch(IOException ignored){return texture(location);}}
    private static int brighten(int channel,double gamma,double gain){double n=Math.max(0,Math.min(1,channel/255.0D));return Math.max(0,Math.min(255,(int)Math.round(Math.pow(n,gamma)*255.0D*gain)));}
    public static boolean has(int location){return TEXTURES.containsKey(location);}
    public static void apply(int location,byte[] png){remove(location);if(png==null||png.length==0)return;SOURCE_PNGS.put(location,Arrays.copyOf(png,png.length));try{NativeImage image=NativeImage.read(new ByteArrayInputStream(png));Identifier id=MinecraftClient.getInstance().getTextureManager().registerDynamicTexture("imba_location_"+(location+1),new NativeImageBackedTexture(image));TEXTURES.put(location,id);}catch(IOException ignored){}}
    public static void remove(int location){Identifier old=TEXTURES.remove(location);if(old!=null)MinecraftClient.getInstance().getTextureManager().destroyTexture(old);SOURCE_PNGS.remove(location);for(String key:java.util.List.copyOf(HOLOGRAM_TEXTURES.keySet()))if(key.startsWith(location+":")){Identifier v=HOLOGRAM_TEXTURES.remove(key);if(v!=null)MinecraftClient.getInstance().getTextureManager().destroyTexture(v);}}
    public static void removeAll(){for(int location:java.util.List.copyOf(TEXTURES.keySet()))remove(location);}
}
