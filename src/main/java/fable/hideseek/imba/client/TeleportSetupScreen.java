package fable.hideseek.imba.client;

import fable.hideseek.imba.game.GameConfig;
import fable.hideseek.imba.net.TeleportToolNetworking;
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
    private static final String[] MODES = buildModes();

    private static String[] buildModes() {
        java.util.List<String> modes = new java.util.ArrayList<>();
        modes.add("lobby");
        for (int i = 1; i <= 12; i++) { modes.add("r" + i + "_hider"); modes.add("r" + i + "_seeker"); }
        for (int i = 2; i <= 8; i++) modes.add("prepare_seeker_" + i);
        return modes.toArray(String[]::new);
    }

    private int modeIndex;
    private TextFieldWidget xField;
    private TextFieldWidget yField;
    private TextFieldWidget zField;
    private TextFieldWidget yawField;
    private TextFieldWidget pitchField;
    private Text status = Text.empty();
    private int statusColor = 0xAAAAAA;

    public TeleportSetupScreen() { super(Text.literal("Настройщик телепортов")); }

    @Override
    protected void init() {
        int left = width / 2 - 110;
        int top = Math.max(8, height / 2 - 112);
        addDrawableChild(ButtonWidget.builder(modeText(), button -> {
            modeIndex = (modeIndex + 1) % MODES.length;
            button.setMessage(modeText());
            status = Text.literal("Выбрана точка: " + modeLabel(MODES[modeIndex]));
            statusColor = 0xAAAAAA;
        }).dimensions(left, top, 220, 20).build());
        var player = MinecraftClient.getInstance().player;
        xField = coordinateField(left, top + 28, player == null ? 0.0D : player.getX());
        yField = coordinateField(left, top + 52, player == null ? 0.0D : player.getY());
        zField = coordinateField(left, top + 76, player == null ? 0.0D : player.getZ());
        yawField = coordinateField(left, top + 100, player == null ? 0.0D : player.getYaw());
        pitchField = coordinateField(left, top + 124, player == null ? 0.0D : player.getPitch());
        addDrawableChild(ButtonWidget.builder(Text.literal("Текущая позиция + автосохранение"), button -> { fillFromPlayer(); save(); })
                .dimensions(left, top + 152, 220, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Сохранить"), button -> save()).dimensions(left, top + 176, 106, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Телепорт"), button -> testTeleport()).dimensions(left + 114, top + 176, 106, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), button -> close()).dimensions(left, top + 200, 220, 20).build());
    }

    private TextFieldWidget coordinateField(int x, int y, double value) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, 220, 20, Text.empty());
        field.setText(format(value)); field.setMaxLength(24); addDrawableChild(field); return field;
    }
    private Text modeText() { return Text.literal("Точка: " + modeLabel(MODES[modeIndex])); }
    private static String modeLabel(String mode) {
        if ("lobby".equals(mode)) return "Лобби";
        if (mode.startsWith("prepare_seeker_")) return "Промежуточный спавн доп. искателя №" + mode.substring("prepare_seeker_".length());
        int underscore = mode.indexOf('_'); if (underscore <= 1) return mode;
        try {
            int index = Integer.parseInt(mode.substring(1, underscore)) - 1;
            String role = mode.endsWith("_hider") ? "прячущийся" : "искатель";
            return GameConfig.getLocationName(index) + " — " + role;
        } catch (NumberFormatException ignored) { return mode; }
    }
    private void fillFromPlayer() {
        var player = MinecraftClient.getInstance().player; if (player == null) return;
        xField.setText(format(player.getX())); yField.setText(format(player.getY())); zField.setText(format(player.getZ()));
        yawField.setText(format(player.getYaw())); pitchField.setText(format(player.getPitch()));
    }
    private void save() {
        try {
            double x=parse(xField),y=parse(yField),z=parse(zField); float yaw=(float)parse(yawField), pitch=(float)parse(pitchField);
            if (!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||!Float.isFinite(yaw)||!Float.isFinite(pitch)||pitch < -90.0F||pitch > 90.0F) throw new NumberFormatException();
            PacketByteBuf buf=PacketByteBufs.create(); buf.writeString(MODES[modeIndex]);buf.writeDouble(x);buf.writeDouble(y);buf.writeDouble(z);buf.writeFloat(yaw);buf.writeFloat(pitch);
            ClientPlayNetworking.send(TeleportToolNetworking.SAVE,buf); status=Text.literal("Автосохранено: "+modeLabel(MODES[modeIndex]));statusColor=0x55FF55;
        } catch (NumberFormatException ignored) { status=Text.literal("Некорректные координаты или угол");statusColor=0xFF5555; }
    }
    private void testTeleport(){PacketByteBuf buf=PacketByteBufs.create();buf.writeString(MODES[modeIndex]);ClientPlayNetworking.send(TeleportToolNetworking.TEST,buf);close();}
    private static double parse(TextFieldWidget field){return Double.parseDouble(field.getText().replace(',','.'));}
    private static String format(double value){return String.format(java.util.Locale.ROOT,"%.3f",value);}
    @Override public void render(DrawContext context,int mouseX,int mouseY,float delta){
        renderBackground(context);int top=Math.max(8,height/2-112),labelX=width/2-128;
        context.drawCenteredTextWithShadow(textRenderer,title,width/2,Math.max(1,top-11),0xFFFFFF);
        context.drawTextWithShadow(textRenderer,"X",labelX,top+34,0xAAAAAA);context.drawTextWithShadow(textRenderer,"Y",labelX,top+58,0xAAAAAA);context.drawTextWithShadow(textRenderer,"Z",labelX,top+82,0xAAAAAA);context.drawTextWithShadow(textRenderer,"Yaw",labelX-12,top+106,0xAAAAAA);context.drawTextWithShadow(textRenderer,"Pitch",labelX-18,top+130,0xAAAAAA);
        if(!status.getString().isEmpty())context.drawCenteredTextWithShadow(textRenderer,status,width/2,top+224,statusColor);super.render(context,mouseX,mouseY,delta);
    }
    @Override public boolean shouldPause(){return false;}
}
