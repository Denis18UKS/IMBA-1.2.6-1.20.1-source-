package fable.hideseek.imba.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class ClientStatueLock {
    private ClientStatueLock() {}
    public static boolean enter(PlayerEntity player, double anchorX, double anchorY, double anchorZ) {
        if (player == null) return false;
        player.setNoGravity(true); player.setVelocity(Vec3d.ZERO); player.fallDistance = 0.0F;
        if (player instanceof ClientPlayerEntity clientPlayer) {
            if (clientPlayer.input != null) { clientPlayer.input.movementForward=0.0F; clientPlayer.input.movementSideways=0.0F; clientPlayer.input.jumping=false; clientPlayer.input.sneaking=false; }
            clientPlayer.setSprinting(false);
        }
        double oldX=player.getX(), oldY=player.getY(), oldZ=player.getZ();
        if (player.getPos().squaredDistanceTo(anchorX, anchorY, anchorZ) > 1.0E-8D) {
            player.setPosition(anchorX, anchorY, anchorZ);
            if (MinecraftClient.getInstance().player == player) {
                player.prevX=oldX; player.prevY=oldY; player.prevZ=oldZ;
                player.lastRenderX=oldX; player.lastRenderY=oldY; player.lastRenderZ=oldZ;
            } else {
                player.prevX=anchorX; player.prevY=anchorY; player.prevZ=anchorZ;
                player.lastRenderX=anchorX; player.lastRenderY=anchorY; player.lastRenderZ=anchorZ;
            }
        }
        return true;
    }
    public static boolean apply(PlayerEntity player) {
        if (player == null || !ClientMaskData.isStatue(player.getUuid())) return false;
        Vec3d anchor=ClientMaskData.getStatueAnchor(player.getUuid()); if(anchor==null)return false;
        player.setNoGravity(true); player.setVelocity(Vec3d.ZERO); player.fallDistance=0.0F;
        if (MinecraftClient.getInstance().player == player) return true;
        player.setPosition(anchor.x,anchor.y,anchor.z); player.prevX=anchor.x;player.prevY=anchor.y;player.prevZ=anchor.z;player.lastRenderX=anchor.x;player.lastRenderY=anchor.y;player.lastRenderZ=anchor.z; return true;
    }
    public static void release(PlayerEntity player) {
        if(player==null)return; player.setNoGravity(false);player.fallDistance=0.0F;player.prevX=player.getX();player.prevY=player.getY();player.prevZ=player.getZ();player.lastRenderX=player.getX();player.lastRenderY=player.getY();player.lastRenderZ=player.getZ();
    }
}
