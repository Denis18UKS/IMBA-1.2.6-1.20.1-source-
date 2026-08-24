package fable.hideseek.imba.client;

import fable.hideseek.imba.config.PanelHitboxConfig;
import fable.hideseek.imba.net.PanelHitboxNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.EnumMap;

public final class PanelHitboxClientNetworking {
    private PanelHitboxClientNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PanelHitboxNetworking.SYNC,
                (client, handler, buf, responseSender) -> {
                    EnumMap<PanelHitboxConfig.Arrow, PanelHitboxConfig.Rect> values =
                            new EnumMap<>(PanelHitboxConfig.Arrow.class);
                    for (PanelHitboxConfig.Arrow arrow : PanelHitboxConfig.Arrow.values()) {
                        values.put(arrow, new PanelHitboxConfig.Rect(
                                buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt()));
                    }
                    client.execute(() -> {
                        PanelHitboxClientState.apply(values);
                        if (client.currentScreen instanceof PanelHitboxScreen screen) {
                            screen.applyServerState(values);
                        }
                    });
                });
    }
}
