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

/** Server side for the mask + support block auto-position editor. */
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
                    String rawMaskId = buf.readString(256);
                    String rawSupportId = buf.readString(256);
                    int xPixels = buf.readInt();
                    int yPixels = buf.readInt();
                    int zPixels = buf.readInt();

                    server.execute(() -> {
                        if (!player.hasPermissionLevel(2)) {
                            player.sendMessage(Text.literal(
                                    "§cДля настройки автопозиции нужны права оператора"), true);
                            return;
                        }

                        Identifier maskId = Identifier.tryParse(rawMaskId);
                        Identifier supportId = Identifier.tryParse(rawSupportId);
                        if (maskId == null || !Registries.BLOCK.containsId(maskId)) {
                            player.sendMessage(Text.literal("§cНеизвестная маска-блок: §f" + rawMaskId), true);
                            return;
                        }
                        if (supportId == null || !Registries.BLOCK.containsId(supportId)) {
                            player.sendMessage(Text.literal("§cНеизвестный опорный блок: §f" + rawSupportId), true);
                            return;
                        }

                        MaskAutoPositionConfig.setOffset(maskId, supportId, xPixels, yPixels, zPixels);
                        sendSync(player);

                        MaskAutoPositionConfig.Offset value = MaskAutoPositionConfig.offsetFor(maskId, supportId);
                        String pair = maskId + " + " + supportId;
                        if (value.isZero()) {
                            player.sendMessage(Text.literal(
                                    "§aИндивидуальная автопозиция сброшена: §f" + pair), true);
                        } else {
                            player.sendMessage(Text.literal(
                                    "§aАвтопозиция §f" + pair
                                            + " §7→ X " + signed(value.xPixels)
                                            + " px, Y " + signed(value.yPixels)
                                            + " px, Z " + signed(value.zPixels) + " px"), true);
                        }
                    });
                });
    }

    public static void sendSync(ServerPlayerEntity player) {
        Map<String, Map<String, MaskAutoPositionConfig.Offset>> values = MaskAutoPositionConfig.snapshot();
        int count = 0;
        for (Map<String, MaskAutoPositionConfig.Offset> supports : values.values()) {
            count += supports.size();
        }

        PacketByteBuf out = PacketByteBufs.create();
        out.writeVarInt(count);
        for (Map.Entry<String, Map<String, MaskAutoPositionConfig.Offset>> maskEntry : values.entrySet()) {
            for (Map.Entry<String, MaskAutoPositionConfig.Offset> supportEntry : maskEntry.getValue().entrySet()) {
                out.writeString(maskEntry.getKey(), 256);
                out.writeString(supportEntry.getKey(), 256);
                MaskAutoPositionConfig.Offset offset = supportEntry.getValue();
                out.writeInt(offset.xPixels);
                out.writeInt(offset.yPixels);
                out.writeInt(offset.zPixels);
            }
        }
        ServerPlayNetworking.send(player, SYNC, out);
    }

    private static String signed(int value) {
        return value >= 0 ? "+" + value : Integer.toString(value);
    }
}
