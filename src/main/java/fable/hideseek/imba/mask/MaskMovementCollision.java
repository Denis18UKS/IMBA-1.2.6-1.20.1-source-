package fable.hideseek.imba.mask;

import net.minecraft.util.math.Box;

/**
 * Collision box used only while the mask owner moves through world geometry.
 *
 * The visible/targetable entity box remains mask-sized outside Entity.move().
 * During movement the owner's box is narrowed to vanilla-player dimensions so
 * every vanilla collision stage sees the same passable shape. After movement
 * the original visual box is restored at the owner's new position.
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

    public static Box restoreVisualBox(Box originalVisualBox, double x, double y, double z) {
        if (originalVisualBox == null) return null;

        double oldCenterX = (originalVisualBox.minX + originalVisualBox.maxX) * 0.5D;
        double oldCenterZ = (originalVisualBox.minZ + originalVisualBox.maxZ) * 0.5D;

        return originalVisualBox.offset(
                x - oldCenterX,
                y - originalVisualBox.minY,
                z - oldCenterZ);
    }
}
