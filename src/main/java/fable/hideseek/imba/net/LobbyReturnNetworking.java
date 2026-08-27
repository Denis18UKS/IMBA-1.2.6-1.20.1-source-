package fable.hideseek.imba.net;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/** Synchronizes the full-screen blackout used while a round returns to the lobby. */
public final class LobbyReturnNetworking {
    public static final Identifier RETURN_BLACKOUT_PACKET = new Identifier("imba", "return_blackout");

    private LobbyReturnNetworking() {
    }

    public static void broadcastReturnBlackout(MinecraftServer server, boolean enabled) {
        if (server == null) {
            return;
        }

        for (ServerPlayerEntity player : PlayerLookup.all(server)) {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeBoolean(enabled);
            ServerPlayNetworking.send(player, RETURN_BLACKOUT_PACKET, buf);
        }
    }
}
