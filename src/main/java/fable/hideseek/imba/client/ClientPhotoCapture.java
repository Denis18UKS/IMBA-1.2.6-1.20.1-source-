package fable.hideseek.imba.client;

import fable.hideseek.imba.net.MaskNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.Text;

import java.io.IOException;

/** Captures a clean, centred square after the camera GUI has closed. */
public final class ClientPhotoCapture {
    private static int pendingLocation = -1;
    private static int delayTicks;

    private ClientPhotoCapture() {}

    public static void schedule(int location) {
        pendingLocation = location;
        delayTicks = 2;
        MinecraftClient.getInstance().setScreen(null);
    }

    public static void tick(MinecraftClient client) {
        if (pendingLocation < 0 || client.player == null || client.world == null) return;
        if (delayTicks-- > 0) return;

        int location = pendingLocation;
        pendingLocation = -1;
        NativeImage screenshot = null;
        NativeImage square = null;
        try {
            screenshot = ScreenshotRecorder.takeScreenshot(client.getFramebuffer());
            int sourceSize = Math.min(screenshot.getWidth(), screenshot.getHeight());
            int targetSize = Math.min(256, sourceSize);
            int sourceX = (screenshot.getWidth() - sourceSize) / 2;
            int sourceY = (screenshot.getHeight() - sourceSize) / 2;
            square = new NativeImage(targetSize, targetSize, false);

            for (int y = 0; y < targetSize; y++) {
                int sampleY = sourceY + y * sourceSize / targetSize;
                for (int x = 0; x < targetSize; x++) {
                    int sampleX = sourceX + x * sourceSize / targetSize;
                    square.setColor(x, y, screenshot.getColor(sampleX, sampleY));
                }
            }

            byte[] png = square.getBytes();
            var buf = PacketByteBufs.create();
            buf.writeVarInt(location);
            buf.writeByteArray(png);
            ClientPlayNetworking.send(MaskNetworking.LOCATION_PHOTO_UPLOAD_PACKET, buf);
            client.player.sendMessage(Text.literal("§aФото локации " + (location + 1) + " сохранено"), true);
        } catch (IOException | RuntimeException e) {
            client.player.sendMessage(Text.literal("§cНе удалось сделать фотографию"), true);
        } finally {
            if (screenshot != null) screenshot.close();
            if (square != null) square.close();
        }
    }
}
