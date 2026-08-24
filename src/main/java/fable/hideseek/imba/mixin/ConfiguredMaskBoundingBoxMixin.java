package fable.hideseek.imba.mixin;

import fable.hideseek.imba.config.MaskHitboxConfig;
import fable.hideseek.imba.mask.MaskState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Server-side entity bounding box for static NON-FULL block disguises. */
@Mixin(Entity.class)
public abstract class ConfiguredMaskBoundingBoxMixin {
    @Inject(method = "calculateBoundingBox", at = @At("HEAD"), cancellable = true)
    private void imba$configuredMaskBox(CallbackInfoReturnable<Box> cir) {
        Entity self = (Entity) (Object) this;
        if (self.getWorld().isClient || !(self instanceof PlayerEntity player)
                || !MaskState.isStatue(player.getUuid())) return;

        MaskState state = MaskState.get(player.getUuid());
        if (state.block == null) return;
        Box box = MaskHitboxConfig.worldBox(state.block, state.rotation,
                state.anchorX, state.anchorY, state.anchorZ);
        if (box != null) cir.setReturnValue(box);
    }
}
