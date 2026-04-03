package dev.thenexusgates.hyguard.core.selection;

import dev.thenexusgates.hyguard.util.BlockPos;

import java.util.concurrent.ConcurrentHashMap;

public final class SelectionService {

    private final ConcurrentHashMap<String, SelectionSession> sessions = new ConcurrentHashMap<>();

    public SelectionSession getOrCreate(String playerUuid) {
        return sessions.computeIfAbsent(playerUuid, SelectionSession::new);
    }

    public void setFirstPoint(String playerUuid, String worldId, BlockPos position) {
        getOrCreate(playerUuid).setFirstPoint(worldId, new SelectionPoint(position));
    }

    public void setSecondPoint(String playerUuid, String worldId, BlockPos position) {
        getOrCreate(playerUuid).setSecondPoint(worldId, new SelectionPoint(position));
    }

    public boolean setNextPoint(String playerUuid, String worldId, BlockPos position) {
        SelectionSession session = getOrCreate(playerUuid);
        boolean firstPoint = session.isNextPointFirst();
        if (firstPoint) {
            session.setFirstPoint(worldId, new SelectionPoint(position));
        } else {
            session.setSecondPoint(worldId, new SelectionPoint(position));
        }
        return firstPoint;
    }

    public SelectionSession get(String playerUuid) {
        return sessions.get(playerUuid);
    }

    public void clear(String playerUuid) {
        sessions.remove(playerUuid);
    }
}