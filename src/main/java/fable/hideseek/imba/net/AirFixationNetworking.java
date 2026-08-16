package fable.hideseek.imba.net;

import fable.hideseek.imba.config.AirFixationConfig;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Map;

public final class AirFixationNetworking {
    public static final Identifier REQUEST = new Identifier("imba", "air_fixation_request");
    public static final Identifier SET = new Identifier("imba", "air_fixation_set_v2");
    public static final Identifier SYNC = new Identifier("imba", "air_fixation_sync_v2");
    private AirFixationNetworking() {}

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST, (server, player, handler, buf, sender) -> server.execute(() -> {
            if (!player.hasPermissionLevel(2)) { player.sendMessage(Text.literal("§cДля настройки фиксации в воздухе нужны права оператора"), true); return; }
            sendSync(player);
        }));
        ServerPlayNetworking.registerGlobalReceiver(SET, (server, player, handler, buf, sender) -> {
            String key = buf.readString(320);
            int modeOrdinal = buf.readUnsignedByte();
            String requiredBlock = buf.readString(256);
            server.execute(() -> {
                if (!player.hasPermissionLevel(2)) return;
                if (!(key.startsWith("block:") || key.startsWith("item:") || key.startsWith("type:"))) return;
                AirFixationConfig.Mode[] modes = AirFixationConfig.Mode.values();
                if (modeOrdinal < 0 || modeOrdinal >= modes.length) return;
                AirFixationConfig.set(key, modes[modeOrdinal], requiredBlock);
                sendSync(player);
            });
        });
    }

    public static void sendSync(ServerPlayerEntity player) {
        Map<String, AirFixationConfig.Rule> map = AirFixationConfig.overridesSnapshot();
        PacketByteBuf out = PacketByteBufs.create();
        out.writeVarInt(map.size());
        for (var e : map.entrySet()) {
            out.writeString(e.getKey(), 320);
            out.writeByte(e.getValue().mode().ordinal());
            out.writeString(e.getValue().requiredBlock(), 256);
        }
        ServerPlayNetworking.send(player, SYNC, out);
    }
}
