package fable.hideseek.imba.mask;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MaskState {

    private static final Map<UUID, MaskState> STATES = new HashMap<>();

    public MaskType type = MaskType.NONE;
    public Block block;
    public Item item;

    public float rotation = 0.0f;
    public float rotationX = 0.0f;
    public float rotationZ = 0.0f;

    public boolean statue = false;
    public boolean frozen = false;

    public double anchorX;
    public double anchorY;
    public double anchorZ;

    public boolean doorOpen = false;
    public boolean buttonPressed = false;
    public int buttonTicks = 0;

    public int sculkStepCount = 0;
    public static final int MAX_SCULK_STEPS = 12;

    public boolean wallClimbing = true;

    public boolean wallAttached = false;
    public boolean attachedToFrame = false;
    public Direction attachmentFacing = Direction.NORTH;
    public int frameRotationStep = 0;

    public static MaskState get(UUID uuid) {
        return STATES.computeIfAbsent(uuid, k -> new MaskState());
    }

    public static void reset(UUID uuid) {
        STATES.remove(uuid);
    }

    public static boolean hasMask(UUID uuid) {
        MaskState state = STATES.get(uuid);
        return state != null && state.type != MaskType.NONE;
    }

    public static boolean isStatue(UUID uuid) {
        return get(uuid).statue;
    }

    public static void enableStatue(UUID uuid, double x, double y, double z) {
        MaskState state = get(uuid);
        state.statue = true;
        state.frozen = true;
        state.anchorX = x;
        state.anchorY = y;
        state.anchorZ = z;
    }

    public static void disableStatue(UUID uuid) {
        MaskState state = get(uuid);
        state.statue = false;
        state.frozen = false;
        state.anchorX = 0.0;
        state.anchorY = 0.0;
        state.anchorZ = 0.0;
        state.wallAttached = false;
        state.attachedToFrame = false;
        state.attachmentFacing = Direction.NORTH;
    }

    public static boolean isFrozen(UUID uuid) {
        return get(uuid).frozen;
    }

    public static double getX(UUID uuid) {
        return get(uuid).anchorX;
    }

    public static double getY(UUID uuid) {
        return get(uuid).anchorY;
    }

    public static double getZ(UUID uuid) {
        return get(uuid).anchorZ;
    }

    public static void rotate(UUID uuid) {
        MaskState state = get(uuid);
        state.rotation = normalize(state.rotation + 90.0f);
    }

    public static void rotateX(UUID uuid) {
        MaskState state = get(uuid);
        state.rotationX = normalize(state.rotationX + 90.0f);
    }

    public static void rotateZ(UUID uuid) {
        MaskState state = get(uuid);
        state.rotationZ = normalize(state.rotationZ + 90.0f);
    }

    public static void rotateFrameStep(UUID uuid) {
        MaskState state = get(uuid);
        state.frameRotationStep = (state.frameRotationStep + 1) % 8;
    }

    public static float getRotation(UUID uuid) {
        return get(uuid).rotation;
    }

    public static float getRotationX(UUID uuid) {
        return get(uuid).rotationX;
    }

    public static float getRotationZ(UUID uuid) {
        return get(uuid).rotationZ;
    }

    public static void setRotation(UUID uuid, float value) {
        get(uuid).rotation = normalize(value);
    }

    public static void setRotationX(UUID uuid, float value) {
        get(uuid).rotationX = normalize(value);
    }

    public static void setRotationZ(UUID uuid, float value) {
        get(uuid).rotationZ = normalize(value);
    }

    private static float normalize(float value) {
        float result = value % 360.0f;
        return result < 0 ? result + 360.0f : result;
    }
}