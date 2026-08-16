package fable.hideseek.imba.client;

import fable.hideseek.imba.net.PanelSettingsNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class PanelSettingsClientNetworking {
    private PanelSettingsClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PanelSettingsNetworking.SYNC_LAYOUT,
                (client, handler, buf, responseSender) -> {
                    String timerLabel = buf.readString(32);
                    String heartsLabel = buf.readString(32);
                    float timerTitleScale = buf.readFloat();
                    float heartsTitleScale = buf.readFloat();
                    float timerValueScale = buf.readFloat();
                    float heartsValueScale = buf.readFloat();
                    float arrowScale = buf.readFloat();
                    int timerX = buf.readInt();
                    int heartsX = buf.readInt();
                    int titleY = buf.readInt();
                    int upArrowY = buf.readInt();
                    int valueY = buf.readInt();
                    int downArrowY = buf.readInt();
                    client.execute(() -> {
                        PanelData.timerLabel = timerLabel == null || timerLabel.isBlank() ? "Таймер" : timerLabel;
                        PanelData.heartsLabel = heartsLabel == null || heartsLabel.isBlank() ? "Сердца" : heartsLabel;
                        PanelData.timerTitleScale = timerTitleScale;
                        PanelData.heartsTitleScale = heartsTitleScale;
                        PanelData.timerValueScale = timerValueScale;
                        PanelData.heartsValueScale = heartsValueScale;
                        PanelData.arrowScale = arrowScale;
                        PanelData.timerX = timerX;
                        PanelData.heartsX = heartsX;
                        PanelData.titleY = titleY;
                        PanelData.upArrowY = upArrowY;
                        PanelData.valueY = valueY;
                        PanelData.downArrowY = downArrowY;
                        if (client.currentScreen instanceof GameSettingsScreen screen) {
                            screen.applyPanelLayout();
                        }
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(PanelSettingsNetworking.OPEN_SCREEN,
                (client, handler, buf, responseSender) -> client.execute(() -> client.setScreen(new GameSettingsScreen())));
    }
}
