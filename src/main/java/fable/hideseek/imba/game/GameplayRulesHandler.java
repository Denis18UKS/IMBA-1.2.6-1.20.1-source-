package fable.hideseek.imba.game;

import fable.hideseek.imba.mask.MaskService;
import fable.hideseek.imba.mask.MaskState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

/**
 * Map-wide restrictions that are independent from the seeker combat rules.
 * They deliberately live outside GameManager so interaction protection also
 * works while the round is idle.
 */
public final class GameplayRulesHandler {
    private static boolean wasGameActive;

    private GameplayRulesHandler() {
    }

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient || player.isCreative()) {
                return ActionResult.PASS;
            }

            if (entity instanceof ItemFrameEntity frame
                    && frame.getHeldItemStack().isEmpty()
                    && !player.getStackInHand(hand).isEmpty()) {
                player.sendMessage(Text.literal("§cКласть предметы в рамки можно только в креативе"), true);
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient || player.isCreative()) {
                return ActionResult.PASS;
            }

            var pos = hitResult.getBlockPos();
            var state = world.getBlockState(pos);
            if (state.createScreenHandlerFactory(world, pos) != null) {
                player.sendMessage(Text.literal("§cОткрывать интерактивные блоки можно только в креативе"), true);
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(GameplayRulesHandler::tick);
    }

    private static void tick(MinecraftServer server) {
        boolean active = GameManager.isGameActive();
        if (wasGameActive && !active) {
            cleanupAfterRound(server);
        }
        wasGameActive = active;
    }

    private static void cleanupAfterRound(MinecraftServer server) {
        GameRoles.clearParticipantTeams(server);

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (MaskState.hasMask(player.getUuid())) {
                MaskService.resetMask(player);
            }

            // Force the normal player dimensions/pose to be recalculated on
            // both sides. This prevents the former hider from keeping stale
            // mask movement dimensions until the first sneak toggle.
            player.setSneaking(false);
            player.setNoGravity(false);
            player.calculateDimensions();
            player.setVelocity(0.0D, 0.0D, 0.0D);
            player.fallDistance = 0.0F;
            player.requestTeleport(player.getX(), player.getY(), player.getZ());
        }
    }
}
