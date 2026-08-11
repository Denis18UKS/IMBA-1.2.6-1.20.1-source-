package fable.hideseek.imba.net;

import fable.hideseek.imba.config.MaskAutoPositionConfig;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Map;

/** Server side for the per-block statue auto-position editor. */
public final class MaskAutoPositionNetworking {
    public static final Identifier REQUEST = new Identifier("imba", "mask_autopos_request");
    public static final Identifier SET = new Identifier("imba", "mask_autopos_set");
    public static final Identifier SYNC = new Identifier("imba", "mask_autopos_sync");

    private MaskAutoPositionNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST,
                (server, player, handler, buf, responseSender) ->
                        server.execute(() -> {
                            if (!player.hasPermissionLevel(2)) {
                                player.sendMessage(Text.literal(
                                        "§cДля настройки автопозиции нужны права оператора"), true);
                                return;
                            }
                            sendSync(player);
                        }));

        ServerPlayNetworking.registerGlobalReceiver(SET,
                (server, player, handler, buf, responseSender) -> {
                    String rawId = buf.readString(256);
                    int xPixels = buf.readInt();
                    int yPixels = buf.readInt();
                    int zPixels = buf.readInt();

                    server.execute(() -> {
                        if (!player.hasPermissionLevel(2)) {
                            player.sendMessage(Text.literal(
                                    "§cДля настройки автопозиции нужны права оператора"), true);
                            return;
                        }

                        Identifier id = Identifier.tryParse(rawId);
                        if (id == null || !Registries.BLOCK.containsId(id)) {
                            player.sendMessage(Text.literal("§cНеизвестный блок: §f" + rawId), true);
                            return;
                        }

                        MaskAutoPositionConfig.setOffset(id, xPixels, yPixels, zPixels);
                        sendSync(player);

                        MaskAutoPositionConfig.Offset value =
                                MaskAutoPositionConfig.offsetFor(Registries.BLOCK.get(id));
                        if (value.isZero()) {
                            player.sendMessage(Text.literal(
                                    "§aИндивидуальная автопозиция сброшена: §f" + id), true);
                        } else {
                            player.sendMessage(Text.literal(
                                    "§aАвтопозиция §f" + id
                                            + " §7→ X " + signed(value.xPixels)
                                            + " px, Y " + signed(value.yPixels)
                                            + " px, Z " + signed(value.zPixels) + " px"), true);
                        }
                    });
                });
    }

    public static void sendSync(ServerPlayerEntity player) {
        Map<String, MaskAutoPositionConfig.Offset> values = MaskAutoPositionConfig.snapshot();
        PacketByteBuf out = PacketByteBufs.create();
        out.writeVarInt(values.size());
        for (Map.Entry<String, MaskAutoPositionConfig.Offset> entry : values.entrySet()) {
            out.writeString(entry.getKey(), 256);
            MaskAutoPositionConfig.Offset offset = entry.getValue();
            out.writeInt(offset.xPixels);
            out.writeInt(offset.yPixels);
            out.writeInt(offset.zPixels);
        }
        ServerPlayNetworking.send(player, SYNC, out);
    }

    private static String signed(int value) {
        return value >= 0 ? "+" + value : Integer.toString(value);
    }
}
