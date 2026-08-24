package fable.hideseek.imba.client;

import fable.hideseek.imba.config.MaskHitboxConfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class MaskHitboxClientState {
    private static final Set<String> NON_FULL = new HashSet<>();
    private static final Map<String, MaskHitboxConfig.BoxSpec> BOXES = new HashMap<>();

    private MaskHitboxClientState() {
    }

    public static void apply(Set<String> nonFull, Map<String, MaskHitboxConfig.BoxSpec> boxes) {
        NON_FULL.clear();
        if (nonFull != null) NON_FULL.addAll(nonFull);
        BOXES.clear();
        if (boxes != null) boxes.forEach((id, box) -> BOXES.put(id, box.copy()));
        MaskHitboxConfig.applyNetworkSnapshot(BOXES);
    }

    public static Set<String> nonFullSnapshot() {
        return new HashSet<>(NON_FULL);
    }

    public static Map<String, MaskHitboxConfig.BoxSpec> boxesSnapshot() {
        Map<String, MaskHitboxConfig.BoxSpec> result = new HashMap<>();
        BOXES.forEach((id, box) -> result.put(id, box.copy()));
        return result;
    }

    public static MaskHitboxConfig.BoxSpec custom(String id) {
        MaskHitboxConfig.BoxSpec box = BOXES.get(id);
        return box == null ? null : box.copy();
    }
}
