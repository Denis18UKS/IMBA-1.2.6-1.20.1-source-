package fable.hideseek.imba.client;

import fable.hideseek.imba.config.PanelHitboxConfig;
import fable.hideseek.imba.net.PanelHitboxNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.EnumMap;
import java.util.Map;

/** Visual editor for the four precise click rectangles on the physical settings panel. */
public final class PanelHitboxScreen extends Screen {
    private static final double PANEL_MIN = -72.0D;
    private static final double PANEL_MAX = 72.0D;

    private final Screen parent;
    private final EnumMap<PanelHitboxConfig.Arrow, PanelHitboxConfig.Rect> rects =
            new EnumMap<>(PanelHitboxConfig.Arrow.class);
    private PanelHitboxConfig.Arrow selected = PanelHitboxConfig.Arrow.TIMER_UP;
    private boolean synced;

    public PanelHitboxScreen(Screen parent) {
        super(Text.literal("Хитбоксы стрелок панели"));
        this.parent = parent;
        applyServerState(PanelHitboxClientState.snapshot());
    }

    @Override
    protected void init() {
        int center = width / 2;
        int top = 34;
        int buttonWidth = 104;
        int gap = 6;
        int total = buttonWidth * 4 + gap * 3;
        int start = center - total / 2;

        PanelHitboxConfig.Arrow[] arrows = PanelHitboxConfig.Arrow.values();
        for (int i = 0; i < arrows.length; i++) {
            PanelHitboxConfig.Arrow arrow = arrows[i];
            addDrawableChild(ButtonWidget.builder(Text.literal(arrow.label), b -> selected = arrow)
                    .dimensions(start + i * (buttonWidth + gap), top, buttonWidth, 20).build());
        }

        int controlsX = center + 105;
        int controlsY = 86;
        addPair(controlsX, controlsY, "X -", () -> adjust(-1, 0, 0, 0), "X +", () -> adjust(1, 0, 0, 0));
        addPair(controlsX, controlsY + 28, "Y -", () -> adjust(0, -1, 0, 0), "Y +", () -> adjust(0, 1, 0, 0));
        addPair(controlsX, controlsY + 56, "Ширина -", () -> adjust(0, 0, -2, 0),
                "Ширина +", () -> adjust(0, 0, 2, 0));
        addPair(controlsX, controlsY + 84, "Высота -", () -> adjust(0, 0, 0, -2),
                "Высота +", () -> adjust(0, 0, 0, 2));

        addDrawableChild(ButtonWidget.builder(Text.literal("Сохранить"), b -> save())
                .dimensions(controlsX, controlsY + 132, 104, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Сбросить"), b ->
                        ClientPlayNetworking.send(PanelHitboxNetworking.RESET, PacketByteBufs.empty()))
                .dimensions(controlsX + 110, controlsY + 132, 104, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), b -> close())
                .dimensions(controlsX, controlsY + 160, 214, 20).build());

        ClientPlayNetworking.send(PanelHitboxNetworking.REQUEST, PacketByteBufs.empty());
    }

    private void addPair(int x, int y, String leftText, Runnable leftAction,
                         String rightText, Runnable rightAction) {
        addDrawableChild(ButtonWidget.builder(Text.literal(leftText), b -> leftAction.run())
                .dimensions(x, y, 104, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(rightText), b -> rightAction.run())
                .dimensions(x + 110, y, 104, 20).build());
    }

    public void applyServerState(Map<PanelHitboxConfig.Arrow, PanelHitboxConfig.Rect> values) {
        if (values == null) return;
        rects.clear();
        for (PanelHitboxConfig.Arrow arrow : PanelHitboxConfig.Arrow.values()) {
            PanelHitboxConfig.Rect rect = values.get(arrow);
            if (rect != null) rects.put(arrow, rect.copy());
        }
        synced = true;
    }

    private void adjust(int dx, int dy, int dw, int dh) {
        PanelHitboxConfig.Rect old = current();
        old.x = MathHelper.clamp(old.x + dx, -70, 70);
        old.y = MathHelper.clamp(old.y + dy, -70, 70);
        old.width = MathHelper.clamp(old.width + dw, 6, 60);
        old.height = MathHelper.clamp(old.height + dh, 6, 60);
        rects.put(selected, old);
    }

    private PanelHitboxConfig.Rect current() {
        PanelHitboxConfig.Rect rect = rects.get(selected);
        if (rect == null) {
            rect = new PanelHitboxConfig.Rect(selected.defaultX, selected.defaultY, 26, 22);
            rects.put(selected, rect);
        }
        return rect.copy();
    }

    private void save() {
        PacketByteBuf out = PacketByteBufs.create();
        for (PanelHitboxConfig.Arrow arrow : PanelHitboxConfig.Arrow.values()) {
            PanelHitboxConfig.Rect rect = rects.get(arrow);
            if (rect == null) rect = new PanelHitboxConfig.Rect(arrow.defaultX, arrow.defaultY, 26, 22);
            out.writeInt(rect.x);
            out.writeInt(rect.y);
            out.writeInt(rect.width);
            out.writeInt(rect.height);
        }
        ClientPlayNetworking.send(PanelHitboxNetworking.SET, out);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                synced ? "Кликните или перетащите рамку на превью. Размер рамки = реальная зона клика."
                        : "Получение хитбоксов с сервера...",
                width / 2, 23, synced ? 0xFFBBBBBB : 0xFFFFFF55);

        Preview preview = preview();
        context.fill(preview.left, preview.top, preview.right(), preview.bottom(), 0xEE181818);
        border(context, preview.left, preview.top, preview.size, preview.size, 0xFF888888);

        for (int i = 1; i < 3; i++) {
            int gx = preview.left + preview.size * i / 3;
            int gy = preview.top + preview.size * i / 3;
            context.fill(gx, preview.top, gx + 1, preview.bottom(), 0xFF555555);
            context.fill(preview.left, gy, preview.right(), gy + 1, 0xFF555555);
        }

        drawArrow(context, preview, "▲", PanelData.timerX, PanelData.upArrowY);
        drawArrow(context, preview, "▼", PanelData.timerX, PanelData.downArrowY);
        drawArrow(context, preview, "▲", PanelData.heartsX, PanelData.upArrowY);
        drawArrow(context, preview, "▼", PanelData.heartsX, PanelData.downArrowY);

        for (PanelHitboxConfig.Arrow arrow : PanelHitboxConfig.Arrow.values()) {
            PanelHitboxConfig.Rect rect = rects.get(arrow);
            if (rect == null) continue;
            int x1 = panelToScreenX(preview, rect.x - rect.width / 2.0D);
            int x2 = panelToScreenX(preview, rect.x + rect.width / 2.0D);
            int y1 = panelToScreenY(preview, rect.y - rect.height / 2.0D);
            int y2 = panelToScreenY(preview, rect.y + rect.height / 2.0D);
            int color = arrow == selected ? 0xFFFFFF55 : 0xFFFF5555;
            border(context, Math.min(x1, x2), Math.min(y1, y2), Math.max(2, Math.abs(x2 - x1)),
                    Math.max(2, Math.abs(y2 - y1)), color);
        }

        PanelHitboxConfig.Rect current = current();
        int infoX = width / 2 + 105;
        int infoY = 64;
        context.drawTextWithShadow(textRenderer, "§6Выбрано: §f" + selected.label, infoX, infoY, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer,
                "X=" + current.x + "  Y=" + current.y + "  W=" + current.width + "  H=" + current.height,
                infoX, infoY + 12, 0xFFCCCCCC);
        context.drawTextWithShadow(textRenderer,
                "Координаты совпадают с системой рендера панели.", preview.left, preview.bottom() + 8, 0xFF999999);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawArrow(DrawContext context, Preview preview, String text, double panelX, double panelY) {
        int x = panelToScreenX(preview, panelX);
        int y = panelToScreenY(preview, panelY);
        context.drawCenteredTextWithShadow(textRenderer, text, x, y - textRenderer.fontHeight / 2, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        return button == 0 && moveSelectedTo(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && moveSelectedTo(mouseX, mouseY)) return true;
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    private boolean moveSelectedTo(double mouseX, double mouseY) {
        Preview preview = preview();
        if (mouseX < preview.left || mouseX > preview.right() || mouseY < preview.top || mouseY > preview.bottom()) {
            return false;
        }
        double nx = (mouseX - preview.left) / preview.size;
        double ny = (mouseY - preview.top) / preview.size;
        int panelX = (int) Math.round(PANEL_MIN + nx * (PANEL_MAX - PANEL_MIN));
        int panelY = (int) Math.round(PANEL_MIN + ny * (PANEL_MAX - PANEL_MIN));
        PanelHitboxConfig.Rect rect = current();
        rect.x = MathHelper.clamp(panelX, -70, 70);
        rect.y = MathHelper.clamp(panelY, -70, 70);
        rects.put(selected, rect);
        return true;
    }

    private Preview preview() {
        int size = Math.min(270, Math.max(170, height - 115));
        return new Preview(width / 2 - 230, 66, size);
    }

    private static int panelToScreenX(Preview preview, double panelX) {
        double t = (panelX - PANEL_MIN) / (PANEL_MAX - PANEL_MIN);
        return preview.left + (int) Math.round(t * preview.size);
    }

    private static int panelToScreenY(Preview preview, double panelY) {
        double t = (panelY - PANEL_MIN) / (PANEL_MAX - PANEL_MIN);
        return preview.top + (int) Math.round(t * preview.size);
    }

    private static void border(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private record Preview(int left, int top, int size) {
        int right() { return left + size; }
        int bottom() { return top + size; }
    }
}
