package fable.hideseek.imba.net;

import fable.hideseek.imba.config.MaskHitboxConfig;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class MaskHitboxNetworking {
    public static final Identifier REQUEST = new Identifier("imba", "mask_hitbox_request"),
            SET = new Identifier("imba", "mask_hitbox_set"),
            RESET = new Identifier("imba", "mask_hitbox_reset"),
            SYNC = new Identifier("imba", "mask_hitbox_sync");

    private MaskHitboxNetworking() {}

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST, (server, player, handler, buf, sender) ->
                server.execute(() -> { if (player.hasPermissionLevel(2)) sendSync(player); }));

        ServerPlayNetworking.registerGlobalReceiver(SET, (server, player, handler, buf, sender) -> {
            String raw = buf.readString(256);
            var bounds = new MaskHitboxConfig.Bounds(buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat());
            server.execute(() -> {
                if (!player.hasPermissionLevel(2)) return;
                Identifier id = Identifier.tryParse(raw);
                if (id == null || !Registries.BLOCK.containsId(id)) return;
                MaskHitboxConfig.set(id, bounds);
                broadcast(server);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(RESET, (server, player, handler, buf, sender) -> {
            String raw = buf.readString(256);
            server.execute(() -> {
                if (!player.hasPermissionLevel(2)) return;
                Identifier id = Identifier.tryParse(raw);
                if (id != null && Registries.BLOCK.containsId(id)) {
                    MaskHitboxConfig.reset(id);
                    broadcast(server);
                }
            });
        });
    }

    public static void sendSync(ServerPlayerEntity player) {
        PacketByteBuf out = PacketByteBufs.create();
        var allBlocks = Registries.BLOCK.getIds().stream().map(Identifier::toString).sorted().toList();
        var custom = MaskHitboxConfig.snapshot();
        out.writeVarInt(allBlocks.size());
        for (String raw : allBlocks) {
            Identifier id = Identifier.tryParse(raw);
            var b = custom.getOrDefault(raw, MaskHitboxConfig.defaultBounds(Registries.BLOCK.get(id)));
            out.writeString(raw, 256);
            out.writeBoolean(custom.containsKey(raw));
            out.writeFloat(b.minX);
            out.writeFloat(b.minY);
            out.writeFloat(b.minZ);
            out.writeFloat(b.maxX);
            out.writeFloat(b.maxY);
            out.writeFloat(b.maxZ);
        }
        ServerPlayNetworking.send(player, SYNC, out);
    }

    public static void broadcast(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) sendSync(player);
    }
}
