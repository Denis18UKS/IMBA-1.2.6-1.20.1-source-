package fable.hideseek.imba.net;

import fable.hideseek.imba.config.PanelSettingsConfig;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/** Small independent channel for the editable world-panel header. */
public final class PanelSettingsNetworking {
    public static final Identifier SET_LABEL = new Identifier("imba", "panel_label_set");
    public static final Identifier SYNC_LABEL = new Identifier("imba", "panel_label_sync");
    public static final Identifier OPEN_SCREEN = new Identifier("imba", "panel_settings_open");

    private PanelSettingsNetworking() {}

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(SET_LABEL, (server, player, handler, buf, responseSender) -> {
            String label = buf.readString(32);
            server.execute(() -> {
                if (!player.hasPermissionLevel(2)) return;
                PanelSettingsConfig.setHeartsLabel(label);
                broadcastSync(server);
            });
        });
    }

    public static void sendOpen(ServerPlayerEntity player) {
        sendSync(player);
        ServerPlayNetworking.send(player, OPEN_SCREEN, PacketByteBufs.empty());
    }

    public static void sendSync(ServerPlayerEntity player) {
        var buf = PacketByteBufs.create();
        buf.writeString(PanelSettingsConfig.heartsLabel(), 32);
        ServerPlayNetworking.send(player, SYNC_LABEL, buf);
    }

    public static void broadcastSync(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) sendSync(player);
    }
}
