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

/** GUI for configuring an exact mask-block + support-block statue offset. */
public final class MaskAutoPositionScreen extends Screen {
    private static final int ROW_HEIGHT = 22;
    private static final int BUTTON_SIZE = 18;

    private final List<BlockEntry> allBlocks = new ArrayList<>();
    private final List<BlockEntry> filteredMasks = new ArrayList<>();
    private final List<BlockEntry> filteredSupports = new ArrayList<>();
    private final Map<String, Map<String, MaskAutoPositionConfig.Offset>> offsets = new HashMap<>();

    private TextFieldWidget maskSearchField;
    private TextFieldWidget supportSearchField;
    private int maskScroll;
    private int supportScroll;
    private boolean synced;
    private BlockEntry selectedMask;
    private BlockEntry selectedSupport;

    public MaskAutoPositionScreen() {
        super(Text.literal("Автопозиция: маска + блок под ней"));
        for (Block block : Registries.BLOCK) {
            Identifier id = Registries.BLOCK.getId(block);
            if (id != null) {
                allBlocks.add(new BlockEntry(block, id));
            }
        }
        allBlocks.sort(Comparator.comparing(entry -> entry.id.toString()));
        filteredMasks.addAll(allBlocks);
        filteredSupports.addAll(allBlocks);
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int columnWidth = columnWidth();

        maskSearchField = new TextFieldWidget(textRenderer, left + 12, 38, columnWidth - 18, 20,
                Text.literal("Поиск маски"));
        maskSearchField.setPlaceholder(Text.literal("Маска: название или ID..."));
        maskSearchField.setMaxLength(128);
        maskSearchField.setChangedListener(value -> {
            rebuildMasks();
            maskScroll = 0;
        });
        addDrawableChild(maskSearchField);

        supportSearchField = new TextFieldWidget(textRenderer, left + columnWidth + 6, 38,
                columnWidth - 18, 20, Text.literal("Поиск опоры"));
        supportSearchField.setPlaceholder(Text.literal("Блок под маской: название или ID..."));
        supportSearchField.setMaxLength(128);
        supportSearchField.setChangedListener(value -> {
            rebuildSupports();
            supportScroll = 0;
        });
        addDrawableChild(supportSearchField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), button -> close())
                .dimensions(width / 2 - 55, height - 27, 110, 20)
                .build());

        ClientPlayNetworking.send(MaskAutoPositionNetworking.REQUEST, PacketByteBufs.create());
    }

    public void applyServerState(Map<String, Map<String, MaskAutoPositionConfig.Offset>> values) {
        offsets.clear();
        values.forEach((maskId, supports) -> {
            Map<String, MaskAutoPositionConfig.Offset> supportCopy = new HashMap<>();
            supports.forEach((supportId, offset) -> supportCopy.put(supportId, offset.copy()));
            offsets.put(maskId, supportCopy);
        });
        synced = true;
    }

    private void rebuildMasks() {
        rebuild(maskSearchField, filteredMasks);
        clampMaskScroll();
    }

    private void rebuildSupports() {
        rebuild(supportSearchField, filteredSupports);
        clampSupportScroll();
    }

    private void rebuild(TextFieldWidget field, List<BlockEntry> target) {
        String needle = field == null ? "" : field.getText().trim().toLowerCase(Locale.ROOT);
        target.clear();
        for (BlockEntry entry : allBlocks) {
            String id = entry.id.toString().toLowerCase(Locale.ROOT);
            String name = entry.block.getName().getString().toLowerCase(Locale.ROOT);
            if (needle.isEmpty() || id.contains(needle) || name.contains(needle)) {
                target.add(entry);
            }
        }
    }

    private int panelWidth() {
        return Math.min(760, width - 20);
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int columnWidth() {
        return panelWidth() / 2;
    }

    private int listTop() {
        return 72;
    }

    private int editorTop() {
        return Math.max(168, height - 104);
    }

    private int listBottom() {
        return editorTop() - 7;
    }

    private int visibleRows() {
        return Math.max(1, (listBottom() - listTop()) / ROW_HEIGHT);
    }

    private void clampMaskScroll() {
        maskScroll = MathHelper.clamp(maskScroll, 0, Math.max(0, filteredMasks.size() - visibleRows()));
    }

    private void clampSupportScroll() {
        supportScroll = MathHelper.clamp(supportScroll, 0, Math.max(0, filteredSupports.size() - visibleRows()));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        int left = panelLeft();
        int right = left + panelWidth();
        int top = 6;
        int bottom = height - 33;
        int middle = left + columnWidth();

        context.fill(left, top, right, bottom, 0xDD181818);
        context.fill(left, top, right, top + 1, 0xFF777777);
        context.fill(left, bottom - 1, right, bottom, 0xFF777777);
        context.fill(left, top, left + 1, bottom, 0xFF777777);
        context.fill(right - 1, top, right, bottom, 0xFF777777);

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 11, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                synced
                        ? "Выбери маску и блок под ней. 1 px = 1/16 блока. Поправка применяется после обычного снапа."
                        : "Получение конфигурации с сервера...",
                width / 2, 24, synced ? 0xAAAAAA : 0xFFFF55);

        context.drawCenteredTextWithShadow(textRenderer, Text.literal("МАСКА /imba_mask"),
                left + columnWidth() / 2, 61, 0xFFFF55);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("БЛОК ПОД МАСКОЙ"),
                middle + columnWidth() / 2, 61, 0x55FFFF);
        context.fill(middle, 61, middle + 1, listBottom(), 0x66555555);

        renderList(context, mouseX, mouseY, true, left + 8, middle - 5);
        renderList(context, mouseX, mouseY, false, middle + 5, right - 8);
        renderEditor(context, left, right);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderList(DrawContext context, int mouseX, int mouseY,
            boolean maskList, int rowLeft, int rowRight) {
        if (maskList) {
            clampMaskScroll();
        } else {
            clampSupportScroll();
        }

        List<BlockEntry> source = maskList ? filteredMasks : filteredSupports;
        int scroll = maskList ? maskScroll : supportScroll;
        BlockEntry selected = maskList ? selectedMask : selectedSupport;
        int y = listTop();

        for (int row = 0; row < visibleRows(); row++) {
            int index = scroll + row;
            if (index >= source.size()) {
                break;
            }

            BlockEntry entry = source.get(index);
            boolean isSelected = selected != null && selected.id.equals(entry.id);
            int bg = isSelected ? 0x88705020 : ((index & 1) == 0 ? 0x55303030 : 0x55404040);
            if (mouseX >= rowLeft && mouseX < rowRight && mouseY >= y && mouseY < y + 20) {
                bg = 0x77606060;
            }
            context.fill(rowLeft, y, rowRight, y + 20, bg);

            ItemStack icon = new ItemStack(entry.block.asItem());
            if (!icon.isEmpty()) {
                context.drawItem(icon, rowLeft + 2, y + 2);
            }

            int textX = rowLeft + 22;
            int available = Math.max(30, rowRight - textX - 5);
            context.drawTextWithShadow(textRenderer,
                    trim(entry.block.getName().getString(), available), textX, y + 2, 0xFFFFFF);
            context.drawTextWithShadow(textRenderer,
                    trim(entry.id.toString(), available), textX, y + 11, 0x888888);

            if (!maskList && selectedMask != null) {
                MaskAutoPositionConfig.Offset value = getOffset(selectedMask.id, entry.id);
                if (!value.isZero()) {
                    String marker = signed(value.yPixels) + "Y";
                    context.drawTextWithShadow(textRenderer, marker,
                            rowRight - textRenderer.getWidth(marker) - 4, y + 2, 0xAAFFAA);
                }
            }
            y += ROW_HEIGHT;
        }
    }

    private void renderEditor(DrawContext context, int left, int right) {
        int top = editorTop();
        context.fill(left + 8, top, right - 8, top + 68, 0x77252525);

        if (selectedMask == null || selectedSupport == null) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Выбери слева маску, справа блок, на котором она стоит"),
                    width / 2, top + 27, 0xAAAAAA);
            return;
        }

        MaskAutoPositionConfig.Offset value = getOffset(selectedMask.id, selectedSupport.id);
        String pair = selectedMask.id + "  +  " + selectedSupport.id;
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(trim(pair, panelWidth() - 40)), width / 2, top + 5, 0xFFFFFF);

        int groupWidth = 116;
        int gap = 8;
        int total = groupWidth * 3 + gap * 2;
        int start = width / 2 - total / 2;
        int y = top + 23;
        drawAxis(context, "X", value.xPixels, start, y);
        drawAxis(context, "Y", value.yPixels, start + groupWidth + gap, y);
        drawAxis(context, "Z", value.zPixels, start + (groupWidth + gap) * 2, y);

        int resetWidth = 170;
        int resetLeft = width / 2 - resetWidth / 2;
        int resetTop = top + 46;
        context.fill(resetLeft, resetTop, resetLeft + resetWidth, resetTop + 17, 0x88603028);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Сбросить эту комбинацию"),
                width / 2, resetTop + 4, 0xFFFFFF);
    }

    private void drawAxis(DrawContext context, String axis, int value, int x, int y) {
        context.drawTextWithShadow(textRenderer, axis + ":", x, y + 5, 0xFFFFFF);
        drawSmallButton(context, x + 18, y, "−");
        drawSmallButton(context, x + 88, y, "+");
        context.drawCenteredTextWithShadow(textRenderer, signed(value) + " px",
                x + 64, y + 5, 0xFFFF55);
    }

    private void drawSmallButton(DrawContext context, int x, int y, String text) {
        context.fill(x, y, x + BUTTON_SIZE, y + BUTTON_SIZE, 0x88606060);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(text),
                x + BUTTON_SIZE / 2, y + 5, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0 || !synced) {
            return false;
        }

        int left = panelLeft();
        int middle = left + columnWidth();
        int right = left + panelWidth();

        if (mouseY >= listTop() && mouseY < listBottom()) {
            if (mouseX >= left + 8 && mouseX < middle - 5) {
                int index = maskScroll + (int) ((mouseY - listTop()) / ROW_HEIGHT);
                if (index >= 0 && index < filteredMasks.size()) {
                    selectedMask = filteredMasks.get(index);
                    return true;
                }
            }
            if (mouseX >= middle + 5 && mouseX < right - 8) {
                int index = supportScroll + (int) ((mouseY - listTop()) / ROW_HEIGHT);
                if (index >= 0 && index < filteredSupports.size()) {
                    selectedSupport = filteredSupports.get(index);
                    return true;
                }
            }
        }

        if (selectedMask == null || selectedSupport == null) {
            return false;
        }

        MaskAutoPositionConfig.Offset old = getOffset(selectedMask.id, selectedSupport.id);
        int xVal = old.xPixels;
        int yVal = old.yPixels;
        int zVal = old.zPixels;
        int groupWidth = 116;
        int gap = 8;
        int total = groupWidth * 3 + gap * 2;
        int start = width / 2 - total / 2;
        int cy = editorTop() + 23;

        for (int i = 0; i < 3; i++) {
            int sx = start + i * (groupWidth + gap);
            if (inside(mouseX, mouseY, sx + 18, cy, BUTTON_SIZE, BUTTON_SIZE)) {
                if (i == 0) xVal--;
                else if (i == 1) yVal--;
                else zVal--;
                send(xVal, yVal, zVal);
                return true;
            }
            if (inside(mouseX, mouseY, sx + 88, cy, BUTTON_SIZE, BUTTON_SIZE)) {
                if (i == 0) xVal++;
                else if (i == 1) yVal++;
                else zVal++;
                send(xVal, yVal, zVal);
                return true;
            }
        }

        int resetWidth = 170;
        int resetLeft = width / 2 - resetWidth / 2;
        int resetTop = editorTop() + 46;
        if (inside(mouseX, mouseY, resetLeft, resetTop, resetWidth, 17)) {
            send(0, 0, 0);
            return true;
        }
        return false;
    }

    private void send(int x, int y, int z) {
        if (selectedMask == null || selectedSupport == null) {
            return;
        }

        x = MathHelper.clamp(x, -MaskAutoPositionConfig.MAX_ABS_PIXELS, MaskAutoPositionConfig.MAX_ABS_PIXELS);
        y = MathHelper.clamp(y, -MaskAutoPositionConfig.MAX_ABS_PIXELS, MaskAutoPositionConfig.MAX_ABS_PIXELS);
        z = MathHelper.clamp(z, -MaskAutoPositionConfig.MAX_ABS_PIXELS, MaskAutoPositionConfig.MAX_ABS_PIXELS);

        String maskId = selectedMask.id.toString();
        String supportId = selectedSupport.id.toString();
        if (x == 0 && y == 0 && z == 0) {
            Map<String, MaskAutoPositionConfig.Offset> supports = offsets.get(maskId);
            if (supports != null) {
                supports.remove(supportId);
                if (supports.isEmpty()) {
                    offsets.remove(maskId);
                }
            }
        } else {
            offsets.computeIfAbsent(maskId, ignored -> new HashMap<>())
                    .put(supportId, new MaskAutoPositionConfig.Offset(x, y, z));
        }

        PacketByteBuf out = PacketByteBufs.create();
        out.writeString(maskId, 256);
        out.writeString(supportId, 256);
        out.writeInt(x);
        out.writeInt(y);
        out.writeInt(z);
        ClientPlayNetworking.send(MaskAutoPositionNetworking.SET, out);
    }

    private MaskAutoPositionConfig.Offset getOffset(Identifier maskId, Identifier supportId) {
        Map<String, MaskAutoPositionConfig.Offset> supports = offsets.get(maskId.toString());
        if (supports == null) {
            return MaskAutoPositionConfig.Offset.ZERO;
        }
        return supports.getOrDefault(supportId.toString(), MaskAutoPositionConfig.Offset.ZERO);
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private String trim(String value, int maxWidth) {
        if (textRenderer.getWidth(value) <= maxWidth) {
            return value;
        }
        String suffix = "…";
        String result = value;
        while (!result.isEmpty() && textRenderer.getWidth(result + suffix) > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + suffix;
    }

    private String signed(int value) {
        return value >= 0 ? "+" + value : Integer.toString(value);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (amount == 0.0D) {
            return super.mouseScrolled(mouseX, mouseY, amount);
        }

        int middle = panelLeft() + columnWidth();
        int delta = (int) Math.signum(amount) * 3;
        if (mouseX < middle) {
            maskScroll -= delta;
            clampMaskScroll();
        } else {
            supportScroll -= delta;
            clampSupportScroll();
        }
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private record BlockEntry(Block block, Identifier id) {
    }
}
