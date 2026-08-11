package fable.hideseek.imba.client;

import fable.hideseek.imba.config.MaskAutoPositionConfig;
import fable.hideseek.imba.net.MaskAutoPositionNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.HashMap;
import java.util.Map;

public final class MaskAutoPositionClientNetworking {
    private MaskAutoPositionClientNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(MaskAutoPositionNetworking.SYNC,
                (client, handler, buf, responseSender) -> {
                    int size = Math.max(0, Math.min(buf.readVarInt(), 100000));
                    Map<String, MaskAutoPositionConfig.Offset> values = new HashMap<>();
                    for (int i = 0; i < size; i++) {
                        String id = buf.readString(256);
                        int x = buf.readInt();
                        int y = buf.readInt();
                        int z = buf.readInt();
                        values.put(id, new MaskAutoPositionConfig.Offset(x, y, z));
                    }

                    client.execute(() -> {
                        if (client.currentScreen instanceof MaskAutoPositionScreen screen) {
                            screen.applyServerState(values);
                        }
                    });
                });
    }
}
