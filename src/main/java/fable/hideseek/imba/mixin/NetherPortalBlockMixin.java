package fable.hideseek.imba.mixin;

import fable.hideseek.imba.config.PortalConfig;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetherPortalBlock.class)
public class NetherPortalBlockMixin {
    private static final java.util.Map<java.util.UUID, Integer> IMBA_PORTAL_TICKS = new java.util.HashMap<>();
    @Inject(method = "onEntityCollision", at = @At("HEAD"), cancellable = true)
    private void imba$redirectPortal(net.minecraft.block.BlockState state, World world, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (world.isClient || !(entity instanceof ServerPlayerEntity player)) {
            return;
        }
        int ticks = IMBA_PORTAL_TICKS.merge(player.getUuid(), 1, Integer::sum);
        if (ticks < Math.max(1, PortalConfig.DATA.portalTicks)) {
            ci.cancel();
            return;
        }
        IMBA_PORTAL_TICKS.remove(player.getUuid());
        ServerWorld targetWorld = player.getServer().getWorld(PortalConfig.worldKey());
        if (targetWorld != null) {
            var target = PortalConfig.targetPos();
            player.teleport(targetWorld, target.x, target.y, target.z, PortalConfig.DATA.yaw, PortalConfig.DATA.pitch);
        }
        ci.cancel();
    }
}
