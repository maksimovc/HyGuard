package dev.thenexusgates.hyguard.visual;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.thenexusgates.hyguard.core.region.Region;

public final class RegionBorderVisualizer {

    private final ParticleAdapter particleAdapter;
    private final VisualScheduler visualScheduler;

    public RegionBorderVisualizer(ParticleAdapter particleAdapter, VisualScheduler visualScheduler) {
        this.particleAdapter = particleAdapter;
        this.visualScheduler = visualScheduler;
    }

    public boolean isSupported() {
        return particleAdapter.supportsParticles();
    }

    public void showRegion(PlayerRef viewer, Region region) {
        if (!isSupported() || viewer == null || region == null || region.getMin() == null || region.getMax() == null) {
            return;
        }
        particleAdapter.drawCuboid(viewer, region.getMin(), region.getMax(), ParticleAdapter.ParticleStyle.REGION_BORDER);
    }

    public VisualScheduler getVisualScheduler() {
        return visualScheduler;
    }
}