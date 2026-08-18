package fable.hideseek.imba.client;

import fable.hideseek.imba.net.PanelSettingsNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class PanelSettingsClientNetworking {
    private PanelSettingsClientNetworking() {}
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PanelSettingsNetworking.SYNC_LAYOUT,(client,handler,buf,responseSender)->{
            String timerLabel=buf.readString(32),heartsLabel=buf.readString(32);float timerTitleScale=buf.readFloat(),heartsTitleScale=buf.readFloat(),timerValueScale=buf.readFloat(),heartsValueScale=buf.readFloat(),arrowScale=buf.readFloat();int timerX=buf.readInt(),heartsX=buf.readInt(),titleY=buf.readInt(),upArrowY=buf.readInt(),valueY=buf.readInt(),downArrowY=buf.readInt();var timerUp=readHitbox(buf);var timerDown=readHitbox(buf);var heartsUp=readHitbox(buf);var heartsDown=readHitbox(buf);
            client.execute(()->{PanelData.timerLabel=timerLabel==null||timerLabel.isBlank()?"Таймер":timerLabel;PanelData.heartsLabel=heartsLabel==null||heartsLabel.isBlank()?"Сердца":heartsLabel;PanelData.timerTitleScale=timerTitleScale;PanelData.heartsTitleScale=heartsTitleScale;PanelData.timerValueScale=timerValueScale;PanelData.heartsValueScale=heartsValueScale;PanelData.arrowScale=arrowScale;PanelData.timerX=timerX;PanelData.heartsX=heartsX;PanelData.titleY=titleY;PanelData.upArrowY=upArrowY;PanelData.valueY=valueY;PanelData.downArrowY=downArrowY;PanelData.timerUpHitbox=timerUp;PanelData.timerDownHitbox=timerDown;PanelData.heartsUpHitbox=heartsUp;PanelData.heartsDownHitbox=heartsDown;if(client.currentScreen instanceof GameSettingsScreen screen)screen.applyPanelLayout();if(client.currentScreen instanceof PanelHitboxScreen screen)screen.applyPanelLayout();});
        });
        ClientPlayNetworking.registerGlobalReceiver(PanelSettingsNetworking.OPEN_SCREEN,(client,handler,buf,responseSender)->client.execute(()->client.setScreen(new GameSettingsScreen())));
    }
    private static fable.hideseek.imba.config.PanelSettingsConfig.Hitbox readHitbox(net.minecraft.network.PacketByteBuf buf){return new fable.hideseek.imba.config.PanelSettingsConfig.Hitbox(buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readFloat());}
}
