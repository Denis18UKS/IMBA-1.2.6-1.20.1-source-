package fable.hideseek.imba.mixin;

import fable.hideseek.imba.game.GameConfig;
import fable.hideseek.imba.game.GameRoles;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class SeekerHealthMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void imba$syncSeekerHearts(CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;

        var attribute = self.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (attribute == null)
            return;

        double wantedHealth = GameRoles.isSeeker(self)
                ? Math.max(1, GameConfig.SEEKER_HEARTS) * 2.0D
                : 20.0D;

        double previousMaximum = attribute.getBaseValue();
        if (Math.abs(previousMaximum - wantedHealth) > 0.001D) {
            attribute.setBaseValue(wantedHealth);
            if (wantedHealth > previousMaximum) {
                float filledHealth = self.getHealth() + (float) (wantedHealth - previousMaximum);
                self.setHealth(Math.min((float) wantedHealth, filledHealth));
            }
        }

        if (self.getHealth() > wantedHealth) {
            self.setHealth((float) wantedHealth);
        }
    }
}
