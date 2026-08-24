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
 * Prevents first-person camera eye-height interpolation from producing a visible
 * vertical twitch when a masked local player enters statue/fixation mode.
 *
 * The entity position, mask anchor, collision and third-person camera are not
 * modified here. We only collapse Camera's previous/current eye-height values to
 * the same already-calculated value while the local player is fixed in first
 * person, so vanilla has nothing to interpolate between on subsequent frames.
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
