package fable.hideseek.imba.client;

import fable.hideseek.imba.config.MessageSettingsConfig;
import fable.hideseek.imba.net.MessageSettingsNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Searchable per-message visibility editor. */
public final class MessageSettingsScreen extends Screen {
    private static final int ROW_HEIGHT = 38;

    private final List<MessageSettingsConfig.Rule> filtered = new ArrayList<>();
    private final Map<String, Boolean> visible = new LinkedHashMap<>();
    private TextFieldWidget searchField;
    private int scroll;
    private boolean synced;

    public MessageSettingsScreen() {
        super(Text.literal("Настройщик сообщений IMBA"));
        for (MessageSettingsConfig.Rule rule : MessageSettingsConfig.rules()) {
            visible.put(rule.id, rule.defaultVisible);
        }
        visible.putAll(MessageSettingsClientNetworking.snapshot());
        filtered.addAll(MessageSettingsConfig.rules());
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(620, width - 24);
        int left = (width - panelWidth) / 2;
        searchField = new TextFieldWidget(textRenderer, left + 12, 36, panelWidth - 24, 20, Text.empty());
        searchField.setPlaceholder(Text.literal("Поиск по названию, ID или тексту сообщения..."));
        searchField.setMaxLength(128);
        searchField.setChangedListener(value -> {
            rebuildFilter();
            scroll = 0;
        });
        addDrawableChild(searchField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Сбросить по умолчанию"), b ->
                        ClientPlayNetworking.send(MessageSettingsNetworking.RESET, PacketByteBufs.empty()))
                .dimensions(width / 2 - 155, height - 30, 150, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), b -> close())
                .dimensions(width / 2 + 5, height - 30, 150, 20).build());

        ClientPlayNetworking.send(MessageSettingsNetworking.REQUEST, PacketByteBufs.empty());
    }

    public void applyServerState(Map<String, Boolean> values) {
        if (values == null) return;
        visible.clear();
        for (MessageSettingsConfig.Rule rule : MessageSettingsConfig.rules()) {
            visible.put(rule.id, values.getOrDefault(rule.id, rule.defaultVisible));
        }
        synced = true;
    }

    private void rebuildFilter() {
        String needle = searchField == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        filtered.clear();
        for (MessageSettingsConfig.Rule rule : MessageSettingsConfig.rules()) {
            if (needle.isEmpty()
                    || rule.id.toLowerCase(Locale.ROOT).contains(needle)
                    || rule.label.toLowerCase(Locale.ROOT).contains(needle)
                    || rule.example.toLowerCase(Locale.ROOT).contains(needle)) {
                filtered.add(rule);
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
        int panelWidth = Math.min(620, width - 24);
        int left = (width - panelWidth) / 2;
        int right = left + panelWidth;
        int top = 10;
        int bottom = height - 36;
        context.fill(left, top, right, bottom, 0xDD181818);
        border(context, left, top, panelWidth, bottom - top, 0xFF777777);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 17, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                synced ? "Отключается только текст. Сам запрет/штраф/механика продолжает работать."
                        : "Получение настроек с сервера...",
                width / 2, 59, synced ? 0xFFAAAAAA : 0xFFFFFF55);

        clampScroll();
        int rowLeft = left + 12;
        int rowRight = right - 12;
        int y = listTop();
        for (int row = 0; row < visibleRows(); row++) {
            int index = scroll + row;
            if (index >= filtered.size()) break;
            MessageSettingsConfig.Rule rule = filtered.get(index);
            boolean show = visible.getOrDefault(rule.id, rule.defaultVisible);
            int bg = (index & 1) == 0 ? 0x55303030 : 0x55404040;
            if (mouseX >= rowLeft && mouseX < rowRight && mouseY >= y && mouseY < y + 34) bg = 0x77606060;
            context.fill(rowLeft, y, rowRight, y + 34, bg);
            context.drawTextWithShadow(textRenderer, trim(rule.label, panelWidth - 190), rowLeft + 6, y + 4, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer, trim(rule.example, panelWidth - 190), rowLeft + 6, y + 17, 0xFF999999);
            int statusWidth = 126;
            int statusLeft = rowRight - statusWidth;
            context.fill(statusLeft, y + 5, rowRight - 4, y + 28, show ? 0xAA1E6B2D : 0xAA7A2525);
            context.drawCenteredTextWithShadow(textRenderer, show ? "ПОКАЗЫВАТЬ" : "СКРЫВАТЬ",
                    statusLeft + (statusWidth - 4) / 2, y + 12, show ? 0xFFAAFFAA : 0xFFFFAAAA);
            y += ROW_HEIGHT;
        }

        context.drawTextWithShadow(textRenderer,
                "Найдено: " + filtered.size() + "  •  Нажмите на строку для переключения  •  Колесо мыши — прокрутка",
                left + 12, bottom - 14, 0xFF999999);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0 || !synced) return false;
        int panelWidth = Math.min(620, width - 24);
        int left = (width - panelWidth) / 2;
        int rowLeft = left + 12;
        int rowRight = left + panelWidth - 12;
        if (mouseX < rowLeft || mouseX >= rowRight || mouseY < listTop() || mouseY >= listBottom()) return false;
        int row = (int) ((mouseY - listTop()) / ROW_HEIGHT);
        int index = scroll + row;
        if (index < 0 || index >= filtered.size()) return false;
        MessageSettingsConfig.Rule rule = filtered.get(index);
        boolean newValue = !visible.getOrDefault(rule.id, rule.defaultVisible);
        visible.put(rule.id, newValue);
        PacketByteBuf out = PacketByteBufs.create();
        out.writeString(rule.id, 128);
        out.writeBoolean(newValue);
        ClientPlayNetworking.send(MessageSettingsNetworking.SET, out);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (amount != 0.0D) {
            scroll -= (int) Math.signum(amount) * 2;
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    private String trim(String value, int maxWidth) {
        if (textRenderer.getWidth(value) <= maxWidth) return value;
        String result = value;
        while (!result.isEmpty() && textRenderer.getWidth(result + "…") > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "…";
    }

    private static void border(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    @Override public boolean shouldPause() { return false; }
}
