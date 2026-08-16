package fable.hideseek.imba.client;

import fable.hideseek.imba.net.MaskNetworking;
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

/** Full editor for values and visual layout of the 3x3 world settings panel. */
public final class GameSettingsScreen extends Screen {
    private int seconds = PanelData.seconds;
    private int hearts = PanelData.hearts;

    private TextFieldWidget timerLabel;
    private TextFieldWidget heartsLabel;
    private TextFieldWidget timerTitleScale;
    private TextFieldWidget heartsTitleScale;
    private TextFieldWidget timerValueScale;
    private TextFieldWidget heartsValueScale;
    private TextFieldWidget arrowScale;
    private TextFieldWidget timerX;
    private TextFieldWidget heartsX;
    private TextFieldWidget titleY;
    private TextFieldWidget upArrowY;
    private TextFieldWidget valueY;
    private TextFieldWidget downArrowY;

    public GameSettingsScreen() {
        super(Text.literal("Настройщик панели IMBA"));
    }

    @Override
    protected void init() {
        int left = width / 2 - 220;
        int top = Math.max(10, height / 2 - 165);

        addDrawableChild(ButtonWidget.builder(Text.literal("Таймер +30 сек."), b -> changeSeconds(30))
                .dimensions(left, top + 24, 106, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Таймер -30 сек."), b -> changeSeconds(-30))
                .dimensions(left + 112, top + 24, 106, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Сердца +1"), b -> changeHearts(1))
                .dimensions(left + 222, top + 24, 106, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Сердца -1"), b -> changeHearts(-1))
                .dimensions(left + 334, top + 24, 106, 20).build());

        timerLabel = field(left, top + 66, 214, PanelData.timerLabel, 24);
        heartsLabel = field(left + 226, top + 66, 214, PanelData.heartsLabel, 24);

        timerTitleScale = field(left, top + 106, 100, fmt(PanelData.timerTitleScale), 8);
        heartsTitleScale = field(left + 112, top + 106, 100, fmt(PanelData.heartsTitleScale), 8);
        timerValueScale = field(left + 226, top + 106, 100, fmt(PanelData.timerValueScale), 8);
        heartsValueScale = field(left + 340, top + 106, 100, fmt(PanelData.heartsValueScale), 8);

        arrowScale = field(left, top + 146, 100, fmt(PanelData.arrowScale), 8);
        timerX = field(left + 112, top + 146, 100, Integer.toString(PanelData.timerX), 8);
        heartsX = field(left + 226, top + 146, 100, Integer.toString(PanelData.heartsX), 8);

        titleY = field(left, top + 186, 100, Integer.toString(PanelData.titleY), 8);
        upArrowY = field(left + 112, top + 186, 100, Integer.toString(PanelData.upArrowY), 8);
        valueY = field(left + 226, top + 186, 100, Integer.toString(PanelData.valueY), 8);
        downArrowY = field(left + 340, top + 186, 100, Integer.toString(PanelData.downArrowY), 8);

        addDrawableChild(ButtonWidget.builder(Text.literal("Применить"), b -> sendAll())
                .dimensions(left, top + 228, 142, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Сбросить внешний вид"), b ->
                        ClientPlayNetworking.send(PanelSettingsNetworking.RESET_LAYOUT, PacketByteBufs.empty()))
                .dimensions(left + 149, top + 228, 142, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), b -> close())
                .dimensions(left + 298, top + 228, 142, 20).build());
    }

    private TextFieldWidget field(int x, int y, int width, String value, int maxLength) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, width, 20, Text.empty());
        field.setMaxLength(maxLength);
        field.setText(value == null ? "" : value);
        addDrawableChild(field);
        return field;
    }

    private void changeSeconds(int delta) {
        seconds = Math.max(30, Math.min(3600, seconds + delta));
        sendGameValues();
    }

    private void changeHearts(int delta) {
        hearts = Math.max(1, Math.min(100, hearts + delta));
        sendGameValues();
    }

    private void sendGameValues() {
        PacketByteBuf settings = PacketByteBufs.create();
        settings.writeVarInt(seconds);
        settings.writeVarInt(hearts);
        settings.writeVarInt(PanelData.selectedLocation);
        ClientPlayNetworking.send(MaskNetworking.GAME_SETTINGS_PACKET, settings);
    }

    private void sendAll() {
        sendGameValues();
        try {
            PacketByteBuf layout = PacketByteBufs.create();
            layout.writeString(cleanLabel(timerLabel.getText(), "Таймер"), 32);
            layout.writeString(cleanLabel(heartsLabel.getText(), "Сердца"), 32);
            layout.writeFloat(parseFloat(timerTitleScale));
            layout.writeFloat(parseFloat(heartsTitleScale));
            layout.writeFloat(parseFloat(timerValueScale));
            layout.writeFloat(parseFloat(heartsValueScale));
            layout.writeFloat(parseFloat(arrowScale));
            layout.writeInt(parseInt(timerX));
            layout.writeInt(parseInt(heartsX));
            layout.writeInt(parseInt(titleY));
            layout.writeInt(parseInt(upArrowY));
            layout.writeInt(parseInt(valueY));
            layout.writeInt(parseInt(downArrowY));
            ClientPlayNetworking.send(PanelSettingsNetworking.SET_LAYOUT, layout);
        } catch (NumberFormatException ignored) {
            if (client != null && client.player != null) {
                client.player.sendMessage(Text.literal("§cПроверь числовые поля настройщика панели"), true);
            }
        }
    }

    public void applyPanelLayout() {
        if (timerLabel == null) return;
        if (!timerLabel.isFocused()) timerLabel.setText(PanelData.timerLabel);
        if (!heartsLabel.isFocused()) heartsLabel.setText(PanelData.heartsLabel);
        setIfNotFocused(timerTitleScale, fmt(PanelData.timerTitleScale));
        setIfNotFocused(heartsTitleScale, fmt(PanelData.heartsTitleScale));
        setIfNotFocused(timerValueScale, fmt(PanelData.timerValueScale));
        setIfNotFocused(heartsValueScale, fmt(PanelData.heartsValueScale));
        setIfNotFocused(arrowScale, fmt(PanelData.arrowScale));
        setIfNotFocused(timerX, Integer.toString(PanelData.timerX));
        setIfNotFocused(heartsX, Integer.toString(PanelData.heartsX));
        setIfNotFocused(titleY, Integer.toString(PanelData.titleY));
        setIfNotFocused(upArrowY, Integer.toString(PanelData.upArrowY));
        setIfNotFocused(valueY, Integer.toString(PanelData.valueY));
        setIfNotFocused(downArrowY, Integer.toString(PanelData.downArrowY));
    }

    private static void setIfNotFocused(TextFieldWidget field, String value) {
        if (field != null && !field.isFocused()) field.setText(value);
    }

    private static String cleanLabel(String value, String fallback) {
        String clean = value == null ? "" : value.trim();
        return clean.isEmpty() ? fallback : clean;
    }

    private static float parseFloat(TextFieldWidget field) {
        return Float.parseFloat(field.getText().replace(',', '.').trim());
    }

    private static int parseInt(TextFieldWidget field) {
        return Integer.parseInt(field.getText().trim());
    }

    private static String fmt(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int left = width / 2 - 220;
        int top = Math.max(10, height / 2 - 165);

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, top, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                "Текущие значения: " + String.format("%02d:%02d", seconds / 60, seconds % 60)
                        + "    ❤ " + hearts,
                width / 2, top + 12, 0xFFBBBBBB);

        label(context, "Название левой колонки", left, top + 54);
        label(context, "Название правой колонки", left + 226, top + 54);

        label(context, "Размер загол. L", left, top + 94);
        label(context, "Размер загол. R", left + 112, top + 94);
        label(context, "Размер значения L", left + 226, top + 94);
        label(context, "Размер значения R", left + 340, top + 94);

        label(context, "Размер стрелок", left, top + 134);
        label(context, "X левой колонки", left + 112, top + 134);
        label(context, "X правой колонки", left + 226, top + 134);

        label(context, "Y заголовков", left, top + 174);
        label(context, "Y верхних ▲", left + 112, top + 174);
        label(context, "Y значений", left + 226, top + 174);
        label(context, "Y нижних ▼", left + 340, top + 174);

        context.drawTextWithShadow(textRenderer,
                "Размеры: 0.40–3.00 • X: -120…120 • Y: -100…100 • изменения видны сразу после «Применить»",
                left, top + 214, 0xFF888888);
        super.render(context, mouseX, mouseY, delta);
    }

    private void label(DrawContext context, String text, int x, int y) {
        context.drawTextWithShadow(textRenderer, text, x, y, 0xFFCCCCCC);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
