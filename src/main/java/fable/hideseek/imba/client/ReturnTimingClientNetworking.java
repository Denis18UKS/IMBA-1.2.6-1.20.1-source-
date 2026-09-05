package fable.hideseek.imba.client;

import fable.hideseek.imba.net.ReturnTimingNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

public final class ReturnTimingClientNetworking {
    private ReturnTimingClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ReturnTimingNetworking.SYNC, (client, handler, buf, sender) -> {
            int preFade = buf.readVarInt();
            int preTeleport = buf.readVarInt();
            client.execute(() -> {
                ReturnTimingClientData.preFadeTicks = preFade;
                ReturnTimingClientData.preTeleportTicks = preTeleport;
                if (client.currentScreen instanceof ReturnTimingScreen screen) screen.applyServerState();
            });
        });
    }

    public static void request() {
        ClientPlayNetworking.send(ReturnTimingNetworking.REQUEST, PacketByteBufs.empty());
    }

    public static void sendUpdate(int preFadeTicks, int preTeleportTicks) {
        var out = PacketByteBufs.create();
        out.writeVarInt(Math.max(0, preFadeTicks));
        out.writeVarInt(Math.max(0, preTeleportTicks));
        ClientPlayNetworking.send(ReturnTimingNetworking.SET, out);
    }
}
