package fable.hideseek.imba.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Server-owned PNG storage shared with every connected client. */
public final class LocationPhotoStorage {
    public static final int LOCATION_COUNT = 12;
    public static final int MAX_PHOTO_BYTES = 512 * 1024;
    private static final Path DIRECTORY =
            FabricLoader.getInstance().getConfigDir().resolve("imba_location_photos");

    private LocationPhotoStorage() {}

    public static byte[] read(int location) {
        if (!validLocation(location)) return null;
        Path path = path(location);
        try {
            if (!Files.isRegularFile(path)) return null;
            byte[] bytes = Files.readAllBytes(path);
            return isValidPng(bytes) ? bytes : null;
        } catch (IOException ignored) {
            return null;
        }
    }

    public static boolean save(int location, byte[] bytes) {
        if (!validLocation(location) || !isValidPng(bytes)) return false;
        try {
            Files.createDirectories(DIRECTORY);
            Path target = path(location);
            Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
            Files.write(temporary, bytes);
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    public static void delete(int location) {
        if (!validLocation(location)) return;
        try {
            Files.deleteIfExists(path(location));
        } catch (IOException ignored) {}
    }

    public static void deleteAll() {
        for (int i = 0; i < LOCATION_COUNT; i++) delete(i);
    }

    private static boolean validLocation(int location) {
        return location >= 0 && location < LOCATION_COUNT;
    }

    private static Path path(int location) {
        return DIRECTORY.resolve("location_" + (location + 1) + ".png");
    }

    private static boolean isValidPng(byte[] bytes) {
        return bytes != null
                && bytes.length >= 8
                && bytes.length <= MAX_PHOTO_BYTES
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A;
    }
}
