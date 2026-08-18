package fable.hideseek.imba.game;

import fable.hideseek.imba.config.MessageSettingsConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Optional action-bar feedback useful for diagnostics/gameplay, now with per-message gates. */
public final class GameMessages {
    private GameMessages() {}

    public static void send(ServerPlayerEntity player, Text message) {
        if (player != null && GameConfig.SHOW_GAMEPLAY_MESSAGES) player.sendMessage(message, true);
    }

    public static void send(ServerPlayerEntity player, String key, Text message) {
        if (player != null && MessageSettingsConfig.isEnabled(key)) {
            player.sendMessage(message, true);
        }
    }
}
