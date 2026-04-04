package dev.thenexusgates.hyguard.event;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent;
import com.hypixel.hytale.server.core.event.events.ecs.InteractivelyPickupItemEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.thenexusgates.hyguard.HyGuardPlugin;
import dev.thenexusgates.hyguard.core.protection.ProtectionAction;
import dev.thenexusgates.hyguard.util.BlockPos;

import java.util.Collections;
import java.util.Set;

public final class HyGuardItemSystem {

    private static final Query<EntityStore> PLAYER_QUERY = Query.and(
            Player.getComponentType(),
            TransformComponent.getComponentType()
    );

    private HyGuardItemSystem() {
    }

    public static final class DropSystem extends EntityEventSystem<EntityStore, DropItemEvent.PlayerRequest> {

        private final HyGuardPlugin plugin;

        public DropSystem(HyGuardPlugin plugin) {
            super(DropItemEvent.PlayerRequest.class);
            this.plugin = plugin;
        }

        @Override
        public void handle(int index,
                           ArchetypeChunk<EntityStore> archetypeChunk,
                           Store<EntityStore> store,
                           CommandBuffer<EntityStore> commandBuffer,
                           DropItemEvent.PlayerRequest event) {
            if (event == null || event.isCancelled()) {
                return;
            }
            handleAction(index, archetypeChunk, store, event, ProtectionAction.PLAYER_ITEM_DROP);
        }

        @Override
        public Query<EntityStore> getQuery() {
            return PLAYER_QUERY;
        }

        @Override
        public Set<Dependency<EntityStore>> getDependencies() {
            return Collections.singleton(RootDependency.last());
        }

        private void handleAction(int index,
                                  ArchetypeChunk<EntityStore> archetypeChunk,
                                  Store<EntityStore> store,
                                  DropItemEvent.PlayerRequest event,
                                  ProtectionAction action) {
            PlayerRef playerRef = store.getComponent(archetypeChunk.getReferenceTo(index), PlayerRef.getComponentType());
            TransformComponent transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
            EntityStore entityStore = store.getExternalData();
            World world = entityStore != null ? entityStore.getWorld() : null;
            if (playerRef == null || transform == null || transform.getPosition() == null || world == null) {
                return;
            }

            BlockPos position = new BlockPos(
                    (int) Math.floor(transform.getPosition().getX()),
                    (int) Math.floor(transform.getPosition().getY()),
                    (int) Math.floor(transform.getPosition().getZ())
            );
            if (!plugin.evaluate(playerRef, world.getName(), position, action).allowed()) {
                event.setCancelled(true);
                plugin.send(playerRef, plugin.getConfigSnapshot().messages.protectionDenied);
            }
        }
    }

    public static final class PickupSystem extends EntityEventSystem<EntityStore, InteractivelyPickupItemEvent> {

        private final HyGuardPlugin plugin;

        public PickupSystem(HyGuardPlugin plugin) {
            super(InteractivelyPickupItemEvent.class);
            this.plugin = plugin;
        }

        @Override
        public void handle(int index,
                           ArchetypeChunk<EntityStore> archetypeChunk,
                           Store<EntityStore> store,
                           CommandBuffer<EntityStore> commandBuffer,
                           InteractivelyPickupItemEvent event) {
            if (event == null || event.isCancelled()) {
                return;
            }

            PlayerRef playerRef = store.getComponent(archetypeChunk.getReferenceTo(index), PlayerRef.getComponentType());
            TransformComponent transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
            EntityStore entityStore = store.getExternalData();
            World world = entityStore != null ? entityStore.getWorld() : null;
            if (playerRef == null || transform == null || transform.getPosition() == null || world == null) {
                return;
            }

            BlockPos position = new BlockPos(
                    (int) Math.floor(transform.getPosition().getX()),
                    (int) Math.floor(transform.getPosition().getY()),
                    (int) Math.floor(transform.getPosition().getZ())
            );
            if (!plugin.evaluate(playerRef, world.getName(), position, ProtectionAction.PLAYER_ITEM_PICKUP).allowed()) {
                event.setCancelled(true);
                plugin.send(playerRef, plugin.getConfigSnapshot().messages.protectionDenied);
            }
        }

        @Override
        public Query<EntityStore> getQuery() {
            return PLAYER_QUERY;
        }

        @Override
        public Set<Dependency<EntityStore>> getDependencies() {
            return Collections.singleton(RootDependency.last());
        }
    }
}