package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.client.ClientGameState;
import fable.hideseek.imba.client.ClientMaskData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class ClientPlayerInputMixin {
    @Inject(method = "tick", at = @At("RETURN"))
    private void tick(boolean slowDown, float f, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        Input input = (Input) (Object) this;

        // Semantic Minecraft sneak state: respects any user-bound keyboard or mouse button.
        boolean rawSneaking = input.sneaking;

        if (ClientMaskData.isStatue(client.player.getUuid()) || ClientGameState.prepareLocked) {
            input.pressingForward = false;
            input.pressingBack = false;
            input.pressingLeft = false;
            input.pressingRight = false;
            input.jumping = false;
            input.sneaking = false;
            input.movementForward = 0.0F;
            input.movementSideways = 0.0F;
        }

        if (ClientGameState.suppressSneakUntilRelease) {
            input.sneaking = false;
            if (!rawSneaking) ClientGameState.clearSneakSuppression();
        }
    }
}
