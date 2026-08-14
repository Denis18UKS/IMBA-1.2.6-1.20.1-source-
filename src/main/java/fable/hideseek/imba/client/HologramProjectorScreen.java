package fable.hideseek.imba.client;

import fable.hideseek.imba.game.GameConfig;
import fable.hideseek.imba.net.HologramNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HologramProjectorScreen extends Screen {
    private List<HologramClientData.Projector> projectors=new ArrayList<>(); private int index, location, light=15; private boolean lightingTab;
    private TextFieldWidget x,y,z,yaw,scale; private ButtonWidget locationButton,tabButton,lightButton;
    public HologramProjectorScreen(){super(Text.literal("Голопроектор локаций"));}
    @Override protected void init(){
        int l=width/2-130,t=Math.max(12,height/2-115);
        addDrawableChild(ButtonWidget.builder(Text.literal("◀"),b->select(index-1)).dimensions(l,t,38,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Новая"),b->select(projectors.size())).dimensions(l+46,t,168,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("▶"),b->select(index+1)).dimensions(l+222,t,38,20).build());
        tabButton=addDrawableChild(ButtonWidget.builder(tabText(),b->{lightingTab=!lightingTab;b.setMessage(tabText());updateVisibility();}).dimensions(l,t+26,260,20).build());
        locationButton=addDrawableChild(ButtonWidget.builder(locationText(),b->{location=(location+1)%GameConfig.ROUNDS.size();b.setMessage(locationText());}).dimensions(l,t+52,260,20).build());
        var p=MinecraftClient.getInstance().player;
        x=field(l,t+78,p==null?0:p.getX()); y=field(l,t+102,p==null?0:p.getY()); z=field(l,t+126,p==null?0:p.getZ()); yaw=field(l,t+150,p==null?0:p.getYaw()); scale=field(l,t+174,1.0);
        lightButton=addDrawableChild(ButtonWidget.builder(lightText(),b->{light=(light+1)%16;b.setMessage(lightText());}).dimensions(l,t+78,260,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Сохранить"),b->save()).dimensions(l,t+200,126,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Удалить"),b->delete()).dimensions(l+134,t+200,126,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"),b->close()).dimensions(l,t+226,260,20).build());
        projectors=new ArrayList<>(HologramClientData.snapshot()); select(Math.min(index,projectors.size())); updateVisibility();
        ClientPlayNetworking.send(HologramNetworking.REQUEST,PacketByteBufs.empty());
    }
    private TextFieldWidget field(int x,int y,double v){TextFieldWidget f=new TextFieldWidget(textRenderer,x,y,260,20,Text.empty());f.setText(String.format(Locale.ROOT,"%.3f",v));f.setMaxLength(24);addDrawableChild(f);return f;}
    private Text tabText(){return Text.literal(lightingTab?"Вкладка: освещение":"Вкладка: позиция и размер");}
    private Text locationText(){return Text.literal("Локация: "+GameConfig.getLocationName(location));}
    private Text lightText(){return Text.literal("Освещение: "+light+" / 15");}
    private void updateVisibility(){boolean g=!lightingTab;locationButton.visible=g;x.visible=g;y.visible=g;z.visible=g;yaw.visible=g;scale.visible=g;lightButton.visible=!g;}
    public void applyServerState(List<HologramClientData.Projector> list){projectors=new ArrayList<>(list);select(Math.min(index,projectors.size()));}
    private void select(int i){index=Math.max(0,Math.min(i,projectors.size()));if(x==null)return;if(index<projectors.size()){var p=projectors.get(index);location=Math.max(0,Math.min(GameConfig.ROUNDS.size()-1,p.location()));x.setText(fmt(p.x()));y.setText(fmt(p.y()));z.setText(fmt(p.z()));yaw.setText(fmt(p.yaw()));scale.setText(fmt(p.scale()));light=p.light();}else{var p=MinecraftClient.getInstance().player;if(p!=null){x.setText(fmt(p.getX()));y.setText(fmt(p.getY()));z.setText(fmt(p.getZ()));yaw.setText(fmt(p.getYaw()));}scale.setText("1.000");light=15;}locationButton.setMessage(locationText());lightButton.setMessage(lightText());}
    private static String fmt(double d){return String.format(Locale.ROOT,"%.3f",d);} private static double parse(TextFieldWidget f){return Double.parseDouble(f.getText().replace(',','.'));}
    private void save(){try{PacketByteBuf b=PacketByteBufs.create();b.writeInt(index<projectors.size()?projectors.get(index).id():-1);b.writeVarInt(location);var client=MinecraftClient.getInstance();String world=client.world==null?"minecraft:overworld":client.world.getRegistryKey().getValue().toString();b.writeString(world,128);b.writeDouble(parse(x));b.writeDouble(parse(y));b.writeDouble(parse(z));b.writeFloat((float)parse(yaw));b.writeFloat((float)parse(scale));b.writeByte(light);ClientPlayNetworking.send(HologramNetworking.SAVE,b);}catch(NumberFormatException ignored){}}
    private void delete(){if(index>=projectors.size())return;PacketByteBuf b=PacketByteBufs.create();b.writeInt(projectors.get(index).id());ClientPlayNetworking.send(HologramNetworking.DELETE,b);index=Math.max(0,index-1);}
    @Override public void render(DrawContext c,int mx,int my,float d){renderBackground(c);int t=Math.max(12,height/2-115);c.drawCenteredTextWithShadow(textRenderer,title,width/2,t-12,0xFFFFFF);c.drawCenteredTextWithShadow(textRenderer,index<projectors.size()?"Голограмма "+(index+1)+" / "+projectors.size():"Новая голограмма",width/2,t+20,0xAAAAAA);if(!lightingTab){c.drawTextWithShadow(textRenderer,"X / Y / Z / Yaw / Размер",width/2-125,t+73,0x888888);}else{c.drawCenteredTextWithShadow(textRenderer,"0 = темно • 15 = максимально ярко",width/2,t+107,0xAAAAAA);}super.render(c,mx,my,d);}
    @Override public boolean shouldPause(){return false;}
}
