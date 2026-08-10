package fable.hideseek.imba.client;

import fable.hideseek.imba.net.WallClimbNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Client prediction state for the dedicated wall-climb channel. */
public final class WallClimbClientNetworking implements ClientModInitializer {
    private static final Map<UUID, Boolean> STATES = new HashMap<>();

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(WallClimbNetworking.STATE,
                (client, handler, buf, responseSender) -> {
                    boolean enabled = buf.readBoolean();
                    client.execute(() -> {
                        if (client.player != null) {
                            STATES.put(client.player.getUuid(), enabled);
                        }
                    });
                });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && !ClientMaskData.hasMask(client.player.getUuid())) {
                STATES.remove(client.player.getUuid());
            }
        });
    }

    public static boolean isEnabled(UUID uuid) {
        return STATES.getOrDefault(uuid, true);
    }
}
