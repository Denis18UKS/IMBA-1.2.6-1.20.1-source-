package fable.hideseek.imba.client;

import fable.hideseek.imba.ImbaMod;
import fable.hideseek.imba.block.SettingsPanelBlock;
import fable.hideseek.imba.block.StartBlock;
import fable.hideseek.imba.block.entity.StartBlockEntity;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import java.util.ArrayList;
import java.util.List;

public final class WorldPanelRenderer {
    private record Anchor(BlockPos pos, Direction facing) {}
    private static final List<Anchor> SETTINGS = new ArrayList<>(), STARTS = new ArrayList<>();
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
            if (state.isOf(ImbaMod.SETTINGS_PANEL) && state.get(SettingsPanelBlock.COLUMN) == 0 && state.get(SettingsPanelBlock.ROW) == 0)
                remember(SETTINGS, new Anchor(pos.toImmutable(), state.get(SettingsPanelBlock.FACING)));
            else if (state.isOf(ImbaMod.START_BLOCK)) remember(STARTS, new Anchor(pos.toImmutable(), state.get(StartBlock.FACING)));
        }
        SETTINGS.removeIf(anchor -> client.world.isChunkLoaded(anchor.pos()) && !client.world.getBlockState(anchor.pos()).isOf(ImbaMod.SETTINGS_PANEL));
        STARTS.removeIf(anchor -> client.world.isChunkLoaded(anchor.pos()) && !client.world.getBlockState(anchor.pos()).isOf(ImbaMod.START_BLOCK));
    }

    private static void remember(List<Anchor> anchors, Anchor candidate) { anchors.removeIf(anchor -> anchor.pos().equals(candidate.pos())); anchors.add(candidate); }

    private static void renderSettings(MatrixStack matrices, VertexConsumerProvider consumers, Anchor anchor, MinecraftClient client) {
        begin(matrices, anchor.pos(), anchor.facing(), 2, client);
        int timerX = -38, heartsX = 38;
        drawCenteredAtScale(matrices, consumers, client.textRenderer, "Таймер", timerX, -30, 1.30F, 0xFFFFAA00);
        drawScaledCentered(matrices, consumers, client.textRenderer, PanelData.heartsLabel == null || PanelData.heartsLabel.isBlank() ? "Сердца" : PanelData.heartsLabel, heartsX, -30, 58.0F, 0xFFFFAA00);
        drawCenteredAtScale(matrices, consumers, client.textRenderer, "▲", timerX, -12, 1.45F, 0xFFFFFFFF);
        drawCenteredAtScale(matrices, consumers, client.textRenderer, "▲", heartsX, -12, 1.45F, 0xFFFFFFFF);
        drawCenteredAtScale(matrices, consumers, client.textRenderer, String.format("%02d:%02d", PanelData.seconds / 60, PanelData.seconds % 60), timerX, 8, 1.60F, 0xFFFFFFFF);
        drawCenteredAtScale(matrices, consumers, client.textRenderer, "❤ " + PanelData.hearts, heartsX, 8, 1.60F, 0xFFFF5555);
        drawCenteredAtScale(matrices, consumers, client.textRenderer, "▼", timerX, 31, 1.45F, 0xFFFFFFFF);
        drawCenteredAtScale(matrices, consumers, client.textRenderer, "▼", heartsX, 31, 1.45F, 0xFFFFFFFF);
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
        matrices.translate(facing.getOffsetX() * .506, 0, facing.getOffsetZ() * .506); matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing.asRotation())); matrices.scale(.021f, -.021f, .021f);
    }
    private static void beginSingle(MatrixStack matrices, BlockPos pos, Direction facing, MinecraftClient client) {
        var camera = client.gameRenderer.getCamera().getPos(); matrices.push(); matrices.translate(pos.getX() - camera.x + .5, pos.getY() - camera.y + .55, pos.getZ() - camera.z + .5); matrices.translate(facing.getOffsetX() * .506, 0, facing.getOffsetZ() * .506); matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing.asRotation())); matrices.scale(.018f, -.018f, .018f);
    }
    private static void draw(MatrixStack matrices, VertexConsumerProvider consumers, TextRenderer renderer, String text, float x, float y, int color) { renderer.draw(text, x, y, color, false, matrices.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE); }
    private static void drawCentered(MatrixStack matrices, VertexConsumerProvider consumers, TextRenderer renderer, String text, float centerX, float y, int color) { draw(matrices, consumers, renderer, text, centerX - renderer.getWidth(text) / 2.0f, y, color); }
    private static void drawCenteredAtScale(MatrixStack matrices, VertexConsumerProvider consumers, TextRenderer renderer, String text, float centerX, float y, float scale, int color) { String value=text==null?"":text; float w=Math.max(1.0F,renderer.getWidth(value)); matrices.push(); matrices.translate(centerX,y,0); matrices.scale(scale,scale,1); draw(matrices,consumers,renderer,value,-w/2,0,color); matrices.pop(); }
    private static void drawScaledCentered(MatrixStack matrices, VertexConsumerProvider consumers, TextRenderer renderer, String text, float centerX, float y, float maximumWidth, int color) { String value=text==null?"":text; float w=Math.max(1.0F,renderer.getWidth(value)); drawCenteredAtScale(matrices,consumers,renderer,value,centerX,y,Math.min(1.0F,maximumWidth/w),color); }
}
