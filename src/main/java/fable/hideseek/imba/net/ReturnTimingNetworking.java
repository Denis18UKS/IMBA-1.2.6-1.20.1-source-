package fable.hideseek.imba.net;

import fable.hideseek.imba.config.ReturnTimingConfig;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class ReturnTimingNetworking {
    public static final Identifier REQUEST = new Identifier("imba", "return_timing_request");
    public static final Identifier SET = new Identifier("imba", "return_timing_set");
    public static final Identifier SYNC = new Identifier("imba", "return_timing_sync");

    private ReturnTimingNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST, (server, player, handler, buf, sender) ->
                server.execute(() -> {
                    if (player.hasPermissionLevel(2)) {
                        sendSync(player);
                    }
                }));
        ServerPlayNetworking.registerGlobalReceiver(SET, (server, player, handler, buf, sender) -> {
            int preFade = buf.readVarInt();
            int preTeleport = buf.readVarInt();
            server.execute(() -> {
                if (!player.hasPermissionLevel(2)) {
                    return;
                }
                ReturnTimingConfig.set(preFade, preTeleport);
                broadcast(server);
            });
        });
    }

    public static void sendSync(ServerPlayerEntity player) {
        PacketByteBuf out = PacketByteBufs.create();
        out.writeVarInt(ReturnTimingConfig.preFadeTicks());
        out.writeVarInt(ReturnTimingConfig.preTeleportTicks());
        ServerPlayNetworking.send(player, SYNC, out);
    }

    public static void broadcast(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            sendSync(player);
        }
    }
}
