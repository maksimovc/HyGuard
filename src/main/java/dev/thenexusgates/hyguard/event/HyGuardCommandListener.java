package dev.thenexusgates.hyguard.event;

import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.thenexusgates.hyguard.HyGuardPlugin;
import dev.thenexusgates.hyguard.util.BlockPos;

public final class HyGuardCommandListener {

    private final HyGuardPlugin plugin;

    public HyGuardCommandListener(HyGuardPlugin plugin) {
        this.plugin = plugin;
    }

    public void handle(PlayerChatEvent event) {
        if (event == null || event.isCancelled()) {
            return;
        }

        String content = event.getContent();
        if (content == null || content.isBlank() || !content.trim().startsWith("/")) {
            return;
        }

        PlayerRef playerRef = event.getSender();
        if (playerRef == null || plugin.canBypassProtection(playerRef)) {
            return;
        }

        String worldId = plugin.resolvePlayerWorldId(playerRef);
        BlockPos position = plugin.resolvePlayerRegionPosition(playerRef);
        if (position == null) {
            return;
        }

        worldId = plugin.resolveRegionWorldId(worldId, position);
        if (worldId == null) {
            return;
        }

        if (!plugin.isCommandBlacklisted(playerRef, worldId, position, content)) {
            return;
        }

        event.setCancelled(true);
        plugin.send(playerRef, plugin.getConfigSnapshot().messages.protectionDenied);
    }
}