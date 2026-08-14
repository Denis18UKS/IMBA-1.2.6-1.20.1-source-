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

public final class StructureLayerScreen extends Screen {
    private static final int ROWS = 7;
    private List<RoundRestoreClientNetworking.Layer> layers = new ArrayList<>();
    private TextFieldWidget search;
    private TextFieldWidget name;
    private int selectedIndex = -1;
    private int page;
    private ButtonWidget toggleButton;
    private ButtonWidget restoreButton;
    private ButtonWidget saveButton;
    private ButtonWidget renameButton;
    private ButtonWidget deleteButton;
    private ButtonWidget prevPageButton;
    private ButtonWidget nextPageButton;

    public StructureLayerScreen() { super(Text.literal("Слои восстановления локации")); }

    @Override protected void init() {
        int left = width / 2 - 250;
        int top = Math.max(16, height / 2 - 135);
        int right = left + 250;

        search = new TextFieldWidget(textRenderer, left, top + 28, 230, 20, Text.literal("Поиск слоя"));
        search.setMaxLength(64);
        search.setPlaceholder(Text.literal("Поиск слоя..."));
        addDrawableChild(search);

        prevPageButton = addDrawableChild(ButtonWidget.builder(Text.literal("◀"), b -> changePage(-1))
                .dimensions(left, top + 205, 34, 20).build());
        nextPageButton = addDrawableChild(ButtonWidget.builder(Text.literal("▶"), b -> changePage(1))
                .dimensions(left + 196, top + 205, 34, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Новый слой"), b -> select(-1))
                .dimensions(left, top + 233, 110, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Обновить"), b -> request())
                .dimensions(left + 120, top + 233, 110, 20).build());

        name = new TextFieldWidget(textRenderer, right, top + 52, 230, 20, Text.literal("Название слоя"));
        name.setMaxLength(64);
        addDrawableChild(name);

        toggleButton = addDrawableChild(ButtonWidget.builder(Text.literal("Автовосстановление"), b -> toggle())
                .dimensions(right, top + 78, 230, 20).build());
        restoreButton = addDrawableChild(ButtonWidget.builder(Text.literal("Восстановить выбранный слой сейчас"), b -> sendIndex(RoundRestoreNetworking.RESTORE_LAYER))
                .dimensions(right, top + 104, 230, 20).build());
        saveButton = addDrawableChild(ButtonWidget.builder(Text.literal("Пересохранить из выбранной A/B"), b -> saveLayer())
                .dimensions(right, top + 130, 230, 20).build());
        renameButton = addDrawableChild(ButtonWidget.builder(Text.literal("Переименовать"), b -> rename())
                .dimensions(right, top + 156, 112, 20).build());
        deleteButton = addDrawableChild(ButtonWidget.builder(Text.literal("Удалить"), b -> sendIndex(RoundRestoreNetworking.DELETE_LAYER))
                .dimensions(right + 118, top + 156, 112, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), b -> close())
                .dimensions(right, top + 233, 230, 20).build());

        layers = new ArrayList<>(RoundRestoreClientNetworking.layers());
        if (!layers.isEmpty()) selectedIndex = layers.get(0).index();
        select(selectedIndex);
        request();
    }

    public void applyLayers(List<RoundRestoreClientNetworking.Layer> values) {
        layers = new ArrayList<>(values);
        if (selected() == null && selectedIndex >= 0) selectedIndex = layers.isEmpty() ? -1 : layers.get(0).index();
        clampPage();
        select(selectedIndex);
    }

    private void request() { ClientPlayNetworking.send(RoundRestoreNetworking.REQUEST, PacketByteBufs.empty()); }
    private RoundRestoreClientNetworking.Layer selected() {
        for (var layer : layers) if (layer.index() == selectedIndex) return layer;
        return null;
    }

    private List<RoundRestoreClientNetworking.Layer> filtered() {
        String q = search == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return List.copyOf(layers);
        List<RoundRestoreClientNetworking.Layer> result = new ArrayList<>();
        for (var layer : layers) {
            String haystack = (layer.name() + " " + layer.world() + " " + layer.index()).toLowerCase(Locale.ROOT);
            if (haystack.contains(q)) result.add(layer);
        }
        return result;
    }

    private void select(int index) {
        selectedIndex = index;
        var layer = selected();
        if (name != null) name.setText(layer == null ? "Слой " + (layers.size() + 1) : layer.name());
        updateButtons();
    }

    private void updateButtons() {
        var layer = selected();
        boolean has = layer != null;
        if (toggleButton != null) {
            toggleButton.active = has;
            toggleButton.setMessage(Text.literal(has
                    ? (layer.enabled() ? "Автовосстановление: ВКЛ" : "Автовосстановление: ВЫКЛ")
                    : "Автовосстановление: для нового слоя ВКЛ"));
        }
        if (restoreButton != null) restoreButton.active = has;
        if (renameButton != null) renameButton.active = has;
        if (deleteButton != null) deleteButton.active = has;
        if (saveButton != null) saveButton.setMessage(Text.literal(has
                ? "Пересохранить из выбранной A/B"
                : "Сохранить выбранную A/B как новый слой"));
    }

    private void toggle() {
        var layer = selected();
        if (layer == null) return;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(layer.index());
        buf.writeBoolean(!layer.enabled());
        ClientPlayNetworking.send(RoundRestoreNetworking.TOGGLE_LAYER, buf);
    }

    private void saveLayer() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(selected() == null ? -1 : selectedIndex);
        buf.writeString(cleanName(), 64);
        ClientPlayNetworking.send(RoundRestoreNetworking.SAVE_LAYER, buf);
    }

    private void rename() {
        if (selected() == null) return;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(selectedIndex);
        buf.writeString(cleanName(), 64);
        ClientPlayNetworking.send(RoundRestoreNetworking.RENAME_LAYER, buf);
    }

    private String cleanName() {
        String value = name == null ? "" : name.getText().trim();
        return value.isEmpty() ? "Слой " + (layers.size() + 1) : value;
    }

    private void sendIndex(net.minecraft.util.Identifier packet) {
        if (selected() == null) return;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(selectedIndex);
        ClientPlayNetworking.send(packet, buf);
    }

    private void changePage(int delta) { page += delta; clampPage(); }
    private void clampPage() {
        int max = Math.max(0, (filtered().size() - 1) / ROWS);
        page = Math.max(0, Math.min(page, max));
        if (prevPageButton != null) prevPageButton.active = page > 0;
        if (nextPageButton != null) nextPageButton.active = page < max;
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = width / 2 - 250;
        int top = Math.max(16, height / 2 - 135);
        if (button == 0 && mouseX >= left && mouseX < left + 230 && mouseY >= top + 58 && mouseY < top + 58 + ROWS * 20) {
            int row = (int) ((mouseY - (top + 58)) / 20);
            List<RoundRestoreClientNetworking.Layer> filtered = filtered();
            int at = page * ROWS + row;
            if (at >= 0 && at < filtered.size()) {
                select(filtered.get(at).index());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int left = width / 2 - 250;
        int top = Math.max(16, height / 2 - 135);
        int right = left + 250;
        context.drawTextWithShadow(textRenderer, title, left, top, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer,
                "Выберите сохранённый слой слева или создайте новый из точек A/B",
                left, top + 14, 0xFFAAAAAA);

        List<RoundRestoreClientNetworking.Layer> filtered = filtered();
        clampPage();
        for (int row = 0; row < ROWS; row++) {
            int at = page * ROWS + row;
            if (at >= filtered.size()) break;
            var layer = filtered.get(at);
            int y = top + 58 + row * 20;
            boolean selected = layer.index() == selectedIndex;
            context.fill(left, y, left + 230, y + 18, selected ? 0xAA3A5A78 : 0x77101010);
            String status = layer.enabled() ? "§aВКЛ §f" : "§7ВЫКЛ §f";
            String layerTitle = layer.name();
            if (layerTitle.length() > 23) layerTitle = layerTitle.substring(0, 22) + "…";
            context.drawTextWithShadow(textRenderer, status + layerTitle, left + 5, y + 3, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer, Integer.toString(layer.blocks()), left + 195, y + 3, 0xFF999999);
        }
        context.drawCenteredTextWithShadow(textRenderer,
                "Страница " + (page + 1) + " / " + Math.max(1, (filtered.size() + ROWS - 1) / ROWS),
                left + 115, top + 211, 0xFF888888);

        var layer = selected();
        context.drawTextWithShadow(textRenderer, layer == null ? "Новый слой" : "Выбранный слой", right, top + 30, 0xFFFFAA00);
        if (layer != null) {
            context.drawTextWithShadow(textRenderer, "Блоков: " + layer.blocks(), right, top + 184, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer, "Мир: " + layer.world(), right, top + 198, 0xFFBBBBBB);
            context.drawTextWithShadow(textRenderer,
                    "От: " + layer.minX() + ", " + layer.minY() + ", " + layer.minZ(), right, top + 212, 0xFFBBBBBB);
            context.drawTextWithShadow(textRenderer,
                    "До: " + layer.maxX() + ", " + layer.maxY() + ", " + layer.maxZ(), right, top + 226, 0xFFBBBBBB);
        } else {
            context.drawTextWithShadow(textRenderer, "ПКМ инструментом = точка A", right, top + 184, 0xFFBBBBBB);
            context.drawTextWithShadow(textRenderer, "Shift+ПКМ инструментом = точка B", right, top + 198, 0xFFBBBBBB);
        }
        context.drawTextWithShadow(textRenderer,
                "Ручное восстановление не запускает игру.", right, top + 258, 0xFF888888);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override public boolean shouldPause() { return false; }
}
