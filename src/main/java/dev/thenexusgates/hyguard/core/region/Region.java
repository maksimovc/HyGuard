package dev.thenexusgates.hyguard.core.region;

import dev.thenexusgates.hyguard.util.BlockPos;
import dev.thenexusgates.hyguard.util.BlockPosUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class Region {

    private String schemaVersion = "1";
    private String id;
    private String name;
    private String ownerUuid;
    private String ownerName;
    private RegionShape shape = RegionShape.CUBOID;
    private BlockPos min;
    private BlockPos max;
    private String worldId;
    private Map<RegionFlag, RegionFlagValue> flags = new HashMap<>();
    private Map<String, RegionMember> members = new HashMap<>();
    private int priority;
    private String parentRegionId;
    private List<String> childRegionIds = new ArrayList<>();
    private long createdAt = Instant.now().toEpochMilli();
    private long lastModifiedAt = createdAt;
    private String createdByName;
    private boolean global;
    private BlockPos spawnPoint;
    private Map<String, String> metadata = new HashMap<>();

    public Region() {
    }

    public static Region create(String name, String ownerUuid, String ownerName, String worldId, BlockPos first, BlockPos second) {
        Region region = new Region();
        region.id = UUID.randomUUID().toString();
        region.name = name;
        region.ownerUuid = ownerUuid;
        region.ownerName = ownerName;
        region.worldId = worldId;
        region.min = BlockPosUtils.min(first, second);
        region.max = BlockPosUtils.max(first, second);
        region.createdByName = ownerName;
        return region;
    }

    public static Region createGlobal(String name, String ownerUuid, String ownerName, String worldId, BlockPos spawnPoint) {
        Region region = new Region();
        region.id = UUID.randomUUID().toString();
        region.name = name;
        region.ownerUuid = ownerUuid;
        region.ownerName = ownerName;
        region.worldId = worldId;
        region.global = true;
        region.createdByName = ownerName;
        region.spawnPoint = spawnPoint;
        return region;
    }

    public boolean contains(BlockPos blockPos) {
        if (global) {
            return blockPos != null;
        }
        return min != null && max != null && BlockPosUtils.contains(min, max, blockPos);
    }

    public String normalizedName() {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    public RegionRole getRoleFor(String uuid) {
        if (uuid == null) {
            return null;
        }
        if (uuid.equals(ownerUuid)) {
            return RegionRole.OWNER;
        }
        RegionMember member = members.get(uuid);
        return member == null ? null : member.getRole();
    }

    public Map<RegionFlag, RegionFlagValue> getFlags() {
        return flags == null ? Collections.emptyMap() : flags;
    }

    public Map<String, RegionMember> getMembers() {
        return members == null ? Collections.emptyMap() : members;
    }

    public Map<String, String> getMetadata() {
        return metadata == null ? Collections.emptyMap() : metadata;
    }

    public String getMetadataValue(String key) {
        if (key == null || key.isBlank() || metadata == null) {
            return null;
        }
        return metadata.get(key);
    }

    public RegionMember getMember(String uuid) {
        if (uuid == null || members == null) {
            return null;
        }
        return members.get(uuid);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getOwnerUuid() {
        return ownerUuid;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public BlockPos getMin() {
        return min;
    }

    public BlockPos getMax() {
        return max;
    }

    public String getWorldId() {
        return worldId;
    }

    public int getPriority() {
        return priority;
    }

    public String getParentRegionId() {
        return parentRegionId;
    }

    public List<String> getChildRegionIds() {
        return childRegionIds == null ? List.of() : List.copyOf(childRegionIds);
    }

    public void setPriority(int priority) {
        this.priority = priority;
        touch();
    }

    public long getLastModifiedAt() {
        return lastModifiedAt;
    }

    public boolean isGlobal() {
        return global;
    }

    public BlockPos getSpawnPoint() {
        return spawnPoint;
    }

    public void touch() {
        this.lastModifiedAt = Instant.now().toEpochMilli();
    }

    public void setBounds(BlockPos first, BlockPos second) {
        this.min = BlockPosUtils.min(first, second);
        this.max = BlockPosUtils.max(first, second);
        touch();
    }

    public void setSpawnPoint(BlockPos spawnPoint) {
        this.spawnPoint = spawnPoint;
        touch();
    }

    public void assignParentRegionId(String parentRegionId) {
        this.parentRegionId = parentRegionId;
    }

    public void replaceChildRegionIds(List<String> childRegionIds) {
        this.childRegionIds = childRegionIds == null ? new ArrayList<>() : new ArrayList<>(childRegionIds);
    }

    public long getVolume() {
        if (global || min == null || max == null) {
            return Long.MAX_VALUE;
        }
        long width = (long) max.getX() - min.getX() + 1L;
        long height = (long) max.getY() - min.getY() + 1L;
        long depth = (long) max.getZ() - min.getZ() + 1L;
        return width * height * depth;
    }

    public void putFlag(RegionFlag flag, RegionFlagValue value) {
        if (flags == null) {
            flags = new HashMap<>();
        }
        flags.put(flag, value);
        touch();
    }

    public void removeFlag(RegionFlag flag) {
        if (flags == null) {
            return;
        }
        flags.remove(flag);
        touch();
    }

    public void addMember(RegionMember member) {
        if (members == null) {
            members = new HashMap<>();
        }
        members.put(member.getUuid(), member);
        touch();
    }

    public boolean removeMember(String uuid) {
        if (uuid == null || members == null) {
            return false;
        }
        boolean removed = members.remove(uuid) != null;
        if (removed) {
            touch();
        }
        return removed;
    }

    public void putMetadata(String key, String value) {
        if (key == null || key.isBlank()) {
            return;
        }
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        if (value == null || value.isBlank()) {
            metadata.remove(key);
        } else {
            metadata.put(key, value);
        }
        touch();
    }

    public void removeMetadata(String key) {
        if (key == null || key.isBlank() || metadata == null) {
            return;
        }
        if (metadata.remove(key) != null) {
            touch();
        }
    }
}