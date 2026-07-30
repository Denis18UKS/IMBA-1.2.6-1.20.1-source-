package fable.hideseek.imba.game;

import fable.hideseek.imba.ImbaMod;
import fable.hideseek.imba.boosty_toggle.BoostyToogler;
import fable.hideseek.imba.config.BreakRulesConfig;
import fable.hideseek.imba.item.ModelEquipHandler;
import fable.hideseek.imba.item.SeekerSwordUtil;
import fable.hideseek.imba.item.TeleportToolHandler;
import fable.hideseek.imba.mask.MaskService;
import fable.hideseek.imba.mask.MaskCollisionShapes;
import fable.hideseek.imba.mask.MaskState;
import fable.hideseek.imba.mask.MaskType;
import fable.hideseek.imba.net.MaskNetworking;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.Block;
import net.minecraft.block.FlowerPotBlock;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.scoreboard.Team;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class GameManager {

    private static final int EFFECT_FOREVER = Integer.MAX_VALUE;
    private static final double SEEKER_INTERACT_REACH = 4.5D;
    private static final Identifier APPLE_ID = new Identifier("minecraft", "apple");
    private static final Identifier ATTACHED_PUMPKIN_STEM_ID =
            new Identifier("minecraft", "attached_pumpkin_stem");
    private static final Identifier HANGING_LANTERN_ID = new Identifier("imba", "hanging_lantern");

    private enum Phase {
        IDLE,
        PREPARE,
        ROUND,
        RETURN
    }

    private static Phase phase = Phase.IDLE;

    private static int currentRoundIndex = -1;
    private static int prepareTicks = 0;
    private static int roundTicks = 0;
    private static int returnTicks = 0;
    private static int standaloneTimerTicks = 0;

    private static ServerPlayerEntity currentHider;
    private static ServerPlayerEntity currentSeeker;
    private static RoundDefinition currentRound;
    private static final Map<UUID, Long> interactionCooldowns = new HashMap<>();
    private static final Map<UUID, Long> blockAttackTicks = new HashMap<>();
    private static final Map<UUID, Set<UUID>> sculkContacts = new HashMap<>();
    private static final Map<UUID, Integer> portalContacts = new HashMap<>();
    private static final Map<UUID, String> lockedTeams = new HashMap<>();
    private static boolean paused;
    private static Vec3d prepareSeekerAnchor;

    private GameManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(GameManager::tick);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();

            if (!isGameActive() && GameConfig.SPAWN_TO_LOBBY_IF_GAME_INACTIVE) {
                teleportToLobby(player);
            }

            forceAdventure(player);
            restorePlayerHealth(player);
            if (isGameActive()) {
                lockedTeams.put(player.getUuid(), player.getScoreboardTeam() == null
                        ? "" : player.getScoreboardTeam().getName());
            }
            MaskNetworking.syncAllTo(player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (isGameActive() && (isCurrentHider(player) || isCurrentSeeker(player))) {
                beginReturn(server, Text.literal("§cРаунд сброшен: один из участников вышел"));
            }
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) {
                return ActionResult.PASS;
            }

            if (!(player instanceof ServerPlayerEntity seeker)) {
                return ActionResult.PASS;
            }

            if (!(entity instanceof ServerPlayerEntity target)) {
                return ActionResult.PASS;
            }

            if (!GameRoles.isSeeker(seeker)) {
                return ActionResult.PASS;
            }

            if (tryInteractMaskedPlayer(seeker, target)) {
                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient) {
                return ActionResult.PASS;
            }

            Block usedBlock = world.getBlockState(hitResult.getBlockPos()).getBlock();
            if (!player.isCreative()
                    && (usedBlock == net.minecraft.block.Blocks.CAVE_VINES
                    || usedBlock == net.minecraft.block.Blocks.CAVE_VINES_PLANT
                    || usedBlock == ImbaMod.GLOWBERRIES)) {
                // The map's glow berries are decoration and must not be picked.
                return ActionResult.FAIL;
            }

            if (!player.isCreative()
                    && usedBlock instanceof FlowerPotBlock) {
                return ActionResult.FAIL;
            }

            if (!(player instanceof ServerPlayerEntity seeker)) {
                return ActionResult.PASS;
            }

            if (!GameRoles.isSeeker(seeker)) {
                return ActionResult.PASS;
            }

            if (tryInteractMaskedPlayerByBlock(seeker, hitResult.getBlockPos())) {
                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) {
                return ActionResult.PASS;
            }

            if (entity instanceof ItemFrameEntity && !player.isCreative()) {
                return ActionResult.FAIL;
            }

            if (!(player instanceof ServerPlayerEntity seeker)) {
                return ActionResult.PASS;
            }

            if (!GameRoles.isSeeker(seeker)) {
                return ActionResult.PASS;
            }

            if (entity instanceof ServerPlayerEntity targetPlayer
                    && MaskState.hasMask(targetPlayer.getUuid())) {
                MaskState targetState = MaskState.get(targetPlayer.getUuid());
                if (targetState.statue && isRotatableFrameItem(targetState)
                        && !(phase == Phase.ROUND && isCurrentSeeker(seeker)
                        && SeekerSwordUtil.isSeekerSword(seeker.getMainHandStack()))) {
                    rotateFrameItem(seeker, targetPlayer);
                    return ActionResult.FAIL;
                }
            }

            if (!SeekerSwordUtil.isSeekerSword(seeker.getMainHandStack())) {
                return ActionResult.FAIL;
            }

            if (!canPunishSeekerAttack(seeker)) {
                return ActionResult.PASS;
            }

            if (entity instanceof ServerPlayerEntity targetPlayer) {
                if (GameRoles.isHider(targetPlayer)) {
                    if (phase == Phase.ROUND && isCurrentHider(targetPlayer)) {
                        finishSeekerWin(seeker.getServer());
                    }
                    return ActionResult.PASS;
                }

                damageSeekerHeart(seeker,
                        "§cМинус сердце: удар не по игроку из команды прячущихся");
                return ActionResult.FAIL;
            }

            damageSeekerHeart(seeker, "§cМинус сердце: удар не по игроку");
            return ActionResult.FAIL;
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world.isClient) {
                return ActionResult.PASS;
            }

            if (!(player instanceof ServerPlayerEntity seeker)) {
                return ActionResult.PASS;
            }

            if (!GameRoles.isSeeker(seeker)) {
                return ActionResult.PASS;
            }

            if (!canPunishSeekerAttack(seeker)) {
                return ActionResult.PASS;
            }

            return handleSeekerBlockAttack(seeker, pos);
        });

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            restorePlayerHealth(newPlayer);
            updateCurrentPlayerReference(newPlayer);
            MaskNetworking.syncAllTo(newPlayer);
        });
    }

    public static boolean isGameActive() {
        return phase != Phase.IDLE;
    }

    public static boolean isPaused() {
        return paused;
    }

    public static boolean isPrepareLocked(PlayerEntity player) {
        return phase == Phase.PREPARE && isCurrentSeeker(player);
    }

    public static boolean pauseGame(MinecraftServer server) {
        if (!isGameActive() || phase == Phase.RETURN || paused) {
            return false;
        }
        paused = true;
        MaskNetworking.broadcastGameState(server);
        server.getPlayerManager().broadcast(Text.literal("§eИгра поставлена на паузу"), false);
        return true;
    }

    public static boolean resumeGame(MinecraftServer server) {
        if (!isGameActive() || !paused) {
            return false;
        }
        paused = false;
        MaskNetworking.broadcastGameState(server);
        server.getPlayerManager().broadcast(Text.literal("§aИгра продолжена"), false);
        return true;
    }

    public static boolean resetRound(MinecraftServer server) {
        if (!isGameActive()) {
            return false;
        }
        server.getPlayerManager().broadcast(Text.literal("§cРаунд полностью сброшен"), false);
        finishReturn(server);
        return true;
    }

    public static boolean startStandaloneTimer(MinecraftServer server, int seconds) {
        if (phase != Phase.IDLE) {
            return false;
        }

        standaloneTimerTicks = Math.max(1, seconds) * 20;
        server.getPlayerManager().broadcast(Text.literal("§bТестовый таймер запущен на " + seconds + " сек."), false);
        return true;
    }

    public static void stopStandaloneTimer(MinecraftServer server) {
        standaloneTimerTicks = 0;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.setExperienceLevel(0);
            player.setExperiencePoints(0);
        }

        server.getPlayerManager().broadcast(Text.literal("§7Тестовый таймер остановлен"), false);
    }

    public static void handleSeekerMiss(ServerPlayerEntity seeker) {
        if (!canPunishSeekerAttack(seeker)) {
            return;
        }

        damageSeekerHeart(seeker, "§cМинус сердце: промах");
    }

    public static ActionResult handleSeekerBlockAttack(ServerPlayerEntity seeker, BlockPos pos) {
        if (!canPunishSeekerAttack(seeker) || pos == null || seeker.getWorld().getBlockState(pos).isAir()) {
            return ActionResult.PASS;
        }
        long tick = seeker.getWorld().getTime();
        Long previousAttack = blockAttackTicks.put(seeker.getUuid(), tick);
        if (previousAttack != null && previousAttack == tick) {
            // The client packet and Fabric callback can describe the same click.
            // The first path already applied the result.
            return ActionResult.SUCCESS;
        }
        Block block = seeker.getWorld().getBlockState(pos).getBlock();
        if (BreakRulesConfig.shouldPunishForBreak(block)) {
            damageSeekerHeart(seeker, "§cМинус сердце: удар по неправильному блоку");
            return ActionResult.FAIL;
        }
        seeker.getWorld().breakBlock(pos, true, seeker);
        return ActionResult.SUCCESS;
    }

    public static void handleSeekerUse(ServerPlayerEntity seeker) {
        if (seeker == null || !GameRoles.isSeeker(seeker)) {
            return;
        }

        tryInteractMaskedPlayerByRay(seeker);
    }

    public static void startNextRound(MinecraftServer server) {
        if (server == null) {
            return;
        }
        if (phase != Phase.IDLE) {
            return;
        }
        if (GameConfig.ROUNDS.isEmpty()) {
            return;
        }

        standaloneTimerTicks = 0;

        ServerPlayerEntity hider = GameRoles.getFirstHider(server);
        ServerPlayerEntity seeker = GameRoles.getFirstSeeker(server);

        if (hider == null || seeker == null) {
            server.getPlayerManager().broadcast(Text.literal(
                    "§cДля запуска нужны минимум 2 игрока: один прячущийся и один искатель"), false);
            return;
        }

        currentRoundIndex = GameConfig.SELECTED_LOCATION;
        GameConfig.setSelectedLocation(currentRoundIndex);
        currentRound = GameConfig.getSelectedLocation();

        currentHider = hider;
        currentSeeker = seeker;

        phase = Phase.PREPARE;
        prepareTicks = GameConfig.PREPARE_SECONDS * 20;
        roundTicks = GameConfig.ROUND_SECONDS * 20;
        returnTicks = 0;
        paused = false;
        sculkContacts.clear();
        portalContacts.clear();
        lockParticipantTeams(server);

        if (currentRound.setEvening) {
            ServerWorld roundWorld = server.getWorld(currentRound.worldKey);
            if (roundWorld != null) {
                roundWorld.setTimeOfDay(13000);
            }
        }

        clearForRound(hider);
        clearForRound(seeker);

        forceAdventure(hider);
        forceAdventure(seeker);

        teleport(hider, currentRound.worldKey, currentRound.hiderPos, currentRound.hiderYaw, currentRound.hiderPitch);
        teleportToLobby(seeker);
        prepareSeekerAnchor = seeker.getPos();

        giveRoundItems(hider, currentRound);
        giveSeekerLoadout(seeker);

        seeker.addStatusEffect(new StatusEffectInstance(
                StatusEffects.BLINDNESS,
                prepareTicks,
                0,
                false,
                false,
                false));

        seeker.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SLOWNESS,
                prepareTicks,
                10,
                false,
                false,
                false));

        restorePlayerHealth(hider);
        restorePlayerHealth(seeker);

        server.getPlayerManager().broadcast(
                Text.literal(
                        "§eРаунд начался. У Юни есть " + GameConfig.PREPARE_SECONDS + " секунд, чтобы спрятаться."),
                false);
        MaskNetworking.broadcastGameState(server);
    }

    public static void stopAndReturnToLobby(MinecraftServer server) {
        if (server == null) {
            return;
        }
        if (phase == Phase.IDLE) {
            return;
        }

        beginReturn(server, Text.literal("§cРаунд остановлен"));
    }

    private static void tick(MinecraftServer server) {
        keepPeaceful(server);
        keepAdventure(server);
        keepFoodFull(server);
        tickButtonStates(server);
        handleMaskCollisions(server);
        tickPortalMasks(server);
        tickSculkVeins(server);
        maintainRoleEffects(server);
        enforceLockedTeams(server);

        if ((phase == Phase.PREPARE || phase == Phase.ROUND) && !participantsAvailable(server)) {
            beginReturn(server, Text.literal("§cРаунд сброшен: участник недоступен"));
            return;
        }
        if (paused) {
            if (phase == Phase.PREPARE) {
                lockPreparingSeeker();
            }
            return;
        }

        switch (phase) {
            case IDLE -> tickStandaloneTimer(server);
            case PREPARE -> tickPrepare(server);
            case ROUND -> tickRound(server);
            case RETURN -> tickReturn(server);
        }
    }

    private static void tickStandaloneTimer(MinecraftServer server) {
        if (standaloneTimerTicks <= 0) {
            return;
        }

        standaloneTimerTicks--;
        int secondsLeft = Math.max(0, standaloneTimerTicks / 20);

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.setExperienceLevel(secondsLeft);
            player.setExperiencePoints(0);
        }

        if (standaloneTimerTicks <= 0) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                player.setExperienceLevel(0);
                player.setExperiencePoints(0);
            }
            server.getPlayerManager().broadcast(Text.literal("§bТестовый таймер завершён"), false);
        }
    }

    private static void tickPrepare(MinecraftServer server) {
        lockPreparingSeeker();
        prepareTicks--;
        int secondsLeft = Math.max(0, (prepareTicks + 19) / 20);

        if (currentHider != null) {
            currentHider.setExperienceLevel(secondsLeft);
            currentHider.setExperiencePoints(0);
        }
        if (currentSeeker != null) {
            currentSeeker.setExperienceLevel(secondsLeft);
            currentSeeker.setExperiencePoints(0);
        }

        if (prepareTicks <= 0) {
            releaseSeeker(server);
        }
    }

    private static void releaseSeeker(MinecraftServer server) {
        phase = Phase.ROUND;
        prepareSeekerAnchor = null;

        if (currentSeeker != null && currentRound != null) {
            teleport(
                    currentSeeker,
                    currentRound.worldKey,
                    currentRound.seekerPos,
                    currentRound.seekerYaw,
                    currentRound.seekerPitch);

            currentSeeker.removeStatusEffect(StatusEffects.BLINDNESS);
            currentSeeker.removeStatusEffect(StatusEffects.SLOWNESS);
            restorePlayerHealth(currentSeeker);
            currentSeeker.setExperienceLevel(GameConfig.ROUND_SECONDS);
            currentSeeker.setExperiencePoints(0);
        }
        if (currentHider != null) {
            currentHider.setExperienceLevel(GameConfig.ROUND_SECONDS);
            currentHider.setExperiencePoints(0);
        }

        server.getPlayerManager().broadcast(Text.literal("§6Искатель выпущен. Время пошло!"), false);
        MaskNetworking.broadcastGameState(server);
    }

    private static void tickRound(MinecraftServer server) {
        roundTicks--;

        int secondsLeft = Math.max(0, roundTicks / 20);

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.setExperienceLevel(secondsLeft);
            player.setExperiencePoints(0);
        }

        if (currentSeeker != null && currentSeeker.getHealth() <= 0.0f) {
            finishHiderWinByHearts(server);
            return;
        }

        if (roundTicks <= 0) {
            finishHiderWinByTime(server);
        }
    }

    private static void tickReturn(MinecraftServer server) {
        returnTicks--;

        if (returnTicks <= 0) {
            finishReturn(server);
        }
    }

    private static void finishSeekerWin(MinecraftServer server) {
        beginReturn(server, Text.literal("§aИскатель нашёл Юни!"));
    }

    private static void finishHiderWinByHearts(MinecraftServer server) {
        String hiderName = currentHider == null ? "Юни" : currentHider.getName().getString();
        String message = BoostyToogler.isEnabled()
                ? "Слышишь \"хи-хи-хи\"? Это " + hiderName + " хихикает, ведь у кого-то закончились сердечки!"
                : "Слышишь \"хи-хи-хи\"? Это Юни хихикает, ведь у кого-то закончились сердечки!";

        beginReturn(server, Text.literal("§e" + message));
    }

    private static void finishHiderWinByTime(MinecraftServer server) {
        String hiderName = currentHider == null ? "Юни" : currentHider.getName().getString();
        String message = BoostyToogler.isEnabled()
                ? "Слышишь \"хи-хи-хи\"? Это " + hiderName + " хихикает, ведь кое-кто не успел!"
                : "Слышишь \"хи-хи-хи\"? Это Юни хихикает, ведь кое-кто не успел её найти!";

        beginReturn(server, Text.literal("§e" + message));
    }

    private static void beginReturn(MinecraftServer server, Text message) {
        MaskNetworking.broadcastPanelData(server);
        server.getPlayerManager().broadcast(message, false);

        phase = Phase.RETURN;
        paused = false;
        prepareSeekerAnchor = null;
        returnTicks = GameConfig.RETURN_TO_LOBBY_SECONDS * 20;
        MaskNetworking.broadcastGameState(server);

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            removeSeekerSword(player);
            if (MaskState.hasMask(player.getUuid())) {
                MaskService.resetMask(player);
            }
            player.clearStatusEffects();

            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.BLINDNESS,
                    returnTicks,
                    0,
                    false,
                    false,
                    false));

            player.setExperienceLevel(0);
            player.setExperiencePoints(0);
        }

        if (currentRound != null && currentRound.setEvening) {
            ServerWorld roundWorld = server.getWorld(currentRound.worldKey);
            if (roundWorld != null) {
                roundWorld.setTimeOfDay(1000);
            }
        }
    }

    private static void finishReturn(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (MaskState.hasMask(player.getUuid())) MaskService.resetMask(player);
            player.clearStatusEffects();
            teleportToLobby(player);
            clearForRound(player);
            restorePlayerHealth(player);
            player.removeStatusEffect(StatusEffects.BLINDNESS);
            player.setExperienceLevel(0);
            player.setExperiencePoints(0);
        }

        currentHider = null;
        currentSeeker = null;
        currentRound = null;

        prepareTicks = 0;
        roundTicks = 0;
        returnTicks = 0;
        paused = false;
        prepareSeekerAnchor = null;
        lockedTeams.clear();
        sculkContacts.clear();
        portalContacts.clear();

        phase = Phase.IDLE;
        MaskNetworking.broadcastGameState(server);
    }

    private static boolean tryInteractMaskedPlayer(ServerPlayerEntity seeker, ServerPlayerEntity targetPlayer) {
        if (!MaskState.hasMask(targetPlayer.getUuid())) {
            return false;
        }

        MaskState state = MaskState.get(targetPlayer.getUuid());
        if (!state.statue) {
            return false;
        }

        if (state.type == MaskType.DOOR) {
            toggleDoor(seeker, targetPlayer, state);
            return true;
        }

        if (state.type == MaskType.BUTTON) {
            pressButton(seeker, targetPlayer, state);
            return true;
        }

        if (isRotatableFrameItem(state)) {
            return rotateFrameItem(seeker, targetPlayer);
        }

        return false;
    }

    private static boolean tryInteractMaskedPlayerByBlock(ServerPlayerEntity seeker, BlockPos clickedPos) {
        if (seeker.getServer() == null) {
            return false;
        }

        for (ServerPlayerEntity target : seeker.getServer().getPlayerManager().getPlayerList()) {
            if (!MaskState.hasMask(target.getUuid())) {
                continue;
            }

            MaskState state = MaskState.get(target.getUuid());
            if (!state.statue) {
                continue;
            }

            BlockPos basePos = BlockPos.ofFloored(state.anchorX, state.anchorY, state.anchorZ);

            if (state.type == MaskType.DOOR) {
                if (clickedPos.equals(basePos) || clickedPos.equals(basePos.up())) {
                    toggleDoor(seeker, target, state);
                    return true;
                }
            }

            if (state.type == MaskType.BUTTON) {
                if (clickedPos.equals(basePos)) {
                    pressButton(seeker, target, state);
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean tryInteractMaskedPlayerByRay(ServerPlayerEntity seeker) {
        if (seeker.getServer() == null) {
            return false;
        }

        Vec3d start = seeker.getCameraPosVec(1.0f);
        Vec3d end = start.add(seeker.getRotationVec(1.0f).multiply(SEEKER_INTERACT_REACH));

        ServerPlayerEntity bestTarget = null;
        MaskState bestState = null;
        double bestDistanceSq = Double.MAX_VALUE;

        for (ServerPlayerEntity target : seeker.getServer().getPlayerManager().getPlayerList()) {
            if (target == seeker) {
                continue;
            }
            if (!MaskState.hasMask(target.getUuid())) {
                continue;
            }

            MaskState state = MaskState.get(target.getUuid());
            if (!state.statue) {
                continue;
            }

            boolean supportedType = state.type == MaskType.DOOR
                    || state.type == MaskType.BUTTON
                    || isRotatableFrameItem(state);

            if (!supportedType) {
                continue;
            }

            Box interactionBox = createInteractionBox(state).expand(0.125D);
            var hit = interactionBox.raycast(start, end);
            if (hit.isEmpty()) {
                continue;
            }

            double distanceSq = start.squaredDistanceTo(hit.get());
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                bestTarget = target;
                bestState = state;
            }
        }

        if (bestTarget == null || bestState == null) {
            return false;
        }

        if (bestState.type == MaskType.DOOR) {
            toggleDoor(seeker, bestTarget, bestState);
            return true;
        }

        if (bestState.type == MaskType.BUTTON) {
            pressButton(seeker, bestTarget, bestState);
            return true;
        }

        if (isRotatableFrameItem(bestState)) {
            return rotateFrameItem(seeker, bestTarget);
        }

        return false;
    }

    private static boolean isRotatableFrameItem(MaskState state) {
        return (state.type == MaskType.ITEM || state.type == MaskType.WALL_CLIMB)
                && state.statue
                && !MaskService.isSpecialPotion(state.item);
    }

    private static boolean rotateFrameItem(ServerPlayerEntity actor, ServerPlayerEntity target) {
        long tick = target.getWorld().getTime();
        Long previous = interactionCooldowns.put(actor.getUuid(), tick);
        if (previous != null && tick - previous < 3L) {
            return true;
        }
        MaskState.rotateFrameStep(target.getUuid());
        actor.swingHand(Hand.MAIN_HAND, true);
        MaskNetworking.refresh(target);
        return true;
    }

    private static Box createInteractionBox(MaskState state) {
        if (state.type == MaskType.DOOR) {
            return new Box(
                    state.anchorX - 0.5,
                    state.anchorY,
                    state.anchorZ - 0.5,
                    state.anchorX + 0.5,
                    state.anchorY + 2.0,
                    state.anchorZ + 0.5);
        }

        return new Box(
                state.anchorX - 0.5,
                state.anchorY,
                state.anchorZ - 0.5,
                state.anchorX + 0.5,
                state.anchorY + 1.0,
                state.anchorZ + 0.5);
    }

    private static void toggleDoor(ServerPlayerEntity actor, ServerPlayerEntity targetPlayer, MaskState state) {
        long tick = targetPlayer.getWorld().getTime();
        UUID actorId = actor == null ? targetPlayer.getUuid() : actor.getUuid();
        Long previous = interactionCooldowns.put(actorId, tick);
        if (previous != null && tick - previous < 3) return;
        state.doorOpen = !state.doorOpen;

        targetPlayer.getWorld().playSound(
                null,
                BlockPos.ofFloored(state.anchorX, state.anchorY, state.anchorZ),
                state.doorOpen ? SoundEvents.BLOCK_WOODEN_DOOR_OPEN : SoundEvents.BLOCK_WOODEN_DOOR_CLOSE,
                SoundCategory.BLOCKS,
                1.0f,
                1.0f);

        if (actor != null) {
            actor.swingHand(Hand.MAIN_HAND, true);
        }

        MaskNetworking.refresh(targetPlayer);
    }

    private static void pressButton(ServerPlayerEntity actor, ServerPlayerEntity targetPlayer, MaskState state) {
        long tick = targetPlayer.getWorld().getTime();
        Long previous = interactionCooldowns.put(actor.getUuid(), tick);
        if (previous != null && tick - previous < 3L) {
            return;
        }
        state.buttonPressed = true;
        state.buttonTicks = 20;
        actor.swingHand(Hand.MAIN_HAND, true);
        MaskNetworking.refresh(targetPlayer);
    }

    private static void tickButtonStates(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!MaskState.hasMask(player.getUuid())) {
                continue;
            }

            MaskState state = MaskState.get(player.getUuid());
            if (state.buttonTicks > 0) {
                state.buttonTicks--;

                if (state.buttonTicks <= 0 && state.buttonPressed) {
                    state.buttonPressed = false;
                    MaskNetworking.refresh(player);
                }
            }
        }
    }

    /**
     * Treats a statue mask as world geometry for seekers. This deliberately does
     * not use vanilla player pushing, so it continues to work when every team's
     * collisionRule is NEVER and never enables player-vs-player shoving.
     */
    private static void handleMaskCollisions(MinecraftServer server) {
        for (ServerPlayerEntity maskedPlayer : server.getPlayerManager().getPlayerList()) {
            if (!MaskState.hasMask(maskedPlayer.getUuid())) {
                continue;
            }

            MaskState state = MaskState.get(maskedPlayer.getUuid());
            if (!state.statue) {
                continue;
            }
            if (!MaskService.hasPhysicalCollision(state)) {
                continue;
            }

            for (Box maskBox : MaskCollisionShapes.create(state)) {
              for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
                if (other == maskedPlayer || !GameRoles.isSeeker(other)) {
                    continue;
                }
                if (other.isSpectator()) {
                    continue;
                }

                Box otherBox = other.getBoundingBox();
                boolean overlapsXZ = otherBox.maxX > maskBox.minX && otherBox.minX < maskBox.maxX
                        && otherBox.maxZ > maskBox.minZ && otherBox.minZ < maskBox.maxZ;
                // Give the top face a small landing tolerance so fast downward
                // movement cannot tunnel through a one-block-high mask.
                if (overlapsXZ && other.getVelocity().y <= 0.0D
                        && otherBox.minY >= maskBox.maxY - 0.35D
                        && otherBox.minY <= maskBox.maxY + 0.20D) {
                    if (Math.abs(other.getY() - maskBox.maxY) > 0.001D) {
                        other.setPosition(other.getX(), maskBox.maxY, other.getZ());
                    }
                    Vec3d velocity = other.getVelocity();
                    other.setVelocity(velocity.x, 0.0D, velocity.z);
                    other.setOnGround(true);
                    other.fallDistance = 0.0F;
                    continue;
                }

                if (!otherBox.intersects(maskBox)) continue;

                double dx = other.getX() - state.anchorX;
                double dz = other.getZ() - state.anchorZ;

                double overlapX = Math.min(otherBox.maxX - maskBox.minX, maskBox.maxX - otherBox.minX);
                double overlapZ = Math.min(otherBox.maxZ - maskBox.minZ, maskBox.maxZ - otherBox.minZ);

                if (overlapX <= 0.0 || overlapZ <= 0.0) {
                    continue;
                }

                double pushX = 0.0;
                double pushZ = 0.0;

                if (overlapX < overlapZ) {
                    pushX = dx >= 0.0 ? overlapX : -overlapX;
                } else {
                    pushZ = dz >= 0.0 ? overlapZ : -overlapZ;
                }

                other.setPosition(other.getX() + pushX, other.getY(), other.getZ() + pushZ);
                // Correct penetration without applying a player push impulse.
                // Thus the hider is an immovable block, not a collidable player.
                Vec3d velocity = other.getVelocity();
                other.setVelocity(pushX == 0.0D ? velocity.x : 0.0D, velocity.y,
                        pushZ == 0.0D ? velocity.z : 0.0D);
              }
            }
        }
    }

    private static boolean canPunishSeekerAttack(ServerPlayerEntity seeker) {
        if (seeker == null || !GameRoles.isSeeker(seeker)) {
            return false;
        }
        if (!SeekerSwordUtil.isSeekerSword(seeker.getMainHandStack())) {
            return false;
        }
        return phase == Phase.ROUND && isCurrentSeeker(seeker);
    }

    private static void damageSeekerHeart(ServerPlayerEntity seeker, String message) {
        float newHealth = seeker.getHealth() - 2.0f;
        seeker.setHealth(Math.max(0.0f, newHealth));
        GameMessages.send(seeker, Text.literal(message));
    }

    private static void giveRoundItems(ServerPlayerEntity hider, RoundDefinition round) {
        Item iconItem = Registries.ITEM.get(round.giveItemId);
        if (ATTACHED_PUMPKIN_STEM_ID.equals(round.maskId)) {
            iconItem = ImbaMod.PUMPKIN_STEM_ICON;
        }
        if (iconItem == net.minecraft.item.Items.AIR && round.sourceKind == RoundDefinition.SourceKind.BLOCK) {
            Block sourceBlock = Registries.BLOCK.get(round.maskId);
            iconItem = sourceBlock.asItem();
        }
        if (iconItem == net.minecraft.item.Items.AIR) {
            iconItem = ImbaMod.MODEL_TOKEN;
        }

        ItemStack modelStack = ModelEquipHandler.createModelItem(
                iconItem,
                round.displayWord,
                round.sourceKind.name(),
                round.maskId);

        hider.getInventory().setStack(8, modelStack);
        hider.getInventory().setStack(0, new ItemStack(ImbaMod.HIDE_BUTTON));

        if (APPLE_ID.equals(round.maskId) || HANGING_LANTERN_ID.equals(round.maskId)) {
            hider.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.JUMP_BOOST,
                    EFFECT_FOREVER,
                    1,
                    false,
                    false,
                    false));
        }
    }

    private static void giveSeekerLoadout(ServerPlayerEntity seeker) {
        seeker.getInventory().insertStack(SeekerSwordUtil.createSword());
    }

    private static void removeSeekerSword(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            if (SeekerSwordUtil.isSeekerSword(player.getInventory().getStack(slot))) {
                player.getInventory().setStack(slot, ItemStack.EMPTY);
            }
        }
    }

    private static void teleport(
            ServerPlayerEntity player,
            net.minecraft.registry.RegistryKey<net.minecraft.world.World> worldKey,
            Vec3d pos,
            float yaw,
            float pitch) {
        if (player == null || player.getServer() == null) {
            return;
        }

        ServerWorld world = player.getServer().getWorld(worldKey);
        if (world == null) {
            return;
        }

        player.teleport(world, pos.x, pos.y, pos.z, yaw, pitch);
    }

    private static void teleportToLobby(ServerPlayerEntity player) {
        if (player == null || player.getServer() == null) {
            return;
        }

        ServerWorld world = player.getServer().getOverworld();
        player.teleport(
                world,
                GameConfig.LOBBY_POS.x,
                GameConfig.LOBBY_POS.y,
                GameConfig.LOBBY_POS.z,
                GameConfig.LOBBY_YAW,
                GameConfig.LOBBY_PITCH);
    }

    private static void restorePlayerHealth(ServerPlayerEntity player) {
        player.setHealth(player.getMaxHealth());
    }

    private static void clearForRound(ServerPlayerEntity player) {
        if (MaskState.hasMask(player.getUuid())) {
            MaskService.resetMask(player);
        }

        player.getInventory().clear();
        player.removeStatusEffect(StatusEffects.JUMP_BOOST);
        player.removeStatusEffect(StatusEffects.BLINDNESS);
        player.removeStatusEffect(StatusEffects.SLOWNESS);
        player.removeStatusEffect(StatusEffects.INVISIBILITY);
        player.setNoGravity(false);
        player.setVelocity(0.0, 0.0, 0.0);
        player.fallDistance = 0.0f;
    }

    private static void updateCurrentPlayerReference(ServerPlayerEntity newPlayer) {
        if (currentHider != null && currentHider.getUuid().equals(newPlayer.getUuid())) {
            currentHider = newPlayer;
        }

        if (currentSeeker != null && currentSeeker.getUuid().equals(newPlayer.getUuid())) {
            currentSeeker = newPlayer;
        }
    }

    private static boolean isCurrentHider(PlayerEntity player) {
        return currentHider != null && currentHider.getUuid().equals(player.getUuid());
    }

    private static boolean isCurrentSeeker(PlayerEntity player) {
        return currentSeeker != null && currentSeeker.getUuid().equals(player.getUuid());
    }

    private static void forceAdventure(ServerPlayerEntity player) {
        if (!player.isCreative() && !player.isSpectator()) {
            player.changeGameMode(GameMode.ADVENTURE);
        }
    }

    private static void keepAdventure(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            forceAdventure(player);
        }
    }

    private static void keepFoodFull(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.getHungerManager().setFoodLevel(20);
            player.getHungerManager().setSaturationLevel(20.0f);
        }
    }

    private static void tickSculkVeins(MinecraftServer server) {
        Set<UUID> activeSculkMasks = new HashSet<>();
        for (ServerPlayerEntity masked : server.getPlayerManager().getPlayerList()) {
            if (!MaskState.hasMask(masked.getUuid())) {
                continue;
            }
            MaskState state = MaskState.get(masked.getUuid());
            if (!state.statue || state.type != MaskType.SCULK_VEIN) {
                continue;
            }
            activeSculkMasks.add(masked.getUuid());
            Set<UUID> previous = sculkContacts.computeIfAbsent(masked.getUuid(), id -> new HashSet<>());
            Set<UUID> current = new HashSet<>();
            Box trigger = new Box(state.anchorX - .58D, state.anchorY - .10D, state.anchorZ - .58D,
                    state.anchorX + .58D, state.anchorY + 1.20D, state.anchorZ + .58D);
            for (ServerPlayerEntity stepping : server.getPlayerManager().getPlayerList()) {
                if (stepping == masked || stepping.isSpectator() || stepping.getWorld() != masked.getWorld()
                        || !stepping.getBoundingBox().intersects(trigger)) {
                    continue;
                }
                current.add(stepping.getUuid());
                if (!previous.contains(stepping.getUuid())
                        && state.sculkStepCount < MaskState.MAX_SCULK_STEPS) {
                    growSculkVeins(masked.getServerWorld(),
                            BlockPos.ofFloored(state.anchorX, state.anchorY, state.anchorZ));
                    state.sculkStepCount++;
                }
            }
            previous.clear();
            previous.addAll(current);
        }
        sculkContacts.keySet().removeIf(id -> !activeSculkMasks.contains(id));
    }

    private static void growSculkVeins(ServerWorld world, BlockPos origin) {
        int amount = 2 + world.random.nextInt(2), placed = 0;
        for (int attempt = 0; attempt < 64 && placed < amount; attempt++) {
            BlockPos floor = origin.add(world.random.nextInt(9) - 4, 2, world.random.nextInt(9) - 4);
            int searchFloor = Math.max(world.getBottomY(), origin.getY() - 3);
            while (floor.getY() > searchFloor && world.getBlockState(floor).isAir()) floor = floor.down();
            BlockPos target = floor.up();
            if (!world.getBlockState(target).isAir() || !world.getBlockState(floor).isSolidBlock(world, floor)) continue;
            world.setBlockState(target, net.minecraft.block.Blocks.SCULK_VEIN.getDefaultState()
                    .with(net.minecraft.block.MultifaceGrowthBlock.getProperty(net.minecraft.util.math.Direction.DOWN), true), 3);
            placed++;
        }
    }

    private static void tickPortalMasks(MinecraftServer server) {
        Set<UUID> inside = new HashSet<>();
        for (ServerPlayerEntity masked : server.getPlayerManager().getPlayerList()) {
            if (!MaskState.hasMask(masked.getUuid())) {
                continue;
            }
            MaskState state = MaskState.get(masked.getUuid());
            if (!state.statue || state.type != MaskType.PORTAL) {
                continue;
            }
            boolean eastWest = Math.floorMod(Math.round(state.rotation / 90.0F), 2) == 1;
            Box trigger = eastWest
                    ? new Box(state.anchorX - 0.18D, state.anchorY, state.anchorZ - 0.5D,
                            state.anchorX + 0.18D, state.anchorY + 1.0D, state.anchorZ + 0.5D)
                    : new Box(state.anchorX - 0.5D, state.anchorY, state.anchorZ - 0.18D,
                            state.anchorX + 0.5D, state.anchorY + 1.0D, state.anchorZ + 0.18D);
            for (ServerPlayerEntity traveler : server.getPlayerManager().getPlayerList()) {
                if (traveler == masked || traveler.getWorld() != masked.getWorld()
                        || !traveler.getBoundingBox().intersects(trigger)) {
                    continue;
                }
                inside.add(traveler.getUuid());
                int ticks = portalContacts.merge(traveler.getUuid(), 1, Integer::sum);
                if (ticks < Math.max(1, fable.hideseek.imba.config.PortalConfig.DATA.portalTicks)) {
                    continue;
                }
                portalContacts.remove(traveler.getUuid());
                ServerWorld targetWorld = server.getWorld(
                        fable.hideseek.imba.config.PortalConfig.worldKey());
                if (targetWorld != null) {
                    Vec3d target = fable.hideseek.imba.config.PortalConfig.targetPos();
                    traveler.teleport(targetWorld, target.x, target.y, target.z,
                            fable.hideseek.imba.config.PortalConfig.DATA.yaw,
                            fable.hideseek.imba.config.PortalConfig.DATA.pitch);
                }
            }
        }
        portalContacts.keySet().removeIf(id -> !inside.contains(id));
    }

    private static void lockPreparingSeeker() {
        if (currentSeeker == null || prepareSeekerAnchor == null) {
            return;
        }
        if (currentSeeker.getPos().squaredDistanceTo(prepareSeekerAnchor) > 0.0001D) {
            currentSeeker.setPosition(prepareSeekerAnchor.x, prepareSeekerAnchor.y, prepareSeekerAnchor.z);
        }
        currentSeeker.setVelocity(Vec3d.ZERO);
        currentSeeker.setOnGround(true);
        currentSeeker.fallDistance = 0.0F;
    }

    private static boolean participantsAvailable(MinecraftServer server) {
        return currentHider != null && currentSeeker != null
                && server.getPlayerManager().getPlayer(currentHider.getUuid()) != null
                && server.getPlayerManager().getPlayer(currentSeeker.getUuid()) != null;
    }

    private static void lockParticipantTeams(MinecraftServer server) {
        lockedTeams.clear();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            lockedTeams.put(player.getUuid(), player.getScoreboardTeam() == null
                    ? "" : player.getScoreboardTeam().getName());
        }
    }

    private static void enforceLockedTeams(MinecraftServer server) {
        if (phase == Phase.IDLE || lockedTeams.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, String> entry : lockedTeams.entrySet()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            String actual = player == null || player.getScoreboardTeam() == null
                    ? "" : player.getScoreboardTeam().getName();
            if (player == null || entry.getValue().equalsIgnoreCase(actual)) {
                continue;
            }
            if (entry.getValue().isEmpty()) {
                server.getScoreboard().clearPlayerTeam(player.getEntityName());
                player.sendMessage(Text.literal("§cВо время игры менять команду нельзя"), true);
                continue;
            }
            Team team = server.getScoreboard().getTeam(entry.getValue());
            if (team == null) {
                team = server.getScoreboard().addTeam(entry.getValue());
            }
            server.getScoreboard().addPlayerToTeam(player.getEntityName(), team);
            player.sendMessage(Text.literal("§cВо время игры менять команду нельзя"), true);
        }
    }


    private static void maintainRoleEffects(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (phase == Phase.IDLE) {
                if (!GameRoles.isHider(player) && !GameRoles.isSeeker(player)) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 4, false, false, false));
                }
                continue;
            }
            if (GameRoles.isHider(player)) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 4, false, false, false));
                player.fallDistance = 0.0f;
            }
        }
    }

    private static void keepPeaceful(MinecraftServer server) {
        if (server.getTicks() % 100 != 0) {
            return;
        }

        server.setDifficulty(Difficulty.PEACEFUL, true);
        server.getGameRules().get(GameRules.NATURAL_REGENERATION).set(false, server);
    }
}
