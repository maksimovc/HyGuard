package dev.thenexusgates.hyguard.core.region;

public enum RegionRole {
    OWNER,
    CO_OWNER,
    MANAGER,
    MEMBER,
    TRUSTED,
    VISITOR;

    public boolean isAtLeast(RegionRole other) {
        return ordinal() <= other.ordinal();
    }
}