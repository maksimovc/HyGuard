package dev.thenexusgates.hyguard.event;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
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

import javax.annotation.Nonnull;

public final class PlayerMoveSystem extends EntityTickingSystem<EntityStore> {

    private static final Query<EntityStore> QUERY = Query.and(
            Player.getComponentType(),
            TransformComponent.getComponentType()
    );

    private final HyGuardPlugin plugin;
    private final EnterExitMessageRenderer messageRenderer;
    private final ConcurrentHashMap<String, PlayerState> states = new ConcurrentHashMap<>();

    public PlayerMoveSystem(HyGuardPlugin plugin,
                            EnterExitMessageRenderer messageRenderer) {
        this.plugin = plugin;
        this.messageRenderer = messageRenderer;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    public void stop() {
        states.clear();
    }

    public void clearPlayer(String playerUuid) {
        if (playerUuid != null) {
            states.remove(playerUuid);
        }
    }

    @Override
    public void tick(float dt,
                     int index,
                     @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> entityRef = archetypeChunk.getReferenceTo(index);
        PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        TransformComponent transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
        EntityStore entityStore = store.getExternalData();
        World world = entityStore == null ? null : entityStore.getWorld();
        if (playerRef == null || playerRef.getUuid() == null || transform == null || transform.getPosition() == null || world == null) {
            return;
        }

        long now = System.currentTimeMillis();
        processPlayer(store, commandBuffer, entityRef, world, playerRef, transform, now);
    }

    private void processPlayer(Store<EntityStore> store,
                               CommandBuffer<EntityStore> commandBuffer,
                               Ref<EntityStore> entityRef,
                               World world,
                               PlayerRef playerRef,
                               TransformComponent transform,
                               long now) {
        String worldId = world.getName();
        String playerUuid = playerRef.getUuid().toString();
        BlockPos currentPosition = resolvePlayerRegionPosition(transform);
        List<BlockPos> currentPositions = resolvePlayerRegionPositions(transform);
        if (currentPosition == null || currentPositions.isEmpty()) {
            return;
        }

        List<Region> currentRegions = plugin.getRegionsAt(worldId, currentPositions);
        List<String> currentRegionIds = currentRegions.stream().map(Region::getId).toList();

        PlayerState previousState = states.get(playerUuid);
        if (previousState == null) {
            plugin.rememberPlayer(playerRef);
            applyRegionState(store, entityRef, playerRef, worldId, currentPosition);
            messageRenderer.sendGreeting(playerRef, currentRegions);
            states.put(playerUuid, PlayerState.create(worldId, currentPosition, currentRegionIds, now));
            return;
        }

        if (!previousState.worldId().equalsIgnoreCase(worldId)) {
            messageRenderer.sendFarewell(playerRef, resolveRegions(previousState.announcedRegionIds()));
            applyRegionState(store, entityRef, playerRef, worldId, currentPosition);
            messageRenderer.sendGreeting(playerRef, currentRegions);
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
                if (teleportBack(commandBuffer, store, entityRef, previousState.safePositionOrFallback())) {
                    messageRenderer.sendExitDenied(playerRef, exitResult.region());
                }
                return;
            }
        }

        List<Region> enteredRegions = resolveEnteredRegions(currentRegions, previousState.regionIds());
        if (positionChanged && !enteredRegions.isEmpty()) {
            Region restrictedEntryRegion = resolveRestrictedEntryRegion(playerRef, enteredRegions);
            if (restrictedEntryRegion != null) {
                if (teleportBack(commandBuffer, store, entityRef, previousState.safePositionOrFallback())) {
                    messageRenderer.sendEntryDenied(playerRef, restrictedEntryRegion);
                }
                return;
            }
            ProtectionResult entryResult = plugin.evaluate(playerRef, worldId, currentPosition, ProtectionAction.ENTRY);
            if (!entryResult.allowed()) {
                if (teleportBack(commandBuffer, store, entityRef, previousState.safePositionOrFallback())) {
                    messageRenderer.sendEntryDenied(playerRef, entryResult.region());
                }
                return;
            }
        }

        applyRegionState(store, entityRef, playerRef, worldId, currentPosition);

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
            messageRenderer.sendFarewell(playerRef, resolveRegions(state.announcedRegionIds()));
            messageRenderer.sendGreeting(playerRef, currentRegions);
            return state.withAnnouncements(currentRegionIds);
        }

        if (state.announcedRegionIds().equals(currentRegionIds)) {
            return state;
        }

        List<Region> announcedRegions = resolveRegions(state.announcedRegionIds());
        List<Region> exitedRegions = resolveExitedRegions(announcedRegions, currentRegionIds);
        List<Region> enteredRegions = resolveEnteredRegions(currentRegions, state.announcedRegionIds());
        if (!exitedRegions.isEmpty()) {
            messageRenderer.sendFarewell(playerRef, exitedRegions);
        }
        if (!enteredRegions.isEmpty()) {
            messageRenderer.sendGreeting(playerRef, currentRegions);
        }
        return state.withAnnouncements(currentRegionIds);
    }

    private void applyRegionState(Store<EntityStore> store,
                                  Ref<EntityStore> entityRef,
                                  PlayerRef playerRef,
                                  String worldId,
                                  BlockPos currentPosition) {
        if (entityRef != null && entityRef.isValid()) {
            plugin.applyRegionState(store, entityRef, playerRef, worldId, currentPosition);
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
            if (plugin.isEntryBlacklisted(region, playerRef)) {
                return region;
            }
        }
        return null;
    }

    private boolean teleportBack(CommandBuffer<EntityStore> commandBuffer,
                                 Store<EntityStore> store,
                                 Ref<EntityStore> entityRef,
                                 BlockPos position) {
        if (entityRef == null || !entityRef.isValid()) {
            return false;
        }
        return plugin.teleportToPosition(commandBuffer, store, entityRef, position);
    }

    private BlockPos resolvePlayerRegionPosition(TransformComponent transform) {
        if (transform == null || transform.getPosition() == null) {
            return null;
        }
        return new BlockPos(
                (int) Math.floor(transform.getPosition().getX()),
                (int) Math.floor(transform.getPosition().getY() + 0.25D),
                (int) Math.floor(transform.getPosition().getZ())
        );
    }

    private List<BlockPos> resolvePlayerRegionPositions(TransformComponent transform) {
        if (transform == null || transform.getPosition() == null) {
            return List.of();
        }

        int blockX = (int) Math.floor(transform.getPosition().getX());
        int minBlockY = (int) Math.floor(transform.getPosition().getY());
        int maxBlockY = Math.max(minBlockY, (int) Math.floor(transform.getPosition().getY() + 1.8D));
        int blockZ = (int) Math.floor(transform.getPosition().getZ());

        ArrayList<BlockPos> positions = new ArrayList<>(maxBlockY - minBlockY + 1);
        for (int blockY = minBlockY; blockY <= maxBlockY; blockY++) {
            positions.add(new BlockPos(blockX, blockY, blockZ));
        }
        return List.copyOf(positions);
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

        private BlockPos safePositionOrFallback() {
            if (safePosition != null) {
                return safePosition.copy();
            }
            return position == null ? null : position.copy();
        }
    }
}