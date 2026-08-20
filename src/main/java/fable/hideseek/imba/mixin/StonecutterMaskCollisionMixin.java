package fable.hideseek.imba.mixin;

import fable.hideseek.imba.ImbaMod;
import fable.hideseek.imba.game.GameManager;
import fable.hideseek.imba.mask.MaskCollisionShapes;
import fable.hideseek.imba.mask.MaskState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Guarantees physical collision for the stonecutter-blade disguise even while
 * it was applied only through /imba_mask and has not been fixed as a statue yet.
 */
@Mixin(GameManager.class)
public abstract class StonecutterMaskCollisionMixin {
    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private static void imba$solidStonecutterMask(MinecraftServer server, CallbackInfo ci) {
        if (server == null) return;

        for (ServerPlayerEntity masked : server.getPlayerManager().getPlayerList()) {
            if (!MaskState.hasMask(masked.getUuid())) continue;
            MaskState state = MaskState.get(masked.getUuid());
            if (state.block != ImbaMod.STONRCUTTER_LEZVIE) continue;

            double x = state.statue ? state.anchorX : masked.getX();
            double y = state.statue ? state.anchorY : masked.getY();
            double z = state.statue ? state.anchorZ : masked.getZ();

            for (Box obstacle : MaskCollisionShapes.create(
                    state.type, state.block, state.rotation, state.doorOpen, x, y, z)) {
                for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
                    if (other == masked || other.isSpectator()
                            || other.getWorld() != masked.getWorld()
                            || MaskState.isStatue(other.getUuid())) {
                        continue;
                    }
                    resolve(other, obstacle);
                }
            }
        }
    }

    private static void resolve(ServerPlayerEntity player, Box obstacle) {
        Box playerBox = player.getBoundingBox();
        boolean overlapsXZ = playerBox.maxX > obstacle.minX && playerBox.minX < obstacle.maxX
                && playerBox.maxZ > obstacle.minZ && playerBox.minZ < obstacle.maxZ;

        if (overlapsXZ && player.getVelocity().y <= 0.0D
                && playerBox.minY >= obstacle.maxY - 0.35D
                && playerBox.minY <= obstacle.maxY + 0.20D) {
            if (Math.abs(player.getY() - obstacle.maxY) > 0.001D) {
                player.setPosition(player.getX(), obstacle.maxY, player.getZ());
                player.requestTeleport(player.getX(), obstacle.maxY, player.getZ());
            }
            Vec3d velocity = player.getVelocity();
            player.setVelocity(velocity.x, 0.0D, velocity.z);
            player.setOnGround(true);
            player.fallDistance = 0.0F;
            return;
        }

        if (!playerBox.intersects(obstacle)) return;
        Vec3d correction = MaskCollisionShapes.nearestHorizontalSeparation(playerBox, obstacle);
        if (correction.lengthSquared() <= 1.0E-12D) return;

        double newX = player.getX() + correction.x;
        double newZ = player.getZ() + correction.z;
        player.setPosition(newX, player.getY(), newZ);
        player.requestTeleport(newX, player.getY(), newZ);

        Vec3d velocity = player.getVelocity();
        player.setVelocity(correction.x == 0.0D ? velocity.x : 0.0D,
                velocity.y,
                correction.z == 0.0D ? velocity.z : 0.0D);
    }
}
