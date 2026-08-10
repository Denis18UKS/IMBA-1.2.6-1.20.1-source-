package fable.hideseek.imba.game;

import fable.hideseek.imba.mask.MaskService;
import fable.hideseek.imba.mask.MaskState;
import fable.hideseek.imba.item.SeekerSwordUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Map-wide restrictions that are independent from the seeker combat rules.
 * They deliberately live outside GameManager so interaction protection also
 * works while the round is idle.
 */
public final class GameplayRulesHandler {
    private static boolean wasGameActive;
    private static boolean wasReturnPhase;
    private static final Map<UUID, RegistryKey<World>> LAST_WORLD = new HashMap<>();
    private static final Map<UUID, Integer> LAST_SEEKER_SWORDS = new HashMap<>();

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
        boolean returnPhase = GameManager.isReturnPhase();

        // As soon as the round result is decided, the hider must lose both
        // "Скрыться" and "Надеть модель" (and every other round item), not
        // several seconds later when the lobby return finishes.
        if (!wasReturnPhase && returnPhase) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (GameRoles.isHider(player)) {
                    player.getInventory().clear();
                }
            }
        }
        wasReturnPhase = returnPhase;

        // Do not let a world/dimension transition manufacture a new seeker
        // sword. Preserve exactly the amount the player had before the world
        // changed. PREPARE is excluded because that is the one legitimate
        // moment where the initial round loadout can be given while moving a
        // seeker back to the lobby.
        Set<UUID> online = new HashSet<>();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID id = player.getUuid();
            online.add(id);
            RegistryKey<World> worldKey = player.getWorld().getRegistryKey();
            int swordCount = countSeekerSwords(player);
            RegistryKey<World> previousWorld = LAST_WORLD.get(id);
            Integer previousCount = LAST_SEEKER_SWORDS.get(id);

            if (GameRoles.isSeeker(player)
                    && !GameManager.isPrepareLocked(player)
                    && previousWorld != null
                    && previousCount != null
                    && !previousWorld.equals(worldKey)
                    && swordCount > previousCount) {
                removeExtraSeekerSwords(player, swordCount - previousCount);
                swordCount = countSeekerSwords(player);
            }

            LAST_WORLD.put(id, worldKey);
            LAST_SEEKER_SWORDS.put(id, swordCount);
        }
        LAST_WORLD.keySet().removeIf(id -> !online.contains(id));
        LAST_SEEKER_SWORDS.keySet().removeIf(id -> !online.contains(id));

        if (wasGameActive && !active) {
            cleanupAfterRound(server);
        }
        wasGameActive = active;
    }

    private static int countSeekerSwords(ServerPlayerEntity player) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            if (SeekerSwordUtil.isSeekerSword(player.getInventory().getStack(slot))) {
                count++;
            }
        }
        return count;
    }

    private static void removeExtraSeekerSwords(ServerPlayerEntity player, int amount) {
        for (int slot = player.getInventory().size() - 1; slot >= 0 && amount > 0; slot--) {
            if (SeekerSwordUtil.isSeekerSword(player.getInventory().getStack(slot))) {
                player.getInventory().setStack(slot, net.minecraft.item.ItemStack.EMPTY);
                amount--;
            }
        }
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
