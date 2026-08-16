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
import java.util.*;

public final class AirFixationConfigScreen extends Screen {
    private static final int ROW_HEIGHT=24;
    private final List<Entry> allBlocks=new ArrayList<>(),allItems=new ArrayList<>(),filtered=new ArrayList<>();
    private final Map<String,Boolean> overrides=new LinkedHashMap<>();
    private TextFieldWidget search; private ButtonWidget tabButton; private int tab,scroll; private boolean synced;
    public AirFixationConfigScreen(){super(Text.literal("Фиксация маскировок в воздухе"));for(Block block:Registries.BLOCK){Identifier id=Registries.BLOCK.getId(block);if(id!=null&&block.asItem()!=net.minecraft.item.Items.AIR)allBlocks.add(new Entry(AirFixationConfig.blockKey(id),block.getName().getString(),id.toString(),new ItemStack(block.asItem())));}for(Item item:Registries.ITEM){Identifier id=Registries.ITEM.getId(item);if(id!=null&&item!=net.minecraft.item.Items.AIR)allItems.add(new Entry(AirFixationConfig.itemKey(id),item.getName().getString(),id.toString(),new ItemStack(item)));}Comparator<Entry> cmp=Comparator.comparing(e->e.id);allBlocks.sort(cmp);allItems.sort(cmp);rebuild();}
    @Override protected void init(){int panelWidth=Math.min(590,width-24),left=(width-panelWidth)/2;tabButton=addDrawableChild(ButtonWidget.builder(tabText(),b->{tab=1-tab;b.setMessage(tabText());scroll=0;rebuild();}).dimensions(left+12,34,panelWidth-24,20).build());search=new TextFieldWidget(textRenderer,left+12,58,panelWidth-24,20,Text.empty());search.setPlaceholder(Text.literal("Поиск маскировки по названию или ID..."));search.setChangedListener(v->{scroll=0;rebuild();});addDrawableChild(search);addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"),b->close()).dimensions(width/2-55,height-30,110,20).build());ClientPlayNetworking.send(AirFixationNetworking.REQUEST,PacketByteBufs.empty());}
    private Text tabText(){return Text.literal(tab==0?"Тип: блоковые маскировки":"Тип: предметные маскировки");}
    public void applyServerState(Map<String,Boolean> values){overrides.clear();overrides.putAll(values);synced=true;}
    private void rebuild(){String needle=search==null?"":search.getText().trim().toLowerCase(Locale.ROOT);filtered.clear();for(Entry e:(tab==0?allBlocks:allItems))if(needle.isEmpty()||e.id.toLowerCase(Locale.ROOT).contains(needle)||e.name.toLowerCase(Locale.ROOT).contains(needle))filtered.add(e);clampScroll();}
    private boolean effectiveAllowed(Entry e){Boolean value=overrides.get(e.key);return value!=null?value:AirFixationConfig.defaultAllowed(e.key);}
    private int listTop(){return 91;}private int listBottom(){return height-42;}private int visibleRows(){return Math.max(1,(listBottom()-listTop())/ROW_HEIGHT);}private void clampScroll(){scroll=MathHelper.clamp(scroll,0,Math.max(0,filtered.size()-visibleRows()));}
    @Override public void render(DrawContext c,int mouseX,int mouseY,float delta){renderBackground(c);int panelWidth=Math.min(590,width-24),left=(width-panelWidth)/2,right=left+panelWidth;c.fill(left,8,right,height-36,0xE0181818);c.drawCenteredTextWithShadow(textRenderer,title,width/2,16,0xFFFFFFFF);String hint=!synced?"Получение правил с сервера...":"ЛКМ по маскировке — разрешить/запретить фиксацию без блока под ней";c.drawCenteredTextWithShadow(textRenderer,hint,width/2,80,synced?0xFFAAAAAA:0xFFFFFF55);clampScroll();int rowLeft=left+12,rowRight=right-12,y=listTop();for(int row=0;row<visibleRows();row++){int index=scroll+row;if(index>=filtered.size())break;Entry e=filtered.get(index);boolean allowed=effectiveAllowed(e);int bg=(index&1)==0?0x55303030:0x55404040;if(mouseX>=rowLeft&&mouseX<rowRight&&mouseY>=y&&mouseY<y+21)bg=0x77606060;c.fill(rowLeft,y,rowRight,y+21,bg);if(!e.icon.isEmpty())c.drawItem(e.icon,rowLeft+2,y+2);c.drawTextWithShadow(textRenderer,trim(e.name,300),rowLeft+22,y+2,0xFFFFFFFF);c.drawTextWithShadow(textRenderer,trim(e.id,300),rowLeft+22,y+11,0xFF888888);int statusLeft=rowRight-132;c.fill(statusLeft,y+2,rowRight-2,y+19,allowed?0xAA1E6B2D:0xAA5A2020);c.drawCenteredTextWithShadow(textRenderer,allowed?"МОЖНО В ВОЗДУХЕ":"НЕЛЬЗЯ В ВОЗДУХЕ",statusLeft+65,y+7,allowed?0xFFAAFFAA:0xFFFFAAAA);y+=ROW_HEIGHT;}c.drawTextWithShadow(textRenderer,"Найдено: "+filtered.size()+" • колесо — прокрутка • правило сохраняется сразу",left+12,height-50,0xFF999999);super.render(c,mouseX,mouseY,delta);}
    private String trim(String value,int maxWidth){if(textRenderer.getWidth(value)<=maxWidth)return value;String r=value;while(!r.isEmpty()&&textRenderer.getWidth(r+"…")>maxWidth)r=r.substring(0,r.length()-1);return r+"…";}
    @Override public boolean mouseClicked(double mouseX,double mouseY,int button){if(super.mouseClicked(mouseX,mouseY,button))return true;if(button!=0||!synced)return false;int panelWidth=Math.min(590,width-24),left=(width-panelWidth)/2,rowLeft=left+12,rowRight=left+panelWidth-12;if(mouseX<rowLeft||mouseX>=rowRight||mouseY<listTop()||mouseY>=listBottom())return false;int index=scroll+(int)((mouseY-listTop())/ROW_HEIGHT);if(index<0||index>=filtered.size())return false;Entry e=filtered.get(index);boolean newValue=!effectiveAllowed(e);if(newValue==AirFixationConfig.defaultAllowed(e.key))overrides.remove(e.key);else overrides.put(e.key,newValue);PacketByteBuf buf=PacketByteBufs.create();buf.writeString(e.key,320);buf.writeBoolean(newValue);ClientPlayNetworking.send(AirFixationNetworking.SET,buf);return true;}
    @Override public boolean mouseScrolled(double mouseX,double mouseY,double amount){if(amount!=0){scroll-=(int)Math.signum(amount)*3;clampScroll();return true;}return super.mouseScrolled(mouseX,mouseY,amount);}
    @Override public boolean shouldPause(){return false;}
    private record Entry(String key,String name,String id,ItemStack icon){}
}
