package fable.hideseek.imba.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

/**
 * Visual-only bridge across the instant local fixation teleport.
 *
 * The player is moved to the authoritative statue anchor immediately. Before that
 * move we remember the actual world-space camera position. On the next camera
 * update we derive the exact offset between that preserved camera position and
 * vanilla's new camera position, then smoothly decay only that visual offset.
 *
 * This keeps gameplay position/collision authoritative while preventing the
 * first-person camera from visibly snapping with the entity.
 */
public final class ClientCameraTransition {
    private static final long DURATION_NANOS = 140_000_000L;
    private static final double EPSILON_SQUARED = 1.0E-10D;

    private static Vec3d preservedCameraPos = Vec3d.ZERO;
    private static Vec3d initialOffset = Vec3d.ZERO;
    private static long startedAt;
    private static boolean pendingBase;
    private static boolean active;

    private ClientCameraTransition() {}

    /**
     * Called immediately before ClientStatueLock moves the local player to anchor.
     * The player positions are kept in the signature for compatibility; the
     * important value is the actual rendered camera position at this moment.
     */
    public static void begin(Vec3d fromPlayerPos, Vec3d anchor) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.gameRenderer == null || client.player == null) {
            clear();
            return;
        }

        preservedCameraPos = client.gameRenderer.getCamera().getPos();
        initialOffset = Vec3d.ZERO;
        startedAt = 0L;
        pendingBase = true;
        active = false;
    }

    /**
     * Must be called after vanilla Camera.update(), with its freshly calculated
     * world-space camera position for the already-teleported player.
     */
    public static Vec3d currentOffset(Vec3d baseCameraPos) {
        if (baseCameraPos == null) return Vec3d.ZERO;

        if (pendingBase) {
            pendingBase = false;
            initialOffset = preservedCameraPos.subtract(baseCameraPos);
            if (initialOffset.lengthSquared() <= EPSILON_SQUARED) {
                clear();
                return Vec3d.ZERO;
            }
            startedAt = System.nanoTime();
            active = true;
        }

        if (!active) return Vec3d.ZERO;

        double t = (System.nanoTime() - startedAt) / (double) DURATION_NANOS;
        if (t >= 1.0D) {
            clear();
            return Vec3d.ZERO;
        }

        t = Math.max(0.0D, t);
        double smooth = t * t * (3.0D - 2.0D * t);
        return initialOffset.multiply(1.0D - smooth);
    }

    public static void clear() {
        preservedCameraPos = Vec3d.ZERO;
        initialOffset = Vec3d.ZERO;
        startedAt = 0L;
        pendingBase = false;
        active = false;
    }
}
