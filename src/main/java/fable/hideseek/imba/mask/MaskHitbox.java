package fable.hideseek.imba.mask;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.item.Item;

public final class MaskHitbox {

    private static final EntityDimensions DEFAULT_PLAYER = EntityDimensions.fixed(0.6f, 1.8f);
    /*
     * These are the hider's movement dimensions, not the visible/attack shape.
     * A one-block-wide player body catches on door jambs and a height of exactly
     * one/two blocks catches on ceilings because of floating point rounding.
     * MaskCollisionShapes keeps the full block/door geometry for seekers.
     */
    private static final EntityDimensions BLOCK_LIKE = EntityDimensions.fixed(0.6f, 0.95f);
    private static final EntityDimensions ITEM = EntityDimensions.fixed(0.5f, 0.5f);
    private static final EntityDimensions DOOR = EntityDimensions.fixed(0.6f, 1.95f);
    private static final EntityDimensions SPECIAL_POTION = EntityDimensions.fixed(0.35f, 0.70f);

    private static final float DEFAULT_PLAYER_EYE = 1.62f;
    private static final float BLOCK_LIKE_EYE = 0.85f;
    private static final float DOOR_EYE = 1.62f;
    private static final float SPECIAL_POTION_EYE = 0.30f;

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
        // Item masks are gameplay masks too: except for the deliberately small
        // 2D potion they must occupy exactly the same space as a block mask.
        if (type == MaskType.ITEM || type == MaskType.WALL_CLIMB) return BLOCK_LIKE;

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
        if (type == MaskType.ITEM || type == MaskType.WALL_CLIMB) return BLOCK_LIKE_EYE;

        return BLOCK_LIKE_EYE;
    }
}
