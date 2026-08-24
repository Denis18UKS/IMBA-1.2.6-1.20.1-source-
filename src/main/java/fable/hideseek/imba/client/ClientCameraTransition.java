package fable.hideseek.imba.client;

import net.minecraft.util.math.Vec3d;

/**
 * Compatibility shell for the old fixation camera bridge.
 *
 * Fixation is now intentionally immediate: the server owns the final anchor,
 * ClientStatueLock snaps render/history coordinates to that same anchor, and no
 * additional camera offset is layered on top. The previous ~140 ms easing could
 * race the vanilla teleport packet and statue sync, producing a visible
 * back-and-forth twitch when locking a disguise.
 */
public final class ClientCameraTransition {
    private ClientCameraTransition() {}

    public static void begin(Vec3d fromPlayerPos, Vec3d anchor) {
        // No-op by design. Keeping the API avoids touching unrelated camera/mixin
        // wiring while guaranteeing that fixation has a single visual position.
    }

    public static Vec3d currentOffset() {
        return Vec3d.ZERO;
    }

    public static void clear() {
        // No state is retained anymore.
    }
}
