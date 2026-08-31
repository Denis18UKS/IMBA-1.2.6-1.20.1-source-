package fable.hideseek.imba.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.Locale;

public final class ReturnTimingScreen extends Screen {
    private int preFadeTicks = ReturnTimingClientData.preFadeTicks;
    private int preTeleportTicks = ReturnTimingClientData.preTeleportTicks;
    private ButtonWidget preFadeValue;
    private ButtonWidget preTeleportValue;

    public ReturnTimingScreen() {
        super(Text.literal("Задержки возврата в лобби"));
    }

    @Override
    protected void init() {
        ReturnTimingClientNetworking.request();
        int left = width / 2 - 120;
        int top = height / 2 - 58;

        addDrawableChild(ButtonWidget.builder(Text.literal("−1 c"), b -> changePreFade(-20))
                .dimensions(left, top + 22, 60, 20).build());
        preFadeValue = addDrawableChild(ButtonWidget.builder(valueText(preFadeTicks), b -> {})
                .dimensions(left + 65, top + 22, 110, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+1 c"), b -> changePreFade(20))
                .dimensions(left + 180, top + 22, 60, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("−1 c"), b -> changePreTeleport(-20))
                .dimensions(left, top + 70, 60, 20).build());
        preTeleportValue = addDrawableChild(ButtonWidget.builder(valueText(preTeleportTicks), b -> {})
                .dimensions(left + 65, top + 70, 110, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+1 c"), b -> changePreTeleport(20))
                .dimensions(left + 180, top + 70, 60, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Готово"), b -> close())
                .dimensions(width / 2 - 50, top + 112, 100, 20).build());
        refreshLabels();
    }

    public void applyServerState() {
        preFadeTicks = ReturnTimingClientData.preFadeTicks;
        preTeleportTicks = ReturnTimingClientData.preTeleportTicks;
        refreshLabels();
    }

    private void changePreFade(int delta) {
        preFadeTicks = clamp(preFadeTicks + delta);
        send();
    }

    private void changePreTeleport(int delta) {
        preTeleportTicks = clamp(preTeleportTicks + delta);
        send();
    }

    private void send() {
        refreshLabels();
        ReturnTimingClientNetworking.sendUpdate(preFadeTicks, preTeleportTicks);
    }

    private void refreshLabels() {
        if (preFadeValue != null) preFadeValue.setMessage(valueText(preFadeTicks));
        if (preTeleportValue != null) preTeleportValue.setMessage(valueText(preTeleportTicks));
    }

    private static int clamp(int ticks) {
        return Math.max(0, Math.min(1200, ticks));
    }

    private static Text valueText(int ticks) {
        return Text.literal(String.format(Locale.ROOT, "%.1f сек.", ticks / 20.0D));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int top = height / 2 - 58;
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, top - 20, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, "До начала затемнения", width / 2 - 120, top + 6, 0xFFE0B0);
        context.drawTextWithShadow(textRenderer, "После полного затемнения до телепорта", width / 2 - 120, top + 54, 0xFFE0B0);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
