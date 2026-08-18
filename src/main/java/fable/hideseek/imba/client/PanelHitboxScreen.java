package fable.hideseek.imba.client;

import fable.hideseek.imba.config.PanelSettingsConfig;
import fable.hideseek.imba.net.PanelSettingsNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import java.util.Locale;
public final class PanelHitboxScreen extends Screen {
    private final TextFieldWidget[][] fields=new TextFieldWidget[4][4];public PanelHitboxScreen(){super(Text.literal("Хитбоксы стрелок панели"));}
    @Override protected void init(){int left=width/2-270,top=Math.max(18,height/2-135);String[] names={"Таймер ▲","Таймер ▼","Сердца ▲","Сердца ▼"};for(int row=0;row<4;row++){int y=top+54+row*42;for(int col=0;col<4;col++){fields[row][col]=new TextFieldWidget(textRenderer,left+112+col*88,y,78,20,Text.empty());fields[row][col].setMaxLength(8);addDrawableChild(fields[row][col]);}addDrawableChild(ButtonWidget.builder(Text.literal(names[row]),b->{}).dimensions(left,y,104,20).build());}addDrawableChild(ButtonWidget.builder(Text.literal("Сохранить hitbox-области"),b->send()).dimensions(left,top+232,260,20).build());addDrawableChild(ButtonWidget.builder(Text.literal("Назад"),b->client.setScreen(new GameSettingsScreen())).dimensions(left+280,top+232,260,20).build());applyPanelLayout();}
    public void applyPanelLayout(){if(fields[0][0]==null)return;PanelSettingsConfig.Hitbox[] h={PanelData.timerUpHitbox,PanelData.timerDownHitbox,PanelData.heartsUpHitbox,PanelData.heartsDownHitbox};for(int r=0;r<4;r++){fields[r][0].setText(fmt(h[r].x));fields[r][1].setText(fmt(h[r].y));fields[r][2].setText(fmt(h[r].width));fields[r][3].setText(fmt(h[r].height));}}
    private String fmt(float v){return String.format(Locale.ROOT,"%.1f",v);} private float parse(TextFieldWidget f){return Float.parseFloat(f.getText().replace(',','.'));}
    private void send(){try{PacketByteBuf b=PacketByteBufs.create();for(int r=0;r<4;r++)for(int c=0;c<4;c++)b.writeFloat(parse(fields[r][c]));ClientPlayNetworking.send(PanelSettingsNetworking.SET_HITBOXES,b);}catch(NumberFormatException ignored){}}
    @Override public void render(DrawContext c,int mx,int my,float d){renderBackground(c);int left=width/2-270,top=Math.max(18,height/2-135);c.drawCenteredTextWithShadow(textRenderer,title,width/2,top,0xFFFFFFFF);c.drawCenteredTextWithShadow(textRenderer,"В режиме редактирования рамки этих областей видны прямо на 3×3 панели",width/2,top+18,0xFFAAAAAA);c.drawTextWithShadow(textRenderer,"X",left+142,top+40,0xFFCCCCCC);c.drawTextWithShadow(textRenderer,"Y",left+230,top+40,0xFFCCCCCC);c.drawTextWithShadow(textRenderer,"Ширина",left+306,top+40,0xFFCCCCCC);c.drawTextWithShadow(textRenderer,"Высота",left+398,top+40,0xFFCCCCCC);c.drawTextWithShadow(textRenderer,"Клик срабатывает только внутри рамки. Координаты совпадают с координатами текста панели.",left,top+214,0xFF888888);super.render(c,mx,my,d);}
    @Override public boolean shouldPause(){return false;}
}
