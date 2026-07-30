package fable.hideseek.imba.mixin;

import fable.hideseek.imba.config.ItemRules;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public class ScreenHandlerMixin {

    @Inject(method = "onSlotClick", at = @At("HEAD"), cancellable = true)
    private void preventActions(int slotIndex, int button, SlotActionType actionType,
                               PlayerEntity player, CallbackInfo ci) {
        if (player.isCreative()) return;
        
        ScreenHandler handler = (ScreenHandler)(Object)this;
        
        // Q в GUI
        if (actionType == SlotActionType.THROW) {
            if (slotIndex >= 0 && slotIndex < handler.slots.size()) {
                ItemStack slotStack = handler.getSlot(slotIndex).getStack();
                if (ItemRules.isRestricted(slotStack)) {
                    ci.cancel();
                    return;
                }
            }
        }
        
        // F (своп в оффхенд) в GUI
        if (actionType == SlotActionType.SWAP && button == 40) {
            if (slotIndex >= 0 && slotIndex < handler.slots.size()) {
                ItemStack slotStack = handler.getSlot(slotIndex).getStack();
                if (ItemRules.isRestricted(slotStack)) {
                    ci.cancel();
                    return;
                }
            }
        }
        
        // Shift+Click
        if (actionType == SlotActionType.QUICK_MOVE) {
            if (slotIndex >= 0 && slotIndex < handler.slots.size()) {
                ItemStack slotStack = handler.getSlot(slotIndex).getStack();
                if (ItemRules.isRestricted(slotStack)) {
                    ci.cancel();
                    return;
                }
            }
        }
        
        // Pickup и обычные перемещения
        if (actionType == SlotActionType.PICKUP) {
            ItemStack cursor = handler.getCursorStack();
            
            ItemStack slotStack = ItemStack.EMPTY;
            if (slotIndex >= 0 && slotIndex < handler.slots.size()) {
                slotStack = handler.getSlot(slotIndex).getStack();
            }
            
            if (ItemRules.isRestricted(slotStack) || ItemRules.isRestricted(cursor)) {
                ci.cancel();
            }
        }
    }
}