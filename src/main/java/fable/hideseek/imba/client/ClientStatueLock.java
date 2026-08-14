package fable.hideseek.imba.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/** Keeps remote statues on their anchor without repeatedly moving the local camera. */
public final class ClientStatueLock {
    private ClientStatueLock() {}
    public static boolean apply(PlayerEntity player) {
        if (player == null || !ClientMaskData.isStatue(player.getUuid())) return false;
        Vec3d anchor = ClientMaskData.getStatueAnchor(player.getUuid()); if (anchor == null) return false;
        player.setNoGravity(true); player.setVelocity(Vec3d.ZERO); player.fallDistance=0f;
        // For the local player the server already sent one authoritative teleport.
        // Rewriting position/prev/render coordinates every client tick fights camera
        // interpolation and is the source of the visible statue camera jitter.
        if (MinecraftClient.getInstance().player == player) return true;
        player.setPosition(anchor.x,anchor.y,anchor.z);
        player.prevX=anchor.x;player.prevY=anchor.y;player.prevZ=anchor.z;
        player.lastRenderX=anchor.x;player.lastRenderY=anchor.y;player.lastRenderZ=anchor.z;
        return true;
    }
    public static void release(PlayerEntity player){if(player==null)return;player.setNoGravity(false);player.fallDistance=0f;player.prevX=player.getX();player.prevY=player.getY();player.prevZ=player.getZ();player.lastRenderX=player.getX();player.lastRenderY=player.getY();player.lastRenderZ=player.getZ();}
}
