package fable.hideseek.imba.game;

import fable.hideseek.imba.mask.MaskCollisionShapes;
import fable.hideseek.imba.mask.MaskState;
import fable.hideseek.imba.mask.MaskType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Dedicated player-only collision for player-doors. */
public final class DoorMaskCollisionHandler {
    private DoorMaskCollisionHandler() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity masked : server.getPlayerManager().getPlayerList()) {
                if (!MaskState.hasMask(masked.getUuid())) continue;
                MaskState state = MaskState.get(masked.getUuid());
                if (!state.statue || state.type != MaskType.DOOR) continue;

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
        });
    }
}
