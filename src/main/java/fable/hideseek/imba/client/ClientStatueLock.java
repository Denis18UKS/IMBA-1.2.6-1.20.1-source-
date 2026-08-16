package fable.hideseek.imba.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/** Keeps statue position exact while leaving camera rotation free. */
public final class ClientStatueLock {
    private ClientStatueLock() {}

    public static boolean enter(PlayerEntity player, double anchorX, double anchorY, double anchorZ) {
        if (player == null) return false;
        Vec3d anchor = new Vec3d(anchorX, anchorY, anchorZ);
        boolean local = MinecraftClient.getInstance().player == player;
        Vec3d oldPos = player.getPos();

        player.setNoGravity(true);
        player.setVelocity(Vec3d.ZERO);
        player.fallDistance = 0.0F;
        player.setSneaking(false);
        player.setPose(EntityPose.STANDING);

        if (player instanceof ClientPlayerEntity clientPlayer) {
            if (clientPlayer.input != null) {
                clientPlayer.input.movementForward = 0.0F;
                clientPlayer.input.movementSideways = 0.0F;
                clientPlayer.input.jumping = false;
                clientPlayer.input.sneaking = false;
            }
            clientPlayer.setSprinting(false);
        }

        if (oldPos.squaredDistanceTo(anchor) > 1.0E-8D) {
            if (local) ClientCameraTransition.begin(oldPos, anchor);
            player.setPosition(anchorX, anchorY, anchorZ);
        }

        player.prevX = anchorX;
        player.prevY = anchorY;
        player.prevZ = anchorZ;
        player.lastRenderX = anchorX;
        player.lastRenderY = anchorY;
        player.lastRenderZ = anchorZ;
        player.calculateDimensions();
        return true;
    }

    public static boolean apply(PlayerEntity player) {
        if (player == null || !ClientMaskData.isStatue(player.getUuid())) return false;
        Vec3d anchor = ClientMaskData.getStatueAnchor(player.getUuid());
        if (anchor == null) return false;
        player.setNoGravity(true);
        player.setVelocity(Vec3d.ZERO);
        player.fallDistance = 0.0F;
        player.setSneaking(false);
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
        if (MinecraftClient.getInstance().player == player) ClientCameraTransition.clear();
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
