package fable.hideseek.imba.mixin;

import fable.hideseek.imba.config.RoundRestoreConfig;
import fable.hideseek.imba.config.TeleportConfig;
import fable.hideseek.imba.game.GameConfig;
import fable.hideseek.imba.game.ConfigurableMessages;
import fable.hideseek.imba.game.GameManager;
import fable.hideseek.imba.game.GameMessages;
import fable.hideseek.imba.game.RoundDefinition;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mixin(GameManager.class)
public abstract class GameManagerExtensionMixin {
    private static final int EFFECT_FOREVER = Integer.MAX_VALUE;
    @Shadow(remap = false) @Final private static Set<UUID> currentSeekers;
    @Shadow(remap = false) @Final private static Set<UUID> eliminatedSeekers;
    @Shadow(remap = false) @Final private static Map<UUID, Vec3d> prepareSeekerAnchors;
    @Shadow(remap = false) private static RoundDefinition currentRound;
    @Shadow(remap = false) private static ServerPlayerEntity currentHider;
    @Shadow(remap = false) private static boolean paused;
    @Shadow(remap = false) private static boolean autoPaused;
    @Shadow(remap = false) private static boolean participantsAvailable(MinecraftServer server) { throw new AssertionError(); }
    @Shadow(remap = false) private static void teleport(ServerPlayerEntity player, RegistryKey<World> worldKey, Vec3d pos, float yaw, float pitch) { throw new AssertionError(); }
    @Shadow(remap = false) private static void teleportToLobby(ServerPlayerEntity player) { throw new AssertionError(); }
    @Shadow(remap = false) private static void removeSeekerSword(ServerPlayerEntity player) { throw new AssertionError(); }
    @Shadow(remap = false) private static void finishHiderWinByHearts(MinecraftServer server) { throw new AssertionError(); }
    @Unique private static boolean imba$roundRestored;
    @Unique private static boolean imba$prepareSpreadDone;

    @Inject(method = "startNextRound*", at = @At("HEAD"), remap = false)
    private static void imba$resetRoundExtensionState(CallbackInfo ci) { imba$roundRestored = false; imba$prepareSpreadDone = false; }

    @Inject(method = "startNextRound*", at = @At("TAIL"), remap = false)
    private static void imba$spreadPreparingSeekers(CallbackInfo ci) {
        if (imba$prepareSpreadDone || currentHider == null || currentSeekers.isEmpty()) return;
        MinecraftServer server = currentHider.getServer(); if (server == null) return;
        imba$prepareSpreadDone = true;
        var ordered = new ArrayList<ServerPlayerEntity>();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) if (currentSeekers.contains(player.getUuid())) ordered.add(player);
        for (int i = 1; i < ordered.size(); i++) {
            ServerPlayerEntity seeker = ordered.get(i); TeleportConfig.ExtraSeekerPoint point = TeleportConfig.getExtraSeekerPrepare(i - 1);
            if (point != null) seeker.teleport(server.getOverworld(), point.pos().x, point.pos().y, point.pos().z, point.yaw(), point.pitch());
            else { Vec3d fallback = imba$fallbackPreparePos(i); seeker.teleport(server.getOverworld(), fallback.x, fallback.y, fallback.z, GameConfig.LOBBY_YAW, GameConfig.LOBBY_PITCH); }
            prepareSeekerAnchors.put(seeker.getUuid(), seeker.getPos());
        }
    }

    @Inject(method = "releaseSeekers", at = @At("TAIL"), remap = false)
    private static void imba$spreadReleasedSeekers(MinecraftServer server, CallbackInfo ci) {
        if (server == null || currentRound == null || currentSeekers.size() < 2) return;
        var ordered = new ArrayList<ServerPlayerEntity>();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) if (currentSeekers.contains(player.getUuid()) && !eliminatedSeekers.contains(player.getUuid())) ordered.add(player);
        for (int i = 1; i < ordered.size(); i++) { double angle = (i - 1) * (Math.PI / 2.0D); double radius = 1.15D + ((i - 1) / 4) * 0.8D; Vec3d separated = currentRound.seekerPos.add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius); teleport(ordered.get(i), currentRound.worldKey, separated, currentRound.seekerYaw, currentRound.seekerPitch); }
    }

    @Inject(method = "damageSeekerHeart", at = @At("HEAD"), cancellable = true, remap = false)
    private static void imba$damageSeekerHeartForAllAssignedSeekers(ServerPlayerEntity seeker, String message, CallbackInfo ci) {
        if (seeker == null || eliminatedSeekers.contains(seeker.getUuid())) { ci.cancel(); return; }
        float newHealth = seeker.getHealth() - 2.0F;
        if (newHealth > 0.0F) { seeker.setHealth(newHealth); GameMessages.send(seeker, Text.literal(message)); ci.cancel(); return; }
        eliminatedSeekers.add(seeker.getUuid()); seeker.setHealth(1.0F); removeSeekerSword(seeker); teleportToLobby(seeker); seeker.clearStatusEffects();
        seeker.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, EFFECT_FOREVER, 0, false, false, false));
        seeker.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, EFFECT_FOREVER, 10, false, false, false));
        GameMessages.send(seeker, "seeker.eliminated", Text.literal(message));
        ConfigurableMessages.actionBar(seeker, "seeker.eliminated", Text.literal("§cВы потеряли все сердца и выбыли из поиска"));
        MinecraftServer server = seeker.getServer(); if (server != null && !currentSeekers.isEmpty() && eliminatedSeekers.containsAll(currentSeekers)) finishHiderWinByHearts(server); ci.cancel();
    }

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private static void imba$autoResumeAfterReconnect(MinecraftServer server, CallbackInfo ci) {
        if (!paused || !autoPaused || server == null || !participantsAvailable(server)) return;
        paused = false; autoPaused = false;
        ConfigurableMessages.broadcast(server, "game.auto_resume", Text.literal("§aИгра автоматически продолжена: ключевые игроки снова в сети"), false);
        fable.hideseek.imba.net.MaskNetworking.broadcastGameState(server);
    }

    @Inject(method = "beginReturn", at = @At("HEAD"), remap = false)
    private static void imba$restoreAtRoundEnd(MinecraftServer server, Text message, CallbackInfo ci) { imba$restoreOnce(server); }
    @Inject(method = "finishReturn", at = @At("HEAD"), remap = false)
    private static void imba$restoreOnForcedReset(MinecraftServer server, CallbackInfo ci) { imba$restoreOnce(server); }
    @Unique private static void imba$restoreOnce(MinecraftServer server) { if (!imba$roundRestored && server != null) { RoundRestoreConfig.restoreAll(server); imba$roundRestored = true; } }
    @Unique private static Vec3d imba$fallbackPreparePos(int index) { int slot = index - 1; int ring = slot / 8 + 1; double angle = (slot % 8) * (Math.PI / 4.0D); double radius = 1.35D * ring; return GameConfig.LOBBY_POS.add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius); }
}
