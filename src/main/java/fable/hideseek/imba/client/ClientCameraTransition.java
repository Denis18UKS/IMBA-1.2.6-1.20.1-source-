package fable.hideseek.imba.client;

import net.minecraft.util.math.Vec3d;

/**
 * Visual-only first-person bridge for the short snap from the player's live
 * position to the already calculated statue anchor. The player itself is moved
 * immediately to the authoritative anchor; only the camera origin eases out.
 * Yaw/pitch are never touched, so mouse look stays completely free.
 */
public final class ClientCameraTransition {
    private static final long DURATION_NANOS = 140_000_000L;
    private static Vec3d initialOffset = Vec3d.ZERO;
    private static long startedAt;
    private static boolean active;

    private ClientCameraTransition() {}

    public static void begin(Vec3d fromPlayerPos, Vec3d anchor) {
        if (fromPlayerPos == null || anchor == null) {
            clear();
            return;
        }
        Vec3d offset = fromPlayerPos.subtract(anchor);
        if (offset.lengthSquared() < 1.0E-8D) {
            clear();
            return;
        }
        initialOffset = offset;
        startedAt = System.nanoTime();
        active = true;
    }

    public static Vec3d currentOffset() {
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
        active = false;
        initialOffset = Vec3d.ZERO;
        startedAt = 0L;
    }
}
