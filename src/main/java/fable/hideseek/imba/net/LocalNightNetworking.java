package fable.hideseek.imba.net;

import fable.hideseek.imba.config.LocalNightConfig;
import fable.hideseek.imba.game.GameConfig;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Locale;

public final class LocalNightNetworking {
    public static final Identifier REQUEST = new Identifier("imba", "local_night_request");
    public static final Identifier SAVE = new Identifier("imba", "local_night_save");
    public static final Identifier SYNC = new Identifier("imba", "local_night_sync");
    private static final String SNOW_VILLAGE_STREET = "Улица снежной деревни";

    private LocalNightNetworking() {}

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST, (server, player, handler, buf, sender) -> server.execute(() -> sendSync(player)));
        ServerPlayNetworking.registerGlobalReceiver(SAVE, (server, player, handler, buf, sender) -> {
            int location = buf.readInt();
            server.execute(() -> {
                if (!player.hasPermissionLevel(2)) return;
                if (location < -1 || location >= GameConfig.ROUNDS.size()) return;
                if (location >= 0 && !isSnowVillageStreet(GameConfig.getLocationName(location))) {
                    player.sendMessage(Text.literal("§cНочь разрешена только на локации «Улица снежной деревни»"), true);
                    return;
                }
                LocalNightConfig.setSelectedLocation(location);
                broadcast(server);
                String name = location < 0 ? "выключена" : GameConfig.getLocationName(location);
                player.sendMessage(Text.literal("§aЛокальная ночь: §f" + name), true);
            });
        });
    }

    private static boolean isSnowVillageStreet(String value) {
        return normalize(value).equals(normalize(SNOW_VILLAGE_STREET));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace('ё', 'е').trim().replaceAll("\\s+", " ");
    }

    public static void broadcast(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayerEntity player : PlayerLookup.all(server)) sendSync(player);
    }

    public static void sendSync(ServerPlayerEntity player) {
        PacketByteBuf out = PacketByteBufs.create();
        out.writeInt(LocalNightConfig.selectedLocation());
        out.writeDouble(LocalNightConfig.radius());
        out.writeVarInt(GameConfig.ROUNDS.size());
        for (var round : GameConfig.ROUNDS) {
            out.writeString(round.worldKey.getValue().toString(), 128);
            out.writeDouble(round.hiderPos.x);
            out.writeDouble(round.hiderPos.z);
        }
        ServerPlayNetworking.send(player, SYNC, out);
    }
}
