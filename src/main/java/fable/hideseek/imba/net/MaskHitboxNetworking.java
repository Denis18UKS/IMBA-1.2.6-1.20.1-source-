package fable.hideseek.imba.net;

import fable.hideseek.imba.config.MaskBlockConfig;
import fable.hideseek.imba.config.MaskHitboxConfig;
import fable.hideseek.imba.mask.MaskState;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Set;

public final class MaskHitboxNetworking {
    public static final Identifier REQUEST = new Identifier("imba", "mask_hitboxes_request");
    public static final Identifier SET = new Identifier("imba", "mask_hitboxes_set");
    public static final Identifier CLEAR = new Identifier("imba", "mask_hitboxes_clear");
    public static final Identifier SYNC = new Identifier("imba", "mask_hitboxes_sync");

    private MaskHitboxNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST, (server, player, handler, buf, responseSender) ->
                server.execute(() -> sendSync(player)));

        ServerPlayNetworking.registerGlobalReceiver(SET, (server, player, handler, buf, responseSender) -> {
            String rawId = buf.readString(256);
            MaskHitboxConfig.BoxSpec box = new MaskHitboxConfig.BoxSpec(
                    buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readInt(), buf.readInt(), buf.readInt());
            server.execute(() -> {
                if (!player.hasPermissionLevel(2)) {
                    player.sendMessage(Text.literal("§cДля настройки хитбоксов масок нужны права оператора"), true);
                    return;
                }
                Identifier id = Identifier.tryParse(rawId);
                if (id == null || !Registries.BLOCK.containsId(id)) {
                    player.sendMessage(Text.literal("§cНеизвестный блок: §f" + rawId), true);
                    return;
                }
                Block block = Registries.BLOCK.get(id);
                MaskHitboxConfig.set(id, box);
                refreshMaskedPlayers(server, block);
                broadcastSync(server);
                player.sendMessage(Text.literal("§aХитбокс маски сохранён: §f" + id), true);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(CLEAR, (server, player, handler, buf, responseSender) -> {
            String rawId = buf.readString(256);
            server.execute(() -> {
                if (!player.hasPermissionLevel(2)) {
                    player.sendMessage(Text.literal("§cДля настройки хитбоксов масок нужны права оператора"), true);
                    return;
                }
                Identifier id = Identifier.tryParse(rawId);
                if (id == null || !Registries.BLOCK.containsId(id)) return;
                Block block = Registries.BLOCK.get(id);
                MaskHitboxConfig.clear(id);
                refreshMaskedPlayers(server, block);
                broadcastSync(server);
                player.sendMessage(Text.literal("§eХитбокс сброшен к стандартной форме: §f" + id), true);
            });
        });
    }

    public static void sendSync(ServerPlayerEntity player) {
        // NON-FULL state stays in the packet for backwards compatibility with the
        // existing client cache, but it no longer limits what the hitbox editor can tune.
        Set<String> nonFull = MaskBlockConfig.nonFullBlocksSnapshot();
        Map<String, MaskHitboxConfig.BoxSpec> boxes = MaskHitboxConfig.snapshot();
        PacketByteBuf out = PacketByteBufs.create();
        out.writeVarInt(nonFull.size());
        for (String id : nonFull) out.writeString(id, 256);
        out.writeVarInt(boxes.size());
        for (Map.Entry<String, MaskHitboxConfig.BoxSpec> entry : boxes.entrySet()) {
            out.writeString(entry.getKey(), 256);
            MaskHitboxConfig.BoxSpec box = entry.getValue();
            out.writeInt(box.minX);
            out.writeInt(box.minY);
            out.writeInt(box.minZ);
            out.writeInt(box.maxX);
            out.writeInt(box.maxY);
            out.writeInt(box.maxZ);
        }
        ServerPlayNetworking.send(player, SYNC, out);
    }

    public static void broadcastSync(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) sendSync(player);
    }

    private static void refreshMaskedPlayers(MinecraftServer server, Block block) {
        if (server == null || block == null) return;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!MaskState.hasMask(player.getUuid())) continue;
            MaskState state = MaskState.get(player.getUuid());
            if (state.block != block) continue;
            player.calculateDimensions();
            player.setPosition(player.getX(), player.getY(), player.getZ());
            MaskNetworking.refresh(player);
        }
    }
}
