package fable.hideseek.imba.client;

import net.minecraft.client.MinecraftClient;

public final class ClientGameState {
    public static boolean paused, prepareLocked;
    /** Suppresses the actual configured sneak input until the user releases/toggles it off. */
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

    public static void suppressSneakForFixation() {
        suppressSneakUntilRelease = true;
    }

    public static void clearSneakSuppression() {
        suppressSneakUntilRelease = false;
    }

    public static void tick() {}
}
