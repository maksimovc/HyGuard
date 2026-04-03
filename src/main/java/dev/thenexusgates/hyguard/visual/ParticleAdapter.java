package dev.thenexusgates.hyguard.visual;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.thenexusgates.hyguard.util.BlockPos;

public interface ParticleAdapter {

    enum ParticleStyle {
        SELECTION_VALID,
        SELECTION_CONFLICT,
        REGION_BORDER
    }

    boolean supportsParticles();

    void drawCuboid(PlayerRef viewer, BlockPos min, BlockPos max, ParticleStyle style);
}