package dev.thenexusgates.hyguard.core.protection;

import dev.thenexusgates.hyguard.util.BlockPos;

public record ProtectionQuery(String playerUuid, String worldId, BlockPos position, ProtectionAction action) {
}