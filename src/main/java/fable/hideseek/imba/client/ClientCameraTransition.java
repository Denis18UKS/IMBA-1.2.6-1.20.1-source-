package fable.hideseek.imba.client;

import net.minecraft.util.math.Vec3d;

/**
 * Compatibility shell for the old fixation camera bridge.
 *
 * Fixation camera position is intentionally no longer animated here. The server
 * owns the anchor and the client applies that same anchor after geometry has been
 * finalized. Keeping this API avoids touching unrelated callers while ensuring
 * there is no second visual position controller fighting vanilla Camera.
 */
public final class ClientCameraTransition {
    private ClientCameraTransition() {}

    public static void begin(Vec3d fromPlayerPos, Vec3d anchor) {
        // No-op by design: there must be only one authoritative fixation position.
    }

    public static Vec3d currentOffset(Vec3d baseCameraPos) {
        return Vec3d.ZERO;
    }

    public static void clear() {
        // No transition state is retained.
    }
}
