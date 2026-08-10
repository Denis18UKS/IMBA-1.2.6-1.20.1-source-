package fable.hideseek.imba.client;

import fable.hideseek.imba.mask.MaskType;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientMaskData {
    public static final Map<UUID, MaskType> TYPES = new HashMap<>();
    public static final Map<UUID, Block> BLOCKS = new HashMap<>();
    public static final Map<UUID, Item> ITEMS = new HashMap<>();
    public static final Map<UUID, Float> ROTATIONS = new HashMap<>();
    public static final Map<UUID, Float> ROTATIONS_X = new HashMap<>();
    public static final Map<UUID, Float> ROTATIONS_Z = new HashMap<>();
    public static final Map<UUID, Boolean> STATUE_MODE = new HashMap<>();
    public static final Map<UUID, Vec3d> STATUE_ANCHORS = new HashMap<>();
    public static final Map<UUID, Boolean> DOOR_OPEN = new HashMap<>();
    public static final Map<UUID, Boolean> BUTTON_PRESSED = new HashMap<>();
    public static final Map<UUID, Boolean> WALL_CLIMBING = new HashMap<>();
    public static final Map<UUID, Boolean> WALL_ATTACHED = new HashMap<>();
    public static final Map<UUID, Boolean> ATTACHED_TO_FRAME = new HashMap<>();
    public static final Map<UUID, Direction> ATTACHMENT_FACING = new HashMap<>();
    public static final Map<UUID, Integer> FRAME_ROTATION_STEP = new HashMap<>();

    public static void setMask(UUID uuid, MaskType type, Block block, Item item,
            float rotationY, float rotationX, float rotationZ,
            boolean doorOpen, boolean buttonPressed,
            boolean wallAttached, boolean attachedToFrame,
            Direction attachmentFacing, int frameRotationStep) {
        TYPES.put(uuid, type);

        if (block != null) {
            BLOCKS.put(uuid, block);
        } else {
            BLOCKS.remove(uuid);
        }

        if (item != null) {
            ITEMS.put(uuid, item);
        } else {
            ITEMS.remove(uuid);
        }

        ROTATIONS.put(uuid, rotationY);
        ROTATIONS_X.put(uuid, rotationX);
        ROTATIONS_Z.put(uuid, rotationZ);
        DOOR_OPEN.put(uuid, doorOpen);
        BUTTON_PRESSED.put(uuid, buttonPressed);
        WALL_ATTACHED.put(uuid, wallAttached);
        ATTACHED_TO_FRAME.put(uuid, attachedToFrame);
        ATTACHMENT_FACING.put(uuid, attachmentFacing == null ? Direction.NORTH : attachmentFacing);
        FRAME_ROTATION_STEP.put(uuid, frameRotationStep);
    }

    public static void reset(UUID uuid) {
        TYPES.remove(uuid);
        BLOCKS.remove(uuid);
        ITEMS.remove(uuid);
        ROTATIONS.remove(uuid);
        ROTATIONS_X.remove(uuid);
        ROTATIONS_Z.remove(uuid);
        STATUE_MODE.remove(uuid);
        STATUE_ANCHORS.remove(uuid);
        DOOR_OPEN.remove(uuid);
        BUTTON_PRESSED.remove(uuid);
        WALL_CLIMBING.remove(uuid);
        WALL_ATTACHED.remove(uuid);
        ATTACHED_TO_FRAME.remove(uuid);
        ATTACHMENT_FACING.remove(uuid);
        FRAME_ROTATION_STEP.remove(uuid);
    }

    public static void clearAll() {
        TYPES.clear();
        BLOCKS.clear();
        ITEMS.clear();
        ROTATIONS.clear();
        ROTATIONS_X.clear();
        ROTATIONS_Z.clear();
        STATUE_MODE.clear();
        STATUE_ANCHORS.clear();
        DOOR_OPEN.clear();
        BUTTON_PRESSED.clear();
        WALL_CLIMBING.clear();
        WALL_ATTACHED.clear();
        ATTACHED_TO_FRAME.clear();
        ATTACHMENT_FACING.clear();
        FRAME_ROTATION_STEP.clear();
    }

    public static boolean hasMask(UUID uuid) {
        return TYPES.containsKey(uuid);
    }

    public static boolean isStatue(UUID uuid) {
        return STATUE_MODE.getOrDefault(uuid, false);
    }

    public static void setStatue(UUID uuid, boolean statue, double x, double y, double z) {
        if (statue) {
            STATUE_MODE.put(uuid, true);
            STATUE_ANCHORS.put(uuid, new Vec3d(x, y, z));
        } else {
            STATUE_MODE.remove(uuid);
            STATUE_ANCHORS.remove(uuid);
        }
    }

    public static Vec3d getStatueAnchor(UUID uuid) {
        return STATUE_ANCHORS.get(uuid);
    }

    public static boolean isDoorOpen(UUID uuid) {
        return DOOR_OPEN.getOrDefault(uuid, false);
    }

    public static void setDoorOpen(UUID uuid, boolean open) {
        DOOR_OPEN.put(uuid, open);
    }

    public static boolean isButtonPressed(UUID uuid) {
        return BUTTON_PRESSED.getOrDefault(uuid, false);
    }

    public static boolean isWallAttached(UUID uuid) {
        return WALL_ATTACHED.getOrDefault(uuid, false);
    }

    public static boolean isAttachedToFrame(UUID uuid) {
        return ATTACHED_TO_FRAME.getOrDefault(uuid, false);
    }

    public static Direction getAttachmentFacing(UUID uuid) {
        return ATTACHMENT_FACING.getOrDefault(uuid, Direction.NORTH);
    }

    public static int getFrameRotationStep(UUID uuid) {
        return FRAME_ROTATION_STEP.getOrDefault(uuid, 0);
    }
}
