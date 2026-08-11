package fable.hideseek.imba.client;

import fable.hideseek.imba.net.MaskBlockConfigNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.HashSet;
import java.util.Set;

public final class MaskBlockConfigClientNetworking {
    private MaskBlockConfigClientNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(MaskBlockConfigNetworking.SYNC,
                (client, handler, buf, responseSender) -> {
                    int size = Math.max(0, Math.min(buf.readVarInt(), 100000));
                    Set<String> nonFull = new HashSet<>();
                    for (int i = 0; i < size; i++) {
                        nonFull.add(buf.readString(256));
                    }
                    client.execute(() -> {
                        if (client.currentScreen instanceof MaskBlockConfigScreen screen) {
                            screen.applyServerState(nonFull);
                        }
                    });
                });
    }
}
