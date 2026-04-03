package dev.thenexusgates.hyguard.visual;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.thenexusgates.hyguard.util.BlockPos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ParticleUtilParticleAdapter implements ParticleAdapter {

    private static final String VALID_PARTICLE_SYSTEM_ID = "Block_Hit_Stone";
    private static final String CONFLICT_PARTICLE_SYSTEM_ID = "Block_Hit_Fail";
    private static final String REGION_PARTICLE_SYSTEM_ID = "Block_Hit_Metal";
    private static final int MAX_POINTS = 192;
    private static final double MIN_SAMPLE_SPACING = 0.5D;
    private static final double TARGET_SEGMENTS_PER_EDGE = 16.0D;

    private volatile boolean supported = true;

    @Override
    public boolean supportsParticles() {
        return supported;
    }

    @Override
    public void drawCuboid(PlayerRef viewer, BlockPos min, BlockPos max, ParticleStyle style) {
        if (!supported || viewer == null || min == null || max == null) {
            return;
        }

        Ref<EntityStore> viewerRef = viewer.getReference();
        if (viewerRef == null || !viewerRef.isValid()) {
            return;
        }

        Store<EntityStore> store = viewerRef.getStore();
        if (store == null) {
            return;
        }

        try {
            List<Ref<EntityStore>> viewers = List.of(viewerRef);
            String particleSystemId = resolveParticleSystemId(style);
            for (Vector3d point : collectOutlinePoints(min, max)) {
                ParticleUtil.spawnParticleEffect(
                        particleSystemId,
                        point,
                        viewers,
                        store
                );
            }
        } catch (Throwable throwable) {
            supported = false;
        }
    }

    private String resolveParticleSystemId(ParticleStyle style) {
        if (style == null) {
            return VALID_PARTICLE_SYSTEM_ID;
        }
        return switch (style) {
            case SELECTION_CONFLICT -> CONFLICT_PARTICLE_SYSTEM_ID;
            case REGION_BORDER -> REGION_PARTICLE_SYSTEM_ID;
            case SELECTION_VALID -> VALID_PARTICLE_SYSTEM_ID;
        };
    }

    private List<Vector3d> collectOutlinePoints(BlockPos min, BlockPos max) {
        double minX = min.getX();
        double minY = min.getY();
        double minZ = min.getZ();
        double maxX = max.getX() + 1.0D;
        double maxY = max.getY() + 1.0D;
        double maxZ = max.getZ() + 1.0D;
        double longestEdge = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
        double spacing = Math.max(MIN_SAMPLE_SPACING, longestEdge / TARGET_SEGMENTS_PER_EDGE);

        Map<String, Vector3d> points = new LinkedHashMap<>();

        addEdge(points, minX, minY, minZ, maxX, minY, minZ, spacing);
        addEdge(points, minX, minY, maxZ, maxX, minY, maxZ, spacing);
        addEdge(points, minX, maxY, minZ, maxX, maxY, minZ, spacing);
        addEdge(points, minX, maxY, maxZ, maxX, maxY, maxZ, spacing);

        addEdge(points, minX, minY, minZ, minX, maxY, minZ, spacing);
        addEdge(points, minX, minY, maxZ, minX, maxY, maxZ, spacing);
        addEdge(points, maxX, minY, minZ, maxX, maxY, minZ, spacing);
        addEdge(points, maxX, minY, maxZ, maxX, maxY, maxZ, spacing);

        addEdge(points, minX, minY, minZ, minX, minY, maxZ, spacing);
        addEdge(points, minX, maxY, minZ, minX, maxY, maxZ, spacing);
        addEdge(points, maxX, minY, minZ, maxX, minY, maxZ, spacing);
        addEdge(points, maxX, maxY, minZ, maxX, maxY, maxZ, spacing);

        ArrayList<Vector3d> sampled = new ArrayList<>(points.values());
        if (sampled.size() <= MAX_POINTS) {
            return sampled;
        }

        ArrayList<Vector3d> downsampled = new ArrayList<>(MAX_POINTS);
        double stride = (sampled.size() - 1D) / (MAX_POINTS - 1D);
        for (int index = 0; index < MAX_POINTS; index++) {
            downsampled.add(sampled.get((int) Math.round(index * stride)));
        }
        return downsampled;
    }

    private void addEdge(Map<String, Vector3d> points,
                         double startX,
                         double startY,
                         double startZ,
                         double endX,
                         double endY,
                         double endZ,
                         double spacing) {
        double deltaX = endX - startX;
        double deltaY = endY - startY;
        double deltaZ = endZ - startZ;
        double length = Math.max(Math.abs(deltaX), Math.max(Math.abs(deltaY), Math.abs(deltaZ)));
        int segments = Math.max(1, (int) Math.ceil(length / spacing));
        for (int index = 0; index <= segments; index++) {
            double t = (double) index / segments;
            put(points, new Vector3d(
                    startX + (deltaX * t),
                    startY + (deltaY * t),
                    startZ + (deltaZ * t)
            ));
        }
    }

    private void put(Map<String, Vector3d> points, Vector3d point) {
        points.putIfAbsent(key(point.getX(), point.getY(), point.getZ()), point);
    }

    private String key(double x, double y, double z) {
        return scaled(x) + ":" + scaled(y) + ":" + scaled(z);
    }

    private long scaled(double value) {
        return Math.round(value * 1000.0D);
    }
}