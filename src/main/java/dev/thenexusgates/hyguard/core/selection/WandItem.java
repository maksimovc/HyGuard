package dev.thenexusgates.hyguard.core.selection;

public final class WandItem {

    private WandItem() {
    }

    public static boolean matches(String heldItemId, String configuredItemId) {
        return heldItemId != null && configuredItemId != null && heldItemId.equalsIgnoreCase(configuredItemId);
    }
}