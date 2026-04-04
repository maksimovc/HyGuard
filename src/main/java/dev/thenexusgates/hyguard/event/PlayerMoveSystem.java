package dev.thenexusgates.hyguard.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.thenexusgates.hyguard.HyGuardPlugin;
import dev.thenexusgates.hyguard.core.protection.ProtectionAction;
import dev.thenexusgates.hyguard.core.protection.ProtectionResult;
import dev.thenexusgates.hyguard.core.region.Region;
import dev.thenexusgates.hyguard.core.region.RegionFlag;
import dev.thenexusgates.hyguard.core.region.RegionFlagValue;
import dev.thenexusgates.hyguard.util.BlockPos;
import dev.thenexusgates.hyguard.visual.EnterExitMessageRenderer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class PlayerMoveSystem {

    private static final long POLL_INTERVAL_MS = 250L;
    private static final long PLAYER_STATE_STALE_AFTER_MS = 5000L;

    private final HyGuardPlugin plugin;
    private final ScheduledExecutorService scheduler;
    private final EnterExitMessageRenderer messageRenderer;
    private final ConcurrentHashMap<String, PlayerState> states = new ConcurrentHashMap<>();

    private volatile ScheduledFuture<?> task;

    public PlayerMoveSystem(HyGuardPlugin plugin,
                            ScheduledExecutorService scheduler,
                            EnterExitMessageRenderer messageRenderer) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.messageRenderer = messageRenderer;
    }

    public synchronized void start() {
        if (scheduler == null || (task != null && !task.isCancelled())) {
            return;
        }
        task = scheduler.scheduleAtFixedRate(this::tickSafely, POLL_INTERVAL_MS, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        if (task != null) {
            task.cancel(false);
            task = null;
        }
        states.clear();
    }

    public void clearPlayer(String playerUuid) {
        if (playerUuid != null) {
            states.remove(playerUuid);
        }
    }

    private void tickSafely() {
        try {
            tick();
        } catch (Throwable throwable) {
            plugin.getLogger().at(Level.WARNING).withCause(throwable).log("[HyGuard] PlayerMoveSystem tick failed.");
        }
    }

    private void tick() {
        Universe universe = Universe.get();
        if (universe == null) {
            return;
        }

        long now = System.currentTimeMillis();
        for (PlayerRef playerRef : universe.getPlayers()) {
            if (playerRef == null || playerRef.getUuid() == null) {
                continue;
            }
            String playerUuid = playerRef.getUuid().toString();
            touchPlayer(playerUuid, now);
            if (playerRef.getWorldUuid() == null || playerRef.getTransform() == null || playerRef.getTransform().getPosition() == null) {
                continue;
            }
            processPlayer(universe, playerRef, now);
        }
        states.entrySet().removeIf(entry -> isStale(entry.getValue(), now));
    }

    private void touchPlayer(String playerUuid, long now) {
        states.computeIfPresent(playerUuid, (ignored, state) -> state.withLastSeenAt(now));
    }

    private boolean isStale(PlayerState state, long now) {
        return state == null || now - state.lastSeenAt() > PLAYER_STATE_STALE_AFTER_MS;
    }

    private void processPlayer(Universe universe, PlayerRef playerRef, long now) {
        World world = universe.getWorld(playerRef.getWorldUuid());
        if (world == null) {
            return;
        }

        plugin.rememberPlayer(playerRef);

        String worldId = world.getName();
        String playerUuid = playerRef.getUuid().toString();
        BlockPos currentPosition = plugin.resolvePlayerRegionPosition(playerRef);
        List<BlockPos> currentPositions = plugin.resolvePlayerRegionPositions(playerRef);
        if (currentPosition == null || currentPositions.isEmpty()) {
            return;
        }

        List<Region> currentRegions = plugin.getRegionsAt(worldId, currentPositions);
        List<String> currentRegionIds = currentRegions.stream().map(Region::getId).toList();

        PlayerState previousState = states.get(playerUuid);
        if (previousState == null) {
            applyRegionState(playerRef, worldId, currentPosition);
            currentRegions.forEach(region -> messageRenderer.sendGreeting(playerRef, region));
            states.put(playerUuid, PlayerState.create(worldId, currentPosition, currentRegionIds, now));
            return;
        }

        if (!previousState.worldId().equalsIgnoreCase(worldId)) {
            resolveRegions(previousState.announcedRegionIds()).forEach(region -> messageRenderer.sendFarewell(playerRef, region));
            applyRegionState(playerRef, worldId, currentPosition);
            currentRegions.forEach(region -> messageRenderer.sendGreeting(playerRef, region));
            states.put(playerUuid, PlayerState.create(worldId, currentPosition, currentRegionIds, now));
            return;
        }

        boolean positionChanged = !previousState.position().equals(currentPosition);
        boolean regionIdsChanged = !previousState.regionIds().equals(currentRegionIds);

        if (!regionIdsChanged) {
            if (positionChanged) {
                states.put(playerUuid, previousState.withObservation(currentPosition, currentRegionIds, now));
            }
            return;
        }

        List<Region> previousRegions = resolveRegions(previousState.regionIds());
        List<Region> exitedRegions = resolveExitedRegions(previousRegions, currentRegionIds);
        if (positionChanged && !exitedRegions.isEmpty()) {
            ProtectionResult exitResult = plugin.evaluate(playerRef, previousState.worldId(), previousState.position(), ProtectionAction.EXIT);
            if (!exitResult.allowed()) {
                if (teleportBack(playerRef, previousState.safePositionOrFallback())) {
                    messageRenderer.sendExitDenied(playerRef, exitResult.region());
                }
                return;
            }
        }

        List<Region> enteredRegions = resolveEnteredRegions(currentRegions, previousState.regionIds());
        if (positionChanged && !enteredRegions.isEmpty()) {
            Region restrictedEntryRegion = resolveRestrictedEntryRegion(playerRef, enteredRegions);
            if (restrictedEntryRegion != null) {
                if (teleportBack(playerRef, previousState.safePositionOrFallback())) {
                    messageRenderer.sendEntryDenied(playerRef, restrictedEntryRegion);
                }
                return;
            }
            ProtectionResult entryResult = plugin.evaluate(playerRef, worldId, currentPosition, ProtectionAction.ENTRY);
            if (!entryResult.allowed()) {
                if (teleportBack(playerRef, previousState.safePositionOrFallback())) {
                    messageRenderer.sendEntryDenied(playerRef, entryResult.region());
                }
                return;
            }
        }

        applyRegionState(playerRef, worldId, currentPosition);

        PlayerState nextState = previousState.withObservation(currentPosition, currentRegionIds, now);
        nextState = syncAnnouncements(playerRef, worldId, currentRegions, currentRegionIds, nextState);
        states.put(playerUuid, nextState);
    }

    private PlayerState syncAnnouncements(PlayerRef playerRef,
                                          String worldId,
                                          List<Region> currentRegions,
                                          List<String> currentRegionIds,
                                          PlayerState state) {
        if (state == null) {
            return PlayerState.create(worldId, null, currentRegionIds, System.currentTimeMillis());
        }

        if (!state.worldId().equalsIgnoreCase(worldId)) {
            resolveRegions(state.announcedRegionIds()).forEach(region -> messageRenderer.sendFarewell(playerRef, region));
            currentRegions.forEach(region -> messageRenderer.sendGreeting(playerRef, region));
            return state.withAnnouncements(currentRegionIds);
        }

        if (state.announcedRegionIds().equals(currentRegionIds)) {
            return state;
        }

        List<Region> announcedRegions = resolveRegions(state.announcedRegionIds());
        List<Region> exitedRegions = resolveExitedRegions(announcedRegions, currentRegionIds);
        List<Region> enteredRegions = resolveEnteredRegions(currentRegions, state.announcedRegionIds());
        exitedRegions.forEach(region -> messageRenderer.sendFarewell(playerRef, region));
        enteredRegions.forEach(region -> messageRenderer.sendGreeting(playerRef, region));
        return state.withAnnouncements(currentRegionIds);
    }

    private void applyRegionState(PlayerRef playerRef, String worldId, BlockPos currentPosition) {
        Ref<EntityStore> entityRef = playerRef.getReference();
        if (entityRef != null && entityRef.isValid()) {
            plugin.applyRegionState(entityRef.getStore(), entityRef, playerRef, worldId, currentPosition);
        }
    }

    private List<Region> resolveRegions(List<String> regionIds) {
        ArrayList<Region> regions = new ArrayList<>(regionIds.size());
        for (String regionId : regionIds) {
            Region region = plugin.findRegionById(regionId);
            if (region != null) {
                regions.add(region);
            }
        }
        return regions;
    }

    private Region resolveRestrictedEntryRegion(PlayerRef playerRef, List<Region> enteredRegions) {
        if (plugin.canBypassProtection(playerRef)) {
            return null;
        }
        for (Region region : enteredRegions) {
            if (!plugin.isFlagAllowed(playerRef, region, RegionFlag.ENTRY_PLAYERS, RegionFlagValue.Mode.ALLOW)) {
                return region;
            }
        }
        return null;
    }

    private boolean teleportBack(PlayerRef playerRef, BlockPos position) {
        Ref<EntityStore> entityRef = playerRef.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return false;
        }
        Store<EntityStore> store = entityRef.getStore();
        return plugin.teleportToPosition(store, entityRef, position);
    }

    private List<Region> resolveEnteredRegions(List<Region> currentRegions, List<String> previousRegionIds) {
        Set<String> previous = new HashSet<>(previousRegionIds);
        ArrayList<Region> entered = new ArrayList<>();
        for (Region region : currentRegions) {
            if (!previous.contains(region.getId())) {
                entered.add(region);
            }
        }
        return entered;
    }

    private List<Region> resolveExitedRegions(List<Region> previousRegions, List<String> currentRegionIds) {
        Set<String> current = new HashSet<>(currentRegionIds);
        ArrayList<Region> exited = new ArrayList<>();
        for (Region region : previousRegions) {
            if (!current.contains(region.getId())) {
                exited.add(region);
            }
        }
        return exited;
    }

    private record PlayerState(String worldId,
                               BlockPos position,
                               BlockPos safePosition,
                               List<String> regionIds,
                               List<String> announcedRegionIds,
                               long lastSeenAt) {

        private static PlayerState create(String worldId, BlockPos position, List<String> regionIds, long lastSeenAt) {
            BlockPos storedPosition = position == null ? null : position.copy();
            List<String> storedRegionIds = List.copyOf(regionIds);
            return new PlayerState(worldId, storedPosition, storedPosition == null ? null : storedPosition.copy(), storedRegionIds, storedRegionIds, lastSeenAt);
        }

        private PlayerState withObservation(BlockPos updatedPosition, List<String> updatedRegionIds, long updatedLastSeenAt) {
            BlockPos storedPosition = updatedPosition == null ? null : updatedPosition.copy();
            BlockPos nextSafePosition = storedPosition == null ? safePositionOrFallback() : storedPosition.copy();
            return new PlayerState(worldId, storedPosition, nextSafePosition, List.copyOf(updatedRegionIds), announcedRegionIds, updatedLastSeenAt);
        }

        private PlayerState withAnnouncements(List<String> updatedAnnouncedRegionIds) {
            return new PlayerState(worldId, position, safePosition, regionIds, List.copyOf(updatedAnnouncedRegionIds), lastSeenAt);
        }

        private PlayerState withLastSeenAt(long updatedLastSeenAt) {
            return new PlayerState(worldId, position, safePosition, regionIds, announcedRegionIds, updatedLastSeenAt);
        }

        private BlockPos safePositionOrFallback() {
            if (safePosition != null) {
                return safePosition.copy();
            }
            return position == null ? null : position.copy();
        }
    }
}