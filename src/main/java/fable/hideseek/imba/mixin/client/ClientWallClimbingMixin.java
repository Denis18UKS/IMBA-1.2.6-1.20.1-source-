package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.ImbaMod;
import fable.hideseek.imba.client.ClientMaskData;
import fable.hideseek.imba.client.WallClimbClientNetworking;
import fable.hideseek.imba.mask.MaskType;
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

/** Local prediction for wall-climbing masks. The server remains authoritative. */
@Mixin(LivingEntity.class)
public abstract class ClientWallClimbingMixin extends Entity {
    protected ClientWallClimbingMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    private boolean imba$canWallClimb() {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof PlayerEntity player)
                || MinecraftClient.getInstance().player != self
                || !ClientMaskData.hasMask(player.getUuid())
                || ClientMaskData.isStatue(player.getUuid())
                || !WallClimbClientNetworking.isEnabled(player.getUuid())) {
            return false;
        }
        MaskType type = ClientMaskData.TYPES.get(player.getUuid());
        return type == MaskType.WALL_CLIMB
                || type == MaskType.BLOCK && ClientMaskData.BLOCKS.get(player.getUuid()) == ImbaMod.GLOWBERRIES;
    }

    @Inject(method = "isClimbing", at = @At("HEAD"), cancellable = true)
    private void imba$predictWallClimbing(CallbackInfoReturnable<Boolean> cir) {
        if (imba$canWallClimb() && this.horizontalCollision) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void imba$predictWallClimbMotion(CallbackInfo ci) {
        if (!imba$canWallClimb() || !this.horizontalCollision) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        self.fallDistance = 0.0F;
        Vec3d velocity = self.getVelocity();
        double y = self.isSneaking()
                ? Math.min(0.0D, velocity.y)
                : Math.max(velocity.y, 0.20D);
        self.setVelocity(velocity.x, y, velocity.z);
    }
}
