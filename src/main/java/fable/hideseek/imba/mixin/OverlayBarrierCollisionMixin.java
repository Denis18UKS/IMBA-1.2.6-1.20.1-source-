package fable.hideseek.imba.mixin;

import fable.hideseek.imba.config.OverlayBarrierConfig;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Adds collision from OverlayBarrierConfig after ordinary block/entity collision is resolved. */
@Mixin(Entity.class)
public abstract class OverlayBarrierCollisionMixin {
    @Shadow
    private static Vec3d adjustMovementForCollisions(Vec3d movement, Box entityBoundingBox,
                                                       List<VoxelShape> collisions) {
        throw new AssertionError();
    }

    @Inject(method = "adjustMovementForCollisions(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;",
            at = @At("RETURN"), cancellable = true)
    private void imba$applyOverlayBarriers(Vec3d requested, CallbackInfoReturnable<Vec3d> cir) {
        Entity self = (Entity) (Object) this;
        Vec3d movement = cir.getReturnValue();
        if (movement == null || movement.lengthSquared() <= 1.0E-14D) return;

        Box query = self.getBoundingBox().stretch(movement).expand(0.05D);
        List<VoxelShape> extra = OverlayBarrierConfig.collisionShapes(self.getWorld(), query);
        if (extra.isEmpty()) return;

        cir.setReturnValue(adjustMovementForCollisions(movement, self.getBoundingBox(), extra));
    }
}
