package fable.hideseek.imba.client;

import fable.hideseek.imba.net.BlockRulesNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** One searchable editor for interaction and Adventure-breaking exceptions. */
public final class BlockRulesScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private final List<Entry> all = new ArrayList<>();
    private final List<Entry> filtered = new ArrayList<>();
    private final Set<String> interactive = new HashSet<>();
    private final Set<String> breakable = new HashSet<>();
    private TextFieldWidget search;
    private ButtonWidget tabButton;
    private int tab; // 0 interactive, 1 Adventure break
    private int scroll;
    private boolean synced;

    public BlockRulesScreen() {
        super(Text.literal("Исключения блоков"));
        for (Block block : Registries.BLOCK) {
            Identifier id = Registries.BLOCK.getId(block);
            if (id != null) all.add(new Entry(block, id));
        }
        all.sort(Comparator.comparing(e -> e.id.toString()));
        filtered.addAll(all);
    }

    @Override protected void init() {
        int panelWidth = Math.min(560, width - 24);
        int left = (width - panelWidth) / 2;
        tabButton = addDrawableChild(ButtonWidget.builder(tabText(), b -> {
            tab = 1 - tab;
            b.setMessage(tabText());
            scroll = 0;
        }).dimensions(left + 12, 34, panelWidth - 24, 20).build());

        search = new TextFieldWidget(textRenderer, left + 12, 58, panelWidth - 24, 20, Text.empty());
        search.setPlaceholder(Text.literal("Поиск по названию или ID..."));
        search.setChangedListener(v -> { rebuild(); scroll = 0; });
        addDrawableChild(search);
        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), b -> close())
                .dimensions(width / 2 - 55, height - 30, 110, 20).build());
        ClientPlayNetworking.send(BlockRulesNetworking.REQUEST, PacketByteBufs.empty());
    }

    private Text tabText() {
        return Text.literal(tab == 0
                ? "Вкладка: исключения интерактивных блоков"
                : "Вкладка: можно ломать в Adventure");
    }

    public void applyServerState(Set<String> interactiveIds, Set<String> breakIds) {
        interactive.clear(); interactive.addAll(interactiveIds);
        breakable.clear(); breakable.addAll(breakIds);
        synced = true;
    }

    private void rebuild() {
        String needle = search == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        filtered.clear();
        for (Entry e : all) {
            if (needle.isEmpty() || e.id.toString().toLowerCase(Locale.ROOT).contains(needle)
                    || e.block.getName().getString().toLowerCase(Locale.ROOT).contains(needle)) filtered.add(e);
        }
        clampScroll();
    }

    private int listTop() { return 86; }
    private int listBottom() { return height - 42; }
    private int visibleRows() { return Math.max(1, (listBottom() - listTop()) / ROW_HEIGHT); }
    private void clampScroll() { scroll = MathHelper.clamp(scroll, 0, Math.max(0, filtered.size() - visibleRows())); }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int panelWidth = Math.min(560, width - 24), left = (width - panelWidth) / 2, right = left + panelWidth;
        context.fill(left, 8, right, height - 36, 0xDD181818);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 16, 0xFFFFFF);
        String hint = !synced ? "Получение настроек с сервера..."
                : tab == 0 ? "Зелёные блоки разрешено открывать/использовать не в Creative"
                : "Зелёные блоки разрешено ломать в Adventure без штрафа";
        context.drawCenteredTextWithShadow(textRenderer, hint, width / 2, 79, synced ? 0xAAAAAA : 0xFFFF55);
        clampScroll();
        int rowLeft = left + 12, rowRight = right - 12, y = listTop();
        Set<String> active = tab == 0 ? interactive : breakable;
        for (int row = 0; row < visibleRows(); row++) {
            int index = scroll + row; if (index >= filtered.size()) break;
            Entry e = filtered.get(index); boolean enabled = active.contains(e.id.toString());
            int bg = (index & 1) == 0 ? 0x55303030 : 0x55404040;
            if (mouseX >= rowLeft && mouseX < rowRight && mouseY >= y && mouseY < y + 21) bg = 0x77606060;
            context.fill(rowLeft, y, rowRight, y + 21, bg);
            ItemStack icon = new ItemStack(e.block.asItem()); if (!icon.isEmpty()) context.drawItem(icon, rowLeft + 2, y + 2);
            context.drawTextWithShadow(textRenderer, trim(e.block.getName().getString(), 260), rowLeft + 22, y + 2, 0xFFFFFF);
            context.drawTextWithShadow(textRenderer, trim(e.id.toString(), 260), rowLeft + 22, y + 11, 0x888888);
            int statusLeft = rowRight - 105;
            context.fill(statusLeft, y + 2, rowRight - 2, y + 19, enabled ? 0xAA1E6B2D : 0xAA5A2020);
            context.drawCenteredTextWithShadow(textRenderer, enabled ? "РАЗРЕШЕНО" : "ЗАПРЕЩЕНО",
                    statusLeft + 51, y + 7, enabled ? 0xAAFFAA : 0xFFFFAA);
            y += ROW_HEIGHT;
        }
        context.drawTextWithShadow(textRenderer, "Найдено: " + filtered.size() + " • ЛКМ — переключить • колесо — прокрутка",
                left + 12, height - 50, 0x999999);
        super.render(context, mouseX, mouseY, delta);
    }

    private String trim(String s, int maxWidth) {
        if (textRenderer.getWidth(s) <= maxWidth) return s;
        String result = s; while (!result.isEmpty() && textRenderer.getWidth(result + "…") > maxWidth) result = result.substring(0, result.length()-1);
        return result + "…";
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0 || !synced) return false;
        int panelWidth = Math.min(560, width - 24), left = (width - panelWidth) / 2;
        int rowLeft = left + 12, rowRight = left + panelWidth - 12;
        if (mouseX < rowLeft || mouseX >= rowRight || mouseY < listTop() || mouseY >= listBottom()) return false;
        int index = scroll + (int)((mouseY - listTop()) / ROW_HEIGHT);
        if (index < 0 || index >= filtered.size()) return false;
        Entry e = filtered.get(index); Set<String> active = tab == 0 ? interactive : breakable;
        boolean newValue = !active.contains(e.id.toString());
        if (newValue) active.add(e.id.toString()); else active.remove(e.id.toString());
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByte(tab); buf.writeString(e.id.toString(), 256); buf.writeBoolean(newValue);
        ClientPlayNetworking.send(BlockRulesNetworking.SET, buf);
        return true;
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (amount != 0) { scroll -= (int)Math.signum(amount) * 3; clampScroll(); return true; }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }
    @Override public boolean shouldPause() { return false; }
    private record Entry(Block block, Identifier id) {}
}
