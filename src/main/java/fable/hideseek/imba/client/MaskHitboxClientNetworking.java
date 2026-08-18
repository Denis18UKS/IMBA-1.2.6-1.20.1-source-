package fable.hideseek.imba.client;

import fable.hideseek.imba.config.MaskHitboxConfig;
import fable.hideseek.imba.net.MaskHitboxNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class MaskHitboxClientNetworking {
    private MaskHitboxClientNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(MaskHitboxNetworking.SYNC,
                (client, handler, buf, responseSender) -> {
                    int nonFullCount = Math.max(0, Math.min(buf.readVarInt(), 100000));
                    Set<String> nonFull = new HashSet<>();
                    for (int i = 0; i < nonFullCount; i++) nonFull.add(buf.readString(256));

                    int boxCount = Math.max(0, Math.min(buf.readVarInt(), 100000));
                    Map<String, MaskHitboxConfig.BoxSpec> boxes = new HashMap<>();
                    for (int i = 0; i < boxCount; i++) {
                        String id = buf.readString(256);
                        boxes.put(id, new MaskHitboxConfig.BoxSpec(
                                buf.readInt(), buf.readInt(), buf.readInt(),
                                buf.readInt(), buf.readInt(), buf.readInt()));
                    }

                    client.execute(() -> {
                        MaskHitboxClientState.apply(nonFull, boxes);
                        if (client.currentScreen instanceof MaskHitboxScreen screen) screen.rebuildFromState();
                        if (client.currentScreen instanceof MaskHitboxEditScreen screen) screen.reloadFromState();
                    });
                });
    }
}
