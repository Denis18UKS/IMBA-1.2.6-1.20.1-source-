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
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
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
        if (!GameRoles.isHider(player)) {
            return false;
        }

        if (!MaskState.hasMask(player.getUuid())) {
            return false;
        }

        MaskState state = MaskState.get(player.getUuid());

        if (state.statue) {
            return false;
        }

        return state.wallClimbing && MaskService.supportsWallClimbing(state);
    }

    private boolean imba$isOnPlayerLadder(LivingEntity entity) {
        return PlayerLadderHelper.isNearPlayerLadder(entity);
    }

    @Inject(method = "isClimbing", at = @At("HEAD"), cancellable = true)
    private void imba$allowCustomClimbing(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (self instanceof PlayerEntity player) {
            if (imba$canSpiderClimb(player) && this.horizontalCollision) {
                cir.setReturnValue(true);
                return;
            }
        }

        if (imba$isOnPlayerLadder(self)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void imba$applyCustomClimbingMotion(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        boolean spiderClimb = self instanceof PlayerEntity player
                && imba$canSpiderClimb(player)
                && this.horizontalCollision;

        boolean playerLadder = imba$isOnPlayerLadder(self);

        if (spiderClimb) {
            self.fallDistance = 0.0f;

            Vec3d vel = self.getVelocity();
            double newY = vel.y;

            if (newY < 0.0D) {
                newY = 0.0D;
            }

            newY = Math.max(newY, 0.20D);
            self.setVelocity(vel.x, newY, vel.z);
            return;
        }

        if (playerLadder) {
            self.fallDistance = 0.0f;

            Vec3d vel = self.getVelocity();
            double newY = vel.y;

            boolean movingIntoLadder = this.horizontalCollision || vel.horizontalLengthSquared() > 1.0E-4D;

            if (movingIntoLadder && !self.isSneaking()) {
                newY = Math.max(newY, 0.20D);
                if (!self.getWorld().isClient && self.age % 8 == 0) {
                    self.getWorld().playSound(null, self.getBlockPos(), SoundEvents.BLOCK_LADDER_STEP,
                            SoundCategory.BLOCKS, 0.35F, 1.0F);
                }
            }

            if (newY < -0.15D) {
                newY = -0.15D;
            }

            self.setVelocity(vel.x, newY, vel.z);
        }
    }

    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void imba$disablePlayerPushForMasks(Entity other, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        boolean selfMasked = self instanceof PlayerEntity player && MaskState.isStatue(player.getUuid());
        boolean otherMasked = other instanceof PlayerEntity player && MaskState.isStatue(player.getUuid());
        if (selfMasked || otherMasked) ci.cancel();
    }
}
