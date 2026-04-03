package dev.thenexusgates.hyguard.event;

import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import dev.thenexusgates.hyguard.HyGuardPlugin;

public final class DisconnectCleanupSystem {

    private final HyGuardPlugin plugin;

    public DisconnectCleanupSystem(HyGuardPlugin plugin) {
        this.plugin = plugin;
    }

    public void handle(PlayerDisconnectEvent event) {
        if (event == null || event.getPlayerRef() == null) {
            return;
        }
        plugin.clearTransientPlayerState(event.getPlayerRef());
    }
}