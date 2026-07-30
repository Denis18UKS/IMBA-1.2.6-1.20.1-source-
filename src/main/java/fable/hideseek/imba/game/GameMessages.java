package fable.hideseek.imba.game;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Optional action-bar feedback that is useful for testing, but noisy in a match. */
public final class GameMessages {
    private GameMessages() {
    }

    public static void send(ServerPlayerEntity player, Text message) {
        if (player != null && GameConfig.SHOW_GAMEPLAY_MESSAGES) {
            player.sendMessage(message, true);
        }
    }
}
