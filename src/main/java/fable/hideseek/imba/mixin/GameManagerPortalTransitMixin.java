package fable.hideseek.imba.mixin;

import fable.hideseek.imba.config.PortalConfig;
import fable.hideseek.imba.game.GameManager;
import fable.hideseek.imba.game.PortalTransitGuard;
import fable.hideseek.imba.mask.MaskState;
import fable.hideseek.imba.mask.MaskType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mixin(GameManager.class)
public abstract class GameManagerPortalTransitMixin {
    private static final int PLAYER_PORTAL_TICKS = 75;

    @Shadow @Final
    private static Map<UUID, Integer> portalContacts;

    @Inject(method = "tickPortalMasks", at = @At("HEAD"), cancellable = true, remap = false)
    private static void imba$guardPlayerPortalTransit(MinecraftServer server, CallbackInfo ci) {
        ci.cancel();
        Set<UUID> inside = new HashSet<>();

        for (ServerPlayerEntity masked : server.getPlayerManager().getPlayerList()) {
            if (!MaskState.hasMask(masked.getUuid())) continue;
            MaskState state = MaskState.get(masked.getUuid());
            if (!state.statue || state.type != MaskType.PORTAL) continue;

            boolean eastWest = Math.floorMod(Math.round(state.rotation / 90.0F), 2) == 1;
            Box trigger = eastWest
                    ? new Box(state.anchorX - 0.18D, state.anchorY, state.anchorZ - 0.5D,
                    state.anchorX + 0.18D, state.anchorY + 1.0D, state.anchorZ + 0.5D)
                    : new Box(state.anchorX - 0.5D, state.anchorY, state.anchorZ - 0.18D,
                    state.anchorX + 0.5D, state.anchorY + 1.0D, state.anchorZ + 0.18D);
            BlockPos portalPos = BlockPos.ofFloored(state.anchorX, state.anchorY, state.anchorZ);

            for (ServerPlayerEntity traveler : server.getPlayerManager().getPlayerList()) {
                if (traveler == masked || traveler.isSpectator()
                        || traveler.getWorld() != masked.getWorld()
                        || !traveler.getBoundingBox().intersects(trigger)) continue;

                inside.add(traveler.getUuid());
                long tick = traveler.getWorld().getTime();
                if (PortalTransitGuard.vanillaContactThisTick(traveler, tick)) {
                    portalContacts.remove(traveler.getUuid());
                    continue;
                }

                int ticks = PortalTransitGuard.touchPlayerPortal(traveler, portalPos, masked.getUuid(), tick);
                if (ticks <= 0) {
                    portalContacts.remove(traveler.getUuid());
                    continue;
                }
                portalContacts.put(traveler.getUuid(), ticks);
                if (ticks < PLAYER_PORTAL_TICKS) continue;

                boolean fromNether = traveler.getWorld().getRegistryKey() == World.NETHER;
                PortalConfig.Data portalConfig = PortalConfig.get(fromNether);
                ServerWorld targetWorld = server.getWorld(PortalConfig.worldKey(fromNether));
                if (targetWorld == null) continue;

                Vec3d target = PortalConfig.targetPos(fromNether);
                portalContacts.remove(traveler.getUuid());
                PortalTransitGuard.clear(traveler.getUuid());
                traveler.teleport(targetWorld, target.x, target.y, target.z, portalConfig.yaw, portalConfig.pitch);
            }
        }

        portalContacts.keySet().removeIf(id -> !inside.contains(id));
    }
}
