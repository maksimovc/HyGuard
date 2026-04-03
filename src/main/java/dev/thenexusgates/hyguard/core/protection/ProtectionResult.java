package dev.thenexusgates.hyguard.core.protection;

import dev.thenexusgates.hyguard.core.region.Region;

public record ProtectionResult(boolean allowed, Region region) {

    public static ProtectionResult allow() {
        return new ProtectionResult(true, null);
    }

    public static ProtectionResult deny(Region region) {
        return new ProtectionResult(false, region);
    }
}