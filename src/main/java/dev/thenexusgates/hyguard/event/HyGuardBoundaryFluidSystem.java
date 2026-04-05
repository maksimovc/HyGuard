package dev.thenexusgates.hyguard.event;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.thenexusgates.hyguard.HyGuardPlugin;
import dev.thenexusgates.hyguard.core.region.Region;
import dev.thenexusgates.hyguard.core.region.RegionFlag;
import dev.thenexusgates.hyguard.core.region.RegionFlagValue;
import dev.thenexusgates.hyguard.util.BlockPos;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public final class HyGuardBoundaryFluidSystem extends EntityTickingSystem<EntityStore> {

    private static final Query<EntityStore> QUERY = Query.and(Player.getComponentType());
    private static final long SCAN_INTERVAL_MS = 500L;

    private final HyGuardPlugin plugin;
    private final ConcurrentHashMap<String, Long> lastScanByWorld = new ConcurrentHashMap<>();

    public HyGuardBoundaryFluidSystem(HyGuardPlugin plugin) {
        this.plugin = plugin;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public void tick(float dt,
                     int index,
                     @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        EntityStore entityStore = store.getExternalData();
        World world = entityStore == null ? null : entityStore.getWorld();
        if (world == null || world.getChunkStore() == null) {
            return;
        }

        String worldId = world.getName();
        long now = System.currentTimeMillis();
        Long previousScan = lastScanByWorld.put(worldId, now);
        if (previousScan != null && now - previousScan < SCAN_INTERVAL_MS) {
            return;
        }

        scanWorld(worldId, world.getChunkStore());
    }

    private void scanWorld(String worldId, ChunkStore chunkStore) {
        List<Region> protectedRegions = plugin.getWorldRegions(worldId).stream()
                .filter(region -> region != null && !region.isGlobal())
                .filter(region -> denies(region, RegionFlag.LIQUID_FLOW) || denies(region, RegionFlag.FIRE_SPREAD))
                .toList();

        for (Region region : protectedRegions) {
            scanRegion(chunkStore, region, denies(region, RegionFlag.LIQUID_FLOW), denies(region, RegionFlag.FIRE_SPREAD));
        }
    }

    private void scanRegion(ChunkStore chunkStore,
                            Region region,
                            boolean blockLiquids,
                            boolean blockFire) {
        BlockPos min = region.getMin();
        BlockPos max = region.getMax();
        if (min == null || max == null) {
            return;
        }

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    inspectCell(chunkStore, region, x, y, z, blockLiquids, blockFire);
                }
            }
        }
    }

    private void inspectCell(ChunkStore chunkStore,
                             Region region,
                             int x,
                             int y,
                             int z,
                             boolean blockLiquids,
                             boolean blockFire) {
        FluidSection section = getFluidSection(chunkStore, x, y, z);
        if (section == null) {
            return;
        }

        int localX = Math.floorMod(x, 16);
        int localY = Math.floorMod(y, 16);
        int localZ = Math.floorMod(z, 16);
        Fluid fluid = section.getFluid(localX, localY, localZ);
        if (fluid == null || fluid == Fluid.EMPTY) {
            return;
        }

        boolean isFire = isFire(fluid);
        if ((isFire && !blockFire) || (!isFire && !blockLiquids)) {
            return;
        }

        if (isFire) {
            section.setFluid(localX, localY, localZ, Fluid.EMPTY, (byte) 0);
            return;
        }

        if (!shouldClearLiquid(chunkStore, section, region, x, y, z, localX, localY, localZ)) {
            return;
        }

        section.setFluid(localX, localY, localZ, Fluid.EMPTY, (byte) 0);
    }

    private boolean shouldClearLiquid(ChunkStore chunkStore,
                                      FluidSection section,
                                      Region region,
                                      int x,
                                      int y,
                                      int z,
                                      int localX,
                                      int localY,
                                      int localZ) {
        return section.getFluidLevel(localX, localY, localZ) > 0
                || hasExternalSource(chunkStore, region, x, y, z, false);
    }

    private boolean hasExternalSource(ChunkStore chunkStore,
                                      Region region,
                                      int x,
                                      int y,
                                      int z,
                                      boolean targetIsFire) {
        return matchesExternalFluid(chunkStore, region, x + 1, y, z, targetIsFire)
                || matchesExternalFluid(chunkStore, region, x - 1, y, z, targetIsFire)
                || matchesExternalFluid(chunkStore, region, x, y + 1, z, targetIsFire)
                || matchesExternalFluid(chunkStore, region, x, y - 1, z, targetIsFire)
                || matchesExternalFluid(chunkStore, region, x, y, z + 1, targetIsFire)
                || matchesExternalFluid(chunkStore, region, x, y, z - 1, targetIsFire);
    }

    private boolean matchesExternalFluid(ChunkStore chunkStore,
                                         Region region,
                                         int x,
                                         int y,
                                         int z,
                                         boolean targetIsFire) {
        if (contains(region, x, y, z)) {
            return false;
        }

        FluidSection neighborSection = getFluidSection(chunkStore, x, y, z);
        if (neighborSection == null) {
            return false;
        }

        Fluid neighborFluid = neighborSection.getFluid(Math.floorMod(x, 16), Math.floorMod(y, 16), Math.floorMod(z, 16));
        if (neighborFluid == null || neighborFluid == Fluid.EMPTY) {
            return false;
        }
        return isFire(neighborFluid) == targetIsFire;
    }

    private FluidSection getFluidSection(ChunkStore chunkStore, int x, int y, int z) {
        Ref<ChunkStore> sectionRef = chunkStore.getChunkSectionReferenceAtBlock(x, y, z);
        if (sectionRef == null) {
            return null;
        }
        return sectionRef.getStore().getComponent(sectionRef, FluidSection.getComponentType());
    }

    private boolean contains(Region region, int x, int y, int z) {
        BlockPos min = region.getMin();
        BlockPos max = region.getMax();
        return min != null
                && max != null
                && x >= min.getX() && x <= max.getX()
                && y >= min.getY() && y <= max.getY()
                && z >= min.getZ() && z <= max.getZ();
    }

    private boolean isFire(Fluid fluid) {
        String fluidId = fluid.getId();
        return fluidId != null && fluidId.toLowerCase(Locale.ROOT).contains("fire");
    }

    private boolean denies(Region region, RegionFlag flag) {
        RegionFlagValue flagValue = region.getFlags().get(flag);
        return flagValue != null && flagValue.getMode() == RegionFlagValue.Mode.DENY;
    }
}