package fable.hideseek.imba.util;

import net.minecraft.entity.Entity;

public final class TeamUtil {

    private TeamUtil() {
    }

    public static boolean isInTeam(Entity entity, String teamName) {
        if (entity == null || teamName == null || teamName.isBlank()) {
            return false;
        }

        var team = entity.getScoreboardTeam();
        return team != null && team.getName().equalsIgnoreCase(teamName);
    }
}