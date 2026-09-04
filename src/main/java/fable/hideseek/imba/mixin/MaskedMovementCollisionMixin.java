package fable.hideseek.imba.mixin;

import fable.hideseek.imba.mask.MaskMovementCollision;
import fable.hideseek.imba.mask.MaskService;
import fable.hideseek.imba.mask.MaskState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Server-side owner movement collision without changing the visible mask hitbox outside movement. */
@Mixin(Entity.class)
public abstract class MaskedMovementCollisionMixin {
    @Unique private Box imba$visualBoxBeforeOwnerMove;
    @Unique private boolean imba$ownerMoveScoped;

    @Inject(method = "move", at = @At("HEAD"))
    private void imba$beginOwnerMove(MovementType movementType, Vec3d movement, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.getWorld().isClient || !(self instanceof PlayerEntity player)
                || !MaskState.hasMask(player.getUuid()) || MaskState.isStatue(player.getUuid())) {
            return;
        }

        MaskState state = MaskState.get(player.getUuid());
        if (state == null || MaskService.isSpecialPotion(state.item)) {
            return;
        }

        Box visualBox = self.getBoundingBox();
        if (visualBox == null) {
            return;
        }

        imba$visualBoxBeforeOwnerMove = visualBox;
        imba$ownerMoveScoped = true;
        self.setBoundingBox(MaskMovementCollision.ownerMovementBox(visualBox));
    }

    @Inject(method = "move", at = @At("RETURN"))
    private void imba$endOwnerMove(MovementType movementType, Vec3d movement, CallbackInfo ci) {
        if (!imba$ownerMoveScoped || imba$visualBoxBeforeOwnerMove == null) {
            return;
        }

        Entity self = (Entity) (Object) this;
        Box visualBox = imba$visualBoxBeforeOwnerMove;
        imba$visualBoxBeforeOwnerMove = null;
        imba$ownerMoveScoped = false;
        self.setBoundingBox(MaskMovementCollision.restoreVisualBox(
                visualBox, self.getX(), self.getY(), self.getZ()));
    }

    @ModifyVariable(
            method = "adjustMovementForCollisions(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Lnet/minecraft/world/World;Ljava/util/List;)Lnet/minecraft/util/math/Vec3d;",
            at = @At("HEAD"),
            argsOnly = true,
            index = 2)
    private static Box imba$useOwnerMovementBox(
            Box visualBox,
            Entity movingEntity,
            Vec3d movement,
            Box entityBoundingBox,
            World world,
            List<VoxelShape> collisions) {
        if (visualBox == null || movingEntity == null || world == null || world.isClient
                || !(movingEntity instanceof PlayerEntity player)
                || !MaskState.hasMask(player.getUuid())) {
            return visualBox;
        }

        MaskState state = MaskState.get(player.getUuid());
        if (state == null || MaskService.isSpecialPotion(state.item)) {
            return visualBox;
        }

        return MaskMovementCollision.ownerMovementBox(visualBox);
    }
}
