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
    private static final float HEADER_Y = -36.0F;
    private static final float TOP_ARROW_Y = -22.0F;
    private static final float PHOTO_TOP = -14.0F;
    private static final float PHOTO_BOTTOM = 8.0F;
    private static final float SINGLE_LOCATION_Y = 20.0F;
    private static final float FIRST_LOCATION_LINE_Y = 14.0F;
    private static final float SECOND_LOCATION_LINE_Y = 24.0F;
    private static final float BOTTOM_ARROW_Y = 38.0F;
    private static final float HEADER_MAX_WIDTH = 44.0F;
    private static final float LOCATION_MAX_WIDTH = 58.0F;

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
            if (scanCooldown-- <= 0) {
                scan(client);
                scanCooldown = 20;
            }
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

    private static boolean isLoadedAndRemoved(MinecraftClient client, BlockPos pos,
            net.minecraft.block.Block block) {
        return client.world.isChunkLoaded(pos) && !client.world.getBlockState(pos).isOf(block);
    }

    private static void renderSettings(MatrixStack matrices, VertexConsumerProvider consumers, Anchor anchor,
            MinecraftClient client) {
        BlockPos pos = anchor.pos();
        Direction facing = anchor.facing();
        begin(matrices, pos, facing, 2, client);

        int[] columnX = {-46, 0, 46};
        String[] headers = {"Таймер", "Локация", "Количество"};
        float sharedHeaderScale = sharedScale(client.textRenderer, headers, HEADER_MAX_WIDTH);

        for (int i = 0; i < headers.length; i++) {
            drawCenteredAtScale(
                    matrices,
                    consumers,
                    client.textRenderer,
                    headers[i],
                    columnX[i],
                    HEADER_Y,
                    sharedHeaderScale,
                    0xFFFFAA00);
        }

        for (int x : columnX) {
            drawCentered(matrices, consumers, client.textRenderer, "▲", x, TOP_ARROW_Y, 0xFFFFFFFF);
        }

        drawCentered(
                matrices,
                consumers,
                client.textRenderer,
                String.format("%02d:%02d", PanelData.seconds / 60, PanelData.seconds % 60),
                columnX[0],
                -2,
                0xFFFFFFFF);

        drawPhoto(
                matrices,
                consumers,
                ClientLocationPhotos.texture(PanelData.selectedLocation),
                -12,
                PHOTO_TOP,
                12,
                PHOTO_BOTTOM);

        drawLocationName(
                matrices,
                consumers,
                client.textRenderer,
                PanelData.locationName(PanelData.selectedLocation),
                columnX[1],
                LOCATION_MAX_WIDTH,
                0xFFFFFFFF);

        drawCentered(
                matrices,
                consumers,
                client.textRenderer,
                "❤ " + PanelData.hearts,
                columnX[2],
                -2,
                0xFFFF5555);

        for (int x : columnX) {
            drawCentered(matrices, consumers, client.textRenderer, "▼", x, BOTTOM_ARROW_Y, 0xFFFFFFFF);
        }

        matrices.pop();
    }

    private static void renderStart(MatrixStack matrices, VertexConsumerProvider consumers, Anchor anchor,
            MinecraftClient client) {
        BlockPos pos = anchor.pos();
        Direction facing = anchor.facing();
        beginSingle(matrices, pos, facing, client);
        String title = client.world.getBlockEntity(pos) instanceof StartBlockEntity start
                ? start.getTitle()
                : "Начать";
        drawCentered(matrices, consumers, client.textRenderer, title, 0, -4, 0xFFFFAA00);
        matrices.pop();
    }

    private static void begin(MatrixStack matrices, BlockPos pos, Direction facing, int height,
            MinecraftClient client) {
        var camera = client.gameRenderer.getCamera().getPos();
        Direction right = facing.rotateYCounterclockwise();
        matrices.push();
        matrices.translate(
                pos.getX() - camera.x + .5 + right.getOffsetX(),
                pos.getY() - camera.y + height / 2.0,
                pos.getZ() - camera.z + .5 + right.getOffsetZ());
        matrices.translate(facing.getOffsetX() * .506, 0, facing.getOffsetZ() * .506);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing.asRotation()));
        matrices.scale(.021f, -.021f, .021f);
    }

    private static void beginSingle(MatrixStack matrices, BlockPos pos, Direction facing,
            MinecraftClient client) {
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

    private static void draw(MatrixStack matrices, VertexConsumerProvider consumers, TextRenderer renderer,
            String text, float x, float y, int color) {
        renderer.draw(
                text,
                x,
                y,
                color,
                false,
                matrices.peek().getPositionMatrix(),
                consumers,
                TextRenderer.TextLayerType.NORMAL,
                0,
                LightmapTextureManager.MAX_LIGHT_COORDINATE);
    }

    private static void drawCentered(MatrixStack matrices, VertexConsumerProvider consumers, TextRenderer renderer,
            String text, float centerX, float y, int color) {
        draw(matrices, consumers, renderer, text, centerX - renderer.getWidth(text) / 2.0f, y, color);
    }

    private static float sharedScale(TextRenderer renderer, String[] values, float maximumWidth) {
        int widest = 1;
        for (String value : values) {
            widest = Math.max(widest, renderer.getWidth(value == null ? "" : value));
        }
        return Math.min(1.0F, maximumWidth / widest);
    }

    private static void drawCenteredAtScale(MatrixStack matrices,
            VertexConsumerProvider consumers,
            TextRenderer renderer,
            String text,
            float centerX,
            float y,
            float scale,
            int color) {
        String value = text == null ? "" : text;
        float width = Math.max(1.0F, renderer.getWidth(value));
        matrices.push();
        matrices.translate(centerX, y, 0.0F);
        matrices.scale(scale, scale, 1.0F);
        draw(matrices, consumers, renderer, value, -width / 2.0F, 0.0F, color);
        matrices.pop();
    }

    private static void drawScaledCentered(MatrixStack matrices,
            VertexConsumerProvider consumers,
            TextRenderer renderer,
            String text,
            float centerX,
            float y,
            float maximumWidth,
            int color) {
        String value = text == null ? "" : text;
        float width = Math.max(1.0F, renderer.getWidth(value));
        float scale = Math.min(1.0F, maximumWidth / width);
        drawCenteredAtScale(matrices, consumers, renderer, value, centerX, y, scale, color);
    }

    private static void drawLocationName(MatrixStack matrices,
            VertexConsumerProvider consumers,
            TextRenderer renderer,
            String text,
            float centerX,
            float maximumWidth,
            int color) {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) {
            return;
        }

        if (renderer.getWidth(value) <= maximumWidth) {
            drawCentered(matrices, consumers, renderer, value, centerX, SINGLE_LOCATION_Y, color);
            return;
        }

        int split = bestSplit(value);
        if (split <= 0 || split >= value.length()) {
            drawScaledCentered(
                    matrices,
                    consumers,
                    renderer,
                    value,
                    centerX,
                    SINGLE_LOCATION_Y,
                    maximumWidth,
                    color);
            return;
        }

        String first = value.substring(0, split).trim();
        String second = value.substring(split).trim();
        drawScaledCentered(
                matrices,
                consumers,
                renderer,
                first,
                centerX,
                FIRST_LOCATION_LINE_Y,
                maximumWidth,
                color);
        drawScaledCentered(
                matrices,
                consumers,
                renderer,
                second,
                centerX,
                SECOND_LOCATION_LINE_Y,
                maximumWidth,
                color);
    }

    private static int bestSplit(String value) {
        int middle = value.length() / 2;
        for (int offset = 0; offset < value.length(); offset++) {
            int left = middle - offset;
            if (left > 0 && Character.isWhitespace(value.charAt(left))) {
                return left;
            }
            int right = middle + offset;
            if (right < value.length() && Character.isWhitespace(value.charAt(right))) {
                return right;
            }
        }
        return -1;
    }

    private static void drawPhoto(MatrixStack matrices, VertexConsumerProvider consumers,
            net.minecraft.util.Identifier texture, float left, float top, float right, float bottom) {
        MatrixStack.Entry entry = matrices.peek();
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
