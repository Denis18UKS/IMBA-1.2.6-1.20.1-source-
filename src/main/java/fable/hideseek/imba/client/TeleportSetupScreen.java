package fable.hideseek.imba.client;

import fable.hideseek.imba.net.MaskNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

public final class TeleportSetupScreen extends Screen {
    private static final String[] MODES = {
            "lobby",
            "r1_hider", "r1_seeker",
            "r2_hider", "r2_seeker",
            "r3_hider", "r3_seeker",
            "r4_hider", "r4_seeker",
            "r5_hider", "r5_seeker",
            "r6_hider", "r6_seeker",
            "r7_hider", "r7_seeker",
            "r8_hider", "r8_seeker",
            "r9_hider", "r9_seeker",
            "r10_hider", "r10_seeker",
            "r11_hider", "r11_seeker",
            "r12_hider", "r12_seeker"
    };

    private int modeIndex;
    private TextFieldWidget xField;
    private TextFieldWidget yField;
    private TextFieldWidget zField;
    private Text error = Text.empty();

    public TeleportSetupScreen() {
        super(Text.translatable("screen.imba.teleport.title"));
    }

    @Override
    protected void init() {
        int left = width / 2 - 100;
        int top = height / 2 - 78;
        var player = MinecraftClient.getInstance().player;

        addDrawableChild(ButtonWidget.builder(modeText(), button -> {
            modeIndex = (modeIndex + 1) % MODES.length;
            button.setMessage(modeText());
        }).dimensions(left, top, 200, 20).build());

        xField = coordinateField(left, top + 30, "X", player == null ? 0.0D : player.getX());
        yField = coordinateField(left, top + 55, "Y", player == null ? 0.0D : player.getY());
        zField = coordinateField(left, top + 80, "Z", player == null ? 0.0D : player.getZ());

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.imba.teleport.save"), button -> save())
                .dimensions(left, top + 110, 98, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Отмена"), button -> close())
                .dimensions(left + 102, top + 110, 98, 20).build());
    }

    private TextFieldWidget coordinateField(int x, int y, String label, double value) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, 200, 20, Text.literal(label));
        field.setText(String.format(java.util.Locale.ROOT, "%.3f", value));
        field.setMaxLength(24);
        addDrawableChild(field);
        return field;
    }

    private Text modeText() {
        return Text.literal("Точка: " + fable.hideseek.imba.game.GameConfig.getPointDisplayName(MODES[modeIndex]));
    }

    private void save() {
        try {
            double x = Double.parseDouble(xField.getText().replace(',', '.'));
            double y = Double.parseDouble(yField.getText().replace(',', '.'));
            double z = Double.parseDouble(zField.getText().replace(',', '.'));
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                throw new NumberFormatException();
            }

            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString(MODES[modeIndex]);
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
            ClientPlayNetworking.send(MaskNetworking.TELEPORT_SAVE_PACKET, buf);
            close();
        } catch (NumberFormatException ignored) {
            error = Text.translatable("screen.imba.teleport.invalid");
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 102, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, "X", width / 2 - 114, height / 2 - 45, 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, "Y", width / 2 - 114, height / 2 - 20, 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, "Z", width / 2 - 114, height / 2 + 5, 0xAAAAAA);
        if (!error.getString().isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, error, width / 2, height / 2 + 58, 0xFF5555);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
