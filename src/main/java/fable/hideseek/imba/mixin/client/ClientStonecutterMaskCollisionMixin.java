package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.ImbaMod;
import fable.hideseek.imba.client.ClientMaskData;
import fable.hideseek.imba.mask.MaskCollisionShapes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Client-side prediction for the solid stonecutter-blade disguise. */
@Mixin(ClientPlayerEntity.class)
public abstract class ClientStonecutterMaskCollisionMixin {
    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void imba$solidStonecutterMask(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
        if (client.player != self || client.world == null || self.isSpectator()) return;

        for (AbstractClientPlayerEntity masked : client.world.getPlayers()) {
            if (masked == self || !ClientMaskData.hasMask(masked.getUuid())) continue;
            var block = ClientMaskData.BLOCKS.get(masked.getUuid());
            if (block != ImbaMod.STONRCUTTER_LEZVIE) continue;

            for (Box obstacle : MaskCollisionShapes.create(
                    ClientMaskData.TYPES.get(masked.getUuid()),
                    block,
                    ClientMaskData.ROTATIONS.getOrDefault(masked.getUuid(), 0.0F),
                    ClientMaskData.DOOR_OPEN.getOrDefault(masked.getUuid(), false),
                    masked.getX(), masked.getY(), masked.getZ())) {
                resolve(self, obstacle);
            }
        }
    }

    private static void resolve(ClientPlayerEntity player, Box obstacle) {
        Box playerBox = player.getBoundingBox();
        boolean overlapsXZ = playerBox.maxX > obstacle.minX && playerBox.minX < obstacle.maxX
                && playerBox.maxZ > obstacle.minZ && playerBox.minZ < obstacle.maxZ;

        if (overlapsXZ && player.getVelocity().y <= 0.0D
                && playerBox.minY >= obstacle.maxY - 0.35D
                && playerBox.minY <= obstacle.maxY + 0.20D) {
            if (Math.abs(player.getY() - obstacle.maxY) > 0.001D) {
                player.setPosition(player.getX(), obstacle.maxY, player.getZ());
            }
            Vec3d velocity = player.getVelocity();
            player.setVelocity(velocity.x, 0.0D, velocity.z);
            player.setOnGround(true);
            player.fallDistance = 0.0F;
            return;
        }

        if (!playerBox.intersects(obstacle)) return;
        Vec3d correction = MaskCollisionShapes.nearestHorizontalSeparation(playerBox, obstacle);
        if (correction.lengthSquared() <= 1.0E-12D) return;

        player.setPosition(player.getX() + correction.x, player.getY(), player.getZ() + correction.z);
        Vec3d velocity = player.getVelocity();
        player.setVelocity(correction.x == 0.0D ? velocity.x : 0.0D,
                velocity.y,
                correction.z == 0.0D ? velocity.z : 0.0D);
    }
}
