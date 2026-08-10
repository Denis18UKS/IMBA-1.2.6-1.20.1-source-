package fable.hideseek.imba.mixin;

import fable.hideseek.imba.mask.MaskCollisionShapes;
import fable.hideseek.imba.mask.MaskState;
import fable.hideseek.imba.mask.MaskType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
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

/**
 * Adds player-door shapes to vanilla entity movement collision. Villagers and
 * every other moving entity therefore treat the disguised player as static
 * geometry instead of as another pushable player.
 */
@Mixin(Entity.class)
public abstract class EntityPlayerDoorCollisionMixin {

    @Inject(
            method = "adjustMovementForCollisions(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Lnet/minecraft/world/World;Ljava/util/List;)Lnet/minecraft/util/math/Vec3d;",
            at = @At("HEAD"))
    private static void imba$appendServerPlayerDoorShapes(
            Entity movingEntity,
            Vec3d movement,
            Box entityBoundingBox,
            World world,
            List<VoxelShape> collisions,
            CallbackInfoReturnable<Vec3d> cir) {
        if (world == null || world.isClient || movingEntity == null || movingEntity.isSpectator()) {
            return;
        }

        Box swept = entityBoundingBox.stretch(movement).expand(1.0E-5D);
        for (PlayerEntity masked : world.getPlayers()) {
            if (masked == movingEntity || !MaskState.hasMask(masked.getUuid())) {
                continue;
            }

            MaskState state = MaskState.get(masked.getUuid());
            if (!state.statue || state.type != MaskType.DOOR) {
                continue;
            }

            for (Box doorBox : MaskCollisionShapes.create(state)) {
                if (doorBox.intersects(swept)) {
                    collisions.add(VoxelShapes.cuboid(doorBox));
                }
            }
        }
    }
}
