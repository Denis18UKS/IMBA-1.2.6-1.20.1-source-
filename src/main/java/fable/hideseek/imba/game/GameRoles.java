package fable.hideseek.imba.game;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public final class GameRoles {

    public static final String HIDER_TEAM = "hider";
    public static final String SEEKER_TEAM = "seeker";

    private GameRoles() {
    }

    public static boolean isHider(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        AbstractTeam team = player.getScoreboardTeam();
        return team != null && HIDER_TEAM.equalsIgnoreCase(team.getName());
    }

    public static boolean isSeeker(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        AbstractTeam team = player.getScoreboardTeam();
        return team != null && SEEKER_TEAM.equalsIgnoreCase(team.getName());
    }

    public static boolean isParticipant(PlayerEntity player) {
        return isHider(player) || isSeeker(player);
    }

    public static ServerPlayerEntity getFirstHider(MinecraftServer server) {
        if (server == null) {
            return null;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (isHider(player)) {
                return player;
            }
        }
        return null;
    }

    public static ServerPlayerEntity getFirstSeeker(MinecraftServer server) {
        List<ServerPlayerEntity> seekers = getSeekers(server);
        return seekers.isEmpty() ? null : seekers.get(0);
    }

    public static List<ServerPlayerEntity> getSeekers(MinecraftServer server) {
        List<ServerPlayerEntity> seekers = new ArrayList<>();
        if (server == null) {
            return seekers;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (isSeeker(player)) {
                seekers.add(player);
            }
        }
        return seekers;
    }

    /**
     * The player who presses the start block becomes the only hider. Every
     * other connected player becomes a seeker for the new round.
     */
    public static List<ServerPlayerEntity> assignForStartButton(
            MinecraftServer server,
            ServerPlayerEntity hider) {
        List<ServerPlayerEntity> seekers = new ArrayList<>();
        if (server == null || hider == null) {
            return seekers;
        }

        Team hiderTeam = getOrCreateTeam(server, HIDER_TEAM);
        Team seekerTeam = getOrCreateTeam(server, SEEKER_TEAM);

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            server.getScoreboard().clearPlayerTeam(player.getEntityName());
            if (player.getUuid().equals(hider.getUuid())) {
                server.getScoreboard().addPlayerToTeam(player.getEntityName(), hiderTeam);
            } else {
                server.getScoreboard().addPlayerToTeam(player.getEntityName(), seekerTeam);
                seekers.add(player);
            }
        }
        return seekers;
    }

    /** Removes both online and offline scoreboard members from round teams. */
    public static void clearParticipantTeams(MinecraftServer server) {
        if (server == null) {
            return;
        }
        clearTeamMembers(server, server.getScoreboard().getTeam(HIDER_TEAM));
        clearTeamMembers(server, server.getScoreboard().getTeam(SEEKER_TEAM));
    }

    private static void clearTeamMembers(MinecraftServer server, Team team) {
        if (team == null) {
            return;
        }
        for (String playerName : new ArrayList<>(team.getPlayerList())) {
            server.getScoreboard().clearPlayerTeam(playerName);
        }
    }

    private static Team getOrCreateTeam(MinecraftServer server, String name) {
        Team team = server.getScoreboard().getTeam(name);
        return team == null ? server.getScoreboard().addTeam(name) : team;
    }
}
