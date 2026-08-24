package fable.hideseek.imba.client;

import fable.hideseek.imba.net.MessageSettingsNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MessageSettingsClientNetworking {
    private static final Map<String, Boolean> VALUES = new LinkedHashMap<>();

    private MessageSettingsClientNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(MessageSettingsNetworking.SYNC,
                (client, handler, buf, responseSender) -> {
                    int size = Math.max(0, Math.min(buf.readVarInt(), 1024));
                    LinkedHashMap<String, Boolean> values = new LinkedHashMap<>();
                    for (int i = 0; i < size; i++) {
                        values.put(buf.readString(128), buf.readBoolean());
                    }
                    client.execute(() -> {
                        VALUES.clear();
                        VALUES.putAll(values);
                        if (client.currentScreen instanceof MessageSettingsScreen screen) {
                            screen.applyServerState(values);
                        }
                    });
                });
    }

    public static Map<String, Boolean> snapshot() {
        return new LinkedHashMap<>(VALUES);
    }
}
