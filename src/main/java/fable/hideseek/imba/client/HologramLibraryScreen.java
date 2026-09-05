package fable.hideseek.imba.client;

import fable.hideseek.imba.net.HologramNetworking;
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

/** Searchable library for every saved location-photo hologram. */
public final class HologramLibraryScreen extends Screen {
    private static final int VISIBLE_ROWS = 8;
    private final List<HologramClientData.Projector> all = new ArrayList<>();
    private final List<HologramClientData.Projector> filtered = new ArrayList<>();
    private final List<ButtonWidget> rowButtons = new ArrayList<>();
    private TextFieldWidget searchField;
    private ButtonWidget editButton, scaleMinus, scaleValue, scalePlus, upButton, downButton;
    private int selectedProjectorId = -1;
    private int scroll;

    public HologramLibraryScreen() {
        super(Text.literal("Библиотека фото-голограмм"));
    }

    @Override
    protected void init() {
        int left = width / 2 - 190;
        int top = Math.max(18, height / 2 - 120);
        searchField = new TextFieldWidget(textRenderer, left, top, 380, 20, Text.literal("Поиск"));
        searchField.setPlaceholder(Text.literal("Поиск по названию локации или номеру голограммы"));
        searchField.setMaxLength(64);
        searchField.setChangedListener(value -> {
            scroll = 0;
            filter();
        });
        addDrawableChild(searchField);

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            final int visibleRow = row;
            rowButtons.add(addDrawableChild(ButtonWidget.builder(Text.empty(), b -> selectVisible(visibleRow))
                    .dimensions(left, top + 28 + row * 22, 350, 20).build()));
        }
        upButton = addDrawableChild(ButtonWidget.builder(Text.literal("▲"), b -> page(-1))
                .dimensions(left + 356, top + 28, 24, 84).build());
        downButton = addDrawableChild(ButtonWidget.builder(Text.literal("▼"), b -> page(1))
                .dimensions(left + 356, top + 120, 24, 84).build());

        editButton = addDrawableChild(ButtonWidget.builder(Text.literal("Открыть / настроить"), b -> editSelected())
                .dimensions(left, top + 212, 180, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Новая голограмма"), b ->
                        client.setScreen(new HologramProjectorScreen(-1)))
                .dimensions(left + 188, top + 212, 192, 20).build());

        scaleMinus = addDrawableChild(ButtonWidget.builder(Text.literal("− размер"), b -> changeScale(-0.10F))
                .dimensions(left, top + 238, 112, 20).build());
        scaleValue = addDrawableChild(ButtonWidget.builder(Text.literal("Размер: —"), b -> {})
                .dimensions(left + 120, top + 238, 140, 20).build());
        scalePlus = addDrawableChild(ButtonWidget.builder(Text.literal("+ размер"), b -> changeScale(0.10F))
                .dimensions(left + 268, top + 238, 112, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), b -> close())
                .dimensions(left, top + 264, 380, 20).build());

        applyServerState(HologramClientData.snapshot());
        ClientPlayNetworking.send(HologramNetworking.REQUEST, PacketByteBufs.empty());
    }

    public void applyServerState(List<HologramClientData.Projector> list) {
        all.clear();
        all.addAll(list);
        if (selectedProjectorId >= 0 && all.stream().noneMatch(p -> p.id() == selectedProjectorId)) {
            selectedProjectorId = -1;
        }
        filter();
    }

    private void filter() {
        filtered.clear();
        String query = normalize(searchField == null ? "" : searchField.getText());
        for (var p : all) {
            String name = PanelData.locationName(p.location());
            String searchable = normalize(name + " " + p.id() + " " + (p.id() + 1) + " " + p.world());
            if (query.isEmpty() || searchable.contains(query)) filtered.add(p);
        }
        int max = Math.max(0, filtered.size() - VISIBLE_ROWS);
        scroll = Math.max(0, Math.min(scroll, max));
        refreshRows();
    }

    private void refreshRows() {
        for (int row = 0; row < rowButtons.size(); row++) {
            int index = scroll + row;
            ButtonWidget button = rowButtons.get(row);
            boolean visible = index < filtered.size();
            button.visible = visible;
            button.active = visible;
            if (!visible) continue;
            var p = filtered.get(index);
            String marker = p.id() == selectedProjectorId ? "▶ " : "";
            String label = marker + "#" + p.id() + " • " + shorten(PanelData.locationName(p.location()), 28)
                    + " • " + String.format(Locale.ROOT, "%.1fx", p.scale());
            button.setMessage(Text.literal(label));
        }
        if (upButton != null) upButton.active = scroll > 0;
        if (downButton != null) downButton.active = scroll + VISIBLE_ROWS < filtered.size();
        var selected = selected();
        boolean has = selected != null;
        if (editButton != null) editButton.active = has;
        if (scaleMinus != null) scaleMinus.active = has;
        if (scalePlus != null) scalePlus.active = has;
        if (scaleValue != null) scaleValue.setMessage(Text.literal(has
                ? "Размер: " + String.format(Locale.ROOT, "%.2fx", selected.scale())
                : "Размер: —"));
    }

    private void selectVisible(int row) {
        int index = scroll + row;
        if (index < 0 || index >= filtered.size()) return;
        selectedProjectorId = filtered.get(index).id();
        refreshRows();
    }

    private void editSelected() {
        if (selectedProjectorId >= 0) client.setScreen(new HologramProjectorScreen(selectedProjectorId));
    }

    private void changeScale(float delta) {
        var p = selected();
        if (p == null) return;
        float next = Math.max(0.20F, Math.min(3.0F, Math.round((p.scale() + delta) * 10.0F) / 10.0F));
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(p.id());
        buf.writeVarInt(p.location());
        buf.writeString(p.world(), 128);
        buf.writeDouble(p.x());
        buf.writeDouble(p.y());
        buf.writeDouble(p.z());
        buf.writeFloat(p.yaw());
        buf.writeFloat(next);
        buf.writeByte(p.light());
        buf.writeBoolean(p.textBackground());
        buf.writeFloat(p.contrast());
        ClientPlayNetworking.send(HologramNetworking.SAVE, buf);
    }

    private HologramClientData.Projector selected() {
        if (selectedProjectorId < 0) return null;
        for (var p : all) if (p.id() == selectedProjectorId) return p;
        return null;
    }

    private void page(int direction) {
        scroll += direction * VISIBLE_ROWS;
        int max = Math.max(0, filtered.size() - VISIBLE_ROWS);
        scroll = Math.max(0, Math.min(scroll, max));
        refreshRows();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        scroll -= (int) Math.signum(amount);
        int max = Math.max(0, filtered.size() - VISIBLE_ROWS);
        scroll = Math.max(0, Math.min(scroll, max));
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
        context.drawCenteredTextWithShadow(textRenderer,
                "Найдено: " + filtered.size() + " / " + all.size(), width / 2, top + 198, 0xFFAAAAAA);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
