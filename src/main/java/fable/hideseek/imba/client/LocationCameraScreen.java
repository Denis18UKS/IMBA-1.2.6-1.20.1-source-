package fable.hideseek.imba.client;

import fable.hideseek.imba.net.MaskNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Location gallery: photo management, custom names and a searchable mask list.
 */
public final class LocationCameraScreen extends Screen {
    private static final int VISIBLE_MASKS = 7;

    private final List<MaskOption> allMasks = new ArrayList<>();
    private final List<MaskOption> filteredMasks = new ArrayList<>();
    private final List<ButtonWidget> maskButtons = new ArrayList<>();

    private int location = PanelData.selectedLocation;
    private int scroll;
    private String selectedKind = "BLOCK";
    private String selectedMaskId = "minecraft:stone";

    private ButtonWidget locationButton;
    private ButtonWidget previousPageButton;
    private ButtonWidget nextPageButton;
    private TextFieldWidget nameField;
    private TextFieldWidget searchField;

    public LocationCameraScreen() {
        super(Text.literal("Галерея локаций"));
    }

    @Override
    protected void init() {
        allMasks.clear();
        addMaskOptions();

        int left = width / 2 - 224;
        int right = width / 2 + 8;
        int top = height / 2 - 104;

        addDrawableChild(ButtonWidget.builder(Text.literal("◀"), button -> changeLocation(-1))
                .dimensions(left, top, 34, 20).build());
        locationButton = addDrawableChild(ButtonWidget.builder(locationText(), button -> {})
                .dimensions(left + 38, top, 136, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("▶"), button -> changeLocation(1))
                .dimensions(left + 178, top, 34, 20).build());

        nameField = new TextFieldWidget(textRenderer, left, top + 38, 212, 20,
                Text.literal("Название локации"));
        nameField.setMaxLength(48);
        nameField.setPlaceholder(Text.literal("Название локации"));
        addDrawableChild(nameField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Телепорт к локации"),
                        button -> teleport(location))
                .dimensions(left, top + 62, 212, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Сделать фото"), button -> {
                    saveSettings();
                    ClientPhotoCapture.schedule(location);
                })
                .dimensions(left, top + 86, 212, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Удалить фото"), button -> delete(false))
                .dimensions(left, top + 110, 104, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Удалить все"), button -> delete(true))
                .dimensions(left + 108, top + 110, 104, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Сохранить настройки"), button -> saveSettings())
                .dimensions(left, top + 134, 212, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("В лобби"),
                        button -> teleport(PanelData.locationCount))
                .dimensions(left, top + 158, 104, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("В кинотеатр"),
                        button -> teleport(PanelData.locationCount + 1))
                .dimensions(left + 108, top + 158, 104, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Готово"), button -> close())
                .dimensions(left, top + 182, 212, 20).build());

        searchField = new TextFieldWidget(textRenderer, right, top + 38, 216, 20,
                Text.literal("Поиск маскировки"));
        searchField.setMaxLength(64);
        searchField.setPlaceholder(Text.literal("Поиск по-русски или по идентификатору"));
        searchField.setChangedListener(value -> {
            scroll = 0;
            filterMasks();
        });
        addDrawableChild(searchField);

        for (int row = 0; row < VISIBLE_MASKS; row++) {
            final int rowIndex = row;
            ButtonWidget button = ButtonWidget.builder(Text.empty(), ignored -> selectVisibleMask(rowIndex))
                    .dimensions(right, top + 62 + row * 20, 216, 18).build();
            maskButtons.add(addDrawableChild(button));
        }

        previousPageButton = addDrawableChild(ButtonWidget.builder(Text.literal("▲"), button -> page(-1))
                .dimensions(right, top + 204, 106, 18).build());
        nextPageButton = addDrawableChild(ButtonWidget.builder(Text.literal("▼"), button -> page(1))
                .dimensions(right + 110, top + 204, 106, 18).build());

        loadLocation();
    }

    private void addMaskOptions() {
        for (Block block : Registries.BLOCK) {
            if (block == Blocks.AIR) {
                continue;
            }
            Identifier id = Registries.BLOCK.getId(block);
            allMasks.add(new MaskOption("BLOCK", id.toString(), block.getName().getString()));
        }
        for (Item item : Registries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            Identifier id = Registries.ITEM.getId(item);
            allMasks.add(new MaskOption("ITEM", id.toString(), item.getName().getString()));
        }

        Collator russian = Collator.getInstance(new Locale("ru", "RU"));
        allMasks.sort(Comparator
                .comparing(MaskOption::displayName, russian)
                .thenComparing(MaskOption::id));
    }

    private void loadLocation() {
        location = Math.floorMod(location, Math.max(1, PanelData.locationCount));
        locationButton.setMessage(locationText());
        nameField.setText(PanelData.configuredName(location));
        selectedKind = PanelData.maskKind(location);
        selectedMaskId = PanelData.maskId(location);
        searchField.setText("");
        scroll = 0;
        filterMasks();
    }

    private void changeLocation(int delta) {
        location = Math.floorMod(location + delta, Math.max(1, PanelData.locationCount));
        loadLocation();
    }

    private Text locationText() {
        return Text.literal("Локация " + (location + 1) + " / " + Math.max(1, PanelData.locationCount));
    }

    private void filterMasks() {
        filteredMasks.clear();
        String query = normalize(searchField == null ? "" : searchField.getText());
        for (MaskOption option : allMasks) {
            if (query.isEmpty()
                    || normalize(option.displayName).contains(query)
                    || normalize(option.id).contains(query)
                    || normalize(option.kindLabel()).contains(query)) {
                filteredMasks.add(option);
            }
        }
        int maximum = Math.max(0, filteredMasks.size() - VISIBLE_MASKS);
        scroll = Math.max(0, Math.min(scroll, maximum));
        refreshMaskButtons();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace('ё', 'е').strip();
    }

    private void refreshMaskButtons() {
        for (int row = 0; row < maskButtons.size(); row++) {
            int index = scroll + row;
            ButtonWidget button = maskButtons.get(row);
            boolean visible = index < filteredMasks.size();
            button.visible = visible;
            button.active = visible;
            if (visible) {
                MaskOption option = filteredMasks.get(index);
                String marker = option.kind.equals(selectedKind) && option.id.equals(selectedMaskId)
                        ? "✓ " : "";
                button.setMessage(Text.literal(marker + option.kindLabel() + ": "
                        + shorten(option.displayName, 27)));
            }
        }
        if (previousPageButton != null) {
            previousPageButton.active = scroll > 0;
            nextPageButton.active = scroll + VISIBLE_MASKS < filteredMasks.size();
        }
    }

    private static String shorten(String value, int maximum) {
        if (value == null || value.length() <= maximum) {
            return value == null ? "" : value;
        }
        return value.substring(0, Math.max(1, maximum - 1)) + "…";
    }

    private void selectVisibleMask(int row) {
        int index = scroll + row;
        if (index < 0 || index >= filteredMasks.size()) {
            return;
        }
        MaskOption option = filteredMasks.get(index);
        selectedKind = option.kind;
        selectedMaskId = option.id;
        refreshMaskButtons();
    }

    private void page(int direction) {
        scroll += direction * VISIBLE_MASKS;
        int maximum = Math.max(0, filteredMasks.size() - VISIBLE_MASKS);
        scroll = Math.max(0, Math.min(scroll, maximum));
        refreshMaskButtons();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseX >= width / 2.0 + 8) {
            scroll -= (int) Math.signum(amount);
            int maximum = Math.max(0, filteredMasks.size() - VISIBLE_MASKS);
            scroll = Math.max(0, Math.min(scroll, maximum));
            refreshMaskButtons();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    private void saveSettings() {
        var buf = PacketByteBufs.create();
        buf.writeVarInt(location);
        buf.writeString(nameField.getText(), 48);
        buf.writeString(selectedKind, 16);
        buf.writeString(selectedMaskId, 128);
        ClientPlayNetworking.send(MaskNetworking.LOCATION_SETTINGS_SAVE_PACKET, buf);

        if (location < PanelData.locationNames.length) {
            PanelData.locationNames[location] = nameField.getText().strip();
            PanelData.locationMaskKinds[location] = selectedKind;
            PanelData.locationMaskIds[location] = selectedMaskId;
        }
    }

    private void delete(boolean all) {
        var buf = PacketByteBufs.create();
        buf.writeVarInt(all ? -1 : location);
        ClientPlayNetworking.send(MaskNetworking.LOCATION_PHOTO_DELETE_PACKET, buf);
    }

    private void teleport(int target) {
        saveSettings();
        var buf = PacketByteBufs.create();
        buf.writeVarInt(target);
        ClientPlayNetworking.send(MaskNetworking.LOCATION_TELEPORT_PACKET, buf);
        close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int left = width / 2 - 224;
        int right = width / 2 + 8;
        int top = height / 2 - 104;
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, top - 22, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, "Название", left, top + 27, 0xAAAAAA);
        context.drawTextWithShadow(textRenderer,
                ClientLocationPhotos.has(location) ? "Фото сохранено" : "Фото отсутствует",
                left, top + 207, ClientLocationPhotos.has(location) ? 0x55FF55 : 0xAAAAAA);
        context.drawTextWithShadow(textRenderer,
                "Маскировка: " + selectedMaskLabel(), right, top + 3, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, "Поиск маскировки", right, top + 27, 0xAAAAAA);
        context.drawTextWithShadow(textRenderer,
                "Найдено: " + filteredMasks.size(), right, top + 187, 0xAAAAAA);
        super.render(context, mouseX, mouseY, delta);
    }

    private String selectedMaskLabel() {
        for (MaskOption option : allMasks) {
            if (option.kind.equals(selectedKind) && option.id.equals(selectedMaskId)) {
                return shorten(option.displayName, 25);
            }
        }
        return selectedMaskId;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private record MaskOption(String kind, String id, String displayName) {
        private String kindLabel() {
            return "BLOCK".equals(kind) ? "Блок" : "Предмет";
        }
    }
}
