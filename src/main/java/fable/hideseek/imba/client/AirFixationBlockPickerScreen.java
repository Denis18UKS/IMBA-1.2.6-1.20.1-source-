package fable.hideseek.imba.client;

import fable.hideseek.imba.config.AirFixationConfig;
import fable.hideseek.imba.net.AirFixationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class AirFixationBlockPickerScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private final String maskKey;
    private final List<Entry> all = new ArrayList<>();
    private final List<Entry> filtered = new ArrayList<>();
    private TextFieldWidget search;
    private int scroll;
    public AirFixationBlockPickerScreen(String maskKey, String current) {
        super(Text.literal("Требуемый блок для фиксации")); this.maskKey = maskKey;
        for (Block block : Registries.BLOCK) { Identifier id = Registries.BLOCK.getId(block); if (id != null) all.add(new Entry(block, id)); }
        all.sort(Comparator.comparing(e -> e.id.toString())); filtered.addAll(all);
    }
    @Override protected void init() {
        int panelWidth = Math.min(590, width - 24), left = (width - panelWidth) / 2;
        search = new TextFieldWidget(textRenderer, left + 12, 38, panelWidth - 24, 20, Text.empty());
        search.setPlaceholder(Text.literal("Поиск блока Minecraft или мода...")); search.setChangedListener(v -> { rebuild(); scroll = 0; }); addDrawableChild(search);
        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), b -> { if (client != null) client.setScreen(new AirFixationConfigScreen()); }).dimensions(width / 2 - 55, height - 30, 110, 20).build());
    }
    private void rebuild() {
        String q = search == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT); filtered.clear();
        for (Entry e : all) if (q.isEmpty() || e.id.toString().toLowerCase(Locale.ROOT).contains(q) || e.block.getName().getString().toLowerCase(Locale.ROOT).contains(q)) filtered.add(e);
        clamp();
    }
    private int top() { return 66; } private int bottom() { return height - 42; } private int rows() { return Math.max(1, (bottom() - top()) / ROW_HEIGHT); }
    private void clamp() { scroll = MathHelper.clamp(scroll, 0, Math.max(0, filtered.size() - rows())); }
    @Override public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        renderBackground(c); int panelWidth = Math.min(590, width - 24), left = (width - panelWidth) / 2, right = left + panelWidth;
        c.fill(left, 8, right, height - 36, 0xE0181818); c.drawCenteredTextWithShadow(textRenderer, title, width / 2, 16, 0xFFFFFFFF);
        c.drawCenteredTextWithShadow(textRenderer, "Выберите блок, который должен находиться в конечной точке маскировки", width / 2, 27, 0xFFAAAAAA);
        clamp(); int y = top();
        for (int row = 0; row < rows(); row++) { int index = scroll + row; if (index >= filtered.size()) break; Entry e = filtered.get(index);
            c.fill(left + 12, y, right - 12, y + 21, (index & 1) == 0 ? 0x55303030 : 0x55404040);
            ItemStack icon = new ItemStack(e.block.asItem()); if (!icon.isEmpty()) c.drawItem(icon, left + 14, y + 2);
            c.drawTextWithShadow(textRenderer, e.block.getName().getString(), left + 36, y + 3, 0xFFFFFFFF); c.drawTextWithShadow(textRenderer, e.id.toString(), left + 250, y + 3, 0xFF999999); y += ROW_HEIGHT; }
        super.render(c, mouseX, mouseY, delta);
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true; if (button != 0) return false;
        int panelWidth = Math.min(590, width - 24), left = (width - panelWidth) / 2;
        if (mouseX < left + 12 || mouseX >= left + panelWidth - 12 || mouseY < top() || mouseY >= bottom()) return false;
        int index = scroll + (int) ((mouseY - top()) / ROW_HEIGHT); if (index < 0 || index >= filtered.size()) return false;
        String id = filtered.get(index).id.toString(); PacketByteBuf buf = PacketByteBufs.create(); buf.writeString(maskKey, 320); buf.writeByte(AirFixationConfig.Mode.REQUIRE_BLOCK.ordinal()); buf.writeString(id, 256);
        ClientPlayNetworking.send(AirFixationNetworking.SET, buf); if (client != null) client.setScreen(new AirFixationConfigScreen()); return true;
    }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double amount) { if (amount != 0) { scroll -= (int) Math.signum(amount) * 3; clamp(); return true; } return super.mouseScrolled(mouseX, mouseY, amount); }
    @Override public boolean shouldPause() { return false; }
    private record Entry(Block block, Identifier id) {}
}
