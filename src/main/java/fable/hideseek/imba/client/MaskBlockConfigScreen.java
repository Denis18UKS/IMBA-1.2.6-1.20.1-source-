package fable.hideseek.imba.client;

import fable.hideseek.imba.net.MaskBlockConfigNetworking;
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

/** Searchable GUI for marking block masks as full/non-full for statue snapping. */
public final class MaskBlockConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 24;

    private final List<BlockEntry> allBlocks = new ArrayList<>();
    private final List<BlockEntry> filtered = new ArrayList<>();
    private final Set<String> nonFullBlocks = new HashSet<>();

    private TextFieldWidget searchField;
    private int scroll;
    private boolean synced;

    public MaskBlockConfigScreen() {
        super(Text.literal("Настройка блоков маскировки"));
        for (Block block : Registries.BLOCK) {
            Identifier id = Registries.BLOCK.getId(block);
            if (id == null) {
                continue;
            }
            allBlocks.add(new BlockEntry(block, id));
        }
        allBlocks.sort(Comparator.comparing(entry -> entry.id.toString()));
        filtered.addAll(allBlocks);
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(520, width - 24);
        int left = (width - panelWidth) / 2;

        searchField = new TextFieldWidget(textRenderer, left + 12, 36, panelWidth - 24, 20,
                Text.literal("Поиск блока"));
        searchField.setPlaceholder(Text.literal("Поиск по названию или ID..."));
        searchField.setMaxLength(128);
        searchField.setChangedListener(value -> {
            rebuildFilter();
            scroll = 0;
        });
        addDrawableChild(searchField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), button -> close())
                .dimensions(width / 2 - 55, height - 30, 110, 20)
                .build());

        requestSync();
    }

    private void requestSync() {
        ClientPlayNetworking.send(MaskBlockConfigNetworking.REQUEST, PacketByteBufs.create());
    }

    public void applyServerState(Set<String> nonFull) {
        nonFullBlocks.clear();
        nonFullBlocks.addAll(nonFull);
        synced = true;
    }

    private void rebuildFilter() {
        String needle = searchField == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        filtered.clear();
        for (BlockEntry entry : allBlocks) {
            String id = entry.id.toString().toLowerCase(Locale.ROOT);
            String name = entry.block.getName().getString().toLowerCase(Locale.ROOT);
            if (needle.isEmpty() || id.contains(needle) || name.contains(needle)) {
                filtered.add(entry);
            }
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
        int panelWidth = Math.min(520, width - 24);
        int left = (width - panelWidth) / 2;
        int right = left + panelWidth;
        int top = 10;
        int bottom = height - 36;
        context.fill(left, top, right, bottom, 0xDD181818);
        context.fill(left, top, right, top + 1, 0xFF777777);
        context.fill(left, bottom - 1, right, bottom, 0xFF777777);
        context.fill(left, top, left + 1, bottom, 0xFF777777);
        context.fill(right - 1, top, right, bottom, 0xFF777777);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 17, 0xFFFFFF);
        String hint = synced
                ? "По умолчанию блок полноценный. Отметьте проблемные как НЕПОЛНЫЙ. Хитбокс не меняется."
                : "Получение конфигурации с сервера...";
        context.drawCenteredTextWithShadow(textRenderer, hint, width / 2, 59, synced ? 0xAAAAAA : 0xFFFF55);
        clampScroll();
        int rows = visibleRows();
        int rowLeft = left + 12;
        int rowRight = right - 12;
        int y = listTop();
        for (int row = 0; row < rows; row++) {
            int index = scroll + row;
            if (index >= filtered.size()) break;
            BlockEntry entry = filtered.get(index);
            boolean full = !nonFullBlocks.contains(entry.id.toString());
            int bg = ((index & 1) == 0) ? 0x55303030 : 0x55404040;
            if (mouseX >= rowLeft && mouseX < rowRight && mouseY >= y && mouseY < y + 21) bg = 0x77606060;
            context.fill(rowLeft, y, rowRight, y + 21, bg);
            ItemStack icon = new ItemStack(entry.block.asItem());
            if (!icon.isEmpty()) context.drawItem(icon, rowLeft + 2, y + 2);
            String name = entry.block.getName().getString();
            context.drawTextWithShadow(textRenderer, trim(name, 215), rowLeft + 22, y + 2, 0xFFFFFF);
            context.drawTextWithShadow(textRenderer, trim(entry.id.toString(), 215), rowLeft + 22, y + 11, 0x888888);
            int statusWidth = 112;
            int statusLeft = rowRight - statusWidth;
            context.fill(statusLeft, y + 2, rowRight - 2, y + 19, full ? 0xAA1E6B2D : 0xAA7A5520);
            context.drawCenteredTextWithShadow(textRenderer, full ? "ПОЛНОЦЕННЫЙ" : "НЕПОЛНЫЙ",
                    statusLeft + (statusWidth - 2) / 2, y + 7, full ? 0xAAFFAA : 0xFFFF55);
            y += ROW_HEIGHT;
        }
        context.drawTextWithShadow(textRenderer,
                "Найдено: " + filtered.size() + "   •   Колесо мыши — прокрутка",
                left + 12, bottom - 14, 0x999999);
        super.render(context, mouseX, mouseY, delta);
    }

    private String trim(String value, int maxWidth) {
        if (textRenderer.getWidth(value) <= maxWidth) return value;
        String suffix = "…";
        int suffixWidth = textRenderer.getWidth(suffix);
        String result = value;
        while (!result.isEmpty() && textRenderer.getWidth(result) + suffixWidth > maxWidth) result = result.substring(0, result.length() - 1);
        return result + suffix;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0 || !synced) return false;
        int panelWidth = Math.min(520, width - 24);
        int left = (width - panelWidth) / 2;
        int rowLeft = left + 12;
        int rowRight = left + panelWidth - 12;
        if (mouseX < rowLeft || mouseX >= rowRight || mouseY < listTop() || mouseY >= listBottom()) return false;
        int row = (int) ((mouseY - listTop()) / ROW_HEIGHT);
        int index = scroll + row;
        if (index < 0 || index >= filtered.size()) return false;
        BlockEntry entry = filtered.get(index);
        String id = entry.id.toString();
        boolean newFull = nonFullBlocks.contains(id);
        if (newFull) nonFullBlocks.remove(id); else nonFullBlocks.add(id);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(id, 256);
        buf.writeBoolean(newFull);
        ClientPlayNetworking.send(MaskBlockConfigNetworking.SET, buf);
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

    @Override public boolean shouldPause() { return false; }
    private record BlockEntry(Block block, Identifier id) {}
}
