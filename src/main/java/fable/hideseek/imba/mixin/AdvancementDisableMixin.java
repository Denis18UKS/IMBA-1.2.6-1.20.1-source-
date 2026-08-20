package fable.hideseek.imba.mixin;

import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.PlayerAdvancementTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** IMBA is a map/gameplay mod: vanilla advancement progress is intentionally disabled. */
@Mixin(PlayerAdvancementTracker.class)
public abstract class AdvancementDisableMixin {
    @Inject(method = "grantCriterion", at = @At("HEAD"), cancellable = true)
    private void imba$disableAdvancements(Advancement advancement, String criterionName,
                                          CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
