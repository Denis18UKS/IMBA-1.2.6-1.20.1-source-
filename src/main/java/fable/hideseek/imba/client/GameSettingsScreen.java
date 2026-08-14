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

public final class GameSettingsScreen extends Screen {
    private int seconds = PanelData.seconds;
    private int hearts = PanelData.hearts;
    private TextFieldWidget heartsLabel;

    public GameSettingsScreen() {
        super(Text.literal("Настройки игровой панели"));
    }

    @Override
    protected void init() {
        int center = width / 2;
        int top = Math.max(30, height / 2 - 100);

        addDrawableChild(ButtonWidget.builder(Text.literal("▲ +30 сек."), b -> changeSeconds(30))
                .dimensions(center - 140, top + 42, 125, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("▼ -30 сек."), b -> changeSeconds(-30))
                .dimensions(center - 140, top + 92, 125, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("▲ +1"), b -> changeHearts(1))
                .dimensions(center + 15, top + 42, 125, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("▼ -1"), b -> changeHearts(-1))
                .dimensions(center + 15, top + 92, 125, 20).build());

        heartsLabel = new TextFieldWidget(textRenderer, center - 140, top + 145, 280, 20,
                Text.literal("Подпись счётчика"));
        heartsLabel.setMaxLength(24);
        heartsLabel.setText(PanelData.heartsLabel == null || PanelData.heartsLabel.isBlank()
                ? "Сердца" : PanelData.heartsLabel);
        addDrawableChild(heartsLabel);

        addDrawableChild(ButtonWidget.builder(Text.literal("Применить подпись"), b -> send())
                .dimensions(center - 140, top + 171, 136, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Сбросить: Сердца"), b -> {
                    heartsLabel.setText("Сердца");
                    send();
                }).dimensions(center + 4, top + 171, 136, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), b -> close())
                .dimensions(center - 140, top + 201, 280, 20).build());
    }

    private void changeSeconds(int delta) {
        seconds = Math.max(30, Math.min(3600, seconds + delta));
        send();
    }

    private void changeHearts(int delta) {
        hearts = Math.max(1, Math.min(100, hearts + delta));
        send();
    }

    private void send() {
        PacketByteBuf settings = PacketByteBufs.create();
        settings.writeVarInt(seconds);
        settings.writeVarInt(hearts);
        settings.writeVarInt(PanelData.selectedLocation);
        ClientPlayNetworking.send(MaskNetworking.GAME_SETTINGS_PACKET, settings);

        String label = heartsLabel == null ? "Сердца" : heartsLabel.getText().trim();
        if (label.isEmpty()) label = "Сердца";
        PacketByteBuf title = PacketByteBufs.create();
        title.writeString(label, 32);
        ClientPlayNetworking.send(PanelSettingsNetworking.SET_LABEL, title);
    }

    public void applyHeartsLabel(String label) {
        if (heartsLabel != null && !heartsLabel.isFocused()) {
            heartsLabel.setText(label == null || label.isBlank() ? "Сердца" : label);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int center = width / 2;
        int top = Math.max(30, height / 2 - 100);
        context.drawCenteredTextWithShadow(textRenderer, title, center, top, 0xFFFFFF);

        context.drawCenteredTextWithShadow(textRenderer, "Таймер", center - 78, top + 27, 0xFFFFAA00);
        context.drawCenteredTextWithShadow(textRenderer,
                String.format("%02d:%02d", seconds / 60, seconds % 60), center - 78, top + 70, 0xFFFFFFFF);

        context.drawCenteredTextWithShadow(textRenderer, "Сердца", center + 78, top + 27, 0xFFFFAA00);
        context.drawCenteredTextWithShadow(textRenderer, "❤ " + hearts, center + 78, top + 70, 0xFFFF5555);

        context.drawTextWithShadow(textRenderer, "Название над счётчиком сердец:", center - 140, top + 132, 0xFFCCCCCC);
        context.drawCenteredTextWithShadow(textRenderer,
                "Изменение применяется всем игрокам сразу и сохраняется на сервере",
                center, top + 194, 0xFF888888);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
