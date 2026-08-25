package ru.fifth.horror.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import ru.fifth.horror.client.AnimationCatalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class AnimationListScreen extends HorrorScreen {
    private final Screen parent;
    private final Consumer<AnimationCatalog.Entry> select;
    private int page;
    private TextFieldWidget search;
    private List<AnimationCatalog.Entry> filtered = List.of();

    public AnimationListScreen(Screen parent, Consumer<AnimationCatalog.Entry> select) {
        super(Text.literal("ПЯТЫЙ / АНИМАЦИИ GECKOLIB"));
        this.parent = parent;
        this.select = select;
    }

    @Override protected void init() {
        int x = width / 2 - 210;
        search = new TextFieldWidget(textRenderer, x, 42, 420, 22, Text.literal("Поиск"));
        search.setMaxLength(128);
        search.setPlaceholder(Text.literal("Поиск по названию, источнику или русскому описанию..."));
        search.setChangedListener(v -> { page = 0; rebuildEntries(); });
        addDrawableChild(search);
        rebuildEntries();
    }

    private void rebuildEntries() {
        String q = search == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        List<AnimationCatalog.Entry> out = new ArrayList<>();
        for (var e : AnimationCatalog.INSTANCE.entries()) {
            String hay = (e.name() + " " + e.file() + " " + e.description()).toLowerCase(Locale.ROOT);
            if (q.isBlank() || hay.contains(q)) out.add(e);
        }
        filtered = List.copyOf(out);
        rebuildButtons();
    }

    private void rebuildButtons() {
        // Keep search field, remove only entry/nav widgets.
        clearChildren();
        addDrawableChild(search);
        int per = Math.max(4, Math.min(8, (height - 140) / 42));
        int maxPage = Math.max(0, (filtered.size() - 1) / per);
        page = Math.min(page, maxPage);
        int start = page * per, x = width / 2 - 210, y = 74;
        for (int i = start; i < Math.min(start + per, filtered.size()); i++) {
            var e = filtered.get(i);
            int yy = y + (i - start) * 42;
            addDrawableChild(new AnimationEntryButton(x, yy, 420, 36, e, () -> {
                if (select != null) select.accept(e);
            }));
        }
        addDrawableChild(HorrorButton.builder(Text.literal("<"), b -> { if (page > 0) { page--; rebuildButtons(); } }).dimensions(x, height - 40, 44, 22).build());
        addDrawableChild(HorrorButton.builder(Text.literal("Назад"), b -> client.setScreen(parent)).dimensions(x + 54, height - 40, 312, 22).build());
        addDrawableChild(HorrorButton.builder(Text.literal(">"), b -> { if ((page + 1) * per < filtered.size()) { page++; rebuildButtons(); } }).dimensions(x + 376, height - 40, 44, 22).build());
    }

    @Override public void render(DrawContext c, int mx, int my, float d) {
        horrorBackground(c);
        int x = width / 2 - 210;
        c.fill(x - 1, 41, x + 421, 65, 0xFF4E3536);
        c.fill(x, 42, x + 420, 64, 0xEE0B0C0E);
        c.drawTextWithShadow(textRenderer, "Найдено: " + filtered.size() + " / " + AnimationCatalog.INSTANCE.entries().size(), x, 67, 0xFF9C918C);
        c.drawCenteredTextWithShadow(textRenderer, "Новые animation.json появляются здесь автоматически после загрузки ресурсов / F3+T.", width / 2, height - 57, 0xFF958A86);
        super.render(c, mx, my, d);
    }

    private static final class AnimationEntryButton extends PressableWidget {
        private final AnimationCatalog.Entry entry;
        private final Runnable action;
        private AnimationEntryButton(int x, int y, int w, int h, AnimationCatalog.Entry entry, Runnable action) {
            super(x, y, w, h, Text.literal(entry.name()));
            this.entry = entry;
            this.action = action;
        }
        @Override public void onPress() { if (action != null) action.run(); }
        @Override protected void appendClickableNarrations(NarrationMessageBuilder builder) { appendDefaultNarrations(builder); }
        @Override protected void renderButton(DrawContext c, int mx, int my, float d) {
            int x = getX(), y = getY();
            int bg = isHovered() ? 0xE02A2223 : 0xD5141517;
            int edge = isHovered() ? 0xFF9A5559 : 0xFF4C3032;
            c.fill(x, y, x + width, y + height, bg);
            c.fill(x, y, x + width, y + 1, edge);
            c.fill(x, y + height - 1, x + width, y + height, 0xFF302526);
            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
            c.drawTextWithShadow(tr, entry.name(), x + 8, y + 6, 0xFFE3DAD3);
            String file = entry.file().getNamespace() + ":" + entry.file().getPath();
            String desc = entry.description() + "   [" + file + "]";
            if (tr.getWidth(desc) > width - 16) desc = tr.trimToWidth(desc, width - 28) + "…";
            c.drawTextWithShadow(tr, desc, x + 8, y + 20, 0xFF9F918C);
        }
    }
}
