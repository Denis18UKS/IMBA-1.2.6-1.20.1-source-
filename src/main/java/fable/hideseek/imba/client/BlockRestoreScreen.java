package fable.hideseek.imba.client;

import fable.hideseek.imba.net.RoundRestoreNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BlockRestoreScreen extends Screen {
    private static final int ROWS = 7;
    private List<RoundRestoreClientNetworking.Single> singles = new ArrayList<>();
    private TextFieldWidget search;
    private int selectedIndex = -1;
    private int page;
    private ButtonWidget restoreButton;
    private ButtonWidget recaptureButton;
    private ButtonWidget deleteButton;
    private ButtonWidget prevPageButton;
    private ButtonWidget nextPageButton;

    public BlockRestoreScreen() {
        super(Text.literal("Восстановление отдельных блоков"));
    }

    @Override
    protected void init() {
        int left = width / 2 - 240;
        int top = Math.max(18, height / 2 - 125);

        search = new TextFieldWidget(textRenderer, left, top + 28, 220, 20, Text.literal("Поиск"));
        search.setMaxLength(64);
        search.setPlaceholder(Text.literal("Поиск: блок, координаты, мир..."));
        addDrawableChild(search);

        prevPageButton = addDrawableChild(ButtonWidget.builder(Text.literal("◀"), b -> changePage(-1))
                .dimensions(left, top + 205, 34, 20).build());
        nextPageButton = addDrawableChild(ButtonWidget.builder(Text.literal("▶"), b -> changePage(1))
                .dimensions(left + 186, top + 205, 34, 20).build());

        int right = left + 240;
        restoreButton = addDrawableChild(ButtonWidget.builder(Text.literal("Восстановить сейчас"), b -> sendIndex(RoundRestoreNetworking.RESTORE_SINGLE))
                .dimensions(right, top + 124, 220, 20).build());
        recaptureButton = addDrawableChild(ButtonWidget.builder(Text.literal("Перезаписать текущим состоянием"), b -> sendIndex(RoundRestoreNetworking.RECAPTURE_SINGLE))
                .dimensions(right, top + 150, 220, 20).build());
        deleteButton = addDrawableChild(ButtonWidget.builder(Text.literal("Удалить точку"), b -> sendIndex(RoundRestoreNetworking.DELETE_SINGLE))
                .dimensions(right, top + 176, 220, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Обновить список"), b -> request())
                .dimensions(right, top + 205, 106, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), b -> close())
                .dimensions(right + 114, top + 205, 106, 20).build());

        singles = new ArrayList<>(RoundRestoreClientNetworking.singles());
        if (!singles.isEmpty()) selectedIndex = singles.get(0).index();
        updateButtons();
        request();
    }

    public void applySingles(List<RoundRestoreClientNetworking.Single> values) {
        singles = new ArrayList<>(values);
        if (selected() == null) selectedIndex = singles.isEmpty() ? -1 : singles.get(0).index();
        clampPage();
        updateButtons();
    }

    private void request() {
        ClientPlayNetworking.send(RoundRestoreNetworking.REQUEST, PacketByteBufs.empty());
    }

    private void sendIndex(net.minecraft.util.Identifier packet) {
        if (selectedIndex < 0) return;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(selectedIndex);
        ClientPlayNetworking.send(packet, buf);
    }

    private RoundRestoreClientNetworking.Single selected() {
        for (var single : singles) if (single.index() == selectedIndex) return single;
        return null;
    }

    private List<RoundRestoreClientNetworking.Single> filtered() {
        String q = search == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return List.copyOf(singles);
        List<RoundRestoreClientNetworking.Single> result = new ArrayList<>();
        for (var single : singles) {
            String haystack = (single.block() + " " + single.world() + " "
                    + single.x() + " " + single.y() + " " + single.z()).toLowerCase(Locale.ROOT);
            if (haystack.contains(q)) result.add(single);
        }
        return result;
    }

    private void changePage(int delta) {
        page += delta;
        clampPage();
    }

    private void clampPage() {
        int maxPage = Math.max(0, (filtered().size() - 1) / ROWS);
        page = Math.max(0, Math.min(page, maxPage));
        if (prevPageButton != null) prevPageButton.active = page > 0;
        if (nextPageButton != null) nextPageButton.active = page < maxPage;
    }

    private void updateButtons() {
        boolean has = selected() != null;
        if (restoreButton != null) restoreButton.active = has;
        if (recaptureButton != null) recaptureButton.active = has;
        if (deleteButton != null) deleteButton.active = has;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = width / 2 - 240;
        int top = Math.max(18, height / 2 - 125);
        if (button == 0 && mouseX >= left && mouseX < left + 220 && mouseY >= top + 58 && mouseY < top + 58 + ROWS * 20) {
            int row = (int) ((mouseY - (top + 58)) / 20);
            List<RoundRestoreClientNetworking.Single> filtered = filtered();
            int at = page * ROWS + row;
            if (at >= 0 && at < filtered.size()) {
                selectedIndex = filtered.get(at).index();
                updateButtons();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int left = width / 2 - 240;
        int top = Math.max(18, height / 2 - 125);
        int right = left + 240;

        context.drawTextWithShadow(textRenderer, title, left, top, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer,
                "ПКМ блоком-настройщиком сохраняет точку • Shift+ПКМ удаляет её по позиции",
                left, top + 14, 0xFFAAAAAA);

        List<RoundRestoreClientNetworking.Single> filtered = filtered();
        clampPage();
        for (int row = 0; row < ROWS; row++) {
            int at = page * ROWS + row;
            if (at >= filtered.size()) break;
            var single = filtered.get(at);
            int y = top + 58 + row * 20;
            boolean selected = single.index() == selectedIndex;
            context.fill(left, y, left + 220, y + 18, selected ? 0xAA3A5A78 : 0x77101010);
            String name = single.block();
            if (name.length() > 29) name = "…" + name.substring(name.length() - 28);
            context.drawTextWithShadow(textRenderer, name, left + 5, y + 3, selected ? 0xFFFFFFFF : 0xFFCCCCCC);
            context.drawTextWithShadow(textRenderer, single.x() + " " + single.y() + " " + single.z(), left + 135, y + 3, 0xFF999999);
        }
        context.drawCenteredTextWithShadow(textRenderer,
                "Страница " + (page + 1) + " / " + Math.max(1, (filtered.size() + ROWS - 1) / ROWS),
                left + 110, top + 211, 0xFF888888);

        var selected = selected();
        context.drawTextWithShadow(textRenderer, "Выбранная точка", right, top + 30, 0xFFFFAA00);
        if (selected == null) {
            context.drawTextWithShadow(textRenderer, "Ничего не выбрано", right, top + 50, 0xFF999999);
        } else {
            context.drawTextWithShadow(textRenderer, "Блок: " + selected.block(), right, top + 50, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer, "Мир: " + selected.world(), right, top + 66, 0xFFBBBBBB);
            context.drawTextWithShadow(textRenderer,
                    "Координаты: " + selected.x() + ", " + selected.y() + ", " + selected.z(), right, top + 82, 0xFFBBBBBB);
            context.drawTextWithShadow(textRenderer,
                    selected.inventoryItems() > 0
                            ? "Сохранено предметов в контейнере: " + selected.inventoryItems()
                            : "Контейнер: нет сохранённых предметов",
                    right, top + 98, 0xFFBBBBBB);
        }
        context.drawTextWithShadow(textRenderer,
                "«Восстановить сейчас» работает без запуска игры.", right, top + 232, 0xFF888888);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
