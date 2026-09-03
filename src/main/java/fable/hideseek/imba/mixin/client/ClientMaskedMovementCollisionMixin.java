package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.client.ClientMaskData;
import fable.hideseek.imba.mask.MaskMovementCollision;
import fable.hideseek.imba.mask.MaskService;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

/** Client prediction counterpart of the server-side mask-owner movement box. */
@Mixin(Entity.class)
public abstract class ClientMaskedMovementCollisionMixin {
    @ModifyVariable(
            method = "adjustMovementForCollisions(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Lnet/minecraft/world/World;Ljava/util/List;)Lnet/minecraft/util/math/Vec3d;",
            at = @At("HEAD"),
            argsOnly = true,
            index = 2)
    private static Box imba$useOwnerMovementBoxClient(
            Box visualBox,
            Entity movingEntity,
            Vec3d movement,
            Box entityBoundingBox,
            World world,
            List<VoxelShape> collisions) {
        if (visualBox == null || movingEntity == null || world == null || !world.isClient
                || !(movingEntity instanceof PlayerEntity player)
                || !ClientMaskData.hasMask(player.getUuid())) {
            return visualBox;
        }

        Item item = ClientMaskData.ITEMS.get(player.getUuid());
        if (MaskService.isSpecialPotion(item)) {
            return visualBox;
        }

        return MaskMovementCollision.ownerMovementBox(visualBox);
    }
}
