package fable.hideseek.imba.client;
import fable.hideseek.imba.net.MaskNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
public final class GameSettingsScreen extends Screen {
    private int seconds=PanelData.seconds,hearts=PanelData.hearts,location=PanelData.selectedLocation;
    public GameSettingsScreen(){super(Text.literal("Настройки IMBA"));}
    @Override protected void init(){
        int x=width/2,y=height/2;
        int[] columns={x-100,x-35,x+30};
        for(int i=0;i<3;i++){
            final int column=i;
            addDrawableChild(ButtonWidget.builder(Text.literal("▲"),b->change(column,1)).dimensions(columns[i],y-45,70,20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("▼"),b->change(column,-1)).dimensions(columns[i],y+25,70,20).build());
        }
    }
    private void change(int column,int direction){
        if(column==0)seconds=Math.max(30,Math.min(3600,seconds+direction*30));
        else if(column==1)location=Math.floorMod(location+direction,Math.max(1,PanelData.locationCount));
        else hearts=Math.max(1,Math.min(100,hearts+direction));
        var buf=PacketByteBufs.create();buf.writeVarInt(seconds);buf.writeVarInt(hearts);buf.writeVarInt(location);
        ClientPlayNetworking.send(MaskNetworking.GAME_SETTINGS_PACKET,buf);
    }
    @Override public void render(DrawContext c,int mx,int my,float d){
        renderBackground(c);int x=width/2,y=height/2;
        c.drawCenteredTextWithShadow(textRenderer,"Таймер",x-65,y-65,0xffe0b0);
        c.drawCenteredTextWithShadow(textRenderer,"Локация",x,y-65,0xffe0b0);
        c.drawCenteredTextWithShadow(textRenderer,"Количество",x+65,y-65,0xffe0b0);
        c.drawCenteredTextWithShadow(textRenderer,String.format("%02d:%02d",seconds/60,seconds%60),x-65,y-8,0xffffff);
        c.drawCenteredTextWithShadow(textRenderer,(location+1)+"/"+Math.max(1,PanelData.locationCount),x,y-8,0xffffff);
        c.drawCenteredTextWithShadow(textRenderer,"❤ "+hearts,x+65,y-8,0xff5555);super.render(c,mx,my,d);
    }
    @Override public boolean shouldPause(){return false;}
}
