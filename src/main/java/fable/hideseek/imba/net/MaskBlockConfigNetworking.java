package fable.hideseek.imba.net;

import fable.hideseek.imba.config.MaskBlockConfig;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Set;

/** Server side for the mask-block classification GUI. */
public final class MaskBlockConfigNetworking {
    public static final Identifier REQUEST = new Identifier("imba", "mask_block_config_request");
    public static final Identifier SET = new Identifier("imba", "mask_block_config_set");
    public static final Identifier SYNC = new Identifier("imba", "mask_block_config_sync");

    private MaskBlockConfigNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST, (server, player, handler, buf, responseSender) ->
                server.execute(() -> {
                    if (!player.hasPermissionLevel(2)) {
                        player.sendMessage(Text.literal("§cДля настройки блоков нужны права оператора"), true);
                        return;
                    }
                    sendSync(player);
                }));

        ServerPlayNetworking.registerGlobalReceiver(SET, (server, player, handler, buf, responseSender) -> {
            String rawId = buf.readString(256);
            boolean full = buf.readBoolean();
            server.execute(() -> {
                if (!player.hasPermissionLevel(2)) {
                    player.sendMessage(Text.literal("§cДля настройки блоков нужны права оператора"), true);
                    return;
                }

                Identifier id = Identifier.tryParse(rawId);
                if (id == null || !Registries.BLOCK.containsId(id)) {
                    player.sendMessage(Text.literal("§cНеизвестный блок: §f" + rawId), true);
                    return;
                }

                MaskBlockConfig.setFull(id, full);
                sendSync(player);
                player.sendMessage(Text.literal(
                        (full ? "§aПолноценный блок: §f" : "§eНеполноценный блок: §f") + id), true);
            });
        });
    }

    public static void sendSync(ServerPlayerEntity player) {
        Set<String> values = MaskBlockConfig.nonFullBlocksSnapshot();
        PacketByteBuf out = PacketByteBufs.create();
        out.writeVarInt(values.size());
        for (String id : values) {
            out.writeString(id, 256);
        }
        ServerPlayNetworking.send(player, SYNC, out);
    }
}
