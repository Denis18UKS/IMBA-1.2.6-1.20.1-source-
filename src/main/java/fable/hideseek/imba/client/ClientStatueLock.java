package fable.hideseek.imba.client;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/** Keeps the rendered entity on the exact server-provided statue anchor. */
public final class ClientStatueLock {
    private ClientStatueLock() {
    }

    public static boolean apply(PlayerEntity player) {
        if (player == null || !ClientMaskData.isStatue(player.getUuid())) {
            return false;
        }
        Vec3d anchor = ClientMaskData.getStatueAnchor(player.getUuid());
        if (anchor == null) {
            return false;
        }

        player.setNoGravity(true);
        player.setVelocity(Vec3d.ZERO);
        player.fallDistance = 0.0F;
        player.setPosition(anchor.x, anchor.y, anchor.z);

        // Entity rendering interpolates between these fields. Keeping all of
        // them at the anchor removes the visible one-frame fall/snap cycle.
        player.prevX = anchor.x;
        player.prevY = anchor.y;
        player.prevZ = anchor.z;
        player.lastRenderX = anchor.x;
        player.lastRenderY = anchor.y;
        player.lastRenderZ = anchor.z;
        return true;
    }

    public static void release(PlayerEntity player) {
        if (player == null) {
            return;
        }
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
