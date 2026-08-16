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

/** Полный редактор значений и внешнего вида 3x3-панели. */
public final class GameSettingsScreen extends Screen {
    public static final String BUILD_MARKER = "PANEL-V2";

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
        super(Text.literal("Редактор панели IMBA v2"));
    }

    @Override
    protected void init() {
        int left = width / 2 - 230;
        int top = Math.max(8, height / 2 - 174);

        // Значения игры.
        addDrawableChild(ButtonWidget.builder(Text.literal("Таймер +30"), b -> changeSeconds(30))
                .dimensions(left, top + 30, 106, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Таймер -30"), b -> changeSeconds(-30))
                .dimensions(left + 112, top + 30, 106, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Сердца +1"), b -> changeHearts(1))
                .dimensions(left + 242, top + 30, 106, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Сердца -1"), b -> changeHearts(-1))
                .dimensions(left + 354, top + 30, 106, 20).build());

        // Названия.
        timerLabel = field(left, top + 78, 218, PanelData.timerLabel, 24);
        heartsLabel = field(left + 242, top + 78, 218, PanelData.heartsLabel, 24);

        // Размеры.
        timerTitleScale = field(left, top + 126, 102, fmt(PanelData.timerTitleScale), 8);
        heartsTitleScale = field(left + 114, top + 126, 102, fmt(PanelData.heartsTitleScale), 8);
        timerValueScale = field(left + 242, top + 126, 102, fmt(PanelData.timerValueScale), 8);
        heartsValueScale = field(left + 356, top + 126, 104, fmt(PanelData.heartsValueScale), 8);

        arrowScale = field(left, top + 174, 102, fmt(PanelData.arrowScale), 8);
        addDrawableChild(ButtonWidget.builder(Text.literal("Все размеры -0.10"), b -> adjustAllScales(-0.10F))
                .dimensions(left + 114, top + 174, 160, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Все размеры +0.10"), b -> adjustAllScales(0.10F))
                .dimensions(left + 286, top + 174, 174, 20).build());

        // Положение.
        timerX = field(left, top + 222, 102, Integer.toString(PanelData.timerX), 8);
        heartsX = field(left + 114, top + 222, 102, Integer.toString(PanelData.heartsX), 8);
        titleY = field(left + 242, top + 222, 102, Integer.toString(PanelData.titleY), 8);
        upArrowY = field(left + 356, top + 222, 104, Integer.toString(PanelData.upArrowY), 8);

        valueY = field(left, top + 270, 102, Integer.toString(PanelData.valueY), 8);
        downArrowY = field(left + 114, top + 270, 102, Integer.toString(PanelData.downArrowY), 8);
        addDrawableChild(ButtonWidget.builder(Text.literal("Стандартные X/Y"), b -> resetPositionFields())
                .dimensions(left + 242, top + 270, 218, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("ПРИМЕНИТЬ ВСЁ"), b -> sendAll())
                .dimensions(left, top + 310, 146, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Сбросить всю панель"), b ->
                        ClientPlayNetworking.send(PanelSettingsNetworking.RESET_LAYOUT, PacketByteBufs.empty()))
                .dimensions(left + 157, top + 310, 146, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), b -> close())
                .dimensions(left + 314, top + 310, 146, 20).build());
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

            if (client != null && client.player != null) {
                client.player.sendMessage(Text.literal("§aНастройки панели IMBA применены"), true);
            }
        } catch (NumberFormatException ignored) {
            if (client != null && client.player != null) {
                client.player.sendMessage(Text.literal("§cПроверь числовые поля настройщика панели"), true);
            }
        }
    }

    private void adjustAllScales(float delta) {
        adjustScale(timerTitleScale, delta);
        adjustScale(heartsTitleScale, delta);
        adjustScale(timerValueScale, delta);
        adjustScale(heartsValueScale, delta);
        adjustScale(arrowScale, delta);
    }

    private void adjustScale(TextFieldWidget field, float delta) {
        try {
            float value = Float.parseFloat(field.getText().replace(',', '.').trim());
            value = Math.max(0.40F, Math.min(3.00F, value + delta));
            field.setText(fmt(value));
        } catch (NumberFormatException ignored) {
        }
    }

    private void resetPositionFields() {
        timerX.setText("-38");
        heartsX.setText("38");
        titleY.setText("-52");
        upArrowY.setText("-26");
        valueY.setText("0");
        downArrowY.setText("28");
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
        int left = width / 2 - 230;
        int top = Math.max(8, height / 2 - 174);

        context.drawCenteredTextWithShadow(textRenderer,
                "Редактор панели IMBA v2  [" + BUILD_MARKER + "]",
                width / 2, top, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                "Текущие значения: " + String.format("%02d:%02d", seconds / 60, seconds % 60)
                        + "    ❤ " + hearts,
                width / 2, top + 14, 0xFFBBBBBB);

        section(context, "ЗНАЧЕНИЯ", left, top + 18);
        section(context, "НАЗВАНИЯ", left, top + 62);
        label(context, "Левая колонка (бывш. Таймер)", left, top + 66);
        label(context, "Правая колонка (бывш. Сердца)", left + 242, top + 66);

        section(context, "РАЗМЕР ТЕКСТА", left, top + 110);
        label(context, "Заголовок L", left, top + 114);
        label(context, "Заголовок R", left + 114, top + 114);
        label(context, "Значение L", left + 242, top + 114);
        label(context, "Значение R", left + 356, top + 114);
        label(context, "Размер ▲ / ▼", left, top + 162);

        section(context, "ПОЛОЖЕНИЕ", left, top + 206);
        label(context, "X левой", left, top + 210);
        label(context, "X правой", left + 114, top + 210);
        label(context, "Y заголовков", left + 242, top + 210);
        label(context, "Y верхних ▲", left + 356, top + 210);
        label(context, "Y значений", left, top + 258);
        label(context, "Y нижних ▼", left + 114, top + 258);

        context.drawTextWithShadow(textRenderer,
                "Scale 0.40–3.00 • X -120…120 • Y -100…100 • кнопка «ПРИМЕНИТЬ ВСЁ» сохраняет на сервере",
                left, top + 296, 0xFF888888);

        super.render(context, mouseX, mouseY, delta);
    }

    private void section(DrawContext context, String text, int x, int y) {
        context.drawTextWithShadow(textRenderer, "§6" + text, x, y, 0xFFFFAA00);
    }

    private void label(DrawContext context, String text, int x, int y) {
        context.drawTextWithShadow(textRenderer, text, x, y, 0xFFCCCCCC);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
