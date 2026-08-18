package fable.hideseek.imba.client;

import fable.hideseek.imba.config.MaskHitboxConfig;
import fable.hideseek.imba.net.MaskHitboxNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Chooses one NON-FULL mask block for precise 3D hitbox editing. */
public final class MaskHitboxScreen extends Screen {
    private static final int ROW_HEIGHT = 26;
    private final List<Entry> all = new ArrayList<>();
    private final List<Entry> filtered = new ArrayList<>();
    private TextFieldWidget searchField;
    private int scroll;
    private boolean synced;

    public MaskHitboxScreen() {
        super(Text.literal("Хитбоксы неполноценных масок"));
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(560, width - 24);
        int left = (width - panelWidth) / 2;
        searchField = new TextFieldWidget(textRenderer, left + 12, 36, panelWidth - 24, 20, Text.empty());
        searchField.setPlaceholder(Text.literal("Поиск неполного блока по названию или ID..."));
        searchField.setMaxLength(128);
        searchField.setChangedListener(value -> {
            rebuildFilter();
            scroll = 0;
        });
        addDrawableChild(searchField);
        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), b -> close())
                .dimensions(width / 2 - 60, height - 30, 120, 20).build());
        rebuildFromState();
        ClientPlayNetworking.send(MaskHitboxNetworking.REQUEST, PacketByteBufs.empty());
    }

    public void rebuildFromState() {
        all.clear();
        Map<String, MaskHitboxConfig.BoxSpec> custom = MaskHitboxClientState.boxesSnapshot();
        for (String raw : MaskHitboxClientState.nonFullSnapshot()) {
            Identifier id = Identifier.tryParse(raw);
            if (id == null || !Registries.BLOCK.containsId(id)) continue;
            all.add(new Entry(Registries.BLOCK.get(id), id, custom.containsKey(raw)));
        }
        all.sort(Comparator.comparing(entry -> entry.id.toString()));
        synced = true;
        rebuildFilter();
    }

    private void rebuildFilter() {
        String needle = searchField == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        filtered.clear();
        for (Entry entry : all) {
            String id = entry.id.toString().toLowerCase(Locale.ROOT);
            String name = entry.block.getName().getString().toLowerCase(Locale.ROOT);
            if (needle.isEmpty() || id.contains(needle) || name.contains(needle)) filtered.add(entry);
        }
        clampScroll();
    }

    private int listTop() { return 66; }
    private int listBottom() { return height - 42; }
    private int visibleRows() { return Math.max(1, (listBottom() - listTop()) / ROW_HEIGHT); }
    private void clampScroll() { scroll = MathHelper.clamp(scroll, 0, Math.max(0, filtered.size() - visibleRows())); }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int panelWidth = Math.min(560, width - 24);
        int left = (width - panelWidth) / 2;
        int right = left + panelWidth;
        int top = 10;
        int bottom = height - 36;
        context.fill(left, top, right, bottom, 0xDD181818);
        border(context, left, top, panelWidth, bottom - top, 0xFF777777);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 17, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                synced ? "Здесь показываются только блоки, отмеченные как НЕПОЛНЫЙ. Выберите блок для 3D-редактора."
                        : "Получение списка с сервера...",
                width / 2, 59, synced ? 0xFFAAAAAA : 0xFFFFFF55);

        int rowLeft = left + 12;
        int rowRight = right - 12;
        int y = listTop();
        clampScroll();
        for (int row = 0; row < visibleRows(); row++) {
            int index = scroll + row;
            if (index >= filtered.size()) break;
            Entry entry = filtered.get(index);
            int bg = (index & 1) == 0 ? 0x55303030 : 0x55404040;
            if (mouseX >= rowLeft && mouseX < rowRight && mouseY >= y && mouseY < y + 22) bg = 0x77606060;
            context.fill(rowLeft, y, rowRight, y + 22, bg);
            ItemStack icon = new ItemStack(entry.block.asItem());
            if (!icon.isEmpty()) context.drawItem(icon, rowLeft + 2, y + 3);
            context.drawTextWithShadow(textRenderer, trim(entry.block.getName().getString(), 235), rowLeft + 23, y + 3, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer, trim(entry.id.toString(), 235), rowLeft + 23, y + 12, 0xFF888888);
            String status = entry.custom ? "РУЧНОЙ ХИТБОКС" : "ФОРМА БЛОКА";
            int statusColor = entry.custom ? 0xFFAAFFAA : 0xFFFFFF55;
            context.drawTextWithShadow(textRenderer, status, rowRight - textRenderer.getWidth(status) - 6, y + 7, statusColor);
            y += ROW_HEIGHT;
        }
        context.drawTextWithShadow(textRenderer,
                "Найдено: " + filtered.size() + "  •  Клик по строке — открыть редактор",
                left + 12, bottom - 14, 0xFF999999);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0 || !synced) return false;
        int panelWidth = Math.min(560, width - 24);
        int left = (width - panelWidth) / 2;
        int rowLeft = left + 12;
        int rowRight = left + panelWidth - 12;
        if (mouseX < rowLeft || mouseX >= rowRight || mouseY < listTop() || mouseY >= listBottom()) return false;
        int index = scroll + (int) ((mouseY - listTop()) / ROW_HEIGHT);
        if (index < 0 || index >= filtered.size()) return false;
        if (client != null) client.setScreen(new MaskHitboxEditScreen(this, filtered.get(index).id));
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (amount != 0.0D) {
            scroll -= (int) Math.signum(amount) * 3;
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    private String trim(String value, int maxWidth) {
        if (textRenderer.getWidth(value) <= maxWidth) return value;
        String result = value;
        while (!result.isEmpty() && textRenderer.getWidth(result + "…") > maxWidth) result = result.substring(0, result.length() - 1);
        return result + "…";
    }

    private static void border(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    @Override public boolean shouldPause() { return false; }
    private record Entry(Block block, Identifier id, boolean custom) {}
}
