package fable.hideseek.imba.client;

import fable.hideseek.imba.net.LocalNightNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

/** Client-only visual time selector. It never mutates the server world's time. */
public final class LocalNightClientData {
    public static final long DAY_TIME = 1000L;
    public static final long NIGHT_TIME = 13000L;

    private static int selectedLocation = -1;
    private static double radius = 128.0D;
    private static List<Anchor> anchors = List.of();

    private LocalNightClientData() {}

    public static void registerNetworking() {
        ClientPlayNetworking.registerGlobalReceiver(LocalNightNetworking.SYNC, (client, handler, buf, sender) -> {
            int selected = buf.readInt();
            double nextRadius = buf.readDouble();
            int count = Math.max(0, Math.min(buf.readVarInt(), 128));
            List<Anchor> next = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                next.add(new Anchor(buf.readString(128), buf.readDouble(), buf.readDouble()));
            }
            client.execute(() -> {
                selectedLocation = selected;
                radius = Math.max(16.0D, Math.min(512.0D, nextRadius));
                anchors = List.copyOf(next);
                if (client.currentScreen instanceof LocalNightScreen screen) screen.applyServerState(selectedLocation);
            });
        });
    }

    public static int selectedLocation() {
        return selectedLocation;
    }

    public static int nearestLocation(String world, double x, double z) {
        int best = -1;
        double bestDistance = radius * radius;
        for (int i = 0; i < anchors.size(); i++) {
            Anchor anchor = anchors.get(i);
            if (!anchor.world.equals(world)) continue;
            double dx = x - anchor.x;
            double dz = z - anchor.z;
            double distance = dx * dx + dz * dz;
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return best;
    }

    public static long visualTime() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return DAY_TIME;
        String world = client.world.getRegistryKey().getValue().toString();
        int nearest = nearestLocation(world, client.player.getX(), client.player.getZ());
        return selectedLocation >= 0 && nearest == selectedLocation ? NIGHT_TIME : DAY_TIME;
    }

    private record Anchor(String world, double x, double z) {}
}
