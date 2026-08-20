package fable.hideseek.imba.net;

import fable.hideseek.imba.config.MessageSettingsConfig;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Map;

public final class MessageSettingsNetworking {
    public static final Identifier REQUEST = new Identifier("imba", "messages_request");
    public static final Identifier SET = new Identifier("imba", "messages_set");
    public static final Identifier RESET = new Identifier("imba", "messages_reset");
    public static final Identifier SYNC = new Identifier("imba", "messages_sync");

    private MessageSettingsNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST, (server, player, handler, buf, responseSender) ->
                server.execute(() -> sendSync(player)));

        ServerPlayNetworking.registerGlobalReceiver(SET, (server, player, handler, buf, responseSender) -> {
            String id = buf.readString(128);
            boolean visible = buf.readBoolean();
            server.execute(() -> {
                if (!player.hasPermissionLevel(2)) {
                    player.sendMessage(Text.literal("§cДля настройки сообщений нужны права оператора"), true);
                    return;
                }
                MessageSettingsConfig.setVisible(id, visible);
                sendSync(player);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(RESET, (server, player, handler, buf, responseSender) ->
                server.execute(() -> {
                    if (!player.hasPermissionLevel(2)) {
                        player.sendMessage(Text.literal("§cДля настройки сообщений нужны права оператора"), true);
                        return;
                    }
                    MessageSettingsConfig.resetDefaults();
                    sendSync(player);
                    player.sendMessage(Text.literal("§eНастройки сообщений сброшены"), true);
                }));
    }

    public static void sendSync(ServerPlayerEntity player) {
        Map<String, Boolean> values = MessageSettingsConfig.snapshot();
        PacketByteBuf out = PacketByteBufs.create();
        out.writeVarInt(values.size());
        for (Map.Entry<String, Boolean> entry : values.entrySet()) {
            out.writeString(entry.getKey(), 128);
            out.writeBoolean(entry.getValue());
        }
        ServerPlayNetworking.send(player, SYNC, out);
    }
}
