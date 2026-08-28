package fable.hideseek.imba.mixin;

import fable.hideseek.imba.mask.MaskState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Small, isolated gameplay overrides so the attached GameManager stays untouched. */
@Mixin(targets = "fable.hideseek.imba.game.GameManager")
public abstract class GameplayFixesMixin {
    private static final int PORTAL_DELAY_TICKS = 75;
    private static final String FAIL_SOUND_COMMAND =
            "playsound minecraft:entity.generic.hurt player @a ~ ~ ~ 10 1";
    private static final String CLEAR_ITEMS_COMMAND = "kill @e[type=item]";

    @ModifyConstant(method = "tickPortalMasks", constant = @Constant(intValue = 30))
    private static int imba$increasePortalMaskDesync(int original) {
        return PORTAL_DELAY_TICKS;
    }

    @Inject(method = "pressButton", at = @At("HEAD"), cancellable = true, remap = false)
    private static void imba$ignoreRepeatedButtonPress(ServerPlayerEntity actor,
                                                        ServerPlayerEntity targetPlayer,
                                                        MaskState state,
                                                        CallbackInfo ci) {
        if (state.buttonPressed) {
            ci.cancel();
        }
    }

    @Inject(method = "damageSeekerHeart", at = @At("HEAD"), remap = false)
    private static void imba$playSeekerFailSound(ServerPlayerEntity seeker,
                                                  String message,
                                                  CallbackInfo ci) {
        if (seeker == null) {
            return;
        }
        MinecraftServer server = seeker.getServer();
        if (server != null) {
            server.getCommandManager().executeWithPrefix(server.getCommandSource(), FAIL_SOUND_COMMAND);
        }
    }

    @Inject(method = "finishReturn", at = @At("TAIL"), remap = false)
    private static void imba$clearDroppedItems(MinecraftServer server, CallbackInfo ci) {
        server.getCommandManager().executeWithPrefix(server.getCommandSource(), CLEAR_ITEMS_COMMAND);
    }
}
