package fable.hideseek.imba.client;

import fable.hideseek.imba.block.entity.StartBlockEntity;
import fable.hideseek.imba.net.MaskNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class StartBlockNameScreen extends Screen {
    private final BlockPos pos;
    private TextFieldWidget titleField;

    public StartBlockNameScreen(BlockPos pos) {
        super(Text.literal("Название блока запуска"));
        this.pos = pos.toImmutable();
    }

    @Override
    protected void init() {
        int left = width / 2 - 100;
        int top = height / 2 - 35;
        String current = "Начать";
        var world = MinecraftClient.getInstance().world;
        if (world != null && world.getBlockEntity(pos) instanceof StartBlockEntity start) {
            current = start.getTitle();
        }

        titleField = new TextFieldWidget(textRenderer, left, top, 200, 20, Text.literal("Название"));
        titleField.setMaxLength(32);
        titleField.setText(current);
        addDrawableChild(titleField);
        setInitialFocus(titleField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Сохранить"), button -> save())
                .dimensions(left, top + 32, 98, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Отмена"), button -> close())
                .dimensions(left + 102, top + 32, 98, 20).build());
    }

    private void save() {
        var buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeString(titleField.getText(), 32);
        ClientPlayNetworking.send(MaskNetworking.START_BLOCK_RENAME_PACKET, buf);
        close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 68, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
