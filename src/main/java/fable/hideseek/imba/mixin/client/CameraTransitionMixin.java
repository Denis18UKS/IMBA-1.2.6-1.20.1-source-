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

@Mixin(Camera.class)
public abstract class CameraTransitionMixin {
    @Shadow public abstract Vec3d getPos();
    @Shadow protected abstract void setPos(double x, double y, double z);

    @Inject(method = "update", at = @At("RETURN"))
    private void imba$smoothFixationCamera(BlockView area, Entity focusedEntity,
            boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || focusedEntity != client.player
                || !ClientMaskData.isStatue(client.player.getUuid())) {
            ClientCameraTransition.clear();
            return;
        }
        Vec3d offset = ClientCameraTransition.currentOffset();
        if (offset.lengthSquared() < 1.0E-12D) return;
        Vec3d pos = getPos();
        setPos(pos.x + offset.x, pos.y + offset.y, pos.z + offset.z);
    }
}
