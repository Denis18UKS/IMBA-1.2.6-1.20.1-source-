package fable.hideseek.imba.client;

import fable.hideseek.imba.net.BlockRulesNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.HashSet;
import java.util.Set;

public final class BlockRulesClientNetworking {
    private BlockRulesClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(BlockRulesNetworking.SYNC,
                (client, handler, buf, sender) -> {
                    int interactiveCount = Math.max(0, Math.min(buf.readVarInt(), 100000));
                    Set<String> interactive = new HashSet<>();
                    for (int i = 0; i < interactiveCount; i++) interactive.add(buf.readString(256));
                    int breakCount = Math.max(0, Math.min(buf.readVarInt(), 100000));
                    Set<String> breakable = new HashSet<>();
                    for (int i = 0; i < breakCount; i++) breakable.add(buf.readString(256));
                    client.execute(() -> {
                        if (client.currentScreen instanceof BlockRulesScreen screen) {
                            screen.applyServerState(interactive, breakable);
                        }
                    });
                });
    }
}
