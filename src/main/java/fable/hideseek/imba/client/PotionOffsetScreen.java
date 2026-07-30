package fable.hideseek.imba.client;

import fable.hideseek.imba.net.MaskNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.Locale;

/** Compact administrator GUI for the special potion attachment offset. */
public final class PotionOffsetScreen extends Screen {
    private static final double STEP = 0.01D;

    private double x = PanelData.potionOffsetX;
    private double y = PanelData.potionOffsetY;
    private double z = PanelData.potionOffsetZ;

    public PotionOffsetScreen() {
        super(Text.literal("Автопозиция 2D-зелья"));
    }

    @Override
    protected void init() {
        int left = width / 2 - 105;
        int top = height / 2 - 55;
        axisButtons(left, top, 0);
        axisButtons(left, top + 28, 1);
        axisButtons(left, top + 56, 2);

        addDrawableChild(ButtonWidget.builder(Text.literal("Сбросить"), button -> {
            x = 0.0D;
            y = 0.15D;
            z = 0.0D;
        }).dimensions(left, top + 91, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Сохранить"), button -> save())
                .dimensions(left + 110, top + 91, 100, 20).build());
    }

    private void axisButtons(int left, int top, int axis) {
        addDrawableChild(ButtonWidget.builder(Text.literal("−"), button -> change(axis, -STEP))
                .dimensions(left + 40, top, 35, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), button -> change(axis, STEP))
                .dimensions(left + 175, top, 35, 20).build());
    }

    private void change(int axis, double delta) {
        if (axis == 0) x = clamp(x + delta);
        else if (axis == 1) y = clamp(y + delta);
        else z = clamp(z + delta);
    }

    private static double clamp(double value) {
        return Math.round(Math.max(-2.0D, Math.min(2.0D, value)) * 100.0D) / 100.0D;
    }

    private void save() {
        var buf = PacketByteBufs.create();
        buf.writeString("potion_2d_offset");
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        ClientPlayNetworking.send(MaskNetworking.ATTACHMENT_SAVE_PACKET, buf);
        close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int left = width / 2 - 105;
        int top = height / 2 - 55;
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, top - 28, 0xFFFFFF);
        drawAxis(context, "X", x, left, top);
        drawAxis(context, "Y", y, left, top + 28);
        drawAxis(context, "Z", z, left, top + 56);
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawAxis(DrawContext context, String axis, double value, int left, int top) {
        context.drawTextWithShadow(textRenderer, axis, left + 8, top + 6, 0xFFE0B0);
        context.drawCenteredTextWithShadow(textRenderer,
                String.format(Locale.ROOT, "%+.2f", value), left + 125, top + 6, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
