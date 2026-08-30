package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.client.ClientGameState;
import fable.hideseek.imba.item.SeekerSwordUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.AttackIndicator;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Redirect(
            method = {"renderCrosshair", "renderHotbar"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;"),
            require = 0)
    private Object imba$hideSeekerAttackIndicator(SimpleOption<?> option) {
        Object value = option.getValue();
        MinecraftClient client = MinecraftClient.getInstance();
        if (value instanceof AttackIndicator
                && client.player != null
                && SeekerSwordUtil.isSeekerSword(client.player.getMainHandStack())) {
            return AttackIndicator.OFF;
        }
        return value;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void imba$renderLobbyReturnBlackout(DrawContext context, float tickDelta, CallbackInfo ci) {
        float alpha = ClientGameState.returnBlackoutAlpha;
        if (alpha <= 0.001F) return;
        MinecraftClient client = MinecraftClient.getInstance();
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        context.getMatrices().push();
        context.getMatrices().translate(0.0F, 0.0F, 10000.0F);
        context.fill(0, 0, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight(), a << 24);
        context.getMatrices().pop();
    }
}
