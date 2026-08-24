package fable.hideseek.imba.mixin;

import fable.hideseek.imba.config.MessageSettingsConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Suppresses only known IMBA messages whose catalogue entry is disabled. */
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerMessageFilterMixin {
    @Inject(method = "sendMessage(Lnet/minecraft/text/Text;Z)V", at = @At("HEAD"), cancellable = true)
    private void imba$filterConfiguredMessages(Text message, boolean overlay, CallbackInfo ci) {
        if (!MessageSettingsConfig.shouldShow(message)) {
            ci.cancel();
        }
    }
}
