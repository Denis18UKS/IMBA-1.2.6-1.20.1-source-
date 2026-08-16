package fable.hideseek.imba.client;

import java.util.Arrays;

public final class PanelData {
    public static int seconds = 120;
    public static int hearts = 5;

    public static String timerLabel = "Таймер";
    public static String heartsLabel = "Сердца";
    public static float timerTitleScale = 1.30F;
    public static float heartsTitleScale = 1.30F;
    public static float timerValueScale = 1.60F;
    public static float heartsValueScale = 1.60F;
    public static float arrowScale = 1.45F;
    public static int timerX = -38;
    public static int heartsX = 38;
    public static int titleY = -52;
    public static int upArrowY = -26;
    public static int valueY = 0;
    public static int downArrowY = 28;

    public static int selectedLocation = 0;
    public static int locationCount = 12;
    public static String locationName = "Локация 1";
    public static String[] locationNames = new String[12];
    public static String[] locationMaskKinds = new String[12];
    public static String[] locationMaskIds = new String[12];
    public static double potionOffsetX = 0.0D;
    public static double potionOffsetY = 0.15D;
    public static double potionOffsetZ = 0.0D;

    static {
        Arrays.fill(locationNames, "");
        Arrays.fill(locationMaskKinds, "BLOCK");
        Arrays.fill(locationMaskIds, "minecraft:stone");
    }

    public static String configuredName(int index) {
        return index >= 0 && index < locationNames.length ? locationNames[index] : "";
    }

    public static String locationName(int index) {
        String configured = configuredName(index);
        return configured == null || configured.isBlank() ? Integer.toString(index + 1) : configured;
    }

    public static String maskKind(int index) {
        return index >= 0 && index < locationMaskKinds.length ? locationMaskKinds[index] : "BLOCK";
    }

    public static String maskId(int index) {
        return index >= 0 && index < locationMaskIds.length ? locationMaskIds[index] : "minecraft:stone";
    }

    private PanelData() {}
}
