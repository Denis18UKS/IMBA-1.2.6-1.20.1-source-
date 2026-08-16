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
    public float rotation = 0.0f, rotationX = 0.0f, rotationZ = 0.0f;
    public boolean statue = false, frozen = false;
    public double anchorX, anchorY, anchorZ;
    public boolean doorOpen = false, buttonPressed = false;
    public int buttonTicks = 0, sculkStepCount = 0;
    public static final int MAX_SCULK_STEPS = 12;
    public boolean wallClimbing = true, wallAttached = false, attachedToFrame = false;
    public Direction attachmentFacing = Direction.NORTH;
    public int frameRotationStep = 0;

    public static MaskState get(UUID uuid) { return STATES.computeIfAbsent(uuid, k -> new MaskState()); }
    public static void reset(UUID uuid) { STATES.remove(uuid); }
    public static boolean hasMask(UUID uuid) { MaskState state=STATES.get(uuid);return state!=null&&state.type!=MaskType.NONE; }
    public static java.util.Set<UUID> snapshotIds() { return java.util.Set.copyOf(STATES.keySet()); }
    public static void resetAll() { STATES.clear(); }
    public static boolean isStatue(UUID uuid) { return get(uuid).statue; }
    public static void enableStatue(UUID uuid,double x,double y,double z){MaskState s=get(uuid);s.statue=true;s.frozen=true;s.anchorX=x;s.anchorY=y;s.anchorZ=z;}
    public static void disableStatue(UUID uuid){MaskState s=get(uuid);s.statue=false;s.frozen=false;s.anchorX=s.anchorY=s.anchorZ=0.0;s.wallAttached=false;s.attachedToFrame=false;s.attachmentFacing=Direction.NORTH;}
    public static boolean isFrozen(UUID uuid){return get(uuid).frozen;}
    public static double getX(UUID uuid){return get(uuid).anchorX;}public static double getY(UUID uuid){return get(uuid).anchorY;}public static double getZ(UUID uuid){return get(uuid).anchorZ;}
    public static void rotate(UUID uuid){MaskState s=get(uuid);s.rotation=normalize(s.rotation+90f);}public static void rotateX(UUID uuid){MaskState s=get(uuid);s.rotationX=normalize(s.rotationX+90f);}public static void rotateZ(UUID uuid){MaskState s=get(uuid);s.rotationZ=normalize(s.rotationZ+90f);}public static void rotateFrameStep(UUID uuid){MaskState s=get(uuid);s.frameRotationStep=(s.frameRotationStep+1)%8;}
    public static float getRotation(UUID uuid){return get(uuid).rotation;}public static float getRotationX(UUID uuid){return get(uuid).rotationX;}public static float getRotationZ(UUID uuid){return get(uuid).rotationZ;}public static void setRotation(UUID uuid,float v){get(uuid).rotation=normalize(v);}public static void setRotationX(UUID uuid,float v){get(uuid).rotationX=normalize(v);}public static void setRotationZ(UUID uuid,float v){get(uuid).rotationZ=normalize(v);}
    private static float normalize(float v){float r=v%360f;return r<0?r+360f:r;}
}
