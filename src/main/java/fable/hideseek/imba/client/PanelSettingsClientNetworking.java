package fable.hideseek.imba.client;

import fable.hideseek.imba.net.PanelSettingsNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class PanelSettingsClientNetworking {
    private PanelSettingsClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PanelSettingsNetworking.SYNC_LABEL,
                (client, handler, buf, responseSender) -> {
                    String label = buf.readString(32);
                    client.execute(() -> {
                        PanelData.heartsLabel = label == null || label.isBlank() ? "Сердца" : label;
                        if (client.currentScreen instanceof GameSettingsScreen screen) {
                            screen.applyHeartsLabel(PanelData.heartsLabel);
                        }
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(PanelSettingsNetworking.OPEN_SCREEN,
                (client, handler, buf, responseSender) -> client.execute(() -> client.setScreen(new GameSettingsScreen())));
    }
}
