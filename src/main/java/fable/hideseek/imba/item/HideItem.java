package fable.hideseek.imba.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HideItem extends Item {

    private static final int NAME_COLOR = 0xA8FF60;
    private static final int TOOLTIP_COLOR = 0x79B35A;

    public HideItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public Text getName(ItemStack stack) {
        return super.getName(stack).copy().styled(style ->
                style.withItalic(true).withColor(TextColor.fromRgb(NAME_COLOR)));
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.imba.hide_button.tooltip").styled(style ->
                style.withItalic(true).withColor(TextColor.fromRgb(TOOLTIP_COLOR))));
        super.appendTooltip(stack, world, tooltip, context);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return false;
    }
}