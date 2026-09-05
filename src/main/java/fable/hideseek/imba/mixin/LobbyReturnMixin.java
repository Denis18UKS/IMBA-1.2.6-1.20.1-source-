package fable.hideseek.imba.mixin;

import fable.hideseek.imba.config.ReturnTimingConfig;
import fable.hideseek.imba.game.GameConfig;
import fable.hideseek.imba.game.GameManager;
import fable.hideseek.imba.net.LobbyReturnNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameManager.class)
public abstract class LobbyReturnMixin {
    private static final String LOBBY_SPREAD_COMMAND = "spreadplayers -131.49 148.72 2 5 under -29 false @a";
    private static final int FADE_IN_TICKS = 10;

    @Unique private static int imba$returnStage;
    @Unique private static int imba$returnStageTicks;

    @Shadow
    private static void finishReturn(MinecraftServer server) {
        throw new AssertionError();
    }

    @Inject(method = "beginReturn", at = @At("TAIL"), remap = false)
    private static void imba$beginDelayedLobbyReturn(MinecraftServer server, Text message, CallbackInfo ci) {
        LobbyReturnNetworking.broadcastReturnBlackout(server, false);
        imba$returnStage = 1;
        imba$returnStageTicks = Math.max(0, ReturnTimingConfig.preFadeTicks());
        if (imba$returnStageTicks == 0) imba$startFade(server);
    }

    @Inject(method = "tickReturn", at = @At("HEAD"), cancellable = true, remap = false)
    private static void imba$tickReturnTiming(MinecraftServer server, CallbackInfo ci) {
        if (imba$returnStage == 0) return;
        ci.cancel();

        if (imba$returnStage == 1) {
            if (--imba$returnStageTicks <= 0) imba$startFade(server);
            return;
        }

        if (imba$returnStage == 2 && --imba$returnStageTicks <= 0) {
            imba$returnStage = 0;
            imba$returnStageTicks = 0;
            finishReturn(server);
        }
    }

    @Unique
    private static void imba$startFade(MinecraftServer server) {
        LobbyReturnNetworking.broadcastReturnBlackout(server, true);
        imba$returnStage = 2;
        imba$returnStageTicks = FADE_IN_TICKS + Math.max(0, ReturnTimingConfig.preTeleportTicks());
    }

    @Inject(method = "finishReturn", at = @At("TAIL"), remap = false)
    private static void imba$spreadLobbyPlayersAndReveal(MinecraftServer server, CallbackInfo ci) {
        imba$returnStage = 0;
        imba$returnStageTicks = 0;
        try {
            server.getCommandManager().executeWithPrefix(server.getCommandSource(), LOBBY_SPREAD_COMMAND);
            normalizeLobbySpreadPositions(server);
        } finally {
            LobbyReturnNetworking.broadcastReturnBlackout(server, false);
        }
    }

    private static void normalizeLobbySpreadPositions(MinecraftServer server) {
        var world = server.getOverworld();
        double configuredY = GameConfig.LOBBY_POS.y;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.getWorld() != world) continue;
            double y = player.getY();
            if (Math.abs(y - configuredY) <= 2.0D) continue;
            double safeY = findLobbyFloorY(world, player.getX(), player.getZ(), configuredY);
            if (!Double.isNaN(safeY)) {
                player.teleport(world, player.getX(), safeY, player.getZ(), player.getYaw(), player.getPitch());
            }
        }
    }

    private static double findLobbyFloorY(net.minecraft.server.world.ServerWorld world, double x, double z, double preferredY) {
        int center = BlockPos.ofFloored(x, preferredY, z).getY();
        for (int dy = 0; dy >= -8; dy--) {
            int y = center + dy;
            BlockPos floorPos = BlockPos.ofFloored(x, y, z);
            BlockState floorState = world.getBlockState(floorPos);
            VoxelShape shape = floorState.getCollisionShape(world, floorPos);
            if (shape.isEmpty()) continue;
            double top = shape.getMax(net.minecraft.util.math.Direction.Axis.Y);
            BlockPos feet = floorPos.up();
            if (!world.getBlockState(feet).isAir() || !world.getBlockState(feet.up()).isAir()) continue;
            return floorPos.getY() + top;
        }
        for (int dy = 1; dy <= 4; dy++) {
            int y = center + dy;
            BlockPos floorPos = BlockPos.ofFloored(x, y, z);
            BlockState floorState = world.getBlockState(floorPos);
            VoxelShape shape = floorState.getCollisionShape(world, floorPos);
            if (shape.isEmpty()) continue;
            double top = shape.getMax(net.minecraft.util.math.Direction.Axis.Y);
            BlockPos feet = floorPos.up();
            if (!world.getBlockState(feet).isAir() || !world.getBlockState(feet.up()).isAir()) continue;
            return floorPos.getY() + top;
        }
        return preferredY;
    }
}
