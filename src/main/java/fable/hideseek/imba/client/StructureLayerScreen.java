package fable.hideseek.imba.client;

import fable.hideseek.imba.net.RoundRestoreNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class StructureLayerScreen extends Screen {
    private List<RoundRestoreClientNetworking.Layer> layers = new ArrayList<>();
    private int index;
    private TextFieldWidget name;
    private ButtonWidget enabledButton;
    private boolean enabled = true;

    public StructureLayerScreen() { super(Text.literal("Восстановление слоёв структуры")); }

    @Override protected void init() {
        int left = width/2-130, top = Math.max(18, height/2-90);
        addDrawableChild(ButtonWidget.builder(Text.literal("◀"), b -> select(index-1)).dimensions(left,top,38,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("▶"), b -> select(index+1)).dimensions(left+222,top,38,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Новый слой"), b -> select(layers.size())).dimensions(left+46,top,168,20).build());
        name = new TextFieldWidget(textRenderer,left,top+30,260,20,Text.literal("Название слоя")); addDrawableChild(name);
        enabledButton = addDrawableChild(ButtonWidget.builder(enabledText(), b -> {
            enabled=!enabled; b.setMessage(enabledText());
            if (index < layers.size()) { PacketByteBuf buf=PacketByteBufs.create(); buf.writeVarInt(index); buf.writeBoolean(enabled); ClientPlayNetworking.send(RoundRestoreNetworking.TOGGLE_LAYER,buf); }
        }).dimensions(left,top+56,260,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Сохранить выбранную область как слой"), b -> save()).dimensions(left,top+82,260,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Удалить слой"), b -> delete()).dimensions(left,top+108,126,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), b -> close()).dimensions(left+134,top+108,126,20).build());
        ClientPlayNetworking.send(RoundRestoreNetworking.REQUEST, PacketByteBufs.empty());
        layers = new ArrayList<>(RoundRestoreClientNetworking.layers()); select(Math.min(index,layers.size()));
    }
    public void applyLayers(List<RoundRestoreClientNetworking.Layer> values) { layers = new ArrayList<>(values); select(Math.min(index,layers.size())); }
    private void select(int next) {
        index = Math.max(0, Math.min(next, layers.size()));
        if (name == null) return;
        if (index < layers.size()) { var layer=layers.get(index); name.setText(layer.name()); enabled=layer.enabled(); }
        else { name.setText("Слой " + (layers.size()+1)); enabled=true; }
        if (enabledButton != null) enabledButton.setMessage(enabledText());
    }
    private Text enabledText() { return Text.literal(enabled ? "Автовосстановление: ВКЛ" : "Автовосстановление: ВЫКЛ"); }
    private void save() { PacketByteBuf buf=PacketByteBufs.create(); buf.writeVarInt(index < layers.size()?index:-1); buf.writeString(name.getText(),64); ClientPlayNetworking.send(RoundRestoreNetworking.SAVE_LAYER,buf); }
    private void delete() { if (index>=layers.size()) return; PacketByteBuf buf=PacketByteBufs.create(); buf.writeVarInt(index); ClientPlayNetworking.send(RoundRestoreNetworking.DELETE_LAYER,buf); index=Math.max(0,index-1); }
    @Override public void render(DrawContext c,int mx,int my,float d) {
        renderBackground(c); int top=Math.max(18,height/2-90);
        c.drawCenteredTextWithShadow(textRenderer,title,width/2,top-17,0xFFFFFF);
        c.drawCenteredTextWithShadow(textRenderer, index<layers.size()?"Слой "+(index+1)+"/"+layers.size()+" • "+layers.get(index).blocks()+" блоков":"Новый слой",width/2,top+23,0xAAAAAA);
        c.drawCenteredTextWithShadow(textRenderer,"ПКМ по блоку = точка A • Shift+ПКМ = точка B",width/2,top+136,0xFFFFAA);
        super.render(c,mx,my,d);
    }
    @Override public boolean shouldPause(){return false;}
}
