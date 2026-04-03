package dev.thenexusgates.hyguard.storage;

import dev.thenexusgates.hyguard.core.region.Region;

import java.util.Collection;

public interface RegionRepository {

    Collection<Region> loadAll();

    void saveRegionAsync(Region region);

    void deleteRegionAsync(Region region);

    void flush();

    void shutdown();
}