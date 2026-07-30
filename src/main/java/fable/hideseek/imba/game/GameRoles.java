package fable.hideseek.imba.game;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class GameRoles {

    public static final String HIDER_TEAM = "hider";
    public static final String SEEKER_TEAM = "seeker";

    private GameRoles() {
    }

    public static boolean isHider(PlayerEntity player) {
        AbstractTeam team = player.getScoreboardTeam();
        return team != null && HIDER_TEAM.equalsIgnoreCase(team.getName());
    }

    public static boolean isSeeker(PlayerEntity player) {
        AbstractTeam team = player.getScoreboardTeam();
        return team != null && SEEKER_TEAM.equalsIgnoreCase(team.getName());
    }

    public static ServerPlayerEntity getFirstHider(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (isHider(player)) return player;
        }
        return null;
    }

    public static ServerPlayerEntity getFirstSeeker(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (isSeeker(player)) return player;
        }
        return null;
    }
}