package fable.hideseek.imba.boosty_toggle;

public final class BoostyToogler {

    private static boolean enabled = false;

    private BoostyToogler() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean toggle() {
        enabled = !enabled;
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }
}