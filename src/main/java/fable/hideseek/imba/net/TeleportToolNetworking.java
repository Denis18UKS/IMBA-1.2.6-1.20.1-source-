package fable.hideseek.imba.net;

import fable.hideseek.imba.game.GameConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/** Server-side packets used by the in-game teleport setup tool. */
public final class TeleportToolNetworking {
    public static final Identifier SAVE = new Identifier("imba", "teleport_tool_save");
    public static final Identifier TEST = new Identifier("imba", "teleport_tool_test");

    private TeleportToolNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(SAVE, (server, player, handler, buf, responseSender) -> {
            String slot = buf.readString(32);
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            float yaw = buf.readFloat();
            float pitch = buf.readFloat();

            server.execute(() -> {
                if (!player.hasPermissionLevel(2)
                        || !GameConfig.isTeleportSlot(slot)
                        || !isFinite(x, y, z, yaw, pitch)) {
                    player.sendMessage(Text.literal("§cНе удалось сохранить точку телепортации"), true);
                    return;
                }

                GameConfig.setPoint(slot, new Vec3d(x, y, z), yaw, pitch);
                player.sendMessage(Text.literal(
                        "§aАвтосохранено: §f" + GameConfig.getPointDisplayName(slot)
                                + " §7→ §f" + format(x) + ", " + format(y) + ", " + format(z)
                                + " §7[§f" + format(yaw) + "°, " + format(pitch) + "°§7]"), true);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(TEST, (server, player, handler, buf, responseSender) -> {
            String slot = buf.readString(32);
            server.execute(() -> {
                if (!player.hasPermissionLevel(2)) {
                    player.sendMessage(Text.literal("§cДля настройки телепортов нужны права оператора"), true);
                    return;
                }

                GameConfig.TeleportPoint point = GameConfig.getTeleportPoint(slot);
                if (point == null) {
                    player.sendMessage(Text.literal("§cТочка телепортации не найдена"), true);
                    return;
                }

                ServerWorld world = server.getWorld(point.worldKey());
                if (world == null) {
                    player.sendMessage(Text.literal("§cМир сохранённой точки недоступен"), true);
                    return;
                }

                player.teleport(world, point.pos().x, point.pos().y, point.pos().z,
                        point.yaw(), point.pitch());
                player.sendMessage(Text.literal("§aПроверка точки: §f" + point.name()), true);
            });
        });
    }

    private static boolean isFinite(double x, double y, double z, float yaw, float pitch) {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                && Float.isFinite(yaw) && Float.isFinite(pitch)
                && Math.abs(x) <= 3.0E7D && Math.abs(y) <= 2048.0D && Math.abs(z) <= 3.0E7D
                && Math.abs(yaw) <= 100000.0F && Math.abs(pitch) <= 90.0F;
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
