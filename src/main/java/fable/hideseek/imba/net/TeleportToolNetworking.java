package fable.hideseek.imba.net;

import fable.hideseek.imba.config.TeleportConfig;
import fable.hideseek.imba.game.GameConfig;
import fable.hideseek.imba.game.RoundDefinition;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

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
                        || !isTeleportSlot(slot)
                        || !isFinite(x, y, z, yaw, pitch)) {
                    player.sendMessage(Text.literal("§cНе удалось сохранить точку телепортации"), true);
                    return;
                }

                setPoint(slot, new Vec3d(x, y, z), yaw, pitch);
                TeleportConfig.save();
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

                TeleportPoint point = getPoint(slot);
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

    private static boolean isTeleportSlot(String slot) {
        if (slot == null) {
            return false;
        }
        if ("lobby".equalsIgnoreCase(slot)) {
            return true;
        }
        ParsedSlot parsed = parseRoundSlot(slot);
        return parsed != null && parsed.index() >= 0 && parsed.index() < GameConfig.ROUNDS.size();
    }

    private static void setPoint(String slot, Vec3d pos, float yaw, float pitch) {
        if ("lobby".equalsIgnoreCase(slot)) {
            GameConfig.LOBBY_POS = pos;
            GameConfig.LOBBY_YAW = yaw;
            GameConfig.LOBBY_PITCH = pitch;
            return;
        }

        ParsedSlot parsed = parseRoundSlot(slot);
        if (parsed == null || parsed.index() < 0 || parsed.index() >= GameConfig.ROUNDS.size()) {
            return;
        }

        RoundDefinition round = GameConfig.ROUNDS.get(parsed.index());
        if (parsed.hider()) {
            round.hiderPos = pos;
            round.hiderYaw = yaw;
            round.hiderPitch = pitch;
        } else {
            round.seekerPos = pos;
            round.seekerYaw = yaw;
            round.seekerPitch = pitch;
        }
    }

    private static TeleportPoint getPoint(String slot) {
        if ("lobby".equalsIgnoreCase(slot)) {
            return new TeleportPoint(World.OVERWORLD, GameConfig.LOBBY_POS,
                    GameConfig.LOBBY_YAW, GameConfig.LOBBY_PITCH, "Лобби");
        }

        ParsedSlot parsed = parseRoundSlot(slot);
        if (parsed == null || parsed.index() < 0 || parsed.index() >= GameConfig.ROUNDS.size()) {
            return null;
        }

        RoundDefinition round = GameConfig.ROUNDS.get(parsed.index());
        String role = parsed.hider() ? "прячущийся" : "искатель";
        String name = GameConfig.getLocationName(parsed.index()) + " — " + role;
        return parsed.hider()
                ? new TeleportPoint(round.worldKey, round.hiderPos, round.hiderYaw, round.hiderPitch, name)
                : new TeleportPoint(round.worldKey, round.seekerPos, round.seekerYaw, round.seekerPitch, name);
    }

    private static ParsedSlot parseRoundSlot(String slot) {
        if (slot == null) {
            return null;
        }
        String lower = slot.toLowerCase(java.util.Locale.ROOT);
        int underscore = lower.indexOf('_');
        if (!lower.startsWith("r") || underscore <= 1) {
            return null;
        }

        String role = lower.substring(underscore + 1);
        boolean hider;
        if ("hider".equals(role)) {
            hider = true;
        } else if ("seeker".equals(role)) {
            hider = false;
        } else {
            return null;
        }

        try {
            int roundNumber = Integer.parseInt(lower.substring(1, underscore));
            return new ParsedSlot(roundNumber - 1, hider);
        } catch (NumberFormatException ignored) {
            return null;
        }
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

    private record ParsedSlot(int index, boolean hider) {
    }

    private record TeleportPoint(
            RegistryKey<World> worldKey,
            Vec3d pos,
            float yaw,
            float pitch,
            String name) {
    }
}
