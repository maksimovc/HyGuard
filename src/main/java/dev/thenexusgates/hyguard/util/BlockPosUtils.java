package dev.thenexusgates.hyguard.util;

public final class BlockPosUtils {

    private BlockPosUtils() {
    }

    public static BlockPos min(BlockPos first, BlockPos second) {
        return new BlockPos(
                Math.min(first.getX(), second.getX()),
                Math.min(first.getY(), second.getY()),
                Math.min(first.getZ(), second.getZ())
        );
    }

    public static BlockPos max(BlockPos first, BlockPos second) {
        return new BlockPos(
                Math.max(first.getX(), second.getX()),
                Math.max(first.getY(), second.getY()),
                Math.max(first.getZ(), second.getZ())
        );
    }

    public static boolean contains(BlockPos min, BlockPos max, BlockPos point) {
        return point.getX() >= min.getX() && point.getX() <= max.getX()
                && point.getY() >= min.getY() && point.getY() <= max.getY()
                && point.getZ() >= min.getZ() && point.getZ() <= max.getZ();
    }
}