package fable.hideseek.imba.mixin;

import fable.hideseek.imba.config.PortalConfig;
import fable.hideseek.imba.game.PortalTransitGuard;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetherPortalBlock.class)
public class NetherPortalBlockMixin {
    @Inject(method = "onEntityCollision", at = @At("HEAD"), cancellable = true)
    private void imba$redirectPortal(BlockState state, World world, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (world.isClient || !(entity instanceof ServerPlayerEntity player)) return;

        boolean fromNether = world.getRegistryKey() == World.NETHER;
        PortalConfig.Data portalConfig = PortalConfig.get(fromNether);
        BlockPos portalAnchor = imba$portalAnchor(world, pos, state);
        long tick = world.getTime();
        int ticks = PortalTransitGuard.touchVanilla(player, world.getRegistryKey(), portalAnchor, tick);
        if (ticks < Math.max(1, portalConfig.portalTicks)) {
            ci.cancel();
            return;
        }

        if (player.getServer() == null) {
            ci.cancel();
            return;
        }

        ServerWorld targetWorld = player.getServer().getWorld(PortalConfig.worldKey(fromNether));
        if (targetWorld != null) {
            Vec3d target = PortalConfig.targetPos(fromNether);
            PortalTransitGuard.clear(player.getUuid());
            player.teleport(targetWorld, target.x, target.y, target.z, portalConfig.yaw, portalConfig.pitch);
        }
        ci.cancel();
    }

    private static BlockPos imba$portalAnchor(World world, BlockPos pos, BlockState initial) {
        Direction.Axis axis = initial.contains(NetherPortalBlock.AXIS)
                ? initial.get(NetherPortalBlock.AXIS) : Direction.Axis.X;
        BlockPos anchor = pos;
        for (int i = 0; i < 24; i++) {
            BlockPos next = anchor.down();
            if (!imba$samePortal(world, next, axis)) break;
            anchor = next;
        }
        Direction side = axis == Direction.Axis.X ? Direction.WEST : Direction.NORTH;
        for (int i = 0; i < 24; i++) {
            BlockPos next = anchor.offset(side);
            if (!imba$samePortal(world, next, axis)) break;
            anchor = next;
        }
        return anchor;
    }

    private static boolean imba$samePortal(World world, BlockPos pos, Direction.Axis axis) {
        BlockState state = world.getBlockState(pos);
        return state.isOf(Blocks.NETHER_PORTAL)
                && state.contains(NetherPortalBlock.AXIS)
                && state.get(NetherPortalBlock.AXIS) == axis;
    }
}
