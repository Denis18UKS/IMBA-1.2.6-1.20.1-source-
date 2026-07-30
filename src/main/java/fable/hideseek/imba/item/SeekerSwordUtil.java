package fable.hideseek.imba.item;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

public final class SeekerSwordUtil {

    public static final String NBT_KEY = "imba_seeker_sword";

    private SeekerSwordUtil() {
    }

    public static ItemStack createSword() {
        ItemStack stack = new ItemStack(Items.IRON_SWORD);
        stack.setCustomName(Text.literal("§cМеч Искателя"));
        stack.getOrCreateNbt().putBoolean(NBT_KEY, true);
        return stack;
    }

    public static boolean isSeekerSword(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && stack.hasNbt()
                && stack.getNbt().getBoolean(NBT_KEY);
    }
}