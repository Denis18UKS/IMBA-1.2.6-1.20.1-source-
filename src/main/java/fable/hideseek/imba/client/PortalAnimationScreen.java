package fable.hideseek.imba.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.Locale;

public final class PortalAnimationScreen extends Screen {
    private int freezeTicks = PortalAnimationClientData.freezeTicks;
    private ButtonWidget value;

    public PortalAnimationScreen() {
        super(Text.literal("Зависание анимации player-portal"));
    }

    @Override
    protected void init() {
        PortalAnimationClientNetworking.request();
        int left = width / 2 - 120;
        int top = height / 2 - 35;
        addDrawableChild(ButtonWidget.builder(Text.literal("−0.1 c"), b -> change(-2))
                .dimensions(left, top + 28, 70, 20).build());
        value = addDrawableChild(ButtonWidget.builder(valueText(), b -> {})
                .dimensions(left + 75, top + 28, 90, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+0.1 c"), b -> change(2))
                .dimensions(left + 170, top + 28, 70, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Готово"), b -> close())
                .dimensions(width / 2 - 50, top + 68, 100, 20).build());
    }

    public void applyServerState() {
        freezeTicks = PortalAnimationClientData.freezeTicks;
        refreshLabel();
    }

    private void change(int delta) {
        freezeTicks = Math.max(0, Math.min(600, freezeTicks + delta));
        refreshLabel();
        PortalAnimationClientNetworking.sendUpdate(freezeTicks);
    }

    private void refreshLabel() {
        if (value != null) value.setMessage(valueText());
    }

    private Text valueText() {
        return Text.literal(String.format(Locale.ROOT, "%.1f сек.", freezeTicks / 20.0D));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int top = height / 2 - 35;
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, top - 20, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                "Пауза после каждого полного цикла (по умолчанию 0.5 сек.)",
                width / 2, top + 4, 0xFFE0B0);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
