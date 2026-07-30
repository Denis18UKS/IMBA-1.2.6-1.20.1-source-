package fable.hideseek.imba.mixin;

import fable.hideseek.imba.config.PortalConfig;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(NetherPortalBlock.class)
public class NetherPortalBlockMixin {

    private static final Map<UUID, Integer> IMBA_PORTAL_TICKS = new HashMap<>();

    @Inject(method = "onEntityCollision", at = @At("HEAD"), cancellable = true)
    private void imba$redirectPortal(
            net.minecraft.block.BlockState state,
            World world,
            BlockPos pos,
            Entity entity,
            CallbackInfo ci) {

        if (world.isClient || !(entity instanceof ServerPlayerEntity player)) {
            return;
        }

        boolean fromNether = world.getRegistryKey() == World.NETHER;
        PortalConfig.Data portalConfig = PortalConfig.get(fromNether);

        int ticks = IMBA_PORTAL_TICKS.merge(player.getUuid(), 1, Integer::sum);
        if (ticks < Math.max(1, portalConfig.portalTicks)) {
            ci.cancel();
            return;
        }

        IMBA_PORTAL_TICKS.remove(player.getUuid());

        if (player.getServer() == null) {
            ci.cancel();
            return;
        }

        ServerWorld targetWorld =
                player.getServer().getWorld(PortalConfig.worldKey(fromNether));

        if (targetWorld != null) {
            Vec3d target = PortalConfig.targetPos(fromNether);

            player.teleport(
                    targetWorld,
                    target.x,
                    target.y,
                    target.z,
                    portalConfig.yaw,
                    portalConfig.pitch);
        }

        ci.cancel();
    }
}
