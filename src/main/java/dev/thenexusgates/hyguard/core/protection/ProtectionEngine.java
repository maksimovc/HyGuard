package dev.thenexusgates.hyguard.core.protection;

import dev.thenexusgates.hyguard.config.HyGuardConfig;
import dev.thenexusgates.hyguard.core.region.Region;
import dev.thenexusgates.hyguard.core.region.RegionFlagValue;
import dev.thenexusgates.hyguard.core.region.RegionRole;
import dev.thenexusgates.hyguard.storage.RegionCache;

import java.util.List;

public final class ProtectionEngine {

    private final RegionCache regionCache;
    private final BypassHandler bypassHandler;
    private final HyGuardConfig config;

    public ProtectionEngine(RegionCache regionCache, BypassHandler bypassHandler, HyGuardConfig config) {
        this.regionCache = regionCache;
        this.bypassHandler = bypassHandler;
        this.config = config;
    }

    public ProtectionResult evaluate(ProtectionQuery query) {
        if (query.playerUuid() == null) {
            return ProtectionResult.allow();
        }
        if (bypassHandler.isBypassing(query.playerUuid())) {
            return ProtectionResult.allow();
        }

        List<Region> regions = regionCache.getRegionsAt(query.worldId(), query.position());
        if (regions.isEmpty()) {
            return ProtectionResult.allow();
        }

        for (Region region : regions) {
            RegionRole role = region.getRoleFor(query.playerUuid());
            if (role == RegionRole.VISITOR) {
                return ProtectionResult.deny(region);
            }
            if (allowsByMembership(query.action(), role)) {
                return ProtectionResult.allow();
            }
            if (role == RegionRole.TRUSTED && query.action() == ProtectionAction.BLOCK_INTERACT) {
                return ProtectionResult.allow();
            }

            RegionFlagValue flagValue = region.getFlags().get(query.action().getFlag());
            if (flagValue == null || flagValue.getMode() == RegionFlagValue.Mode.INHERIT) {
                continue;
            }

            RegionFlagValue.Mode mode = flagValue.getMode();
            switch (mode) {
                case ALLOW -> {
                    return ProtectionResult.allow();
                }
                case DENY -> {
                    return ProtectionResult.deny(region);
                }
                case ALLOW_MEMBERS -> {
                    if (role != null && role.isAtLeast(RegionRole.MEMBER)) {
                        return ProtectionResult.allow();
                    }
                    return ProtectionResult.deny(region);
                }
                case ALLOW_TRUSTED -> {
                    if (role != null && role.isAtLeast(RegionRole.TRUSTED)) {
                        return ProtectionResult.allow();
                    }
                    return ProtectionResult.deny(region);
                }
                case INHERIT -> {
                }
            }
        }
        return ProtectionResult.allow();
    }

    private boolean allowsByMembership(ProtectionAction action, RegionRole role) {
        if (role == null) {
            return false;
        }
        if (action == ProtectionAction.ENTRY || action == ProtectionAction.EXIT) {
            return false;
        }
        return role == RegionRole.OWNER
                || role == RegionRole.CO_OWNER
                || role == RegionRole.MANAGER
                || role == RegionRole.MEMBER;
    }

    private RegionFlagValue.Mode defaultMode(ProtectionAction action) {
        return switch (action) {
            case BLOCK_BREAK -> RegionFlagValue.Mode.valueOf(config.defaults.blockBreak);
            case BLOCK_PLACE -> RegionFlagValue.Mode.valueOf(config.defaults.blockPlace);
            case BLOCK_INTERACT -> RegionFlagValue.Mode.valueOf(config.defaults.blockInteract);
            case PVP, PLAYER_DAMAGE, PLAYER_FALL_DAMAGE, PLAYER_ITEM_DROP, PLAYER_ITEM_PICKUP, MOB_DAMAGE_PLAYERS, ENTITY_DAMAGE -> RegionFlagValue.Mode.DENY;
            case ENTRY, EXIT -> RegionFlagValue.Mode.ALLOW;
        };
    }
}