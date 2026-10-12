package fable.hideseek.imba.client;

import fable.hideseek.imba.net.HologramNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Searchable selector used only by the dedicated hologram text tool. */
public final class HologramTextLibraryScreen extends Screen {
    private static final int VISIBLE_ROWS = 8;
    private final List<HologramClientData.Projector> all = new ArrayList<>();
    private final List<HologramClientData.Projector> filtered = new ArrayList<>();
    private final List<ButtonWidget> rows = new ArrayList<>();
    private TextFieldWidget searchField;
    private ButtonWidget openButton, upButton, downButton;
    private int selectedId = -1;
    private int scroll;

    public HologramTextLibraryScreen() {
        super(Text.literal("Текст фото-голограмм"));
    }

    @Override
    protected void init() {
        int left = width / 2 - 190;
        int top = Math.max(18, height / 2 - 120);
        searchField = new TextFieldWidget(textRenderer, left, top, 380, 20, Text.literal("Поиск"));
        searchField.setPlaceholder(Text.literal("Поиск по названию локации или номеру голограммы"));
        searchField.setMaxLength(64);
        searchField.setChangedListener(value -> { scroll = 0; filter(); });
        addDrawableChild(searchField);

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            final int visibleRow = row;
            rows.add(addDrawableChild(ButtonWidget.builder(Text.empty(), b -> selectVisible(visibleRow))
                    .dimensions(left, top + 28 + row * 22, 350, 20).build()));
        }
        upButton = addDrawableChild(ButtonWidget.builder(Text.literal("▲"), b -> page(-1))
                .dimensions(left + 356, top + 28, 24, 84).build());
        downButton = addDrawableChild(ButtonWidget.builder(Text.literal("▼"), b -> page(1))
                .dimensions(left + 356, top + 120, 24, 84).build());
        openButton = addDrawableChild(ButtonWidget.builder(Text.literal("Настроить текст"), b -> openSelected())
                .dimensions(left, top + 214, 380, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), b -> close())
                .dimensions(left, top + 240, 380, 20).build());

        applyServerState(HologramClientData.snapshot());
        ClientPlayNetworking.send(HologramNetworking.REQUEST, PacketByteBufs.empty());
    }

    public void applyServerState(List<HologramClientData.Projector> list) {
        all.clear();
        all.addAll(list);
        if (selectedId >= 0 && all.stream().noneMatch(p -> p.id() == selectedId)) selectedId = -1;
        filter();
    }

    private void filter() {
        filtered.clear();
        String query = normalize(searchField == null ? "" : searchField.getText());
        for (var p : all) {
            String name = PanelData.locationName(p.location());
            String searchable = normalize(name + " " + p.id() + " " + p.world());
            if (query.isEmpty() || searchable.contains(query)) filtered.add(p);
        }
        clampScroll();
        refreshRows();
    }

    private void refreshRows() {
        for (int row = 0; row < rows.size(); row++) {
            int index = scroll + row;
            ButtonWidget button = rows.get(row);
            boolean visible = index < filtered.size();
            button.visible = visible;
            button.active = visible;
            if (!visible) continue;
            var p = filtered.get(index);
            String marker = p.id() == selectedId ? "▶ " : "";
            button.setMessage(Text.literal(marker + "#" + p.id() + " • " + shorten(PanelData.locationName(p.location()), 31)
                    + " • текст " + String.format(Locale.ROOT, "%.1fx", p.textScale())));
        }
        if (openButton != null) openButton.active = selectedId >= 0;
        if (upButton != null) upButton.active = scroll > 0;
        if (downButton != null) downButton.active = scroll + VISIBLE_ROWS < filtered.size();
    }

    private void selectVisible(int row) {
        int index = scroll + row;
        if (index < 0 || index >= filtered.size()) return;
        selectedId = filtered.get(index).id();
        refreshRows();
    }

    private void openSelected() {
        if (selectedId >= 0) client.setScreen(new HologramTextSettingsScreen(selectedId));
    }

    private void page(int direction) { scroll += direction * VISIBLE_ROWS; clampScroll(); refreshRows(); }
    private void clampScroll() { scroll = Math.max(0, Math.min(scroll, Math.max(0, filtered.size() - VISIBLE_ROWS))); }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        scroll -= (int) Math.signum(amount);
        clampScroll();
        refreshRows();
        return true;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace('ё', 'е').strip();
    }
    private static String shorten(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, Math.max(1, max - 1)) + "…";
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int top = Math.max(18, height / 2 - 120);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, top - 14, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, "Найдено: " + filtered.size() + " / " + all.size(),
                width / 2, top + 200, 0xFFAAAAAA);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override public boolean shouldPause() { return false; }
}
