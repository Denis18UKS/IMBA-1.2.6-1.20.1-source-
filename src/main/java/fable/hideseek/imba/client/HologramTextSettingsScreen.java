package fable.hideseek.imba.client;

import fable.hideseek.imba.net.HologramNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Live text-only editor. It never changes photo size, position or lighting. */
public final class HologramTextSettingsScreen extends Screen {
    private final int projectorId;
    private HologramClientData.Projector projector;
    private float textScale = 1.0F;
    private int titleBreak;
    private ButtonWidget scaleValue, breakValue;

    public HologramTextSettingsScreen(int projectorId) {
        super(Text.literal("Текст голограммы"));
        this.projectorId = projectorId;
        findProjector(HologramClientData.snapshot());
    }

    @Override
    protected void init() {
        int left = width / 2 - 170;
        int top = height / 2 - 80;
        addDrawableChild(ButtonWidget.builder(Text.literal("− текст"), b -> changeTextScale(-0.10F))
                .dimensions(left, top, 104, 20).build());
        scaleValue = addDrawableChild(ButtonWidget.builder(scaleText(), b -> {})
                .dimensions(left + 112, top, 116, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+ текст"), b -> changeTextScale(0.10F))
                .dimensions(left + 236, top, 104, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("◀ перенос"), b -> changeTitleBreak(-1))
                .dimensions(left, top + 32, 104, 20).build());
        breakValue = addDrawableChild(ButtonWidget.builder(breakText(), b -> changeTitleBreak(1))
                .dimensions(left + 112, top + 32, 116, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("перенос ▶"), b -> changeTitleBreak(1))
                .dimensions(left + 236, top + 32, 104, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Без переноса"), b -> { titleBreak = 0; sendSave(); refresh(); })
                .dimensions(left, top + 64, 340, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Назад в библиотеку"), b -> client.setScreen(new HologramTextLibraryScreen()))
                .dimensions(left, top + 118, 340, 20).build());

        refresh();
        ClientPlayNetworking.send(HologramNetworking.REQUEST, PacketByteBufs.empty());
    }

    public void applyServerState(List<HologramClientData.Projector> list) {
        findProjector(list);
        refresh();
    }

    private void findProjector(List<HologramClientData.Projector> list) {
        for (var p : list) {
            if (p.id() == projectorId) {
                projector = p;
                textScale = p.textScale();
                titleBreak = p.titleBreak();
                return;
            }
        }
        projector = null;
    }

    private void changeTextScale(float delta) {
        if (projector == null) return;
        textScale = Math.max(0.50F, Math.min(3.0F, Math.round((textScale + delta) * 10.0F) / 10.0F));
        sendSave();
        refresh();
    }

    private void changeTitleBreak(int direction) {
        if (projector == null) return;
        List<Integer> options = breakOptions(title());
        int current = options.indexOf(titleBreak);
        if (current < 0) current = 0;
        current = Math.floorMod(current + direction, options.size());
        titleBreak = options.get(current);
        sendSave();
        refresh();
    }

    private List<Integer> breakOptions(String value) {
        List<Integer> result = new ArrayList<>();
        result.add(0);
        if (value != null) {
            for (int i = 1; i < value.length() - 1; i++) {
                if (Character.isWhitespace(value.charAt(i))) result.add(i + 1);
            }
        }
        return result;
    }

    private void sendSave() {
        if (projector == null) return;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(projector.id());
        buf.writeVarInt(projector.location());
        buf.writeString(projector.world(), 128);
        buf.writeDouble(projector.x());
        buf.writeDouble(projector.y());
        buf.writeDouble(projector.z());
        buf.writeFloat(projector.yaw());
        buf.writeFloat(projector.scale());
        buf.writeByte(projector.light());
        buf.writeBoolean(projector.textBackground());
        buf.writeFloat(projector.contrast());
        buf.writeFloat(textScale);
        buf.writeInt(titleBreak);
        ClientPlayNetworking.send(HologramNetworking.SAVE, buf);
    }

    private String title() { return projector == null ? "" : PanelData.locationName(projector.location()); }
    private Text scaleText() { return Text.literal("Текст: " + String.format(Locale.ROOT, "%.1fx", textScale)); }
    private Text breakText() {
        if (titleBreak <= 0) return Text.literal("Перенос: нет");
        String value = title();
        String first = titleBreak < value.length() ? value.substring(0, titleBreak).trim() : value;
        return Text.literal("После: " + shorten(first, 12));
    }
    private void refresh() {
        if (scaleValue != null) scaleValue.setMessage(scaleText());
        if (breakValue != null) breakValue.setMessage(breakText());
    }
    private static String shorten(String value, int max) {
        return value.length() <= max ? value : value.substring(0, Math.max(1, max - 1)) + "…";
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int top = height / 2 - 80;
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, top - 28, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, projector == null ? "Голограмма не найдена" : PanelData.locationName(projector.location()),
                width / 2, top - 12, projector == null ? 0xFFFF7777 : 0xFFBBBBBB);
        context.drawCenteredTextWithShadow(textRenderer,
                "Размер меняет только подпись. Перенос выбирается по границам слов.", width / 2, top + 92, 0xFF999999);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override public boolean shouldPause() { return false; }
}
