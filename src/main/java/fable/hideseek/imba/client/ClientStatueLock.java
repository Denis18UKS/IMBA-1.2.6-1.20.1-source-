package fable.hideseek.imba.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/** Keeps statues on their anchor without continuously fighting camera interpolation. */
public final class ClientStatueLock {
    private ClientStatueLock() {}

    /**
     * Called exactly on MOVING -> STATUE transition. Local interpolation and
     * input are collapsed once here; subsequent statue ticks do not rewrite
     * prev/render coordinates, which avoids camera jitter.
     */
    public static boolean enter(PlayerEntity player, double anchorX, double anchorY, double anchorZ) {
        if (player == null) return false;
        player.setNoGravity(true);
        player.setVelocity(Vec3d.ZERO);
        player.fallDistance = 0.0F;

        if (player instanceof ClientPlayerEntity clientPlayer) {
            if (clientPlayer.input != null) {
                clientPlayer.input.movementForward = 0.0F;
                clientPlayer.input.movementSideways = 0.0F;
                clientPlayer.input.jumping = false;
                clientPlayer.input.sneaking = false;
            }
            clientPlayer.setSprinting(false);
        }

        if (player.getPos().squaredDistanceTo(anchorX, anchorY, anchorZ) > 1.0E-8D) {
            player.setPosition(anchorX, anchorY, anchorZ);
        }
        player.prevX = anchorX;
        player.prevY = anchorY;
        player.prevZ = anchorZ;
        player.lastRenderX = anchorX;
        player.lastRenderY = anchorY;
        player.lastRenderZ = anchorZ;
        return true;
    }

    public static boolean apply(PlayerEntity player) {
        if (player == null || !ClientMaskData.isStatue(player.getUuid())) return false;
        Vec3d anchor = ClientMaskData.getStatueAnchor(player.getUuid());
        if (anchor == null) return false;

        player.setNoGravity(true);
        player.setVelocity(Vec3d.ZERO);
        player.fallDistance = 0.0F;

        // The local player's position/render history is synchronized only once
        // in enter(). Rewriting it every tick is exactly what causes camera jitter.
        if (MinecraftClient.getInstance().player == player) return true;

        player.setPosition(anchor.x, anchor.y, anchor.z);
        player.prevX = anchor.x;
        player.prevY = anchor.y;
        player.prevZ = anchor.z;
        player.lastRenderX = anchor.x;
        player.lastRenderY = anchor.y;
        player.lastRenderZ = anchor.z;
        return true;
    }

    public static void release(PlayerEntity player) {
        if (player == null) return;
        player.setNoGravity(false);
        player.fallDistance = 0.0F;
        player.prevX = player.getX();
        player.prevY = player.getY();
        player.prevZ = player.getZ();
        player.lastRenderX = player.getX();
        player.lastRenderY = player.getY();
        player.lastRenderZ = player.getZ();
    }
}
