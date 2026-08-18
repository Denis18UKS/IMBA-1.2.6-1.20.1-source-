package fable.hideseek.imba.client;

import fable.hideseek.imba.ImbaMod;
import fable.hideseek.imba.ImbaExtension;
import fable.hideseek.imba.block.SettingsPanelBlock;
import fable.hideseek.imba.block.StartBlock;
import fable.hideseek.imba.block.entity.StartBlockEntity;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

import java.util.ArrayList;
import java.util.List;

public final class WorldPanelRenderer {
    private record Anchor(BlockPos pos, Direction facing) {}

    private static final List<Anchor> SETTINGS = new ArrayList<>();
    private static final List<Anchor> STARTS = new ArrayList<>();
    private static int scanCooldown;
    private static net.minecraft.client.world.ClientWorld cachedWorld;

    private WorldPanelRenderer() {}

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null || client.player == null || context.matrixStack() == null) return;
            if (cachedWorld != client.world) { cachedWorld = client.world; SETTINGS.clear(); STARTS.clear(); scanCooldown = 0; }
            if (scanCooldown-- <= 0) { scan(client); scanCooldown = 20; }
            for (Anchor anchor : List.copyOf(SETTINGS)) renderSettings(context.matrixStack(), context.consumers(), anchor, client);
            for (Anchor anchor : List.copyOf(STARTS)) renderStart(context.matrixStack(), context.consumers(), anchor, client);
        });
    }

    private static void scan(MinecraftClient client) {
        BlockPos center = client.player.getBlockPos();
        for (BlockPos pos : BlockPos.iterate(center.add(-16, -8, -16), center.add(16, 8, 16))) {
            BlockState state = client.world.getBlockState(pos);
            if (state.isOf(ImbaMod.SETTINGS_PANEL) && state.get(SettingsPanelBlock.COLUMN) == 0 && state.get(SettingsPanelBlock.ROW) == 0) remember(SETTINGS, new Anchor(pos.toImmutable(), state.get(SettingsPanelBlock.FACING)));
            else if (state.isOf(ImbaMod.START_BLOCK)) remember(STARTS, new Anchor(pos.toImmutable(), state.get(StartBlock.FACING)));
        }
        SETTINGS.removeIf(anchor -> client.world.isChunkLoaded(anchor.pos()) && !client.world.getBlockState(anchor.pos()).isOf(ImbaMod.SETTINGS_PANEL));
        STARTS.removeIf(anchor -> client.world.isChunkLoaded(anchor.pos()) && !client.world.getBlockState(anchor.pos()).isOf(ImbaMod.START_BLOCK));
    }

    private static void remember(List<Anchor> anchors, Anchor candidate) { anchors.removeIf(anchor -> anchor.pos().equals(candidate.pos())); anchors.add(candidate); }

    private static void renderSettings(MatrixStack matrices, VertexConsumerProvider consumers, Anchor anchor, MinecraftClient client) {
        begin(matrices, anchor.pos(), anchor.facing(), 3, client);
        String timerLabel = PanelData.timerLabel == null || PanelData.timerLabel.isBlank() ? "Таймер" : PanelData.timerLabel;
        String heartsLabel = PanelData.heartsLabel == null || PanelData.heartsLabel.isBlank() ? "Сердца" : PanelData.heartsLabel;
        drawCenteredAtScale(matrices, consumers, client.textRenderer, timerLabel, PanelData.timerX, PanelData.titleY, PanelData.timerTitleScale, 0xFFFFAA00);
        drawCenteredAtScale(matrices, consumers, client.textRenderer, heartsLabel, PanelData.heartsX, PanelData.titleY, PanelData.heartsTitleScale, 0xFFFFAA00);
        drawCenteredAtScale(matrices, consumers, client.textRenderer, "▲", PanelData.timerX, PanelData.upArrowY, PanelData.arrowScale, 0xFFFFFFFF);
        drawCenteredAtScale(matrices, consumers, client.textRenderer, "▲", PanelData.heartsX, PanelData.upArrowY, PanelData.arrowScale, 0xFFFFFFFF);
        drawCenteredAtScale(matrices, consumers, client.textRenderer, String.format("%02d:%02d", PanelData.seconds / 60, PanelData.seconds % 60), PanelData.timerX, PanelData.valueY, PanelData.timerValueScale, 0xFFFFFFFF);
        drawCenteredAtScale(matrices, consumers, client.textRenderer, "❤ " + PanelData.hearts, PanelData.heartsX, PanelData.valueY, PanelData.heartsValueScale, 0xFFFF5555);
        drawCenteredAtScale(matrices, consumers, client.textRenderer, "▼", PanelData.timerX, PanelData.downArrowY, PanelData.arrowScale, 0xFFFFFFFF);
        drawCenteredAtScale(matrices, consumers, client.textRenderer, "▼", PanelData.heartsX, PanelData.downArrowY, PanelData.arrowScale, 0xFFFFFFFF);
        boolean editHitboxes = client.currentScreen instanceof PanelHitboxScreen || client.currentScreen instanceof GameSettingsScreen || client.player.getMainHandStack().isOf(ImbaExtension.PANEL_SETTINGS_TOOL) || client.player.getOffHandStack().isOf(ImbaExtension.PANEL_SETTINGS_TOOL);
        if (editHitboxes) {
            drawHitbox(matrices, consumers, PanelData.timerUpHitbox, 80, 255, 120);
            drawHitbox(matrices, consumers, PanelData.timerDownHitbox, 255, 100, 100);
            drawHitbox(matrices, consumers, PanelData.heartsUpHitbox, 80, 255, 120);
            drawHitbox(matrices, consumers, PanelData.heartsDownHitbox, 255, 100, 100);
        }
        matrices.pop();
    }

    private static void renderStart(MatrixStack matrices, VertexConsumerProvider consumers, Anchor anchor, MinecraftClient client) {
        beginSingle(matrices, anchor.pos(), anchor.facing(), client);
        String title = client.world.getBlockEntity(anchor.pos()) instanceof StartBlockEntity start ? start.getTitle() : "Начать";
        drawCentered(matrices, consumers, client.textRenderer, title, 0, -4, 0xFFFFAA00);
        matrices.pop();
    }

    private static void begin(MatrixStack matrices, BlockPos pos, Direction facing, int height, MinecraftClient client) {
        var camera = client.gameRenderer.getCamera().getPos(); Direction right = facing.rotateYCounterclockwise(); matrices.push();
        matrices.translate(pos.getX() - camera.x + .5 + right.getOffsetX(), pos.getY() - camera.y + height / 2.0, pos.getZ() - camera.z + .5 + right.getOffsetZ());
        matrices.translate(facing.getOffsetX() * .506, 0, facing.getOffsetZ() * .506);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing.asRotation())); matrices.scale(.021F, -.021F, .021F);
    }

    private static void beginSingle(MatrixStack matrices, BlockPos pos, Direction facing, MinecraftClient client) {
        var camera = client.gameRenderer.getCamera().getPos(); matrices.push(); matrices.translate(pos.getX() - camera.x + .5, pos.getY() - camera.y + .55, pos.getZ() - camera.z + .5);
        matrices.translate(facing.getOffsetX() * .506, 0, facing.getOffsetZ() * .506); matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing.asRotation())); matrices.scale(.018F, -.018F, .018F);
    }

    private static void draw(MatrixStack matrices, VertexConsumerProvider consumers, TextRenderer renderer, String text, float x, float y, int color) { renderer.draw(text, x, y, color, false, matrices.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE); }
    private static void drawCentered(MatrixStack matrices, VertexConsumerProvider consumers, TextRenderer renderer, String text, float centerX, float y, int color) { draw(matrices, consumers, renderer, text, centerX - renderer.getWidth(text) / 2.0F, y, color); }
    private static void drawCenteredAtScale(MatrixStack matrices, VertexConsumerProvider consumers, TextRenderer renderer, String text, float centerX, float y, float scale, int color) { String value = text == null ? "" : text; float width = Math.max(1.0F, renderer.getWidth(value)); matrices.push(); matrices.translate(centerX, y, 0.0F); matrices.scale(scale, scale, 1.0F); draw(matrices, consumers, renderer, value, -width / 2.0F, 0.0F, color); matrices.pop(); }
    private static void drawHitbox(MatrixStack matrices, VertexConsumerProvider consumers, fable.hideseek.imba.config.PanelSettingsConfig.Hitbox h, int r, int g, int b) {
        if (h == null) return; float x0 = h.x - h.width / 2.0F, x1 = h.x + h.width / 2.0F; float y0 = h.y - h.height / 2.0F, y1 = h.y + h.height / 2.0F; float z = -0.08F;
        VertexConsumer v = consumers.getBuffer(RenderLayer.getLines()); MatrixStack.Entry e = matrices.peek();
        WorldDebugBoxRenderer.line(v,e,x0,y0,z,x1,y0,z,r,g,b,255); WorldDebugBoxRenderer.line(v,e,x1,y0,z,x1,y1,z,r,g,b,255); WorldDebugBoxRenderer.line(v,e,x1,y1,z,x0,y1,z,r,g,b,255); WorldDebugBoxRenderer.line(v,e,x0,y1,z,x0,y0,z,r,g,b,255);
    }
}
