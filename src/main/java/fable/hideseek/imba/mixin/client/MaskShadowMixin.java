package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.client.ClientMaskData;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A masked player must not cast its player-shaped shadow onto its own model. */
@Mixin(EntityRenderDispatcher.class)
public abstract class MaskShadowMixin {
    @Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true)
    private static void imba$hideMaskShadow(MatrixStack matrices, VertexConsumerProvider consumers,
            Entity entity, float opacity, float tickDelta, WorldView world, float radius, CallbackInfo ci) {
        if (ClientMaskData.hasMask(entity.getUuid())) ci.cancel();
    }
}
