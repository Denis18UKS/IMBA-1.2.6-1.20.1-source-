package fable.hideseek.imba.config;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;

import fable.hideseek.imba.ImbaMod;

public class ItemRules {
    private static final Set<String> RESTRICTED_ITEMS = new HashSet<>();

    static {
        RESTRICTED_ITEMS.add("imba:hide_button");
    }

    public static boolean isRestricted(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if(stack.getItem() == ImbaMod.HIDE_BUTTON) return true;
        if (fable.hideseek.imba.item.ModelEquipHandler.isModelItem(stack)) return true;
        
        Identifier id = Registries.ITEM.getId(stack.getItem());

        NbtCompound nbt = stack.getNbt();
        if (nbt == null) return false;

        return RESTRICTED_ITEMS.contains(id.toString());
    }
}