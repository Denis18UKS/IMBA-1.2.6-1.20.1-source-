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

/**
 * HUD-specific IMBA behavior: hide the seeker attack-ready marker and cover the
 * complete game view while a finished round is being returned to the lobby.
 */
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
        if (!ClientGameState.returnBlackout) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        context.fill(
                0,
                0,
                client.getWindow().getScaledWidth(),
                client.getWindow().getScaledHeight(),
                0xFF000000);
    }
}
