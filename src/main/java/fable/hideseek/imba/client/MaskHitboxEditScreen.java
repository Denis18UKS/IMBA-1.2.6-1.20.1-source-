package fable.hideseek.imba.client;

import fable.hideseek.imba.config.MaskHitboxConfig;
import fable.hideseek.imba.net.MaskHitboxNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Exact box editor with front (X/Y) and top (X/Z) previews. */
public final class MaskHitboxEditScreen extends Screen {
    private static final double PREVIEW_MIN = MaskHitboxConfig.MIN_COORD;
    private static final double PREVIEW_MAX = MaskHitboxConfig.MAX_COORD;

    private final Screen parent;
    private final Identifier blockId;
    private TextFieldWidget minX;
    private TextFieldWidget maxX;
    private TextFieldWidget minY;
    private TextFieldWidget maxY;
    private TextFieldWidget minZ;
    private TextFieldWidget maxZ;
    private MaskHitboxConfig.BoxSpec box;

    public MaskHitboxEditScreen(Screen parent, Identifier blockId) {
        super(Text.literal("3D-хитбокс маски"));
        this.parent = parent;
        this.blockId = blockId;
        loadBox();
    }

    @Override
    protected void init() {
        int fieldsY = Math.max(278, height - 130);
        int startX = width / 2 - 267;
        int widthField = 80;
        int gap = 10;
        minX = field(startX, fieldsY, widthField, box.minX);
        maxX = field(startX + (widthField + gap), fieldsY, widthField, box.maxX);
        minY = field(startX + 2 * (widthField + gap), fieldsY, widthField, box.minY);
        maxY = field(startX + 3 * (widthField + gap), fieldsY, widthField, box.maxY);
        minZ = field(startX + 4 * (widthField + gap), fieldsY, widthField, box.minZ);
        maxZ = field(startX + 5 * (widthField + gap), fieldsY, widthField, box.maxZ);

        addDrawableChild(ButtonWidget.builder(Text.literal("Сохранить"), b -> save())
                .dimensions(width / 2 - 190, height - 30, 120, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Сбросить к форме блока"), b -> reset())
                .dimensions(width / 2 - 60, height - 30, 180, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), b -> close())
                .dimensions(width / 2 + 130, height - 30, 120, 20).build());
    }

    private TextFieldWidget field(int x, int y, int w, int value) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, w, 20, Text.empty());
        field.setMaxLength(4);
        field.setText(Integer.toString(value));
        addDrawableChild(field);
        return field;
    }

    public void reloadFromState() {
        MaskHitboxConfig.BoxSpec custom = MaskHitboxClientState.custom(blockId.toString());
        Block block = Registries.BLOCK.get(blockId);
        box = custom == null ? MaskHitboxConfig.defaultFor(block) : custom;
        if (minX != null) {
            minX.setText(Integer.toString(box.minX));
            maxX.setText(Integer.toString(box.maxX));
            minY.setText(Integer.toString(box.minY));
            maxY.setText(Integer.toString(box.maxY));
            minZ.setText(Integer.toString(box.minZ));
            maxZ.setText(Integer.toString(box.maxZ));
        }
    }

    private void loadBox() {
        MaskHitboxConfig.BoxSpec custom = MaskHitboxClientState.custom(blockId.toString());
        Block block = Registries.BLOCK.get(blockId);
        box = custom == null ? MaskHitboxConfig.defaultFor(block) : custom;
    }

    private MaskHitboxConfig.BoxSpec fieldsBox() {
        try {
            return new MaskHitboxConfig.BoxSpec(
                    Integer.parseInt(minX.getText().trim()),
                    Integer.parseInt(minY.getText().trim()),
                    Integer.parseInt(minZ.getText().trim()),
                    Integer.parseInt(maxX.getText().trim()),
                    Integer.parseInt(maxY.getText().trim()),
                    Integer.parseInt(maxZ.getText().trim()));
        } catch (RuntimeException e) {
            return box.copy();
        }
    }

    private void save() {
        MaskHitboxConfig.BoxSpec value = fieldsBox();
        if (value.maxX <= value.minX || value.maxY <= value.minY || value.maxZ <= value.minZ) {
            if (client != null && client.player != null) {
                client.player.sendMessage(Text.literal("§cДля каждой оси MAX должен быть больше MIN"), true);
            }
            return;
        }
        PacketByteBuf out = PacketByteBufs.create();
        out.writeString(blockId.toString(), 256);
        out.writeInt(value.minX);
        out.writeInt(value.minY);
        out.writeInt(value.minZ);
        out.writeInt(value.maxX);
        out.writeInt(value.maxY);
        out.writeInt(value.maxZ);
        ClientPlayNetworking.send(MaskHitboxNetworking.SET, out);
    }

    private void reset() {
        PacketByteBuf out = PacketByteBufs.create();
        out.writeString(blockId.toString(), 256);
        ClientPlayNetworking.send(MaskHitboxNetworking.CLEAR, out);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        Block block = Registries.BLOCK.get(blockId);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                block.getName().getString() + "  §8(" + blockId + ")", width / 2, 25, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                "Координаты в пикселях модели: 16 = один блок. Жёлтая рамка — реальный хитбокс.",
                width / 2, 38, 0xFFBBBBBB);

        MaskHitboxConfig.BoxSpec previewBox = fieldsBox();
        int size = Math.min(190, Math.max(130, height - 205));
        Preview front = new Preview(width / 2 - size - 20, 68, size);
        Preview top = new Preview(width / 2 + 20, 68, size);
        drawProjection(context, front, previewBox.minX, previewBox.maxX, previewBox.minY, previewBox.maxY, true);
        drawProjection(context, top, previewBox.minX, previewBox.maxX, previewBox.minZ, previewBox.maxZ, false);
        context.drawCenteredTextWithShadow(textRenderer, "СПЕРЕДИ  X / Y", front.left + size / 2, front.top - 12, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, "СВЕРХУ  X / Z", top.left + size / 2, top.top - 12, 0xFFFFFFFF);

        int fieldsY = Math.max(278, height - 130);
        int startX = width / 2 - 267;
        int step = 90;
        String[] labels = {"MIN X", "MAX X", "MIN Y", "MAX Y", "MIN Z", "MAX Z"};
        for (int i = 0; i < labels.length; i++) {
            context.drawCenteredTextWithShadow(textRenderer, labels[i], startX + i * step + 40, fieldsY - 11, 0xFFCCCCCC);
        }
        context.drawCenteredTextWithShadow(textRenderer,
                "Допустимый диапазон: " + MaskHitboxConfig.MIN_COORD + " … " + MaskHitboxConfig.MAX_COORD,
                width / 2, fieldsY + 27, 0xFF888888);
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawProjection(DrawContext context, Preview preview,
                                int minHorizontal, int maxHorizontal,
                                int minVertical, int maxVertical,
                                boolean verticalUp) {
        context.fill(preview.left, preview.top, preview.right(), preview.bottom(), 0xEE181818);
        border(context, preview.left, preview.top, preview.size, preview.size, 0xFF666666);

        int cellX1 = map(preview, 0);
        int cellX2 = map(preview, 16);
        int cellY1 = mapY(preview, 0, verticalUp);
        int cellY2 = mapY(preview, 16, verticalUp);
        border(context, Math.min(cellX1, cellX2), Math.min(cellY1, cellY2),
                Math.max(2, Math.abs(cellX2 - cellX1)), Math.max(2, Math.abs(cellY2 - cellY1)), 0xFF777777);

        int x1 = map(preview, minHorizontal);
        int x2 = map(preview, maxHorizontal);
        int y1 = mapY(preview, minVertical, verticalUp);
        int y2 = mapY(preview, maxVertical, verticalUp);
        border(context, Math.min(x1, x2), Math.min(y1, y2),
                Math.max(2, Math.abs(x2 - x1)), Math.max(2, Math.abs(y2 - y1)), 0xFFFFFF55);
    }

    private static int map(Preview preview, double value) {
        double t = (value - PREVIEW_MIN) / (PREVIEW_MAX - PREVIEW_MIN);
        return preview.left + (int) Math.round(t * preview.size);
    }

    private static int mapY(Preview preview, double value, boolean verticalUp) {
        double t = (value - PREVIEW_MIN) / (PREVIEW_MAX - PREVIEW_MIN);
        if (verticalUp) t = 1.0D - t;
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

    @Override public boolean shouldPause() { return false; }

    private record Preview(int left, int top, int size) {
        int right() { return left + size; }
        int bottom() { return top + size; }
    }
}
