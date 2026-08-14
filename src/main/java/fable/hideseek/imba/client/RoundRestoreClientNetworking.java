package fable.hideseek.imba.client;

import fable.hideseek.imba.net.RoundRestoreNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import java.util.ArrayList;
import java.util.List;

public final class RoundRestoreClientNetworking {
    public record Layer(String name, boolean enabled, int blocks) {}
    private static final List<Layer> LAYERS = new ArrayList<>();
    private RoundRestoreClientNetworking() {}
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(RoundRestoreNetworking.SYNC, (client, handler, buf, sender) -> {
            int count = Math.max(0, Math.min(buf.readVarInt(), 1000));
            List<Layer> next = new ArrayList<>();
            for (int i=0;i<count;i++) next.add(new Layer(buf.readString(64), buf.readBoolean(), buf.readVarInt()));
            client.execute(() -> {
                LAYERS.clear(); LAYERS.addAll(next);
                if (client.currentScreen instanceof StructureLayerScreen screen) screen.applyLayers(List.copyOf(LAYERS));
            });
        });
    }
    public static List<Layer> layers() { return List.copyOf(LAYERS); }
}
