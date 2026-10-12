package fable.hideseek.imba.client;

import fable.hideseek.imba.net.HologramNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HologramProjectorScreen extends Screen {
    private List<HologramClientData.Projector> projectors = new ArrayList<>();
    private int requestedProjectorId;
    private int index;
    private int location;
    private int light = 15;
    private float contrast = 1.0F;
    private boolean lightingTab;
    private boolean textBackground;
    private TextFieldWidget x, y, z, yaw, scale;
    private ButtonWidget locationPrev, locationButton, locationNext, positionTab, lightingTabButton;
    private ButtonWidget lightMinus, lightValue, lightPlus, lightDark, lightMid, lightBright;
    private ButtonWidget contrastMinus, contrastValue, contrastPlus, contrastLow, contrastDefault, contrastHigh;
    private ButtonWidget takePosition, takeYaw, deleteButton, textBackgroundButton;

    public HologramProjectorScreen() {
        this(Integer.MIN_VALUE);
    }

    public HologramProjectorScreen(int requestedProjectorId) {
        super(Text.literal("Голопроектор локаций"));
        this.requestedProjectorId = requestedProjectorId;
    }

    @Override
    protected void init() {
        int left = width / 2 - 155;
        int top = Math.max(14, height / 2 - 130);
        addDrawableChild(ButtonWidget.builder(Text.literal("◀"), b -> select(index - 1)).dimensions(left, top, 36, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Новая голограмма"), b -> select(projectors.size())).dimensions(left + 42, top, 226, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("▶"), b -> select(index + 1)).dimensions(left + 274, top, 36, 20).build());

        positionTab = addDrawableChild(ButtonWidget.builder(Text.literal("Положение и вид"), b -> {
            lightingTab = false;
            refreshTabs();
        }).dimensions(left, top + 28, 152, 20).build());
        lightingTabButton = addDrawableChild(ButtonWidget.builder(Text.literal("Освещение"), b -> {
            lightingTab = true;
            refreshTabs();
        }).dimensions(left + 158, top + 28, 152, 20).build());

        locationPrev = addDrawableChild(ButtonWidget.builder(Text.literal("◀"), b -> changeLocation(-1)).dimensions(left, top + 70, 30, 20).build());
        locationButton = addDrawableChild(ButtonWidget.builder(locationText(), b -> changeLocation(1)).dimensions(left + 36, top + 70, 238, 20).build());
        locationNext = addDrawableChild(ButtonWidget.builder(Text.literal("▶"), b -> changeLocation(1)).dimensions(left + 280, top + 70, 30, 20).build());

        x = field(left, top + 111, 98, 0.0);
        y = field(left + 106, top + 111, 98, 0.0);
        z = field(left + 212, top + 111, 98, 0.0);
        yaw = field(left, top + 151, 151, 0.0);
        scale = field(left + 159, top + 151, 151, 0.80);
        takePosition = addDrawableChild(ButtonWidget.builder(Text.literal("Взять мою позицию"), b -> copyPlayerPosition()).dimensions(left, top + 181, 152, 20).build());
        takeYaw = addDrawableChild(ButtonWidget.builder(Text.literal("Взять мой поворот"), b -> copyPlayerYaw()).dimensions(left + 158, top + 181, 152, 20).build());
        textBackgroundButton = addDrawableChild(ButtonWidget.builder(textBackgroundText(), b -> {
            textBackground = !textBackground;
            b.setMessage(textBackgroundText());
        }).dimensions(left, top + 206, 310, 20).build());

        lightMinus = addDrawableChild(ButtonWidget.builder(Text.literal("−"), b -> changeLight(-1)).dimensions(left, top + 88, 42, 20).build());
        lightValue = addDrawableChild(ButtonWidget.builder(lightText(), b -> changeLight(1)).dimensions(left + 48, top + 88, 214, 20).build());
        lightPlus = addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> changeLight(1)).dimensions(left + 268, top + 88, 42, 20).build());
        lightDark = addDrawableChild(ButtonWidget.builder(Text.literal("Тёмная: 0"), b -> setLight(0)).dimensions(left, top + 120, 96, 20).build());
        lightMid = addDrawableChild(ButtonWidget.builder(Text.literal("Средняя: 8"), b -> setLight(8)).dimensions(left + 107, top + 120, 96, 20).build());
        lightBright = addDrawableChild(ButtonWidget.builder(Text.literal("Яркая: 15"), b -> setLight(15)).dimensions(left + 214, top + 120, 96, 20).build());
        contrastMinus = addDrawableChild(ButtonWidget.builder(Text.literal("−"), b -> changeContrast(-0.10F)).dimensions(left, top + 151, 42, 20).build());
        contrastValue = addDrawableChild(ButtonWidget.builder(contrastText(), b -> changeContrast(0.10F)).dimensions(left + 48, top + 151, 214, 20).build());
        contrastPlus = addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> changeContrast(0.10F)).dimensions(left + 268, top + 151, 42, 20).build());
        contrastLow = addDrawableChild(ButtonWidget.builder(Text.literal("Низкая: 0.5"), b -> setContrast(0.5F)).dimensions(left, top + 181, 96, 20).build());
        contrastDefault = addDrawableChild(ButtonWidget.builder(Text.literal("Обычная: 1.0"), b -> setContrast(1.0F)).dimensions(left + 107, top + 181, 96, 20).build());
        contrastHigh = addDrawableChild(ButtonWidget.builder(Text.literal("Высокая: 1.5"), b -> setContrast(1.5F)).dimensions(left + 214, top + 181, 96, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Сохранить"), b -> save()).dimensions(left, top + 232, 150, 20).build());
        deleteButton = addDrawableChild(ButtonWidget.builder(Text.literal("Удалить"), b -> delete()).dimensions(left + 160, top + 232, 150, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Назад в библиотеку"), b ->
                MinecraftClient.getInstance().setScreen(new HologramLibraryScreen()))
                .dimensions(left, top + 258, 310, 20).build());

        projectors = new ArrayList<>(HologramClientData.snapshot());
        if (requestedProjectorId == -1) {
            select(projectors.size());
            requestedProjectorId = Integer.MIN_VALUE;
        } else if (requestedProjectorId >= 0) {
            int requestedIndex = findProjectorIndex(requestedProjectorId);
            if (requestedIndex < projectors.size()) {
                select(requestedIndex);
                requestedProjectorId = Integer.MIN_VALUE;
            } else {
                select(Math.min(index, projectors.size()));
            }
        } else {
            select(Math.min(index, projectors.size()));
        }
        lightingTab = false;
        refreshTabs();
        ClientPlayNetworking.send(HologramNetworking.REQUEST, PacketByteBufs.empty());
    }

    private TextFieldWidget field(int x, int y, int width, double value) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, width, 20, Text.empty());
        field.setText(fmt(value));
        field.setMaxLength(24);
        addDrawableChild(field);
        return field;
    }

    private void refreshTabs() {
        positionTab.active = lightingTab;
        lightingTabButton.active = !lightingTab;
        boolean position = !lightingTab;
        locationPrev.visible = position;
        locationButton.visible = position;
        locationNext.visible = position;
        x.visible = position;
        y.visible = position;
        z.visible = position;
        yaw.visible = position;
        scale.visible = position;
        takePosition.visible = position;
        takeYaw.visible = position;
        textBackgroundButton.visible = position;
        lightMinus.visible = lightingTab;
        lightValue.visible = lightingTab;
        lightPlus.visible = lightingTab;
        lightDark.visible = lightingTab;
        lightMid.visible = lightingTab;
        lightBright.visible = lightingTab;
        contrastMinus.visible = lightingTab;
        contrastValue.visible = lightingTab;
        contrastPlus.visible = lightingTab;
        contrastLow.visible = lightingTab;
        contrastDefault.visible = lightingTab;
        contrastHigh.visible = lightingTab;
    }

    private void changeLocation(int delta) {
        int count = Math.max(1, PanelData.locationCount);
        location = Math.floorMod(location + delta, count);
        locationButton.setMessage(locationText());
    }

    private void changeLight(int delta) {
        setLight(light + delta);
    }

    private void setLight(int value) {
        light = Math.max(0, Math.min(15, value));
        lightValue.setMessage(lightText());
    }

    private Text locationText() {
        return Text.literal("Локация: " + PanelData.locationName(location));
    }

    private Text lightText() {
        return Text.literal("Яркость: " + light + " / 15");
    }

    private Text contrastText() { return Text.literal("Контраст: " + String.format(Locale.ROOT, "%.1fx", contrast)); }
    private void changeContrast(float delta) { setContrast(contrast + delta); }
    private void setContrast(float value) { contrast = Math.max(0.50F, Math.min(2.0F, Math.round(value * 10.0F) / 10.0F)); contrastValue.setMessage(contrastText()); }

    private Text textBackgroundText() {
        return Text.literal("Фон под названием: " + (textBackground ? "ВКЛ" : "ВЫКЛ"));
    }

    private void copyPlayerPosition() {
        var player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        x.setText(fmt(player.getX()));
        y.setText(fmt(player.getY() + 1.25D));
        z.setText(fmt(player.getZ()));
    }

    private void copyPlayerYaw() {
        var player = MinecraftClient.getInstance().player;
        if (player != null) {
            yaw.setText(fmt(player.getYaw()));
        }
    }

    public void applyServerState(List<HologramClientData.Projector> list) {
        int preservedId = index < projectors.size() ? projectors.get(index).id() : requestedProjectorId;
        projectors = new ArrayList<>(list);
        if (preservedId >= 0) select(findProjectorIndex(preservedId));
        else select(Math.min(index, projectors.size()));
        requestedProjectorId = Integer.MIN_VALUE;
    }

    private int findProjectorIndex(int id) {
        for (int i = 0; i < projectors.size(); i++) if (projectors.get(i).id() == id) return i;
        return projectors.size();
    }

    private void select(int next) {
        index = Math.max(0, Math.min(next, projectors.size()));
        if (x == null) {
            return;
        }
        if (index < projectors.size()) {
            var p = projectors.get(index);
            location = Math.max(0, Math.min(Math.max(0, PanelData.locationCount - 1), p.location()));
            x.setText(fmt(p.x()));
            y.setText(fmt(p.y()));
            z.setText(fmt(p.z()));
            yaw.setText(fmt(p.yaw()));
            scale.setText(fmt(p.scale()));
            light = p.light();
            textBackground = p.textBackground();
            contrast = p.contrast();
        } else {
            var player = MinecraftClient.getInstance().player;
            if (player != null) {
                x.setText(fmt(player.getX()));
                y.setText(fmt(player.getY() + 1.25D));
                z.setText(fmt(player.getZ()));
                yaw.setText(fmt(player.getYaw()));
            } else {
                x.setText("0.000");
                y.setText("0.000");
                z.setText("0.000");
                yaw.setText("0.000");
            }
            scale.setText("0.800");
            light = 15;
            textBackground = false;
            contrast = 1.0F;
        }
        locationButton.setMessage(locationText());
        lightValue.setMessage(lightText());
        textBackgroundButton.setMessage(textBackgroundText());
        contrastValue.setMessage(contrastText());
        deleteButton.active = index < projectors.size();
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static double parse(TextFieldWidget field) {
        return Double.parseDouble(field.getText().replace(',', '.'));
    }

    private void save() {
        try {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(index < projectors.size() ? projectors.get(index).id() : -1);
            buf.writeVarInt(location);
            var client = MinecraftClient.getInstance();
            String world = client.world == null ? "minecraft:overworld" : client.world.getRegistryKey().getValue().toString();
            buf.writeString(world, 128);
            buf.writeDouble(parse(x));
            buf.writeDouble(parse(y));
            buf.writeDouble(parse(z));
            buf.writeFloat((float) parse(yaw));
            buf.writeFloat((float) parse(scale));
            buf.writeByte(light);
            buf.writeBoolean(textBackground);
            buf.writeFloat(contrast);
            float currentTextScale = index < projectors.size() ? projectors.get(index).textScale() : 1.0F;
            int currentTitleBreak = index < projectors.size() ? projectors.get(index).titleBreak() : 0;
            buf.writeFloat(currentTextScale);
            buf.writeInt(currentTitleBreak);
            ClientPlayNetworking.send(HologramNetworking.SAVE, buf);
        } catch (NumberFormatException ignored) {
        }
    }

    private void delete() {
        if (index >= projectors.size()) {
            return;
        }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(projectors.get(index).id());
        ClientPlayNetworking.send(HologramNetworking.DELETE, buf);
        index = Math.max(0, index - 1);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int left = width / 2 - 155;
        int top = Math.max(14, height / 2 - 130);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, top - 12, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                index < projectors.size() ? "Голограмма " + (index + 1) + " / " + projectors.size() : "Новая голограмма",
                width / 2, top + 14, 0xFFAAAAAA);
        if (!lightingTab) {
            context.drawTextWithShadow(textRenderer, "Выберите фотографию локации", left, top + 58, 0xFFBBBBBB);
            context.drawTextWithShadow(textRenderer, "X", left, top + 99, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer, "Y", left + 106, top + 99, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer, "Z", left + 212, top + 99, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer, "Поворот Yaw", left, top + 139, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer, "Размер", left + 159, top + 139, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer,
                    "1.0 = квадратная голограмма • текст всегда белый и только с лицевой стороны", left, top + 221, 0xFF888888);
        } else {
            context.drawTextWithShadow(textRenderer, "Яркость фотографии", left, top + 70, 0xFFBBBBBB);
            context.drawTextWithShadow(textRenderer,
                    "15 = максимально светлая фотография без затемнённой entity-подачи", left, top + 153, 0xFFAAAAAA);
            context.drawTextWithShadow(textRenderer,
                    "Контраст: 0.5x–2.0x. Фото двухстороннее и непрозрачное.", left, top + 169, 0xFF888888);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
