package fable.hideseek.imba.client;

import fable.hideseek.imba.game.GameRoles;
import fable.hideseek.imba.mask.MaskCollisionShapes;
import fable.hideseek.imba.mask.MaskService;
import fable.hideseek.imba.mask.MaskType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Client-side counterpart of the authoritative server collision for smooth movement. */
public final class ClientMaskCollision {
    private ClientMaskCollision() {}

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null || !GameRoles.isSeeker(client.player)) return;
        for (AbstractClientPlayerEntity masked : client.world.getPlayers()) {
            if (masked == client.player || !ClientMaskData.hasMask(masked.getUuid()) || !ClientMaskData.isStatue(masked.getUuid())) continue;
            MaskType type = ClientMaskData.TYPES.get(masked.getUuid());
            if (!MaskService.hasPhysicalCollision(type, ClientMaskData.BLOCKS.get(masked.getUuid()))) continue;
            var obstacles = MaskCollisionShapes.create(type, ClientMaskData.BLOCKS.get(masked.getUuid()),
                    ClientMaskData.ROTATIONS.getOrDefault(masked.getUuid(), 0.0F),
                    ClientMaskData.DOOR_OPEN.getOrDefault(masked.getUuid(), false),
                    masked.getX(), masked.getY(), masked.getZ());
            for (Box obstacle : obstacles) {
                resolve(client, masked, obstacle);
            }
        }
    }

    private static void resolve(MinecraftClient client, AbstractClientPlayerEntity masked, Box obstacle) {
            Box playerBox = client.player.getBoundingBox();
            boolean overlapXZ = playerBox.maxX > obstacle.minX && playerBox.minX < obstacle.maxX
                    && playerBox.maxZ > obstacle.minZ && playerBox.minZ < obstacle.maxZ;
            if (overlapXZ && client.player.getVelocity().y <= 0.0D
                    && playerBox.minY >= obstacle.maxY - .35D && playerBox.minY <= obstacle.maxY + .20D) {
                if (Math.abs(client.player.getY() - obstacle.maxY) > .001D)
                    client.player.setPosition(client.player.getX(), obstacle.maxY, client.player.getZ());
                Vec3d velocity = client.player.getVelocity();
                client.player.setVelocity(velocity.x, 0, velocity.z);
                client.player.setOnGround(true);
                client.player.fallDistance = 0;
                return;
            }
            if (!playerBox.intersects(obstacle)) return;
            double overlapX = Math.min(playerBox.maxX-obstacle.minX, obstacle.maxX-playerBox.minX);
            double overlapZ = Math.min(playerBox.maxZ-obstacle.minZ, obstacle.maxZ-playerBox.minZ);
            double dx=client.player.getX()-masked.getX(), dz=client.player.getZ()-masked.getZ(), moveX=0, moveZ=0;
            if(overlapX<overlapZ) moveX=dx>=0?overlapX:-overlapX; else moveZ=dz>=0?overlapZ:-overlapZ;
            client.player.setPosition(client.player.getX()+moveX,client.player.getY(),client.player.getZ()+moveZ);
            Vec3d velocity=client.player.getVelocity();
            client.player.setVelocity(moveX==0?velocity.x:0,velocity.y,moveZ==0?velocity.z:0);
    }

    public static boolean canKeepSneakMovement(Vec3d movement) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || !GameRoles.isSeeker(client.player)) {
            return false;
        }
        Box current = client.player.getBoundingBox();
        Box moved = current.offset(movement.x, 0.0D, movement.z);
        for (AbstractClientPlayerEntity masked : client.world.getPlayers()) {
            if (masked == client.player || !ClientMaskData.hasMask(masked.getUuid())
                    || !ClientMaskData.isStatue(masked.getUuid())) {
                continue;
            }
            for (Box obstacle : MaskCollisionShapes.create(
                    ClientMaskData.TYPES.get(masked.getUuid()),
                    ClientMaskData.BLOCKS.get(masked.getUuid()),
                    ClientMaskData.ROTATIONS.getOrDefault(masked.getUuid(), 0.0F),
                    ClientMaskData.DOOR_OPEN.getOrDefault(masked.getUuid(), false),
                    masked.getX(), masked.getY(), masked.getZ())) {
                boolean standing = Math.abs(current.minY - obstacle.maxY) <= 0.08D;
                boolean staysAbove = moved.maxX > obstacle.minX && moved.minX < obstacle.maxX
                        && moved.maxZ > obstacle.minZ && moved.minZ < obstacle.maxZ;
                if (standing && staysAbove) {
                    return true;
                }
            }
        }
        return false;
    }
}
