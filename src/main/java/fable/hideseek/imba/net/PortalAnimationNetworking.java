package fable.hideseek.imba.net;

import fable.hideseek.imba.config.PortalAnimationConfig;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class PortalAnimationNetworking {
    public static final Identifier REQUEST = new Identifier("imba", "portal_animation_request");
    public static final Identifier SET = new Identifier("imba", "portal_animation_set");
    public static final Identifier SYNC = new Identifier("imba", "portal_animation_sync");

    private PortalAnimationNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST, (server, player, handler, buf, sender) ->
                server.execute(() -> {
                    if (player.hasPermissionLevel(2)) {
                        sendSync(player);
                    }
                }));
        ServerPlayNetworking.registerGlobalReceiver(SET, (server, player, handler, buf, sender) -> {
            int freezeTicks = buf.readVarInt();
            server.execute(() -> {
                if (!player.hasPermissionLevel(2)) {
                    return;
                }
                PortalAnimationConfig.setFreezeTicks(freezeTicks);
                broadcast(server);
            });
        });
    }

    public static void sendSync(ServerPlayerEntity player) {
        PacketByteBuf out = PacketByteBufs.create();
        out.writeVarInt(PortalAnimationConfig.freezeTicks());
        ServerPlayNetworking.send(player, SYNC, out);
    }

    public static void broadcast(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            sendSync(player);
        }
    }
}
