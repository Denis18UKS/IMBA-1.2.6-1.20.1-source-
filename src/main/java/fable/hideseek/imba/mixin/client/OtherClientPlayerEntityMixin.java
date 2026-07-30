package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.client.ClientMaskData;
import fable.hideseek.imba.client.ClientStatueLock;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OtherClientPlayerEntity.class)
public abstract class OtherClientPlayerEntityMixin {
    @Shadow
    private Vec3d clientVelocity;

    @Shadow
    private int velocityLerpDivisor;

    @Inject(method = "tickMovement", at = @At("HEAD"), cancellable = true)
    private void stopStatueInterpolation(CallbackInfo ci) {
        OtherClientPlayerEntity player = (OtherClientPlayerEntity) (Object) this;
        if (ClientMaskData.isStatue(player.getUuid())) {
            clientVelocity = Vec3d.ZERO;
            velocityLerpDivisor = 0;
            ClientStatueLock.apply(player);
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void keepExactStatueAnchor(CallbackInfo ci) {
        ClientStatueLock.apply((OtherClientPlayerEntity) (Object) this);
    }

    @Inject(method = "setVelocityClient", at = @At("HEAD"), cancellable = true)
    private void ignoreStatueVelocity(double x, double y, double z, CallbackInfo ci) {
        OtherClientPlayerEntity player = (OtherClientPlayerEntity) (Object) this;
        if (ClientMaskData.isStatue(player.getUuid())) {
            clientVelocity = Vec3d.ZERO;
            velocityLerpDivisor = 0;
            ci.cancel();
        }
    }
}
