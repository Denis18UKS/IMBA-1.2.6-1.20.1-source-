package fable.hideseek.imba.client;

import fable.hideseek.imba.net.PortalAnimationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

public final class PortalAnimationClientNetworking {
    private PortalAnimationClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PortalAnimationNetworking.SYNC, (client, handler, buf, sender) -> {
            int freezeTicks = buf.readVarInt();
            client.execute(() -> {
                PortalAnimationClientData.freezeTicks = freezeTicks;
                if (client.currentScreen instanceof PortalAnimationScreen screen) screen.applyServerState();
            });
        });
    }

    public static void request() {
        ClientPlayNetworking.send(PortalAnimationNetworking.REQUEST, PacketByteBufs.empty());
    }

    public static void sendUpdate(int freezeTicks) {
        var out = PacketByteBufs.create();
        out.writeVarInt(Math.max(0, freezeTicks));
        ClientPlayNetworking.send(PortalAnimationNetworking.SET, out);
    }
}
