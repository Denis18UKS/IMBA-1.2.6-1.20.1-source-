package fable.hideseek.imba.client;

import fable.hideseek.imba.config.MaskAutoPositionConfig;
import fable.hideseek.imba.net.MaskAutoPositionNetworking;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MaskAutoPositionScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int BUTTON_SIZE = 18;
    private final List<BlockEntry> allBlocks = new ArrayList<>();
    private final List<BlockEntry> filtered = new ArrayList<>();
    private final Map<String, MaskAutoPositionConfig.Offset> offsets = new HashMap<>();
    private TextFieldWidget searchField;
    private int scroll;
    private boolean synced;
    private BlockEntry selected;

    public MaskAutoPositionScreen() {
        super(Text.literal("Индивидуальная автопозиция масок"));
        for (Block block : Registries.BLOCK) {
            Identifier id = Registries.BLOCK.getId(block);
            if (id != null) allBlocks.add(new BlockEntry(block, id));
        }
        allBlocks.sort(Comparator.comparing(entry -> entry.id.toString()));
        filtered.addAll(allBlocks);
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(600, width - 24);
        int left = (width - panelWidth) / 2;
        searchField = new TextFieldWidget(textRenderer, left + 12, 36, panelWidth - 24, 20,
                Text.literal("Поиск блока"));
        searchField.setPlaceholder(Text.literal("Поиск по названию или ID..."));
        searchField.setMaxLength(128);
        searchField.setChangedListener(value -> { rebuildFilter(); scroll = 0; });
        addDrawableChild(searchField);
        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), button -> close())
                .dimensions(width / 2 - 55, height - 27, 110, 20).build());
        ClientPlayNetworking.send(MaskAutoPositionNetworking.REQUEST, PacketByteBufs.create());
    }

    public void applyServerState(Map<String, MaskAutoPositionConfig.Offset> values) {
        offsets.clear();
        values.forEach((id, offset) -> offsets.put(id, offset.copy()));
        synced = true;
    }

    private void rebuildFilter() {
        String needle = searchField == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        filtered.clear();
        for (BlockEntry entry : allBlocks) {
            if (needle.isEmpty() || entry.id.toString().toLowerCase(Locale.ROOT).contains(needle)
                    || entry.block.getName().getString().toLowerCase(Locale.ROOT).contains(needle)) filtered.add(entry);
        }
        clampScroll();
    }

    private int panelWidth() { return Math.min(600, width - 24); }
    private int panelLeft() { return (width - panelWidth()) / 2; }
    private int listTop() { return 66; }
    private int editorTop() { return Math.max(132, height - 100); }
    private int listBottom() { return editorTop() - 6; }
    private int visibleRows() { return Math.max(1, (listBottom() - listTop()) / ROW_HEIGHT); }
    private void clampScroll() { scroll = MathHelper.clamp(scroll, 0, Math.max(0, filtered.size() - visibleRows())); }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int left = panelLeft(), right = left + panelWidth(), top = 10, bottom = height - 33;
        context.fill(left, top, right, bottom, 0xDD181818);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 17, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                synced ? "1 px = 1/16 блока. Без настройки работает обычная автопозиция."
                       : "Получение конфигурации с сервера...",
                width / 2, 59, synced ? 0xAAAAAA : 0xFFFF55);
        renderList(context, mouseX, mouseY, left, right);
        renderEditor(context, mouseX, mouseY, left, right);
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderList(DrawContext context, int mouseX, int mouseY, int left, int right) {
        clampScroll();
        int rowLeft = left + 12, rowRight = right - 12, y = listTop();
        for (int row = 0; row < visibleRows(); row++) {
            int index = scroll + row;
            if (index >= filtered.size()) break;
            BlockEntry entry = filtered.get(index);
            MaskAutoPositionConfig.Offset value = offsets.getOrDefault(entry.id.toString(), MaskAutoPositionConfig.Offset.ZERO);
            int bg = selected != null && selected.id.equals(entry.id) ? 0x88705020 : ((index & 1) == 0 ? 0x55303030 : 0x55404040);
            if (mouseX >= rowLeft && mouseX < rowRight && mouseY >= y && mouseY < y + 21) bg = 0x77606060;
            context.fill(rowLeft, y, rowRight, y + 21, bg);
            ItemStack icon = new ItemStack(entry.block.asItem());
            if (!icon.isEmpty()) context.drawItem(icon, rowLeft + 2, y + 2);
            context.drawTextWithShadow(textRenderer, entry.block.getName().getString(), rowLeft + 22, y + 2, 0xFFFFFF);
            context.drawTextWithShadow(textRenderer, entry.id.toString(), rowLeft + 22, y + 11, 0x888888);
            String v = value.isZero() ? "обычная" : "X " + signed(value.xPixels) + " Y " + signed(value.yPixels) + " Z " + signed(value.zPixels) + " px";
            context.drawTextWithShadow(textRenderer, v, rowRight - textRenderer.getWidth(v) - 5, y + 7, value.isZero() ? 0x999999 : 0xAAFFAA);
            y += ROW_HEIGHT;
        }
    }

    private void renderEditor(DrawContext context, int mouseX, int mouseY, int left, int right) {
        int top = editorTop();
        context.fill(left + 12, top, right - 12, top + 62, 0x77252525);
        if (selected == null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Выберите блок в списке"), width / 2, top + 25, 0xAAAAAA);
            return;
        }
        MaskAutoPositionConfig.Offset value = offsets.getOrDefault(selected.id.toString(), MaskAutoPositionConfig.Offset.ZERO);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(selected.block.getName().getString() + " (" + selected.id + ")"), width / 2, top + 5, 0xFFFFFF);
        int groupWidth = 128, gap = 8, total = groupWidth * 3 + gap * 2, start = width / 2 - total / 2, y = top + 22;
        drawAxis(context, "X", value.xPixels, start, y, mouseX, mouseY);
        drawAxis(context, "Y", value.yPixels, start + groupWidth + gap, y, mouseX, mouseY);
        drawAxis(context, "Z", value.zPixels, start + (groupWidth + gap) * 2, y, mouseX, mouseY);
        int resetWidth = 130, resetLeft = width / 2 - resetWidth / 2, resetTop = top + 43;
        context.fill(resetLeft, resetTop, resetLeft + resetWidth, resetTop + 16, 0x88603028);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Сбросить для блока"), width / 2, resetTop + 4, 0xFFFFFF);
    }

    private void drawAxis(DrawContext context, String axis, int value, int x, int y, int mouseX, int mouseY) {
        context.drawTextWithShadow(textRenderer, axis + ":", x, y + 5, 0xFFFFFF);
        drawSmallButton(context, x + 18, y, "−");
        drawSmallButton(context, x + 88, y, "+");
        context.drawCenteredTextWithShadow(textRenderer, signed(value) + " px", x + 67, y + 5, 0xFFFF55);
    }

    private void drawSmallButton(DrawContext context, int x, int y, String text) {
        context.fill(x, y, x + BUTTON_SIZE, y + BUTTON_SIZE, 0x88606060);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(text), x + BUTTON_SIZE / 2, y + 5, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0 || !synced) return false;
        int left = panelLeft(), rowLeft = left + 12, rowRight = left + panelWidth() - 12;
        if (mouseX >= rowLeft && mouseX < rowRight && mouseY >= listTop() && mouseY < listBottom()) {
            int index = scroll + (int)((mouseY - listTop()) / ROW_HEIGHT);
            if (index >= 0 && index < filtered.size()) { selected = filtered.get(index); return true; }
        }
        if (selected == null) return false;
        MaskAutoPositionConfig.Offset old = offsets.getOrDefault(selected.id.toString(), MaskAutoPositionConfig.Offset.ZERO);
        int xVal = old.xPixels, yVal = old.yPixels, zVal = old.zPixels;
        int groupWidth = 128, gap = 8, total = groupWidth * 3 + gap * 2, start = width / 2 - total / 2, cy = editorTop() + 22;
        for (int i = 0; i < 3; i++) {
            int sx = start + i * (groupWidth + gap);
            if (inside(mouseX, mouseY, sx + 18, cy, BUTTON_SIZE, BUTTON_SIZE)) {
                if (i == 0) xVal--; else if (i == 1) yVal--; else zVal--;
                send(xVal, yVal, zVal); return true;
            }
            if (inside(mouseX, mouseY, sx + 88, cy, BUTTON_SIZE, BUTTON_SIZE)) {
                if (i == 0) xVal++; else if (i == 1) yVal++; else zVal++;
                send(xVal, yVal, zVal); return true;
            }
        }
        int rw = 130, rl = width / 2 - rw / 2, rt = editorTop() + 43;
        if (inside(mouseX, mouseY, rl, rt, rw, 16)) { send(0, 0, 0); return true; }
        return false;
    }

    private void send(int x, int y, int z) {
        x = MathHelper.clamp(x, -MaskAutoPositionConfig.MAX_ABS_PIXELS, MaskAutoPositionConfig.MAX_ABS_PIXELS);
        y = MathHelper.clamp(y, -MaskAutoPositionConfig.MAX_ABS_PIXELS, MaskAutoPositionConfig.MAX_ABS_PIXELS);
        z = MathHelper.clamp(z, -MaskAutoPositionConfig.MAX_ABS_PIXELS, MaskAutoPositionConfig.MAX_ABS_PIXELS);
        String id = selected.id.toString();
        if (x == 0 && y == 0 && z == 0) offsets.remove(id); else offsets.put(id, new MaskAutoPositionConfig.Offset(x, y, z));
        PacketByteBuf out = PacketByteBufs.create();
        out.writeString(id, 256); out.writeInt(x); out.writeInt(y); out.writeInt(z);
        ClientPlayNetworking.send(MaskAutoPositionNetworking.SET, out);
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    private String signed(int v) { return v >= 0 ? "+" + v : Integer.toString(v); }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (amount != 0.0D) { scroll -= (int)Math.signum(amount) * 3; clampScroll(); return true; }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override public boolean shouldPause() { return false; }
    private record BlockEntry(Block block, Identifier id) {}
}
