package fable.hideseek.imba.client;

import fable.hideseek.imba.net.LocalNightNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class LocalNightScreen extends Screen {
    private int selected = LocalNightClientData.selectedLocation();
    private ButtonWidget locationButton;

    public LocalNightScreen() {
        super(Text.literal("Локальная ночь"));
    }

    @Override
    protected void init() {
        int left = width / 2 - 150;
        int top = height / 2 - 62;
        addDrawableChild(ButtonWidget.builder(Text.literal("◀"), b -> change(-1))
                .dimensions(left, top, 34, 20).build());
        locationButton = addDrawableChild(ButtonWidget.builder(locationText(), b -> change(1))
                .dimensions(left + 40, top, 220, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("▶"), b -> change(1))
                .dimensions(left + 266, top, 34, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Отключить локальную ночь"), b -> {
            selected = -1;
            refresh();
        }).dimensions(left, top + 30, 300, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Сохранить"), b -> save())
                .dimensions(left, top + 60, 146, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), b -> close())
                .dimensions(left + 154, top + 60, 146, 20).build());
        ClientPlayNetworking.send(LocalNightNetworking.REQUEST, PacketByteBufs.empty());
    }

    public void applyServerState(int value) {
        selected = value;
        refresh();
    }

    private void change(int delta) {
        int snow = findSnowVillageStreet();
        selected = selected == snow ? -1 : snow;
        refresh();
    }

    private int findSnowVillageStreet() {
        for (int i = 0; i < PanelData.locationCount; i++) {
            if (LocalNightClientData.isSnowVillageStreet(PanelData.locationName(i))) return i;
        }
        return -1;
    }

    private void refresh() {
        if (locationButton != null) locationButton.setMessage(locationText());
    }

    private Text locationText() {
        return selected < 0
                ? Text.literal("Ночная локация: ВЫКЛ")
                : Text.literal("Ночная локация: " + PanelData.locationName(selected));
    }

    private void save() {
        var buf = PacketByteBufs.create();
        buf.writeInt(selected);
        ClientPlayNetworking.send(LocalNightNetworking.SAVE, buf);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int top = height / 2 - 62;
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, top - 24, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                "Ночь доступна только на «Улице снежной деревни»; везде остальное — день.",
                width / 2, top - 8, 0xFFBBBBBB);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
