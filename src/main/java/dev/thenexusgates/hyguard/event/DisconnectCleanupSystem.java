package dev.thenexusgates.hyguard.event;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.RemovedPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
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

    public void handle(RemovedPlayerFromWorldEvent event) {
        clearFromHolder(event == null ? null : event.getHolder());
    }

    public void handle(DrainPlayerFromWorldEvent event) {
        clearFromHolder(event == null ? null : event.getHolder());
    }

    private void clearFromHolder(Holder<EntityStore> holder) {
        if (holder == null) {
            return;
        }
        PlayerRef playerRef = holder.getComponent(PlayerRef.getComponentType());
        if (playerRef != null) {
            plugin.clearTransientPlayerState(playerRef);
        }
    }
}