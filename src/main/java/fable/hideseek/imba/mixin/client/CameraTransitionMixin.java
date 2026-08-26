package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.client.ClientCameraTransition;
import fable.hideseek.imba.client.ClientMaskData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * First-person-only camera stabilization for statue fixation.
 *
 * 1) Eye-height interpolation is collapsed while the local player is fixed.
 * 2) After vanilla computes the post-teleport camera position, a short visual-only
 *    offset keeps the first rendered frame at the exact pre-teleport camera world
 *    position and smoothly decays to the new anchor camera position.
 */
@Mixin(Camera.class)
public abstract class CameraTransitionMixin {
    @Shadow private float cameraY;
    @Shadow private float lastCameraY;
    @Shadow private Entity focusedEntity;
    @Shadow private boolean thirdPerson;
    @Shadow public abstract Vec3d getPos();
    @Shadow protected abstract void setPos(double x, double y, double z);

    @Inject(method = "updateEyeHeight", at = @At("RETURN"))
    private void imba$stabilizeFirstPersonStatueEyeHeight(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || thirdPerson || focusedEntity != client.player
                || !ClientMaskData.isStatue(client.player.getUuid())) {
            return;
        }

        float eyeHeight = focusedEntity.getEyeHeight(focusedEntity.getPose());
        cameraY = eyeHeight;
        lastCameraY = eyeHeight;
    }

    @Inject(method = "update", at = @At("RETURN"))
    private void imba$bridgeFirstPersonFixationTeleport(BlockView area, Entity focusedEntity,
            boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || thirdPerson || focusedEntity != client.player
                || !ClientMaskData.isStatue(client.player.getUuid())) {
            ClientCameraTransition.clear();
            return;
        }

        Vec3d base = getPos();
        Vec3d offset = ClientCameraTransition.currentOffset(base);
        if (offset.lengthSquared() <= 1.0E-12D) return;

        setPos(base.x + offset.x, base.y + offset.y, base.z + offset.z);
    }
}
