package fable.hideseek.imba.mask;

import fable.hideseek.imba.ImbaMod;
import fable.hideseek.imba.config.MaskBlockConfig;
import fable.hideseek.imba.config.MaskHitboxConfig;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.item.Item;

public final class MaskHitbox {
    private static final EntityDimensions DEFAULT_PLAYER = EntityDimensions.fixed(.6F, 1.8F),
            BLOCK_LIKE = EntityDimensions.fixed(.98F, .98F),
            DOOR = EntityDimensions.fixed(.98F, 1.98F),
            SPECIAL_POTION = EntityDimensions.fixed(.50F, .70F);
    private static final float DEFAULT_PLAYER_EYE = 1.62F, BLOCK_LIKE_EYE = .85F,
            DOOR_EYE = 1.62F, SPECIAL_POTION_EYE = .30F;

    private MaskHitbox() {}

    public static EntityDimensions getDimensions(MaskType type, Item item) {
        return getDimensions(type, null, item);
    }

    public static EntityDimensions getDimensions(MaskType type, Block block, Item item) {
        if (type == null || type == MaskType.NONE) return DEFAULT_PLAYER;
        if (type == MaskType.DOOR) return DOOR;
        if (type == MaskType.ITEM && MaskService.isSpecialPotion(item)) return SPECIAL_POTION;
        if (usesConfiguredBounds(block)) {
            var bounds = MaskHitboxConfig.boundsFor(block);
            return EntityDimensions.fixed(Math.max(.05F, bounds.width()), Math.max(.05F, bounds.height()));
        }
        return BLOCK_LIKE;
    }

    public static float getEyeHeight(MaskType type, Item item) {
        return getEyeHeight(type, null, item);
    }

    public static float getEyeHeight(MaskType type, Block block, Item item) {
        if (type == null || type == MaskType.NONE) return DEFAULT_PLAYER_EYE;
        if (type == MaskType.DOOR) return DOOR_EYE;
        if (type == MaskType.ITEM && MaskService.isSpecialPotion(item)) return SPECIAL_POTION_EYE;
        if (usesConfiguredBounds(block)) {
            float h = Math.max(.05F, MaskHitboxConfig.boundsFor(block).height());
            return Math.min(h * .85F, Math.max(.05F, h - .05F));
        }
        return BLOCK_LIKE_EYE;
    }

    private static boolean usesConfiguredBounds(Block block) {
        return block != null && (!MaskBlockConfig.isFull(block)
                || MaskHitboxConfig.hasCustom(block)
                || block == ImbaMod.STONRCUTTER_LEZVIE
                || block == ImbaMod.HANGING_LANTERN);
    }
}
