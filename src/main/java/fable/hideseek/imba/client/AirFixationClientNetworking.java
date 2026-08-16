package fable.hideseek.imba.client;

import fable.hideseek.imba.config.AirFixationConfig;
import fable.hideseek.imba.net.AirFixationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AirFixationClientNetworking {
    private AirFixationClientNetworking() {}
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(AirFixationNetworking.SYNC, (client, handler, buf, sender) -> {
            int count = Math.max(0, Math.min(buf.readVarInt(), 100000));
            Map<String, AirFixationConfig.Rule> values = new LinkedHashMap<>();
            AirFixationConfig.Mode[] modes = AirFixationConfig.Mode.values();
            for (int i = 0; i < count; i++) {
                String key = buf.readString(320);
                int ordinal = buf.readUnsignedByte();
                String required = buf.readString(256);
                AirFixationConfig.Mode mode = ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : AirFixationConfig.Mode.DENY;
                values.put(key, new AirFixationConfig.Rule(mode, required));
            }
            client.execute(() -> { if (client.currentScreen instanceof AirFixationConfigScreen screen) screen.applyServerState(values); });
        });
    }
}
