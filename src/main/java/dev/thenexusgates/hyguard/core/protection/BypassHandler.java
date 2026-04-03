package dev.thenexusgates.hyguard.core.protection;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class BypassHandler {

    private final Set<String> bypassPlayers = ConcurrentHashMap.newKeySet();

    public boolean isBypassing(String playerUuid) {
        return bypassPlayers.contains(playerUuid);
    }

    public boolean toggle(String playerUuid) {
        if (bypassPlayers.contains(playerUuid)) {
            bypassPlayers.remove(playerUuid);
            return false;
        }
        bypassPlayers.add(playerUuid);
        return true;
    }

    public void clear(String playerUuid) {
        bypassPlayers.remove(playerUuid);
    }
}