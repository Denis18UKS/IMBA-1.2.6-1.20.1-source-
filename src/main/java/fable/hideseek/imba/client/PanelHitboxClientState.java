package fable.hideseek.imba.client;

import fable.hideseek.imba.config.PanelHitboxConfig;

import java.util.EnumMap;
import java.util.Map;

/** Client copy used only by the editor preview. The server remains authoritative. */
public final class PanelHitboxClientState {
    private static final EnumMap<PanelHitboxConfig.Arrow, PanelHitboxConfig.Rect> RECTS =
            new EnumMap<>(PanelHitboxConfig.Arrow.class);

    static {
        resetLocalDefaults();
    }

    private PanelHitboxClientState() {
    }

    public static void apply(Map<PanelHitboxConfig.Arrow, PanelHitboxConfig.Rect> values) {
        if (values == null) return;
        for (PanelHitboxConfig.Arrow arrow : PanelHitboxConfig.Arrow.values()) {
            PanelHitboxConfig.Rect rect = values.get(arrow);
            if (rect != null) RECTS.put(arrow, rect.copy());
        }
    }

    public static PanelHitboxConfig.Rect get(PanelHitboxConfig.Arrow arrow) {
        PanelHitboxConfig.Rect rect = RECTS.get(arrow);
        return rect == null ? new PanelHitboxConfig.Rect(arrow.defaultX, arrow.defaultY, 26, 22) : rect.copy();
    }

    public static Map<PanelHitboxConfig.Arrow, PanelHitboxConfig.Rect> snapshot() {
        EnumMap<PanelHitboxConfig.Arrow, PanelHitboxConfig.Rect> result =
                new EnumMap<>(PanelHitboxConfig.Arrow.class);
        for (PanelHitboxConfig.Arrow arrow : PanelHitboxConfig.Arrow.values()) {
            result.put(arrow, get(arrow));
        }
        return result;
    }

    private static void resetLocalDefaults() {
        RECTS.clear();
        for (PanelHitboxConfig.Arrow arrow : PanelHitboxConfig.Arrow.values()) {
            RECTS.put(arrow, new PanelHitboxConfig.Rect(arrow.defaultX, arrow.defaultY, 26, 22));
        }
    }
}
