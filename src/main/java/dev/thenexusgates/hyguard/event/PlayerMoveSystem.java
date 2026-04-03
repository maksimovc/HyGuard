package dev.thenexusgates.hyguard.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.thenexusgates.hyguard.HyGuardPlugin;
import dev.thenexusgates.hyguard.core.protection.ProtectionAction;
import dev.thenexusgates.hyguard.core.protection.ProtectionResult;
import dev.thenexusgates.hyguard.core.region.Region;
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

        Set<String> onlinePlayers = new HashSet<>();
        for (PlayerRef playerRef : universe.getPlayers()) {
            if (playerRef == null || playerRef.getUuid() == null || playerRef.getWorldUuid() == null || playerRef.getTransform() == null || playerRef.getTransform().getPosition() == null) {
                continue;
            }
            onlinePlayers.add(playerRef.getUuid().toString());
            processPlayer(universe, playerRef);
        }
        states.keySet().removeIf(playerUuid -> !onlinePlayers.contains(playerUuid));
    }

    private void processPlayer(Universe universe, PlayerRef playerRef) {
        World world = universe.getWorld(playerRef.getWorldUuid());
        if (world == null) {
            return;
        }

        plugin.rememberPlayer(playerRef);

        String worldId = world.getName();
        BlockPos currentPosition = new BlockPos(
                (int) Math.floor(playerRef.getTransform().getPosition().getX()),
                (int) Math.floor(playerRef.getTransform().getPosition().getY()),
                (int) Math.floor(playerRef.getTransform().getPosition().getZ())
        );
        List<Region> currentRegions = plugin.getRegionsAt(worldId, currentPosition);
        List<String> currentRegionIds = currentRegions.stream().map(Region::getId).toList();

        String playerUuid = playerRef.getUuid().toString();
        PlayerState previousState = states.get(playerUuid);
        if (previousState == null || !previousState.worldId().equalsIgnoreCase(worldId)) {
            Ref<EntityStore> entityRef = playerRef.getReference();
            if (entityRef != null && entityRef.isValid()) {
                plugin.applyRegionState(entityRef.getStore(), entityRef, playerRef, worldId, currentPosition);
            }
            states.put(playerUuid, new PlayerState(worldId, currentPosition, currentRegionIds));
            return;
        }
        if (previousState.position().equals(currentPosition) && previousState.regionIds().equals(currentRegionIds)) {
            Ref<EntityStore> entityRef = playerRef.getReference();
            if (entityRef != null && entityRef.isValid()) {
                plugin.applyRegionState(entityRef.getStore(), entityRef, playerRef, worldId, currentPosition);
            }
            return;
        }

        List<Region> previousRegions = plugin.getRegionsAt(previousState.worldId(), previousState.position());
        List<Region> exitedRegions = resolveExitedRegions(previousRegions, currentRegionIds);
        if (!exitedRegions.isEmpty()) {
            ProtectionResult exitResult = plugin.evaluate(playerRef, previousState.worldId(), previousState.position(), ProtectionAction.EXIT);
            if (!exitResult.allowed()) {
                if (teleportBack(playerRef, previousState.position())) {
                    messageRenderer.sendExitDenied(playerRef, exitResult.region());
                }
                return;
            }
        }

        List<Region> enteredRegions = resolveEnteredRegions(currentRegions, previousState.regionIds());
        if (!enteredRegions.isEmpty()) {
            ProtectionResult entryResult = plugin.evaluate(playerRef, worldId, currentPosition, ProtectionAction.ENTRY);
            if (!entryResult.allowed()) {
                if (teleportBack(playerRef, previousState.position())) {
                    messageRenderer.sendEntryDenied(playerRef, entryResult.region());
                }
                return;
            }
        }

        exitedRegions.forEach(region -> messageRenderer.sendFarewell(playerRef, region));
        enteredRegions.forEach(region -> messageRenderer.sendGreeting(playerRef, region));
        Ref<EntityStore> entityRef = playerRef.getReference();
        if (entityRef != null && entityRef.isValid()) {
            plugin.applyRegionState(entityRef.getStore(), entityRef, playerRef, worldId, currentPosition);
        }
        states.put(playerUuid, new PlayerState(worldId, currentPosition, currentRegionIds));
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

    private record PlayerState(String worldId, BlockPos position, List<String> regionIds) {
    }
}