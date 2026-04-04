package dev.thenexusgates.hyguard.permission;

import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.thenexusgates.hyguard.config.HyGuardConfig;

public final class HyGuardPermissions {

    private final HyGuardConfig.General general;

    public HyGuardPermissions(HyGuardConfig.General general) {
        this.general = general;
    }

    public boolean hasUse(PlayerRef playerRef) {
        return has(playerRef, general.usePermission) || hasAdmin(playerRef);
    }

    public boolean hasAdmin(PlayerRef playerRef) {
        return has(playerRef, general.adminPermission);
    }

    public boolean hasBypass(PlayerRef playerRef) {
        return has(playerRef, general.bypassPermission);
    }

    public boolean has(PlayerRef playerRef, String permission) {
        PermissionsModule permissionsModule = PermissionsModule.get();
        return playerRef != null
                && playerRef.getUuid() != null
                && permissionsModule != null
                && permission != null
                && !permission.isBlank()
                && permissionsModule.hasPermission(playerRef.getUuid(), permission);
    }
}
