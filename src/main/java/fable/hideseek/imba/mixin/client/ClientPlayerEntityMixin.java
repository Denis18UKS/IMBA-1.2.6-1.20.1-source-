package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.client.ClientMaskData;
import fable.hideseek.imba.client.ClientStatueLock;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends PlayerEntity {

    @Shadow
    public Input input;

    public ClientPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Inject(method = "isSneaking", at = @At("HEAD"), cancellable = true)
    private void preventSneakingInStatueMode(CallbackInfoReturnable<Boolean> cir) {
        if (ClientMaskData.isStatue(this.getUuid())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "tickMovement", at = @At("HEAD"), cancellable = true)
    private void blockSneakingInput(CallbackInfo ci) {
        if (ClientMaskData.isStatue(this.getUuid())) {
            if (this.input != null) {
                this.input.sneaking = false;
            }
            ClientStatueLock.apply(this);
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void blockSneakingInTick(CallbackInfo ci) {
        if (ClientMaskData.isStatue(this.getUuid())) {
            if (this.input != null) {
                this.input.sneaking = false;
            }
            this.setSneaking(false);
            ClientStatueLock.apply(this);
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void keepExactStatueAnchor(CallbackInfo ci) {
        ClientStatueLock.apply(this);
    }
}
