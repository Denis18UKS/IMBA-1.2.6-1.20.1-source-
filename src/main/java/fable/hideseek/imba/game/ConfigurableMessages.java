package fable.hideseek.imba.game;

import fable.hideseek.imba.config.MessageSettingsConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Central gate for map/gameplay feedback which admins can toggle at runtime. */
public final class ConfigurableMessages {
    private ConfigurableMessages() {}

    public static void actionBar(ServerPlayerEntity player, String key, Text text) {
        if (player != null && MessageSettingsConfig.isEnabled(key)) player.sendMessage(text, true);
    }

    public static void chat(ServerPlayerEntity player, String key, Text text) {
        if (player != null && MessageSettingsConfig.isEnabled(key)) player.sendMessage(text, false);
    }

    public static void broadcast(MinecraftServer server, String key, Text text, boolean overlay) {
        if (server != null && MessageSettingsConfig.isEnabled(key)) {
            server.getPlayerManager().broadcast(text, overlay);
        }
    }
}
