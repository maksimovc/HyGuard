package dev.thenexusgates.hyguard.core.selection;

public final class SelectionSession {

    private final String playerUuid;
    private String worldId;
    private SelectionPoint firstPoint;
    private SelectionPoint secondPoint;
    private boolean nextPointIsFirst = true;
    private long updatedAt;

    public SelectionSession(String playerUuid) {
        this.playerUuid = playerUuid;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public String getWorldId() {
        return worldId;
    }

    public SelectionPoint getFirstPoint() {
        return firstPoint;
    }

    public SelectionPoint getSecondPoint() {
        return secondPoint;
    }

    public boolean isNextPointFirst() {
        return nextPointIsFirst;
    }

    public boolean hasAnyPoint() {
        return firstPoint != null || secondPoint != null;
    }

    public boolean isComplete() {
        return firstPoint != null && secondPoint != null && worldId != null;
    }

    public void setFirstPoint(String worldId, SelectionPoint firstPoint) {
        this.worldId = worldId;
        this.firstPoint = firstPoint;
        this.nextPointIsFirst = false;
        touch();
    }

    public void setSecondPoint(String worldId, SelectionPoint secondPoint) {
        this.worldId = worldId;
        this.secondPoint = secondPoint;
        this.nextPointIsFirst = true;
        touch();
    }

    public void setNextPointIsFirst(boolean nextPointIsFirst) {
        this.nextPointIsFirst = nextPointIsFirst;
        touch();
    }

    private void touch() {
        this.updatedAt = System.currentTimeMillis();
    }
}