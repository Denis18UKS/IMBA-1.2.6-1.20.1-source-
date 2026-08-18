package fable.hideseek.imba.client;

import fable.hideseek.imba.config.AirFixationConfig;
import fable.hideseek.imba.net.AirFixationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Searchable editor for air-fixation rules, including conditional block rules. */
public final class AirFixationConfigScreen extends Screen {
    private static final int ROW_HEIGHT=24;
    private final List<Entry> allBlocks=new ArrayList<>(),allItems=new ArrayList<>(),filtered=new ArrayList<>();
    private final Map<String,AirFixationConfig.Rule> overrides=new LinkedHashMap<>();
    private TextFieldWidget search; private ButtonWidget tabButton,modeButton,blockButton; private int tab,scroll; private boolean synced; private String selectedKey;

    public AirFixationConfigScreen(){
        super(Text.literal("Фиксация маскировок в воздухе"));
        for(Block block:Registries.BLOCK){Identifier id=Registries.BLOCK.getId(block);if(id!=null&&block.asItem()!=net.minecraft.item.Items.AIR)allBlocks.add(new Entry(AirFixationConfig.blockKey(id),block.getName().getString(),id.toString(),new ItemStack(block.asItem())));}
        for(Item item:Registries.ITEM){Identifier id=Registries.ITEM.getId(item);if(id!=null&&item!=net.minecraft.item.Items.AIR)allItems.add(new Entry(AirFixationConfig.itemKey(id),item.getName().getString(),id.toString(),new ItemStack(item)));}
        Comparator<Entry> cmp=Comparator.comparing(e->e.id);allBlocks.sort(cmp);allItems.sort(cmp);rebuild();
    }

    @Override protected void init(){
        int panelWidth=Math.min(680,width-24),left=(width-panelWidth)/2;
        tabButton=addDrawableChild(ButtonWidget.builder(tabText(),b->{tab=1-tab;b.setMessage(tabText());selectedKey=null;scroll=0;rebuild();refreshRuleButtons();}).dimensions(left+12,34,panelWidth-24,20).build());
        search=new TextFieldWidget(textRenderer,left+12,58,panelWidth-24,20,Text.empty());search.setPlaceholder(Text.literal("Поиск маскировки по названию или ID..."));search.setChangedListener(v->{scroll=0;rebuild();});addDrawableChild(search);
        modeButton=addDrawableChild(ButtonWidget.builder(Text.literal("Выберите маскировку"),b->cycleMode()).dimensions(left+12,height-62,250,20).build());
        blockButton=addDrawableChild(ButtonWidget.builder(Text.literal("Выбрать требуемый блок..."),b->openBlockPicker()).dimensions(left+270,height-62,250,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"),b->close()).dimensions(left+panelWidth-122,height-62,110,20).build());
        refreshRuleButtons();ClientPlayNetworking.send(AirFixationNetworking.REQUEST,PacketByteBufs.empty());
    }
    private Text tabText(){return Text.literal(tab==0?"Тип: блоковые маскировки":"Тип: предметные маскировки");}
    public void applyServerState(Map<String,AirFixationConfig.Rule> values){overrides.clear();overrides.putAll(values);synced=true;refreshRuleButtons();}
    private AirFixationConfig.Rule effective(String key){AirFixationConfig.Rule v=overrides.get(key);return v!=null?v:AirFixationConfig.defaultRule(key);}
    private void cycleMode(){if(selectedKey==null||!synced)return;AirFixationConfig.Rule current=effective(selectedKey);AirFixationConfig.Mode next=switch(current.mode()){case DENY->AirFixationConfig.Mode.ALLOW;case ALLOW->AirFixationConfig.Mode.REQUIRE_BLOCK;case REQUIRE_BLOCK->AirFixationConfig.Mode.DENY;};String required=next==AirFixationConfig.Mode.REQUIRE_BLOCK?(current.requiredBlock().isBlank()?"minecraft:water":current.requiredBlock()):"";sendRule(selectedKey,next,required);overrides.put(selectedKey,new AirFixationConfig.Rule(next,required));refreshRuleButtons();}
    private void openBlockPicker(){if(selectedKey==null)return;AirFixationConfig.Rule rule=effective(selectedKey);if(rule.mode()!=AirFixationConfig.Mode.REQUIRE_BLOCK)return;if(client!=null)client.setScreen(new AirFixationBlockPickerScreen(selectedKey,rule.requiredBlock()));}
    private void sendRule(String key,AirFixationConfig.Mode mode,String required){PacketByteBuf buf=PacketByteBufs.create();buf.writeString(key,320);buf.writeByte(mode.ordinal());buf.writeString(required==null?"":required,256);ClientPlayNetworking.send(AirFixationNetworking.SET,buf);}
    private void refreshRuleButtons(){if(modeButton==null||blockButton==null)return;if(selectedKey==null){modeButton.setMessage(Text.literal("Выберите маскировку"));modeButton.active=false;blockButton.active=false;blockButton.setMessage(Text.literal("Выбрать требуемый блок..."));return;}modeButton.active=synced;AirFixationConfig.Rule rule=effective(selectedKey);modeButton.setMessage(Text.literal(switch(rule.mode()){case DENY->"Режим: ЗАПРЕЩЕНО";case ALLOW->"Режим: РАЗРЕШЕНО ВЕЗДЕ";case REQUIRE_BLOCK->"Режим: ТОЛЬКО ПРИ БЛОКЕ";}));blockButton.active=synced&&rule.mode()==AirFixationConfig.Mode.REQUIRE_BLOCK;blockButton.setMessage(Text.literal(rule.mode()==AirFixationConfig.Mode.REQUIRE_BLOCK?"Блок: "+(rule.requiredBlock().isBlank()?"не выбран":rule.requiredBlock()):"Выбрать требуемый блок..."));}
    private void rebuild(){String needle=search==null?"":search.getText().trim().toLowerCase(Locale.ROOT);filtered.clear();for(Entry e:tab==0?allBlocks:allItems)if(needle.isEmpty()||e.id.toLowerCase(Locale.ROOT).contains(needle)||e.name.toLowerCase(Locale.ROOT).contains(needle))filtered.add(e);clampScroll();}
    private int listTop(){return 91;}private int listBottom(){return height-72;}private int visibleRows(){return Math.max(1,(listBottom()-listTop())/ROW_HEIGHT);}private void clampScroll(){scroll=MathHelper.clamp(scroll,0,Math.max(0,filtered.size()-visibleRows()));}
    @Override public void render(DrawContext c,int mouseX,int mouseY,float delta){renderBackground(c);int panelWidth=Math.min(680,width-24),left=(width-panelWidth)/2,right=left+panelWidth;c.fill(left,8,right,height-36,0xE0181818);c.drawCenteredTextWithShadow(textRenderer,title,width/2,16,0xFFFFFFFF);c.drawCenteredTextWithShadow(textRenderer,!synced?"Получение правил с сервера...":"ЛКМ — выбрать маскировку • кнопка режима — переключить правило",width/2,80,synced?0xFFAAAAAA:0xFFFFFF55);clampScroll();int rowLeft=left+12,rowRight=right-12,y=listTop();for(int row=0;row<visibleRows();row++){int index=scroll+row;if(index>=filtered.size())break;Entry e=filtered.get(index);AirFixationConfig.Rule rule=effective(e.key);boolean selected=e.key.equals(selectedKey);int bg=selected?0xAA3A5A78:(index&1)==0?0x55303030:0x55404040;if(mouseX>=rowLeft&&mouseX<rowRight&&mouseY>=y&&mouseY<y+21)bg=selected?0xCC4A6A88:0x77606060;c.fill(rowLeft,y,rowRight,y+21,bg);if(!e.icon.isEmpty())c.drawItem(e.icon,rowLeft+2,y+2);c.drawTextWithShadow(textRenderer,trim(e.name,300),rowLeft+22,y+2,0xFFFFFFFF);c.drawTextWithShadow(textRenderer,trim(e.id,300),rowLeft+22,y+11,0xFF888888);String status=switch(rule.mode()){case DENY->"ЗАПРЕЩЕНО";case ALLOW->"ВЕЗДЕ";case REQUIRE_BLOCK->"ПРИ "+shortId(rule.requiredBlock());};int sl=rowRight-180;c.fill(sl,y+2,rowRight-2,y+19,rule.mode()==AirFixationConfig.Mode.DENY?0xAA5A2020:0xAA1E6B2D);c.drawCenteredTextWithShadow(textRenderer,status,sl+89,y+7,rule.mode()==AirFixationConfig.Mode.DENY?0xFFFFAAAA:0xFFAAFFAA);y+=ROW_HEIGHT;}super.render(c,mouseX,mouseY,delta);}
    private String shortId(String id){if(id==null||id.isBlank())return"БЛОКЕ";int colon=id.indexOf(':');return colon>=0?id.substring(colon+1):id;}private String trim(String value,int maxWidth){if(textRenderer.getWidth(value)<=maxWidth)return value;String r=value;while(!r.isEmpty()&&textRenderer.getWidth(r+"…")>maxWidth)r=r.substring(0,r.length()-1);return r+"…";}
    @Override public boolean mouseClicked(double mouseX,double mouseY,int button){if(super.mouseClicked(mouseX,mouseY,button))return true;if(button!=0||!synced)return false;int panelWidth=Math.min(680,width-24),left=(width-panelWidth)/2,rowLeft=left+12,rowRight=left+panelWidth-12;if(mouseX<rowLeft||mouseX>=rowRight||mouseY<listTop()||mouseY>=listBottom())return false;int index=scroll+(int)((mouseY-listTop())/ROW_HEIGHT);if(index<0||index>=filtered.size())return false;selectedKey=filtered.get(index).key;refreshRuleButtons();return true;}
    @Override public boolean mouseScrolled(double mouseX,double mouseY,double amount){if(amount!=0){scroll-=(int)Math.signum(amount)*3;clampScroll();return true;}return super.mouseScrolled(mouseX,mouseY,amount);}@Override public boolean shouldPause(){return false;}private record Entry(String key,String name,String id,ItemStack icon){}
}
