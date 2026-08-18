package fable.hideseek.imba.mixin;

import fable.hideseek.imba.game.GameManager;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Manual pauses stay manual; only a pause created by GameManager.autoPause is resumed automatically. */
@Mixin(GameManager.class)
public abstract class GameAutoResumeMixin {
    @Shadow(remap = false) private static boolean autoPaused;

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private static void imba$resumeWhenRequiredParticipantsReturn(MinecraftServer server, CallbackInfo ci) {
        if (autoPaused && GameManager.isPaused()) {
            // resumeGame already checks participantsAvailable(server), so this is
            // a no-op until the current hider and at least one active seeker are online.
            GameManager.resumeGame(server);
        }
    }
}
