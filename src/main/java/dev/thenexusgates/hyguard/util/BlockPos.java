package dev.thenexusgates.hyguard.util;

import com.hypixel.hytale.math.vector.Vector3i;

import java.util.Objects;

public final class BlockPos {

    private int x;
    private int y;
    private int z;

    public BlockPos() {
    }

    public BlockPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static BlockPos fromVector(Vector3i vector) {
        return new BlockPos(vector.getX(), vector.getY(), vector.getZ());
    }

    public Vector3i toVector() {
        return new Vector3i(x, y, z);
    }

    public BlockPos copy() {
        return new BlockPos(x, y, z);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setZ(int z) {
        this.z = z;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BlockPos blockPos)) {
            return false;
        }
        return x == blockPos.x && y == blockPos.y && z == blockPos.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return x + "," + y + "," + z;
    }
}