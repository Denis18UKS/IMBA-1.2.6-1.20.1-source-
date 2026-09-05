package fable.hideseek.imba.game;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PortalTransitGuard {
    private enum Kind { VANILLA, PLAYER_MASK }

    private static final Map<UUID, Contact> CONTACTS = new HashMap<>();

    private PortalTransitGuard() {
    }

    public static synchronized int touchVanilla(ServerPlayerEntity player, RegistryKey<World> world,
                                                BlockPos portalPos, long tick) {
        return touch(player.getUuid(), world, "vanilla:" + portalPos.asLong(), Kind.VANILLA, tick);
    }

    public static synchronized int touchPlayerPortal(ServerPlayerEntity player, BlockPos portalPos,
                                                     UUID portalOwner, long tick) {
        RegistryKey<World> world = player.getWorld().getRegistryKey();
        Contact previous = CONTACTS.get(player.getUuid());
        if (previous != null && previous.kind == Kind.VANILLA
                && previous.world.equals(world) && previous.tick == tick) {
            return 0;
        }
        String key = "player:" + portalOwner + ":" + portalPos.asLong();
        return touch(player.getUuid(), world, key, Kind.PLAYER_MASK, tick);
    }

    public static synchronized boolean vanillaContactThisTick(ServerPlayerEntity player, long tick) {
        Contact contact = CONTACTS.get(player.getUuid());
        return contact != null
                && contact.kind == Kind.VANILLA
                && contact.world.equals(player.getWorld().getRegistryKey())
                && contact.tick == tick;
    }

    public static synchronized void clear(UUID playerId) {
        CONTACTS.remove(playerId);
    }

    private static int touch(UUID playerId, RegistryKey<World> world, String portalKey, Kind kind, long tick) {
        Contact previous = CONTACTS.get(playerId);
        if (previous != null
                && previous.world.equals(world)
                && previous.kind == kind
                && previous.tick == tick) {
            return previous.ticks;
        }

        int ticks = 1;
        if (previous != null
                && previous.world.equals(world)
                && previous.kind == kind
                && previous.portalKey.equals(portalKey)
                && previous.tick + 1L == tick) {
            ticks = previous.ticks + 1;
        }

        CONTACTS.put(playerId, new Contact(world, portalKey, kind, tick, ticks));
        return ticks;
    }

    private record Contact(RegistryKey<World> world, String portalKey, Kind kind, long tick, int ticks) {
    }
}
