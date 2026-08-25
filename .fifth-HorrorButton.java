package ru.fifth.horror.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class HorrorButton extends PressableWidget {
    private final Consumer<HorrorButton> pressAction;

    public HorrorButton(int x, int y, int width, int height, Text message, Consumer<HorrorButton> pressAction) {
        super(x, y, width, height, message);
        this.pressAction = pressAction;
    }

    public static Builder builder(Text message, Consumer<HorrorButton> action) {
        return new Builder(message, action);
    }

    @Override
    public void onPress() {
        if (active && pressAction != null) pressAction.accept(this);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        int x1 = getX(), y1 = getY(), x2 = x1 + width, y2 = y1 + height;
        boolean hot = isHovered() && active;
        int bg = active ? (hot ? 0xE02C2424 : 0xD5161719) : 0xA0101011;
        int edge = active ? (hot ? 0xFF9A5256 : 0xFF4D3032) : 0xFF292526;
        int edge2 = hot ? 0xFFCA8588 : 0xFF765052;

        context.fill(x1, y1, x2, y2, bg);
        context.fill(x1, y1, x2, y1 + 1, edge2);
        context.fill(x1, y2 - 1, x2, y2, edge);
        context.fill(x1, y1, x1 + 1, y2, edge);
        context.fill(x2 - 1, y1, x2, y2, edge);
        if (hot) context.fill(x1 + 3, y1 + 3, x1 + 5, y2 - 3, 0xFF8E2D33);

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int color = active ? (hot ? 0xFFF3E9E1 : 0xFFD2C9C1) : 0xFF6D6662;
        context.drawCenteredTextWithShadow(tr, getMessage(), x1 + width / 2, y1 + (height - 8) / 2, color);
    }

    public static final class Builder {
        private final Text message;
        private final Consumer<HorrorButton> action;
        private int x, y, width = 150, height = 20;

        private Builder(Text message, Consumer<HorrorButton> action) {
            this.message = message;
            this.action = action;
        }

        public Builder dimensions(int x, int y, int width, int height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
            return this;
        }

        public HorrorButton build() {
            return new HorrorButton(x, y, width, height, message, action);
        }
    }
}
