package dev.thenexusgates.hyguard.map;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.worldmap.IWorldMap;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapLoadException;
import com.hypixel.hytale.server.core.universe.world.worldmap.provider.IWorldMapProvider;

public final class HyGuardWorldMapProvider implements IWorldMapProvider {

    public static final String ID = "HyGuard";
    public static final BuilderCodec<HyGuardWorldMapProvider> CODEC = BuilderCodec.builder(
            HyGuardWorldMapProvider.class,
            HyGuardWorldMapProvider::new
    ).build();

    @Override
    public IWorldMap getGenerator(World world) throws WorldMapLoadException {
        return HyGuardChunkWorldMap.INSTANCE;
    }
}