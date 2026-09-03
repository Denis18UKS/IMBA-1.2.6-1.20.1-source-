package fable.hideseek.imba.mask;

import net.minecraft.util.math.Box;

/**
 * Collision box used only while the mask owner moves through world geometry.
 *
 * The real entity bounding box stays mask-sized so F3+B, targeting and the
 * seeker-facing block collision remain unchanged. Only the box passed into
 * Minecraft's movement collision solver is narrowed to player-like width.
 */
public final class MaskMovementCollision {
    public static final double OWNER_MAX_WIDTH = 0.60D;
    public static final double OWNER_MAX_HEIGHT = 1.80D;

    private MaskMovementCollision() {}

    public static Box ownerMovementBox(Box visualBox) {
        if (visualBox == null) return null;

        double visualWidthX = visualBox.maxX - visualBox.minX;
        double visualWidthZ = visualBox.maxZ - visualBox.minZ;
        double visualHeight = visualBox.maxY - visualBox.minY;

        double widthX = Math.min(visualWidthX, OWNER_MAX_WIDTH);
        double widthZ = Math.min(visualWidthZ, OWNER_MAX_WIDTH);
        double height = Math.min(visualHeight, OWNER_MAX_HEIGHT);

        double centerX = (visualBox.minX + visualBox.maxX) * 0.5D;
        double centerZ = (visualBox.minZ + visualBox.maxZ) * 0.5D;
        double halfX = widthX * 0.5D;
        double halfZ = widthZ * 0.5D;

        return new Box(
                centerX - halfX, visualBox.minY, centerZ - halfZ,
                centerX + halfX, visualBox.minY + height, centerZ + halfZ);
    }
}
