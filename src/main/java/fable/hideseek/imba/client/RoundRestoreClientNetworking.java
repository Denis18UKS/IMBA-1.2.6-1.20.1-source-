package fable.hideseek.imba.client;

import fable.hideseek.imba.net.RoundRestoreNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.ArrayList;
import java.util.List;

public final class RoundRestoreClientNetworking {
    public record Single(int index, String world, int x, int y, int z, String block, int inventoryItems) {}
    public record Layer(int index, String name, boolean enabled, int blocks, String world,
                        int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {}

    private static final List<Single> SINGLES = new ArrayList<>();
    private static final List<Layer> LAYERS = new ArrayList<>();

    private RoundRestoreClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(RoundRestoreNetworking.SYNC, (client, handler, buf, sender) -> {
            int singleCount = Math.max(0, Math.min(buf.readVarInt(), 10_000));
            List<Single> nextSingles = new ArrayList<>();
            for (int i = 0; i < singleCount; i++) {
                nextSingles.add(new Single(
                        buf.readInt(), buf.readString(128), buf.readInt(), buf.readInt(), buf.readInt(),
                        buf.readString(128), buf.readVarInt()));
            }

            int layerCount = Math.max(0, Math.min(buf.readVarInt(), 1000));
            List<Layer> nextLayers = new ArrayList<>();
            for (int i = 0; i < layerCount; i++) {
                nextLayers.add(new Layer(
                        buf.readInt(), buf.readString(64), buf.readBoolean(), buf.readVarInt(), buf.readString(128),
                        buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt()));
            }

            client.execute(() -> {
                SINGLES.clear();
                SINGLES.addAll(nextSingles);
                LAYERS.clear();
                LAYERS.addAll(nextLayers);
                if (client.currentScreen instanceof StructureLayerScreen screen) {
                    screen.applyLayers(List.copyOf(LAYERS));
                }
                if (client.currentScreen instanceof BlockRestoreScreen screen) {
                    screen.applySingles(List.copyOf(SINGLES));
                }
            });
        });
    }

    public static List<Single> singles() { return List.copyOf(SINGLES); }
    public static List<Layer> layers() { return List.copyOf(LAYERS); }
}
