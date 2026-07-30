package fable.hideseek.imba.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Dynamic client textures received from the server photo store. */
public final class ClientLocationPhotos {
    private static final Map<Integer, Identifier> TEXTURES = new HashMap<>();
    private static final Identifier PLACEHOLDER =
            new Identifier("minecraft", "textures/block/light_gray_concrete.png");

    private ClientLocationPhotos() {}

    public static Identifier texture(int location) {
        return TEXTURES.getOrDefault(location, PLACEHOLDER);
    }

    public static boolean has(int location) {
        return TEXTURES.containsKey(location);
    }

    public static void apply(int location, byte[] png) {
        remove(location);
        if (png == null || png.length == 0) return;

        try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(png));
            Identifier id = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture(
                    "imba_location_" + (location + 1),
                    new NativeImageBackedTexture(image));
            TEXTURES.put(location, id);
        } catch (IOException ignored) {}
    }

    public static void remove(int location) {
        Identifier old = TEXTURES.remove(location);
        if (old != null) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(old);
        }
    }

    public static void removeAll() {
        for (int location : java.util.List.copyOf(TEXTURES.keySet())) remove(location);
    }
}
