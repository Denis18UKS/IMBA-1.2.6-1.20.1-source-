package fable.hideseek.imba.mask;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.item.Item;

/** Dimensions used by both server and client for masked players. */
public final class MaskHitbox {

    private static final EntityDimensions DEFAULT_PLAYER = EntityDimensions.fixed(0.6F, 1.8F);
    private static final EntityDimensions BLOCK_LIKE = EntityDimensions.fixed(0.98F, 0.98F);
    private static final EntityDimensions DOOR = EntityDimensions.fixed(0.98F, 1.98F);
    private static final EntityDimensions SPECIAL_POTION = EntityDimensions.fixed(0.50F, 0.70F);

    private static final float DEFAULT_PLAYER_EYE = 1.62F;
    private static final float BLOCK_LIKE_EYE = 0.85F;
    private static final float DOOR_EYE = 1.62F;
    private static final float SPECIAL_POTION_EYE = 0.30F;

    private MaskHitbox() {
    }

    public static EntityDimensions getDimensions(MaskType type, Item item) {
        if (type == null || type == MaskType.NONE) {
            return DEFAULT_PLAYER;
        }
        if (type == MaskType.DOOR) {
            return DOOR;
        }
        if (type == MaskType.ITEM && MaskService.isSpecialPotion(item)) {
            return SPECIAL_POTION;
        }
        return BLOCK_LIKE;
    }

    public static float getEyeHeight(MaskType type, Item item) {
        if (type == null || type == MaskType.NONE) {
            return DEFAULT_PLAYER_EYE;
        }
        if (type == MaskType.DOOR) {
            return DOOR_EYE;
        }
        if (type == MaskType.ITEM && MaskService.isSpecialPotion(item)) {
            return SPECIAL_POTION_EYE;
        }
        return BLOCK_LIKE_EYE;
    }
}
