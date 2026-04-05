package dev.thenexusgates.hyguard.storage;

import dev.thenexusgates.hyguard.core.region.Region;
import dev.thenexusgates.hyguard.util.BlockPos;
import dev.thenexusgates.hyguard.util.GeometryUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RegionCache {

    private final ConcurrentHashMap<String, Region> byId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> regionIdByName = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<Long, List<String>>> chunkIndex = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<String>> globalRegions = new ConcurrentHashMap<>();

    public void loadAll(Collection<Region> regions) {
        byId.clear();
        regionIdByName.clear();
        chunkIndex.clear();
        globalRegions.clear();
        for (Region region : regions) {
            byId.put(region.getId(), region);
            regionIdByName.put(key(region.getWorldId(), region.getName()), region.getId());
        }
        rebuildAllIndexes();
    }

    public void put(Region region) {
        Region previous = byId.put(region.getId(), region);
        if (previous != null) {
            regionIdByName.remove(key(previous.getWorldId(), previous.getName()), previous.getId());
        }
        regionIdByName.put(key(region.getWorldId(), region.getName()), region.getId());
        if (previous != null && !normalize(previous.getWorldId()).equals(normalize(region.getWorldId()))) {
            rebuildWorldIndex(previous.getWorldId());
            rebuildWorldIndex(region.getWorldId());
            return;
        }
        rebuildWorldIndex(region.getWorldId());
    }

    public Region remove(String regionId) {
        Region removed = byId.remove(regionId);
        if (removed == null) {
            return null;
        }
        regionIdByName.remove(key(removed.getWorldId(), removed.getName()));
        rebuildWorldIndex(removed.getWorldId());
        return removed;
    }

    public Region findByName(String worldId, String name) {
        String regionId = regionIdByName.get(key(worldId, name));
        return regionId == null ? null : byId.get(regionId);
    }

    public Region findById(String regionId) {
        if (regionId == null || regionId.isBlank()) {
            return null;
        }
        return byId.get(regionId);
    }

    public List<Region> getRegionsAt(String worldId, BlockPos blockPos) {
        ArrayList<Region> regions = new ArrayList<>();
        ConcurrentHashMap<Long, List<String>> worldIndex = chunkIndex.get(normalize(worldId));
        if (worldIndex != null) {
            long chunkKey = GeometryUtils.chunkKey(blockPos.getX() >> 4, blockPos.getZ() >> 4);
            List<String> regionIds = worldIndex.get(chunkKey);
            if (regionIds != null) {
                for (String regionId : regionIds) {
                    Region region = byId.get(regionId);
                    if (region != null && region.contains(blockPos)) {
                        regions.add(region);
                    }
                }
            }
        }

        if (regions.isEmpty()) {
            for (String regionId : globalRegions.getOrDefault(normalize(worldId), List.of())) {
                Region region = byId.get(regionId);
                if (region != null) {
                    regions.add(region);
                }
            }
        }

        regions.sort(Comparator
            .comparingLong(Region::getVolume)
            .thenComparing(Comparator.comparingInt(Region::getPriority).reversed())
                .thenComparing(Region::getName, String.CASE_INSENSITIVE_ORDER));
        return regions;
    }

    public Collection<Region> allRegions() {
        return List.copyOf(byId.values());
    }

    private void index(Region region) {
        if (region.isGlobal()) {
            globalRegions.compute(normalize(region.getWorldId()), (ignored, current) -> {
                ArrayList<String> next = current == null ? new ArrayList<>() : new ArrayList<>(current);
                if (!next.contains(region.getId())) {
                    next.add(region.getId());
                }
                return List.copyOf(next);
            });
            return;
        }

        ConcurrentHashMap<Long, List<String>> worldIndex = chunkIndex.computeIfAbsent(
                normalize(region.getWorldId()),
                ignored -> new ConcurrentHashMap<>()
        );

        int minChunkX = region.getMin().getX() >> 4;
        int maxChunkX = region.getMax().getX() >> 4;
        int minChunkZ = region.getMin().getZ() >> 4;
        int maxChunkZ = region.getMax().getZ() >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                long key = GeometryUtils.chunkKey(chunkX, chunkZ);
                worldIndex.compute(key, (ignored, current) -> {
                    ArrayList<String> next = current == null ? new ArrayList<>() : new ArrayList<>(current);
                    if (!next.contains(region.getId())) {
                        next.add(region.getId());
                    }
                    return List.copyOf(next);
                });
            }
        }
    }

    private void rebuildWorldIndex(String worldId) {
        String normalizedWorldId = normalize(worldId);
        if (normalizedWorldId.isEmpty()) {
            return;
        }

        chunkIndex.remove(normalizedWorldId);
        globalRegions.remove(normalizedWorldId);

        List<Region> worldRegions = byId.values().stream()
                .filter(region -> normalizedWorldId.equals(normalize(region.getWorldId())))
                .toList();
        rebuildRelationships(worldRegions);
        for (Region region : worldRegions) {
            index(region);
        }
    }

    private void rebuildAllIndexes() {
        chunkIndex.clear();
        globalRegions.clear();

        Map<String, List<Region>> worlds = new HashMap<>();
        for (Region region : byId.values()) {
            worlds.computeIfAbsent(normalize(region.getWorldId()), ignored -> new ArrayList<>()).add(region);
        }
        for (List<Region> worldRegions : worlds.values()) {
            rebuildRelationships(worldRegions);
            for (Region region : worldRegions) {
                index(region);
            }
        }
    }

    private void rebuildRelationships(List<Region> worldRegions) {
        for (Region region : worldRegions) {
            region.assignParentRegionId(null);
            region.replaceChildRegionIds(List.of());
        }

        Map<String, List<String>> childIdsByParent = new HashMap<>();
        for (Region child : worldRegions) {
            Region parent = findParent(worldRegions, child);
            if (parent == null) {
                continue;
            }
            child.assignParentRegionId(parent.getId());
            childIdsByParent.computeIfAbsent(parent.getId(), ignored -> new ArrayList<>()).add(child.getId());
        }

        for (Region region : worldRegions) {
            region.replaceChildRegionIds(childIdsByParent.getOrDefault(region.getId(), List.of()));
        }
    }

    private Region findParent(List<Region> worldRegions, Region child) {
        if (child.isGlobal()) {
            return null;
        }

        Region best = null;
        for (Region candidate : worldRegions) {
            if (candidate.isGlobal()
                    || candidate.getId().equals(child.getId())
                    || !GeometryUtils.contains(candidate, child)) {
                continue;
            }
            if (best == null
                    || candidate.getVolume() < best.getVolume()
                    || (candidate.getVolume() == best.getVolume() && candidate.getPriority() > best.getPriority())) {
                best = candidate;
            }
        }
        return best;
    }

    private static String key(String worldId, String name) {
        return normalize(worldId) + ":" + normalize(name);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}