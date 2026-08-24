package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.client.ClientMaskData;
import fable.hideseek.imba.client.ClientStatueLock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/** Applies statue lock in the same client task that receives the new anchor. */
@Mixin(ClientMaskData.class)
public abstract class ClientStatueSyncMixin {
    @Inject(method = "setStatue", at = @At("RETURN"), remap = false)
    private static void imba$applyAnchorImmediately(UUID uuid, boolean statue,
            double x, double y, double z, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        PlayerEntity player = client.world.getPlayerByUuid(uuid);
        if (player == null) return;

        if (statue) {
            ClientStatueLock.enter(player, x, y, z);
        } else {
            ClientStatueLock.release(player);
        }
    }
}
