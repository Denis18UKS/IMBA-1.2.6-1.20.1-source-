package fable.hideseek.imba.client;

import fable.hideseek.imba.config.OverlayBarrierConfig;
import fable.hideseek.imba.net.OverlayBarrierNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class OverlayBarrierClientNetworking {
    private OverlayBarrierClientNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(OverlayBarrierNetworking.SYNC,
                (client, handler, buf, responseSender) -> {
                    int dimensions = Math.max(0, Math.min(buf.readVarInt(), 128));
                    Map<String, Set<Long>> values = new HashMap<>();
                    for (int d = 0; d < dimensions; d++) {
                        String dimension = buf.readString(128);
                        int count = Math.max(0, Math.min(buf.readVarInt(), 200000));
                        Set<Long> positions = new HashSet<>();
                        for (int i = 0; i < count; i++) positions.add(buf.readLong());
                        values.put(dimension, positions);
                    }
                    client.execute(() -> OverlayBarrierConfig.applyNetworkSnapshot(values));
                });
    }
}
