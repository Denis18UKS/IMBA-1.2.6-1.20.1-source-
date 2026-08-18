package fable.hideseek.imba.net;

import fable.hideseek.imba.config.PanelHitboxConfig;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.EnumMap;

/** Server-authoritative synchronization for exact panel arrow click rectangles. */
public final class PanelHitboxNetworking {
    public static final Identifier REQUEST = new Identifier("imba", "panel_hitboxes_request");
    public static final Identifier SET = new Identifier("imba", "panel_hitboxes_set");
    public static final Identifier RESET = new Identifier("imba", "panel_hitboxes_reset");
    public static final Identifier SYNC = new Identifier("imba", "panel_hitboxes_sync");

    private PanelHitboxNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST, (server, player, handler, buf, responseSender) ->
                server.execute(() -> sendSync(player)));

        ServerPlayNetworking.registerGlobalReceiver(SET, (server, player, handler, buf, responseSender) -> {
            EnumMap<PanelHitboxConfig.Arrow, PanelHitboxConfig.Rect> values = new EnumMap<>(PanelHitboxConfig.Arrow.class);
            for (PanelHitboxConfig.Arrow arrow : PanelHitboxConfig.Arrow.values()) {
                int x = buf.readInt();
                int y = buf.readInt();
                int width = buf.readInt();
                int height = buf.readInt();
                values.put(arrow, new PanelHitboxConfig.Rect(x, y, width, height));
            }
            server.execute(() -> {
                if (!player.hasPermissionLevel(2)) {
                    player.sendMessage(Text.literal("§cДля настройки хитбоксов панели нужны права оператора"), true);
                    return;
                }
                PanelHitboxConfig.setAll(values);
                sendSync(player);
                player.sendMessage(Text.literal("§aХитбоксы стрелок панели сохранены"), true);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(RESET, (server, player, handler, buf, responseSender) ->
                server.execute(() -> {
                    if (!player.hasPermissionLevel(2)) {
                        player.sendMessage(Text.literal("§cДля настройки хитбоксов панели нужны права оператора"), true);
                        return;
                    }
                    PanelHitboxConfig.resetDefaults();
                    sendSync(player);
                    player.sendMessage(Text.literal("§eХитбоксы стрелок панели сброшены"), true);
                }));
    }

    public static void sendSync(ServerPlayerEntity player) {
        PacketByteBuf out = PacketByteBufs.create();
        for (PanelHitboxConfig.Arrow arrow : PanelHitboxConfig.Arrow.values()) {
            PanelHitboxConfig.Rect rect = PanelHitboxConfig.get(arrow);
            out.writeInt(rect.x);
            out.writeInt(rect.y);
            out.writeInt(rect.width);
            out.writeInt(rect.height);
        }
        ServerPlayNetworking.send(player, SYNC, out);
    }
}
