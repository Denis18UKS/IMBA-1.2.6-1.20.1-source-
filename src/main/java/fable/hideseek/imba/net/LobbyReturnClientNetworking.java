package fable.hideseek.imba.net;

import fable.hideseek.imba.client.ClientGameState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client receiver for the lobby-return blackout state. */
public final class LobbyReturnClientNetworking implements ClientModInitializer {
    public static final net.minecraft.util.Identifier RETURN_BLACKOUT_PACKET =
            LobbyReturnNetworking.RETURN_BLACKOUT_PACKET;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(RETURN_BLACKOUT_PACKET,
                (client, handler, buf, responseSender) -> {
                    boolean enabled = buf.readBoolean();
                    client.execute(() -> ClientGameState.setReturnBlackout(enabled));
                });
    }
}
