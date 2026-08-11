package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.client.ClientPlayerLadderHelper;
import fable.hideseek.imba.game.GameRoles;
import net.minecraft.client.MinecraftClient;
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
public abstract class ClientPlayerLadderMixin extends Entity {
    protected ClientPlayerLadderMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    private boolean imba$isLocalSeekerOnPlayerLadder() {
        LivingEntity self = (LivingEntity) (Object) this;
        return self instanceof PlayerEntity player
                && MinecraftClient.getInstance().player == self
                && GameRoles.isSeeker(player)
                && ClientPlayerLadderHelper.isTouchingPlayerLadder(self);
    }

    @Inject(method = "isClimbing", at = @At("HEAD"), cancellable = true)
    private void imba$usePlayerLadderForPrediction(CallbackInfoReturnable<Boolean> cir) {
        if (imba$isLocalSeekerOnPlayerLadder()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void imba$applyPlayerLadderMotion(CallbackInfo ci) {
        if (!imba$isLocalSeekerOnPlayerLadder()) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        self.fallDistance = 0.0F;
        Vec3d velocity = self.getVelocity();
        double y = Math.max(velocity.y, -0.15D);
        if (ClientPlayerLadderHelper.isMovingTowardPlayerLadder(self) && !self.isSneaking()) {
            y = Math.max(y, 0.20D);
        } else if (self.isSneaking()) {
            y = 0.0D;
        }
        self.setVelocity(velocity.x, y, velocity.z);
    }
}
