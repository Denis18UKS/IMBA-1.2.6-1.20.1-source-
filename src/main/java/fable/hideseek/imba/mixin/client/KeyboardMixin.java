package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.config.ItemRules;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void preventDropAndSwap(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.player.isCreative())
            return;
        if (action == 0)
            return;

        ItemStack mainHand = client.player.getMainHandStack();
        ItemStack offHand = client.player.getOffHandStack();

        boolean hasRestrictedInHands = ItemRules.isRestricted(mainHand) || ItemRules.isRestricted(offHand);
        if (!hasRestrictedInHands)
            return;

        if (key == 81) {
            ci.cancel();
            return;
        }

        if (key == 70) {
            ci.cancel();
        }
    }
}