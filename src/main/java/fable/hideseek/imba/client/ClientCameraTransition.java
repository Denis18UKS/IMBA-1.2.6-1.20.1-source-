package fable.hideseek.imba.client;

import net.minecraft.util.math.Vec3d;

/**
 * Compatibility shell for the old fixation camera bridge.
 *
 * Fixation is intentionally immediate now: the server owns the final anchor,
 * ClientStatueLock snaps render/history coordinates to that same anchor, and no
 * additional camera offset is layered on top. The previous ~140 ms easing could
 * race the vanilla teleport packet and statue sync and produce a visible twitch.
 */
public final class ClientCameraTransition {
    private ClientCameraTransition() {}

    public static void begin(Vec3d fromPlayerPos, Vec3d anchor) {
        // No-op by design. Keeping the API avoids touching unrelated camera/mixin wiring.
    }

    public static Vec3d currentOffset() {
        return Vec3d.ZERO;
    }

    public static void clear() {
        // No state is retained anymore.
    }
}
