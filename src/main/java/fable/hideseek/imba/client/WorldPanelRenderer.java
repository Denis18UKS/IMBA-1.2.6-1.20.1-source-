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
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

import java.util.ArrayList;
import java.util.List;

/** Draws settings and start labels directly on their block faces. */
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
            if (cachedWorld != client.world) {
                cachedWorld = client.world;
                SETTINGS.clear();
                STARTS.clear();
                scanCooldown = 0;
            }
            if (scanCooldown-- <= 0) { scan(client); scanCooldown = 20; }
            for (Anchor anchor : List.copyOf(SETTINGS)) {
                renderSettings(context.matrixStack(), context.consumers(), anchor, client);
            }
            for (Anchor anchor : List.copyOf(STARTS)) {
                renderStart(context.matrixStack(), context.consumers(), anchor, client);
            }
        });
    }

    private static void scan(MinecraftClient client) {
        BlockPos center = client.player.getBlockPos();
        for (BlockPos pos : BlockPos.iterate(center.add(-16, -8, -16), center.add(16, 8, 16))) {
            BlockState state = client.world.getBlockState(pos);
            if (state.isOf(ImbaMod.SETTINGS_PANEL) && state.get(SettingsPanelBlock.COLUMN) == 0
                    && state.get(SettingsPanelBlock.ROW) == 0) {
                remember(SETTINGS, new Anchor(pos.toImmutable(), state.get(SettingsPanelBlock.FACING)));
            } else if (state.isOf(ImbaMod.START_BLOCK)) {
                remember(STARTS, new Anchor(pos.toImmutable(), state.get(StartBlock.FACING)));
            }
        }
        SETTINGS.removeIf(anchor -> isLoadedAndRemoved(client, anchor.pos(), ImbaMod.SETTINGS_PANEL));
        STARTS.removeIf(anchor -> isLoadedAndRemoved(client, anchor.pos(), ImbaMod.START_BLOCK));
    }

    private static void remember(List<Anchor> anchors, Anchor candidate) {
        anchors.removeIf(anchor -> anchor.pos().equals(candidate.pos()));
        anchors.add(candidate);
    }

    private static boolean isLoadedAndRemoved(MinecraftClient client, BlockPos pos, net.minecraft.block.Block block) {
        return client.world.isChunkLoaded(pos) && !client.world.getBlockState(pos).isOf(block);
    }

    private static void renderSettings(MatrixStack matrices, VertexConsumerProvider consumers, Anchor anchor,
            MinecraftClient client) {
        BlockPos pos = anchor.pos();
        Direction facing = anchor.facing();
        begin(matrices, pos, facing, 2, client);
        int[] x = {-46, 0, 46};
        drawCentered(matrices, consumers, client.textRenderer, "Таймер", x[0], -35, 0xFFFFAA00);
        drawCentered(matrices, consumers, client.textRenderer, "Локация", x[1], -35, 0xFFFFAA00);
        drawCentered(matrices, consumers, client.textRenderer, "Количество", x[2], -35, 0xFFFFAA00);
        for (int columnX : x) drawCentered(matrices, consumers, client.textRenderer, "▲", columnX, -22, 0xFFFFFFFF);

        drawCentered(matrices, consumers, client.textRenderer,
                String.format("%02d:%02d", PanelData.seconds / 60, PanelData.seconds % 60),
                x[0], -2, 0xFFFFFFFF);
        drawPhoto(matrices, consumers, ClientLocationPhotos.texture(PanelData.selectedLocation), -12, -13, 12, 11);
        drawCentered(matrices, consumers, client.textRenderer,
                fit(client.textRenderer, PanelData.locationName(PanelData.selectedLocation), 58),
                x[1], 13, 0xFFFFFFFF);
        drawCentered(matrices, consumers, client.textRenderer, "❤ " + PanelData.hearts, x[2], -2, 0xFFFF5555);

        for (int columnX : x) drawCentered(matrices, consumers, client.textRenderer, "▼", columnX, 25, 0xFFFFFFFF);
        matrices.pop();
    }

    private static void renderStart(MatrixStack matrices, VertexConsumerProvider consumers, Anchor anchor,
            MinecraftClient client) {
        BlockPos pos = anchor.pos();
        Direction facing = anchor.facing();
        beginSingle(matrices, pos, facing, client);
        String title = client.world.getBlockEntity(pos) instanceof StartBlockEntity start
                ? start.getTitle() : "Начать";
        drawCentered(matrices, consumers, client.textRenderer, title, 0, -4, 0xFFFFAA00);
        matrices.pop();
    }

    private static void begin(MatrixStack matrices, BlockPos pos, Direction facing, int height, MinecraftClient client) {
        var camera = client.gameRenderer.getCamera().getPos();
        Direction right = facing.rotateYCounterclockwise();
        matrices.push();
        // Anchor the text to the physical centre of the 3-wide multiblock, then
        // move it just in front of the blocks.  Previously the pixel offsets
        // were applied from the lower corner and placed the text above/behind
        // the panel.
        matrices.translate(
                pos.getX() - camera.x + .5 + right.getOffsetX(),
                pos.getY() - camera.y + height / 2.0,
                pos.getZ() - camera.z + .5 + right.getOffsetZ());
        matrices.translate(facing.getOffsetX() * .506, 0, facing.getOffsetZ() * .506);
        // This is the same horizontal convention used by sign text.  The old
        // 180-degree offset made both panels face away from their placer.
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing.asRotation()));
        matrices.scale(.021f, -.021f, .021f);
    }

    private static void beginSingle(MatrixStack matrices, BlockPos pos, Direction facing, MinecraftClient client) {
        var camera = client.gameRenderer.getCamera().getPos();
        matrices.push();
        matrices.translate(
                pos.getX() - camera.x + .5,
                pos.getY() - camera.y + .55,
                pos.getZ() - camera.z + .5);
        matrices.translate(facing.getOffsetX() * .506, 0, facing.getOffsetZ() * .506);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing.asRotation()));
        matrices.scale(.018f, -.018f, .018f);
    }

    private static void draw(MatrixStack matrices, VertexConsumerProvider consumers, TextRenderer renderer, String text, float x, float y, int color) {
        renderer.draw(text, x, y, color, false, matrices.peek().getPositionMatrix(), consumers,
                TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
    }

    private static void drawCentered(MatrixStack matrices, VertexConsumerProvider consumers, TextRenderer renderer,
            String text, float centerX, float y, int color) {
        draw(matrices, consumers, renderer, text, centerX - renderer.getWidth(text) / 2.0f, y, color);
    }

    private static String fit(TextRenderer renderer, String value, int maximumWidth) {
        String result = value == null ? "" : value;
        if (renderer.getWidth(result) <= maximumWidth) {
            return result;
        }
        while (!result.isEmpty() && renderer.getWidth(result + "…") > maximumWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "…";
    }

    private static void drawPhoto(MatrixStack matrices, VertexConsumerProvider consumers,
            net.minecraft.util.Identifier texture, float left, float top, float right, float bottom) {
        MatrixStack.Entry entry = matrices.peek();
        // Emissive entity layer bypasses directional/diffuse lighting and is
        // stable with Sodium, Indium and Iris shader pipelines.
        VertexConsumer vertices = consumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(texture));
        photoVertex(vertices, entry, left, bottom, 0.05f, 0, 1);
        photoVertex(vertices, entry, right, bottom, 0.05f, 1, 1);
        photoVertex(vertices, entry, right, top, 0.05f, 1, 0);
        photoVertex(vertices, entry, left, top, 0.05f, 0, 0);
    }

    private static void photoVertex(VertexConsumer vertices, MatrixStack.Entry entry,
            float x, float y, float z, float u, float v) {
        vertices.vertex(entry.getPositionMatrix(), x, y, z)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(entry.getNormalMatrix(), 0, 0, 1)
                .next();
    }
}
