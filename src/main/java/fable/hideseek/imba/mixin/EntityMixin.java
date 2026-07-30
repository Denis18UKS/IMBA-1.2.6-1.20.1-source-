package fable.hideseek.imba.mixin;

import fable.hideseek.imba.game.GameRoles;
import fable.hideseek.imba.mask.MaskCollisionShapes;
import fable.hideseek.imba.mask.MaskState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "adjustMovementForSneaking", at = @At("HEAD"), cancellable = true)
    private void imba$walkOnMaskWhileSneaking(Vec3d movement, net.minecraft.entity.MovementType type,
            CallbackInfoReturnable<Vec3d> cir) {
        Entity self = (Entity) (Object) this;
        if (self.getWorld().isClient || !self.isSneaking()
                || !(self instanceof PlayerEntity seeker) || !GameRoles.isSeeker(seeker)) {
            return;
        }

        Box current = self.getBoundingBox();
        Box moved = current.offset(movement.x, 0.0D, movement.z);
        for (PlayerEntity masked : self.getWorld().getPlayers()) {
            if (masked == seeker || !MaskState.isStatue(masked.getUuid())) {
                continue;
            }
            for (Box obstacle : MaskCollisionShapes.create(MaskState.get(masked.getUuid()))) {
                boolean standing = Math.abs(current.minY - obstacle.maxY) <= 0.08D;
                boolean staysAbove = moved.maxX > obstacle.minX && moved.minX < obstacle.maxX
                        && moved.maxZ > obstacle.minZ && moved.minZ < obstacle.maxZ;
                if (standing && staysAbove) {
                    cir.setReturnValue(movement);
                    return;
                }
            }
        }
    }

    @Inject(method = "collidesWith", at = @At("HEAD"), cancellable = true)
    private void imba$disableVanillaMaskCollision(Entity other, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        boolean selfMasked = self instanceof PlayerEntity player && MaskState.isStatue(player.getUuid());
        boolean otherMasked = other instanceof PlayerEntity player && MaskState.isStatue(player.getUuid());
        if (selfMasked || otherMasked) {
            // Full block masks are handled by GameManager's stable block-style
            // collision. Every other mask intentionally has no physical push.
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isInsideWall", at = @At("HEAD"), cancellable = true)
    private void insideWall(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof PlayerEntity player) {
            if (MaskState.hasMask(player.getUuid())) {
                cir.setReturnValue(false);
            }
        }
    }
}
