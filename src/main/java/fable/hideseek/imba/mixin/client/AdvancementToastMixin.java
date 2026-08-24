package fable.hideseek.imba.mixin.client;

import net.minecraft.client.toast.AdvancementToast;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** IMBA maps do not show vanilla advancement/achievement popups. */
@Mixin(ToastManager.class)
public abstract class AdvancementToastMixin {
    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void imba$hideAdvancementToasts(Toast toast, CallbackInfo ci) {
        if (toast instanceof AdvancementToast) ci.cancel();
    }
}
