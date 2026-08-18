package fable.hideseek.imba.game;

import fable.hideseek.imba.mask.MaskCollisionShapes;
import fable.hideseek.imba.mask.MaskState;
import fable.hideseek.imba.mask.MaskType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Dedicated collision for player-doors.
 *
 * Players/seeker collision follows the real thin vanilla door leaf.
 * Villagers are intentionally special: for them a player-door behaves like a
 * solid 1x2 map obstacle, so their AI keeps walking into it instead of passing
 * through. Villagers are returned to their last nearby safe position instead
 * of being separated with a large correction vector, which avoids the
 * "villager gets launched out of the door" effect.
 */
public final class DoorMaskCollisionHandler {
    private static final Map<UUID, Vec3d> VILLAGER_LAST_SAFE = new HashMap<>();
    private static final double VILLAGER_TRACK_DISTANCE = 1.75D;
    private static final double MAX_SAFE_RESTORE_DISTANCE_SQ = 9.0D;

    private DoorMaskCollisionHandler() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity masked : server.getPlayerManager().getPlayerList()) {
                if (!MaskState.hasMask(masked.getUuid())) continue;
                MaskState state = MaskState.get(masked.getUuid());
                if (!state.statue || state.type != MaskType.DOOR) continue;

                resolvePlayerCollision(server, masked, state);
                resolveVillagerCollision(masked);
            }
        });
    }

    private static void resolvePlayerCollision(net.minecraft.server.MinecraftServer server,
                                               ServerPlayerEntity masked,
                                               MaskState state) {
        for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
            if (other == masked || other.isSpectator() || !GameRoles.isSeeker(other)) continue;
            for (Box leaf : MaskCollisionShapes.create(state)) {
                Box playerBox = other.getBoundingBox();
                if (!playerBox.intersects(leaf)) continue;
                Vec3d correction = MaskCollisionShapes.nearestHorizontalSeparation(playerBox, leaf);
                if (correction.lengthSquared() <= 1.0E-12D) continue;
                other.setPosition(other.getX() + correction.x, other.getY(), other.getZ() + correction.z);
                Vec3d velocity = other.getVelocity();
                other.setVelocity(correction.x == 0.0D ? velocity.x : 0.0D,
                        velocity.y,
                        correction.z == 0.0D ? velocity.z : 0.0D);
            }
        }
    }

    private static void resolveVillagerCollision(ServerPlayerEntity masked) {
        // Villager-only rule: the complete door footprint is a solid map obstacle,
        // even when the visual door leaf is open. This does NOT affect players or
        // any other entity type.
        Box barrier = new Box(masked.getX() - 0.5D, masked.getY(), masked.getZ() - 0.5D,
                masked.getX() + 0.5D, masked.getY() + 2.0D, masked.getZ() + 0.5D);
        Box tracking = barrier.expand(VILLAGER_TRACK_DISTANCE);

        for (VillagerEntity villager : masked.getServerWorld().getEntitiesByClass(
                VillagerEntity.class, tracking, VillagerEntity::isAlive)) {
            Box currentBox = villager.getBoundingBox();
            UUID uuid = villager.getUuid();

            if (!currentBox.intersects(barrier)) {
                // Always keep a recent position close to the door that is known
                // not to intersect the door footprint.
                VILLAGER_LAST_SAFE.put(uuid, villager.getPos());
                continue;
            }

            // Stop the AI's horizontal movement like a normal solid obstacle.
            Vec3d velocity = villager.getVelocity();
            villager.setVelocity(0.0D, velocity.y, 0.0D);

            Vec3d safe = VILLAGER_LAST_SAFE.get(uuid);
            if (safe == null || safe.squaredDistanceTo(villager.getPos()) > MAX_SAFE_RESTORE_DISTANCE_SQ) {
                continue;
            }

            Box safeBox = currentBox.offset(
                    safe.x - villager.getX(),
                    0.0D,
                    safe.z - villager.getZ());
            if (safeBox.intersects(barrier)) continue;

            // Restore only the horizontal coordinates. No nearest-side shove and
            // no large separation teleport: the villager simply fails to advance
            // through the player-door, like repeatedly walking into a block.
            villager.setPosition(safe.x, villager.getY(), safe.z);
        }
    }
}
