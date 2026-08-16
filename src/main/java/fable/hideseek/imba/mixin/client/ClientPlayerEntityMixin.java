package fable.hideseek.imba.mixin.client;

import com.mojang.authlib.GameProfile;
import fable.hideseek.imba.client.ClientMaskData;
import fable.hideseek.imba.client.ClientStatueLock;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends PlayerEntity {
    @Shadow public Input input;
    @Unique private boolean imba$wasStatue;

    public ClientPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Inject(method = "isSneaking", at = @At("HEAD"), cancellable = true)
    private void preventSneakingInStatueMode(CallbackInfoReturnable<Boolean> cir) {
        if (ClientMaskData.isStatue(getUuid())) cir.setReturnValue(false);
    }

    @Inject(method = "tickMovement", at = @At("HEAD"), cancellable = true)
    private void blockMovementInStatueMode(CallbackInfo ci) {
        if (!ClientMaskData.isStatue(getUuid())) return;
        clearMovementInput();
        ClientStatueLock.apply(this);
        ci.cancel();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void handleStatueTransition(CallbackInfo ci) {
        boolean statue = ClientMaskData.isStatue(getUuid());
        if (statue && !imba$wasStatue) {
            Vec3d anchor = ClientMaskData.getStatueAnchor(getUuid());
            if (anchor != null) ClientStatueLock.enter(this, anchor.x, anchor.y, anchor.z);
        }
        imba$wasStatue = statue;
        if (!statue) return;
        clearMovementInput();
        setSneaking(false);
        ClientStatueLock.apply(this);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void keepStatueStable(CallbackInfo ci) {
        if (ClientMaskData.isStatue(getUuid())) ClientStatueLock.apply(this);
    }

    @Unique
    private void clearMovementInput() {
        if (input == null) return;
        input.movementForward = 0.0F;
        input.movementSideways = 0.0F;
        input.jumping = false;
        input.sneaking = false;
    }
}
