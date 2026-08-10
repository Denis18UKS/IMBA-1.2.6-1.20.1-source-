package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.client.ClientMaskData;
import fable.hideseek.imba.mask.MaskCollisionShapes;
import fable.hideseek.imba.mask.MaskType;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Client prediction counterpart for the server-side virtual player-door. */
@Mixin(Entity.class)
public abstract class ClientEntityPlayerDoorCollisionMixin {

    @Inject(
            method = "adjustMovementForCollisions(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Lnet/minecraft/world/World;Ljava/util/List;)Lnet/minecraft/util/math/Vec3d;",
            at = @At("HEAD"))
    private static void imba$appendClientPlayerDoorShapes(
            Entity movingEntity,
            Vec3d movement,
            Box entityBoundingBox,
            World world,
            List<VoxelShape> collisions,
            CallbackInfoReturnable<Vec3d> cir) {
        if (world == null || !world.isClient || movingEntity == null || movingEntity.isSpectator()) {
            return;
        }

        Box swept = entityBoundingBox.stretch(movement).expand(1.0E-5D);
        for (AbstractClientPlayerEntity masked : world.getPlayers()) {
            if (masked == movingEntity
                    || !ClientMaskData.hasMask(masked.getUuid())
                    || !ClientMaskData.isStatue(masked.getUuid())
                    || ClientMaskData.TYPES.get(masked.getUuid()) != MaskType.DOOR) {
                continue;
            }

            Vec3d anchor = ClientMaskData.getStatueAnchor(masked.getUuid());
            double x = anchor == null ? masked.getX() : anchor.x;
            double y = anchor == null ? masked.getY() : anchor.y;
            double z = anchor == null ? masked.getZ() : anchor.z;

            for (Box doorBox : MaskCollisionShapes.create(
                    MaskType.DOOR,
                    ClientMaskData.BLOCKS.get(masked.getUuid()),
                    ClientMaskData.ROTATIONS.getOrDefault(masked.getUuid(), 0.0F),
                    ClientMaskData.DOOR_OPEN.getOrDefault(masked.getUuid(), false),
                    x,
                    y,
                    z)) {
                if (doorBox.intersects(swept)) {
                    collisions.add(VoxelShapes.cuboid(doorBox));
                }
            }
        }
    }
}
