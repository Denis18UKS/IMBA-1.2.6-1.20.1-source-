package fable.hideseek.imba.mask;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Fully restores vanilla player geometry after a mask is removed.
 *
 * A normal Shift press fixes the stale collision because vanilla goes through
 * a real pose transition and rebuilds the entity box. Do the same transition
 * programmatically and then explicitly install the vanilla STANDING box.
 * No player dimensions are hard-coded here.
 */
public final class MaskResetGeometry {
    private MaskResetGeometry() {
    }

    public static void forceStanding(PlayerEntity player) {
        if (player == null) {
            return;
        }

        player.setNoGravity(false);
        player.setSwimming(false);

        // Reproduce the geometry-changing part of a real crouch cycle. Merely
        // assigning STANDING again is not sufficient when the cached server
        // collision box still came from a mask.
        player.setSneaking(true);
        player.setPose(EntityPose.CROUCHING);
        player.calculateDimensions();

        player.setSneaking(false);
        player.setPose(EntityPose.STANDING);
        player.calculateDimensions();

        // calculateDimensions normally rebuilds this box, but force it from
        // vanilla's current STANDING dimensions as well. This fixes the case
        // where the rendered/client box is already normal while the physical
        // server box remains stale until the next manual Shift press.
        EntityDimensions standing = player.getDimensions(EntityPose.STANDING);
        player.setBoundingBox(standing.getBoxAt(player.getPos()));
    }
}
