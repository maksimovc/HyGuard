package dev.thenexusgates.hyguard.core.region;

public final class RegionMember {

    private String uuid;
    private String name;
    private RegionRole role = RegionRole.MEMBER;

    public RegionMember() {
    }

    public RegionMember(String uuid, String name, RegionRole role) {
        this.uuid = uuid;
        this.name = name;
        this.role = role;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public RegionRole getRole() {
        return role == null ? RegionRole.MEMBER : role;
    }
}