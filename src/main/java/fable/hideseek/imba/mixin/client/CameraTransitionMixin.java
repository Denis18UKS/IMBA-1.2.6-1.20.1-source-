package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.client.ClientMaskData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * First-person eye-height stabilization for statue fixation.
 *
 * Position smoothing does not belong here: the server anchor and the local
 * player's position are synchronized separately. This mixin only prevents
 * vanilla from interpolating between stale and current eye heights after the
 * geometry/pose transition.
 */
@Mixin(Camera.class)
public abstract class CameraTransitionMixin {
    @Shadow private float cameraY;
    @Shadow private float lastCameraY;
    @Shadow private Entity focusedEntity;
    @Shadow private boolean thirdPerson;

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
}
