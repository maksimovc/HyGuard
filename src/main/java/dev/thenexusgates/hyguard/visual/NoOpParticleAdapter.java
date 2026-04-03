package dev.thenexusgates.hyguard.visual;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.thenexusgates.hyguard.util.BlockPos;

public final class NoOpParticleAdapter implements ParticleAdapter {

    @Override
    public boolean supportsParticles() {
        return false;
    }

    @Override
    public void drawCuboid(PlayerRef viewer, BlockPos min, BlockPos max, ParticleStyle style) {
    }
}