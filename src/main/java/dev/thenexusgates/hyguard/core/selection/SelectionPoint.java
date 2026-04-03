package dev.thenexusgates.hyguard.core.selection;

import dev.thenexusgates.hyguard.util.BlockPos;

public final class SelectionPoint {

    private final BlockPos position;

    public SelectionPoint(BlockPos position) {
        this.position = position;
    }

    public BlockPos getPosition() {
        return position;
    }
}