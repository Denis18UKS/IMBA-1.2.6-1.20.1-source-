package fable.hideseek.imba.game;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class RoundDefinition {

    public enum SourceKind {
        BLOCK,
        ITEM
    }

    public final String id;
    public final RegistryKey<World> worldKey;

    public Vec3d hiderPos;
    public float hiderYaw;
    public float hiderPitch;

    public Vec3d seekerPos;
    public float seekerYaw;
    public float seekerPitch;

    public SourceKind sourceKind;
    public Identifier maskId;
    public Identifier giveItemId;
    public String displayWord;
    public String locationName;
    public final boolean setEvening;
    public Vec3d cameraPos;
    public float cameraYaw;
    public float cameraPitch;

    public RoundDefinition(String id,
            RegistryKey<World> worldKey,
            Vec3d hiderPos, float hiderYaw, float hiderPitch,
            Vec3d seekerPos, float seekerYaw, float seekerPitch,
            SourceKind sourceKind,
            Identifier maskId,
            Identifier giveItemId,
            String displayWord,
            String locationName,
            boolean setEvening) {
        this.id = id;
        this.worldKey = worldKey;
        this.hiderPos = hiderPos;
        this.hiderYaw = hiderYaw;
        this.hiderPitch = hiderPitch;
        this.seekerPos = seekerPos;
        this.seekerYaw = seekerYaw;
        this.seekerPitch = seekerPitch;
        this.sourceKind = sourceKind;
        this.maskId = maskId;
        this.giveItemId = giveItemId;
        this.displayWord = displayWord;
        this.locationName = locationName == null ? "" : locationName;
        this.setEvening = setEvening;
        this.cameraPos = hiderPos;
        this.cameraYaw = hiderYaw;
        this.cameraPitch = hiderPitch;
    }

    public void setMask(SourceKind sourceKind, Identifier maskId, String displayWord) {
        this.sourceKind = sourceKind;
        this.maskId = maskId;
        this.giveItemId = maskId;
        this.displayWord = displayWord;
    }

    public void setCameraPoint(Vec3d cameraPos, float cameraYaw, float cameraPitch) {
        this.cameraPos = cameraPos;
        this.cameraYaw = cameraYaw;
        this.cameraPitch = cameraPitch;
    }
}
