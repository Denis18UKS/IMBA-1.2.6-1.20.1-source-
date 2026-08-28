package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.client.ClientMaskData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A fixation teleport is not real walking. Vanilla first-person view bob can
 * interpret the one-tick position delta as a large step and add its own camera
 * transform on top of the otherwise-correct statue position.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererFixationMixin {
    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void imba$disableViewBobWhileStatue(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && ClientMaskData.isStatue(client.player.getUuid())) {
            ci.cancel();
        }
    }
}
