package fable.hideseek.imba.game;

import fable.hideseek.imba.config.MessageSettingsConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Central helper for optional action-bar feedback. */
public final class GameMessages {
    private GameMessages() {
    }

    public static void send(ServerPlayerEntity player, Text message) {
        if (player != null && MessageSettingsConfig.shouldShow(message)) {
            player.sendMessage(message, true);
        }
    }
}
