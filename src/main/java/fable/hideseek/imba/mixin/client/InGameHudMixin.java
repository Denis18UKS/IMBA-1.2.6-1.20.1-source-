package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.item.SeekerSwordUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.AttackIndicator;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The vanilla attack-ready marker changes as soon as the crosshair finds the
 * invisible hider entity. Hide only that marker while the seeker sword is held;
 * the normal crosshair and the player's setting remain untouched.
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
}
