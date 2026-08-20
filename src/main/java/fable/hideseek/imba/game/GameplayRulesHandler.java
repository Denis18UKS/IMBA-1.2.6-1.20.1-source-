package fable.hideseek.imba.game;

import fable.hideseek.imba.config.BreakRulesConfig;
import fable.hideseek.imba.item.SeekerSwordUtil;
import fable.hideseek.imba.mask.MaskService;
import fable.hideseek.imba.mask.MaskState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowerPotBlock;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.inventory.Inventory;
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

public final class GameplayRulesHandler {
    private static boolean wasGameActive;
    private static boolean wasReturnPhase;
    private static final Map<UUID, RegistryKey<World>> LAST_WORLD = new HashMap<>();
    private static final Map<UUID, Integer> LAST_SEEKER_SWORDS = new HashMap<>();
    /** UUIDs whose glowing effect was explicitly added by IMBA. */
    private static final Set<UUID> IMBA_GLOWING = new HashSet<>();

    private GameplayRulesHandler() {}

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient || player.isCreative()) return ActionResult.PASS;
            if (entity instanceof Inventory && !BreakRulesConfig.isInteractiveEntityAllowed(entity.getType())) {
                player.sendMessage(Text.literal("§cЭта интерактивная сущность запрещена настройками карты"), true);
                return ActionResult.FAIL;
            }
            if (entity instanceof ItemFrameEntity frame
                    && frame.getHeldItemStack().isEmpty()
                    && !player.getStackInHand(hand).isEmpty()) {
                player.sendMessage(Text.literal("§cКласть предметы в рамки можно только в креативе"), true);
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (player.isCreative()) return ActionResult.PASS;

            var pos = hit.getBlockPos();
            var state = world.getBlockState(pos);

            // Block this on both logical sides. Returning FAIL client-side prevents
            // vanilla from optimistically removing the flower and creating the
            // short-lived "phantom" item before the server rejects the action.
            if (state.getBlock() instanceof FlowerPotBlock && !state.isOf(Blocks.FLOWER_POT)) {
                return ActionResult.FAIL;
            }

            if (world.isClient) return ActionResult.PASS;
            if (state.createScreenHandlerFactory(world, pos) != null
                    && !BreakRulesConfig.isInteractiveAllowed(state.getBlock())) {
                player.sendMessage(Text.literal("§cЭтот интерактивный блок запрещён настройками карты"), true);
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world.isClient || player.isCreative() || world.getBlockState(pos).isAir()) return ActionResult.PASS;
            if (BreakRulesConfig.isAdventureBreakAllowed(world.getBlockState(pos).getBlock())) {
                world.breakBlock(pos, false, player);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(GameplayRulesHandler::tick);
    }

    private static void tick(MinecraftServer server) {
        boolean active = GameManager.isGameActive();
        boolean returnPhase = GameManager.isReturnPhase();

        if (!wasReturnPhase && returnPhase) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (GameRoles.isHider(player)) player.getInventory().clear();
            }
        }
        wasReturnPhase = returnPhase;

        Set<UUID> online = new HashSet<>();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID id = player.getUuid();
            online.add(id);
            syncSeekerGlow(player, active);

            RegistryKey<World> world = player.getWorld().getRegistryKey();
            int swords = countSeekerSwords(player);
            RegistryKey<World> previousWorld = LAST_WORLD.get(id);
            Integer previousSwordCount = LAST_SEEKER_SWORDS.get(id);
            if (GameRoles.isSeeker(player)
                    && !GameManager.isPrepareLocked(player)
                    && previousWorld != null
                    && previousSwordCount != null
                    && !previousWorld.equals(world)
                    && swords > previousSwordCount) {
                removeExtraSeekerSwords(player, swords - previousSwordCount);
                swords = countSeekerSwords(player);
            }
            LAST_WORLD.put(id, world);
            LAST_SEEKER_SWORDS.put(id, swords);
        }

        LAST_WORLD.keySet().removeIf(id -> !online.contains(id));
        LAST_SEEKER_SWORDS.keySet().removeIf(id -> !online.contains(id));

        if (wasGameActive && !active) cleanupAfterRound(server);
        wasGameActive = active;
    }

    private static void syncSeekerGlow(ServerPlayerEntity player, boolean gameActive) {
        UUID id = player.getUuid();
        boolean shouldGlow = gameActive
                && GameRoles.isSeeker(player)
                && GameManager.isCurrentParticipant(player);
        StatusEffectInstance current = player.getStatusEffect(StatusEffects.GLOWING);

        if (shouldGlow) {
            if (IMBA_GLOWING.contains(id)) {
                if (current == null || current.getDuration() < 20) {
                    player.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.GLOWING, 60, 0, false, false, false));
                }
            } else if (current == null) {
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.GLOWING, 60, 0, false, false, false));
                IMBA_GLOWING.add(id);
            }
            return;
        }

        if (IMBA_GLOWING.remove(id)) {
            player.removeStatusEffect(StatusEffects.GLOWING);
        }
    }

    private static int countSeekerSwords(ServerPlayerEntity player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (SeekerSwordUtil.isSeekerSword(player.getInventory().getStack(i))) count++;
        }
        return count;
    }

    private static void removeExtraSeekerSwords(ServerPlayerEntity player, int amount) {
        for (int i = player.getInventory().size() - 1; i >= 0 && amount > 0; i--) {
            if (SeekerSwordUtil.isSeekerSword(player.getInventory().getStack(i))) {
                player.getInventory().setStack(i, net.minecraft.item.ItemStack.EMPTY);
                amount--;
            }
        }
    }

    private static void cleanupAfterRound(MinecraftServer server) {
        GameRoles.clearParticipantTeams(server);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (IMBA_GLOWING.remove(player.getUuid())) {
                player.removeStatusEffect(StatusEffects.GLOWING);
            }
            if (MaskState.hasMask(player.getUuid())) MaskService.resetMask(player);
            player.setSneaking(false);
            player.setNoGravity(false);
            player.calculateDimensions();
            player.setVelocity(0, 0, 0);
            player.fallDistance = 0;
            player.requestTeleport(player.getX(), player.getY(), player.getZ());
        }
    }
}
