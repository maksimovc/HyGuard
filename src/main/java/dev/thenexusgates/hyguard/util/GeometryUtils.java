package dev.thenexusgates.hyguard.util;

import dev.thenexusgates.hyguard.core.region.Region;

public final class GeometryUtils {

    private GeometryUtils() {
    }

    public static boolean intersects(Region first, Region second) {
        if (first == null || second == null) {
            return false;
        }
        if (first.isGlobal() || second.isGlobal()) {
            return first.getWorldId() != null && first.getWorldId().equalsIgnoreCase(second.getWorldId());
        }
        if (!first.getWorldId().equalsIgnoreCase(second.getWorldId())) {
            return false;
        }
        return intersects(first.getMin(), first.getMax(), second.getMin(), second.getMax());
    }

    public static boolean intersects(BlockPos firstMin, BlockPos firstMax, BlockPos secondMin, BlockPos secondMax) {
        return firstMin.getX() <= secondMax.getX() && firstMax.getX() >= secondMin.getX()
                && firstMin.getY() <= secondMax.getY() && firstMax.getY() >= secondMin.getY()
                && firstMin.getZ() <= secondMax.getZ() && firstMax.getZ() >= secondMin.getZ();
    }

    public static boolean contains(Region container, Region child) {
        if (container == null || child == null) {
            return false;
        }
        if (container.getWorldId() == null || child.getWorldId() == null || !container.getWorldId().equalsIgnoreCase(child.getWorldId())) {
            return false;
        }
        if (container.isGlobal()) {
            return true;
        }
        if (child.isGlobal() || container.getMin() == null || container.getMax() == null || child.getMin() == null || child.getMax() == null) {
            return false;
        }
        return contains(container.getMin(), container.getMax(), child.getMin())
                && contains(container.getMin(), container.getMax(), child.getMax());
    }

    public static boolean contains(BlockPos min, BlockPos max, BlockPos point) {
        return min != null && max != null && point != null && BlockPosUtils.contains(min, max, point);
    }

    public static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
    }
}