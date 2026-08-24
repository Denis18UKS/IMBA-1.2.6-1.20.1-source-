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

    /**
     * Run before GameManager.tick can hit its paused early-return. The previous
     * TAIL injection was unreachable while paused, which made auto-resume a
     * permanent no-op exactly when it was needed. resumeGame performs the
     * authoritative participantsAvailable(server) check and clears autoPaused
     * only after the required hider/seeker set is really back online.
     */
    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private static void imba$resumeWhenRequiredParticipantsReturn(MinecraftServer server, CallbackInfo ci) {
        if (autoPaused && GameManager.isPaused()) {
            GameManager.resumeGame(server);
        }
    }
}
