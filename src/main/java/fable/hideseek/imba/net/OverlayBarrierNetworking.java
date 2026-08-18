package fable.hideseek.imba.net;

import fable.hideseek.imba.config.OverlayBarrierConfig;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Set;

public final class OverlayBarrierNetworking {
    public static final Identifier SYNC = new Identifier("imba", "overlay_barriers_sync");

    private OverlayBarrierNetworking() {
    }

    public static void register() {
        // No client-to-server packet is needed: placement is handled by the
        // ordinary server-side UseBlockCallback of the admin tool.
    }

    public static void sendSync(ServerPlayerEntity player) {
        Map<String, Set<Long>> values = OverlayBarrierConfig.snapshot();
        PacketByteBuf out = PacketByteBufs.create();
        out.writeVarInt(values.size());
        for (Map.Entry<String, Set<Long>> entry : values.entrySet()) {
            out.writeString(entry.getKey(), 128);
            out.writeVarInt(entry.getValue().size());
            for (long packed : entry.getValue()) out.writeLong(packed);
        }
        ServerPlayNetworking.send(player, SYNC, out);
    }

    public static void broadcast(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) sendSync(player);
    }
}
