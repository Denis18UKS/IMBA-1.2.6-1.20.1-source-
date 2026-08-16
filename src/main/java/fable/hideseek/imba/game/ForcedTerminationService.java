package fable.hideseek.imba.game;

import fable.hideseek.imba.config.RoundRestoreConfig;
import fable.hideseek.imba.mask.MaskService;
import fable.hideseek.imba.mask.MaskState;
import fable.hideseek.imba.net.MaskNetworking;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

/** Hard administrative cleanup for /imba_termination. */
public final class ForcedTerminationService {
    private ForcedTerminationService() {}

    public static void terminate(MinecraftServer server) {
        if (server == null) return;
        if (!GameManager.resetRound(server)) GameManager.stopStandaloneTimer(server);

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (MaskState.hasMask(player.getUuid())) MaskService.resetMask(player);
            else MaskState.reset(player.getUuid());
            player.getInventory().clear();
            player.clearStatusEffects();
            player.setSneaking(false);
            player.setNoGravity(false);
            player.setVelocity(Vec3d.ZERO);
            player.fallDistance = 0.0F;
            player.setExperienceLevel(0);
            player.setExperiencePoints(0);
            player.setHealth(player.getMaxHealth());
            server.getScoreboard().clearPlayerTeam(player.getEntityName());
            player.teleport(server.getOverworld(), GameConfig.LOBBY_POS.x, GameConfig.LOBBY_POS.y,
                    GameConfig.LOBBY_POS.z, GameConfig.LOBBY_YAW, GameConfig.LOBBY_PITCH);
            player.removeStatusEffect(StatusEffects.BLINDNESS);
            player.removeStatusEffect(StatusEffects.SLOWNESS);
        }

        // This clears mask/statue entries belonging to currently offline players too.
        MaskState.resetAll();
        GameRoles.clearParticipantTeams(server);
        RoundRestoreConfig.restoreEnabled(server);
        MaskNetworking.resyncAll(server);
        MaskNetworking.broadcastPanelData(server);
        server.getPlayerManager().broadcast(
                net.minecraft.text.Text.literal("§cИгра принудительно завершена и полностью очищена"), false);
    }
}
