package fable.hideseek.imba.client;

import net.minecraft.client.MinecraftClient;

public final class ClientGameState {
    public static boolean paused, prepareLocked;
    public static boolean returnBlackout, returnBlackoutTarget;
    public static float returnBlackoutAlpha;
    public static boolean suppressSneakUntilRelease;

    private ClientGameState() {}

    public static boolean isHider() {
        var p = MinecraftClient.getInstance().player;
        var t = p == null ? null : p.getScoreboardTeam();
        return t != null && "hider".equalsIgnoreCase(t.getName());
    }

    public static boolean isSeeker() {
        var p = MinecraftClient.getInstance().player;
        var t = p == null ? null : p.getScoreboardTeam();
        return t != null && "seeker".equalsIgnoreCase(t.getName());
    }

    public static void suppressSneakForFixation() { suppressSneakUntilRelease = true; }
    public static void clearSneakSuppression() { suppressSneakUntilRelease = false; }

    public static void setReturnBlackout(boolean enabled) { returnBlackoutTarget = enabled; }

    public static void resetReturnBlackout() {
        returnBlackoutTarget = false;
        returnBlackout = false;
        returnBlackoutAlpha = 0.0F;
    }

    public static void tick() {
        final float step = 0.10F; // ~0.5s fade for 60fps/20tps clients
        if (returnBlackoutTarget) {
            returnBlackoutAlpha = Math.min(1.0F, returnBlackoutAlpha + step);
        } else {
            returnBlackoutAlpha = Math.max(0.0F, returnBlackoutAlpha - step);
        }
        returnBlackout = returnBlackoutTarget || returnBlackoutAlpha > 0.001F;
    }
}
