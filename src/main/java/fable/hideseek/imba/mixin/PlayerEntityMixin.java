package fable.hideseek.imba.mixin;

import fable.hideseek.imba.config.ItemRules;
import fable.hideseek.imba.game.GameManager;
import fable.hideseek.imba.mask.MaskHitbox;
import fable.hideseek.imba.mask.MaskService;
import fable.hideseek.imba.mask.MaskState;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    /**
     * Keep the existing mask-specific camera/eye height, but deliberately do
     * not override getDimensions. The server owner's real physics box must stay
     * vanilla so world collision never sees the block-sized disguise bounds.
     */
    @Inject(method = "getActiveEyeHeight", at = @At("HEAD"), cancellable = true)
    private void eyeHeight(EntityPose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!MaskState.hasMask(self.getUuid())) return;
        var state = MaskState.get(self.getUuid());
        cir.setReturnValue(MaskHitbox.getEyeHeight(state.type, state.block, state.item));
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this; if (self.getWorld().isClient) return;
        MaskService.tickResetRecovery(self);
        if (!MaskState.hasMask(self.getUuid())) { self.setNoGravity(false); return; }
        MaskState state = MaskState.get(self.getUuid());
        if (state.statue) { self.setVelocity(0,0,0); self.fallDistance=0.0f; self.setNoGravity(true); double dx=Math.abs(self.getX()-state.anchorX),dy=Math.abs(self.getY()-state.anchorY),dz=Math.abs(self.getZ()-state.anchorZ); if(dx>.01||dy>.01||dz>.01)self.setPosition(state.anchorX,state.anchorY,state.anchorZ); }
        else self.setNoGravity(false);
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void travel(Vec3d movementInput, CallbackInfo ci) { PlayerEntity self=(PlayerEntity)(Object)this; if(MaskState.isStatue(self.getUuid())){self.setVelocity(0,0,0);ci.cancel();} }

    @Inject(method = "tickMovement", at = @At("HEAD"), cancellable = true)
    private void tickMovement(CallbackInfo ci) { PlayerEntity self=(PlayerEntity)(Object)this; if(MaskState.isStatue(self.getUuid())){self.setVelocity(0,0,0);ci.cancel();} }

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void jump(CallbackInfo ci) { PlayerEntity self=(PlayerEntity)(Object)this; if(MaskState.isStatue(self.getUuid())||GameManager.isPrepareLocked(self))ci.cancel(); }

    @Inject(method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;", at = @At("HEAD"), cancellable = true)
    private void preventDrop(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> cir) { PlayerEntity player=(PlayerEntity)(Object)this;if(player.isCreative())return;if(ItemRules.isRestricted(stack))cir.cancel(); }

    @Inject(method = "dropInventory", at = @At("HEAD"), cancellable = true)
    private void preventDropInventory(CallbackInfo ci) { PlayerEntity player=(PlayerEntity)(Object)this;if(player.isCreative())return;for(int i=0;i<player.getInventory().size();i++)if(ItemRules.isRestricted(player.getInventory().getStack(i))){ci.cancel();return;} }
}
