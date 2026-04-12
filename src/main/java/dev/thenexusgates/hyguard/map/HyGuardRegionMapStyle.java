package dev.thenexusgates.hyguard.map;

import dev.thenexusgates.hyguard.core.region.Region;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public final class HyGuardRegionMapStyle {

    public static final String ENABLED_KEY = "mapOverlay.enabled";
    public static final String COLOR_KEY = "mapOverlay.color";
    public static final String SHOW_LABEL_KEY = "mapOverlay.showLabel";

    public static final boolean DEFAULT_ENABLED = true;
    public static final boolean DEFAULT_SHOW_LABEL = false;
    public static final int DEFAULT_OPACITY_PERCENT = 35;

    private static final int[] DEFAULT_PALETTE = {
            0x2F7C9F,
            0x2F7A58,
            0x8F4440,
            0x7B5D2E,
            0x5A6EAF,
            0x8B5A8F,
            0x2D7D73,
            0xA06A38
    };

    private HyGuardRegionMapStyle() {
    }

    public static boolean isVisible(Region region) {
        if (region == null || region.isGlobal()) {
            return false;
        }

        String raw = region.getMetadataValue(ENABLED_KEY);
        if (raw == null || raw.isBlank()) {
            return DEFAULT_ENABLED;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    public static int resolveColor(Region region) {
        int configured = parseColor(region == null ? null : region.getMetadataValue(COLOR_KEY));
        return configured >= 0 ? configured : resolveDefaultColor(region);
    }

    public static int[] availableColors() {
        return Arrays.copyOf(DEFAULT_PALETTE, DEFAULT_PALETTE.length);
    }

    public static int defaultColor() {
        return DEFAULT_PALETTE[0];
    }

    public static String resolveColorHex(Region region) {
        return String.format(Locale.ROOT, "#%06X", resolveColor(region));
    }

    public static int resolveOpacityPercent(Region region) {
        return DEFAULT_OPACITY_PERCENT;
    }

    public static float resolveOpacity(Region region) {
        return resolveOpacityPercent(region) / 100F;
    }

    public static boolean isLabelVisible(Region region) {
        return false;
    }

    public static String normalizeColor(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        if (normalized.length() != 6) {
            return null;
        }
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            boolean hexDigit = current >= '0' && current <= '9'
                    || current >= 'A' && current <= 'F';
            if (!hexDigit) {
                return null;
            }
        }
        return "#" + normalized;
    }

    public static int parseColor(String raw) {
        String normalized = normalizeColor(raw);
        if (normalized == null) {
            return -1;
        }
        return Integer.parseInt(normalized.substring(1), 16);
    }

    private static int resolveDefaultColor(Region region) {
        if (region == null) {
            return DEFAULT_PALETTE[0];
        }

        int paletteIndex = Math.floorMod(
                Objects.hash(region.getOwnerUuid(), region.getOwnerName(), region.getId()),
                DEFAULT_PALETTE.length);
        return DEFAULT_PALETTE[paletteIndex];
    }
}