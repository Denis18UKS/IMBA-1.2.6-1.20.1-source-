package fable.hideseek.imba.mixin;

import fable.hideseek.imba.ImbaMod;
import fable.hideseek.imba.game.GameManager;
import fable.hideseek.imba.game.RoundDefinition;
import fable.hideseek.imba.item.ModelEquipHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gives nether_portal a genuine generated 2D icon because vanilla has no BlockItem for it. */
@Mixin(GameManager.class)
public abstract class PortalRoundIconMixin {
    @Inject(method="giveRoundItems",at=@At("HEAD"),cancellable=true,remap=false)
    private static void imba$portalIcon(ServerPlayerEntity hider, RoundDefinition round, CallbackInfo ci) {
        if (round == null || !"minecraft:nether_portal".equals(round.maskId.toString())) return;
        ItemStack model=ModelEquipHandler.createModelItem(ImbaMod.NETHER_PORTAL_ICON,round.displayWord,round.sourceKind.name(),round.maskId);
        hider.getInventory().setStack(8,model);
        hider.getInventory().setStack(0,new ItemStack(ImbaMod.HIDE_BUTTON));
        ci.cancel();
    }
}
