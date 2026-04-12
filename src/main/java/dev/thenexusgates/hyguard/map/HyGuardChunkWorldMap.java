package dev.thenexusgates.hyguard.map;

import com.hypixel.hytale.protocol.packets.worldmap.MapMarker;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.map.WorldMap;
import com.hypixel.hytale.server.core.universe.world.worldmap.IWorldMap;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapSettings;
import com.hypixel.hytale.server.core.universe.world.worldmap.provider.chunk.ChunkWorldMap;
import dev.thenexusgates.hyguard.HyGuardPlugin;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class HyGuardChunkWorldMap implements IWorldMap {

    public static final HyGuardChunkWorldMap INSTANCE = new HyGuardChunkWorldMap();

    private HyGuardChunkWorldMap() {
    }

    @Override
    public WorldMapSettings getWorldMapSettings() {
        return ChunkWorldMap.INSTANCE.getWorldMapSettings();
    }

    @Override
    public CompletableFuture<WorldMap> generate(World world, int width, int height, LongSet chunkIndexes) {
        return ChunkWorldMap.INSTANCE.generate(world, width, height, chunkIndexes)
                .thenApply(worldMap -> {
                    HyGuardPlugin plugin = HyGuardPlugin.getInstance();
                    if (plugin != null && plugin.getMapOverlayManager() != null) {
                        plugin.getMapOverlayManager().applyOverlays(world, worldMap);
                    }
                    return worldMap;
                });
    }

    @Override
    public CompletableFuture<Map<String, MapMarker>> generatePointsOfInterest(World world) {
        return ChunkWorldMap.INSTANCE.generatePointsOfInterest(world)
                .thenApply(pointsOfInterest -> {
                    HyGuardPlugin plugin = HyGuardPlugin.getInstance();
                    if (plugin == null || plugin.getMapOverlayManager() == null) {
                        return pointsOfInterest;
                    }

                    HashMap<String, MapMarker> merged = new HashMap<>(pointsOfInterest);
                    merged.putAll(plugin.getMapOverlayManager().generatePointsOfInterest(world));
                    return merged;
                });
    }
}