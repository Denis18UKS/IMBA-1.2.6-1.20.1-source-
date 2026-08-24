package fable.hideseek.imba.net;

import fable.hideseek.imba.config.PanelSettingsConfig;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/** Server-authoritative synchronization for the editable 3x3 panel layout. */
public final class PanelSettingsNetworking {
    public static final Identifier SET_LAYOUT = new Identifier("imba", "panel_layout_set");
    public static final Identifier RESET_LAYOUT = new Identifier("imba", "panel_layout_reset");
    public static final Identifier SYNC_LAYOUT = new Identifier("imba", "panel_layout_sync");
    public static final Identifier OPEN_SCREEN = new Identifier("imba", "panel_settings_open");

    private PanelSettingsNetworking() {}

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(SET_LAYOUT, (server, player, handler, buf, responseSender) -> {
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
            server.execute(() -> {
                if (!player.hasPermissionLevel(2)) return;
                PanelSettingsConfig.setLayout(timerLabel, heartsLabel,
                        timerTitleScale, heartsTitleScale, timerValueScale, heartsValueScale,
                        arrowScale, timerX, heartsX, titleY, upArrowY, valueY, downArrowY);
                broadcastSync(server);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(RESET_LAYOUT, (server, player, handler, buf, responseSender) ->
                server.execute(() -> {
                    if (!player.hasPermissionLevel(2)) return;
                    PanelSettingsConfig.resetDefaults();
                    broadcastSync(server);
                }));
    }

    public static void sendOpen(ServerPlayerEntity player) {
        sendSync(player);
        ServerPlayNetworking.send(player, OPEN_SCREEN, PacketByteBufs.empty());
    }

    public static void sendSync(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(PanelSettingsConfig.timerLabel(), 32);
        buf.writeString(PanelSettingsConfig.heartsLabel(), 32);
        buf.writeFloat(PanelSettingsConfig.timerTitleScale());
        buf.writeFloat(PanelSettingsConfig.heartsTitleScale());
        buf.writeFloat(PanelSettingsConfig.timerValueScale());
        buf.writeFloat(PanelSettingsConfig.heartsValueScale());
        buf.writeFloat(PanelSettingsConfig.arrowScale());
        buf.writeInt(PanelSettingsConfig.timerX());
        buf.writeInt(PanelSettingsConfig.heartsX());
        buf.writeInt(PanelSettingsConfig.titleY());
        buf.writeInt(PanelSettingsConfig.upArrowY());
        buf.writeInt(PanelSettingsConfig.valueY());
        buf.writeInt(PanelSettingsConfig.downArrowY());
        ServerPlayNetworking.send(player, SYNC_LAYOUT, buf);
    }

    public static void broadcastSync(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) sendSync(player);
    }
}
