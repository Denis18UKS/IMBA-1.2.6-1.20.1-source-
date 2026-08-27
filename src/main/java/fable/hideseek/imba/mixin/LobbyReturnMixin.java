package fable.hideseek.imba.mixin;

import fable.hideseek.imba.game.GameManager;
import fable.hideseek.imba.net.LobbyReturnNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the whole lobby return hidden until players have been separated. */
@Mixin(GameManager.class)
public abstract class LobbyReturnMixin {
    private static final String LOBBY_SPREAD_COMMAND =
            "spreadplayers -131.49 148.72 2 5 under -29 false @a";

    @Inject(method = "beginReturn", at = @At("HEAD"), remap = false)
    private static void imba$enableLobbyReturnBlackout(MinecraftServer server, Text message, CallbackInfo ci) {
        LobbyReturnNetworking.broadcastReturnBlackout(server, true);
    }

    @Inject(method = "finishReturn", at = @At("TAIL"), remap = false)
    private static void imba$spreadLobbyPlayersAndReveal(MinecraftServer server, CallbackInfo ci) {
        try {
            server.getCommandManager().executeWithPrefix(server.getCommandSource(), LOBBY_SPREAD_COMMAND);
        } finally {
            LobbyReturnNetworking.broadcastReturnBlackout(server, false);
        }
    }
}
