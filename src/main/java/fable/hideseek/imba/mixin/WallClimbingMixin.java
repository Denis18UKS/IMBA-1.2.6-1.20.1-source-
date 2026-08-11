package fable.hideseek.imba.mixin;

import fable.hideseek.imba.game.GameRoles;
import fable.hideseek.imba.mask.MaskState;
import fable.hideseek.imba.mask.MaskService;
import fable.hideseek.imba.util.PlayerLadderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class WallClimbingMixin extends Entity {

    public WallClimbingMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    private boolean imba$canSpiderClimb(PlayerEntity player) {
        if (!MaskState.hasMask(player.getUuid())) {
            return false;
        }
        MaskState state = MaskState.get(player.getUuid());
        return !state.statue
                && state.wallClimbing
                && MaskService.supportsWallClimbing(state);
    }

    private boolean imba$isOnPlayerLadder(LivingEntity entity) {
        return entity instanceof PlayerEntity player
                && GameRoles.isSeeker(player)
                && PlayerLadderHelper.isTouchingPlayerLadder(entity);
    }

    @Inject(method = "isClimbing", at = @At("HEAD"), cancellable = true)
    private void imba$allowCustomClimbing(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (self instanceof PlayerEntity player
                && imba$canSpiderClimb(player)
                && this.horizontalCollision) {
            cir.setReturnValue(true);
            return;
        }

        if (imba$isOnPlayerLadder(self)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void imba$applyCustomClimbingMotion(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (self instanceof PlayerEntity player
                && imba$canSpiderClimb(player)
                && this.horizontalCollision) {
            self.fallDistance = 0.0F;
            Vec3d velocity = self.getVelocity();
            double y = self.isSneaking() ? Math.min(0.0D, velocity.y) : Math.max(velocity.y, 0.20D);
            self.setVelocity(velocity.x, y, velocity.z);
            return;
        }

        if (!imba$isOnPlayerLadder(self)) {
            return;
        }

        self.fallDistance = 0.0F;
        Vec3d velocity = self.getVelocity();
        double y = Math.max(velocity.y, -0.15D);

        if (PlayerLadderHelper.isMovingTowardPlayerLadder(self) && !self.isSneaking()) {
            y = Math.max(y, 0.20D);
        } else if (self.isSneaking()) {
            y = 0.0D;
        }

        self.setVelocity(velocity.x, y, velocity.z);
    }

    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void imba$disablePlayerPushForMasks(Entity other, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        boolean selfMasked = self instanceof PlayerEntity player && MaskState.isStatue(player.getUuid());
        boolean otherMasked = other instanceof PlayerEntity player && MaskState.isStatue(player.getUuid());
        if (selfMasked || otherMasked) {
            ci.cancel();
        }
    }
}
