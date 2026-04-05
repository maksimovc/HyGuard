package dev.thenexusgates.hyguard.event;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent;
import com.hypixel.hytale.server.core.event.events.ecs.InteractivelyPickupItemEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PickupItemComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.thenexusgates.hyguard.HyGuardPlugin;
import dev.thenexusgates.hyguard.core.protection.ProtectionAction;
import dev.thenexusgates.hyguard.util.BlockPos;

import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

public final class HyGuardItemSystem {

    private static final PreventPickupAccess PREVENT_PICKUP_ACCESS = PreventPickupAccess.load();

    private static final Query<EntityStore> PLAYER_QUERY = Query.and(
            Player.getComponentType(),
            TransformComponent.getComponentType()
    );

    private static final Query<EntityStore> PICKUP_ITEM_QUERY = Query.and(
            PickupItemComponent.getComponentType(),
            TransformComponent.getComponentType()
    );

    private HyGuardItemSystem() {
    }

    private record PickupContext(Ref<EntityStore> itemEntityRef,
                                 PlayerRef playerRef,
                                 BlockPos position,
                                 World world) {
    }

    private record PreventPickupAccess(ComponentType<EntityStore, ?> componentType,
                                       Constructor<?> constructor) {

        static PreventPickupAccess load() {
            try {
                Class<?> preventPickupClass = Class.forName("com.hypixel.hytale.server.core.modules.entity.item.PreventPickup");
                Method getComponentType = preventPickupClass.getMethod("getComponentType");
                @SuppressWarnings("unchecked")
                ComponentType<EntityStore, ?> componentType = (ComponentType<EntityStore, ?>) getComponentType.invoke(null);
                Constructor<?> constructor = preventPickupClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                return new PreventPickupAccess(componentType, constructor);
            } catch (Throwable throwable) {
                return null;
            }
        }

        Object newInstance() {
            try {
                return constructor.newInstance();
            } catch (Throwable throwable) {
                return null;
            }
        }
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

            PickupContext context = resolvePickupContext(index, archetypeChunk, store);
            if (context == null) {
                return;
            }

            boolean allowed = plugin.evaluate(
                    context.playerRef(),
                    context.world().getName(),
                    context.position(),
                    ProtectionAction.PLAYER_ITEM_PICKUP
            ).allowed();
            syncPreventPickupMarker(context.itemEntityRef(), commandBuffer, !allowed);
            if (!allowed) {
                event.setCancelled(true);
                plugin.send(context.playerRef(), plugin.getConfigSnapshot().messages.protectionDenied);
            }
        }

        @Override
        public Query<EntityStore> getQuery() {
            return PICKUP_ITEM_QUERY;
        }

        @Override
        public Set<Dependency<EntityStore>> getDependencies() {
            return Collections.singleton(RootDependency.first());
        }
    }

    public static final class PickupGuardSystem extends EntityTickingSystem<EntityStore> {

        private final HyGuardPlugin plugin;

        public PickupGuardSystem(HyGuardPlugin plugin) {
            this.plugin = plugin;
        }

        @Nonnull
        @Override
        public Query<EntityStore> getQuery() {
            return PICKUP_ITEM_QUERY;
        }

        @Override
        public void tick(float dt,
                         int index,
                         @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                         @Nonnull Store<EntityStore> store,
                         @Nonnull CommandBuffer<EntityStore> commandBuffer) {
            Ref<EntityStore> itemEntityRef = archetypeChunk.getReferenceTo(index);
            PickupContext context = resolvePickupContext(index, archetypeChunk, store);
            if (context == null) {
                syncPreventPickupMarker(itemEntityRef, commandBuffer, false);
                return;
            }

            boolean allowed = plugin.evaluate(
                    context.playerRef(),
                    context.world().getName(),
                    context.position(),
                    ProtectionAction.PLAYER_ITEM_PICKUP
            ).allowed();
            syncPreventPickupMarker(context.itemEntityRef(), commandBuffer, !allowed);
        }

        @Override
        public Set<Dependency<EntityStore>> getDependencies() {
            return Collections.singleton(RootDependency.first());
        }
    }

    private static PickupContext resolvePickupContext(int index,
                                                      ArchetypeChunk<EntityStore> archetypeChunk,
                                                      Store<EntityStore> store) {
        Ref<EntityStore> itemEntityRef = archetypeChunk.getReferenceTo(index);
        PickupItemComponent pickupItem = archetypeChunk.getComponent(index, PickupItemComponent.getComponentType());
        Ref<EntityStore> playerEntityRef = pickupItem == null ? null : pickupItem.getTargetRef();
        if (itemEntityRef == null || !itemEntityRef.isValid() || playerEntityRef == null || !playerEntityRef.isValid()) {
            return null;
        }

        Store<EntityStore> playerStore;
        try {
            playerStore = playerEntityRef.getStore();
        } catch (Throwable throwable) {
            return null;
        }
        if (playerStore == null) {
            return null;
        }

        PlayerRef playerRef = playerStore.getComponent(playerEntityRef, PlayerRef.getComponentType());
        TransformComponent transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
        EntityStore entityStore = store.getExternalData();
        World world = entityStore != null ? entityStore.getWorld() : null;
        if (playerRef == null || transform == null || transform.getPosition() == null || world == null) {
            return null;
        }

        BlockPos position = new BlockPos(
                (int) Math.floor(transform.getPosition().getX()),
                (int) Math.floor(transform.getPosition().getY()),
                (int) Math.floor(transform.getPosition().getZ())
        );
        return new PickupContext(itemEntityRef, playerRef, position, world);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void syncPreventPickupMarker(Ref<EntityStore> itemEntityRef,
                                                CommandBuffer<EntityStore> commandBuffer,
                                                boolean preventPickup) {
        if (PREVENT_PICKUP_ACCESS == null || itemEntityRef == null || !itemEntityRef.isValid() || commandBuffer == null) {
            return;
        }

        ComponentType componentType = PREVENT_PICKUP_ACCESS.componentType();
        if (componentType == null) {
            return;
        }

        if (!preventPickup) {
            commandBuffer.tryRemoveComponent(itemEntityRef, componentType);
            return;
        }

        Object marker = PREVENT_PICKUP_ACCESS.newInstance();
        if (marker != null) {
            invokePutComponent(commandBuffer, itemEntityRef, componentType, marker);
        }
    }

    private static void invokePutComponent(CommandBuffer<EntityStore> commandBuffer,
                                           Ref<EntityStore> itemEntityRef,
                                           ComponentType<EntityStore, ?> componentType,
                                           Object marker) {
        for (Method method : CommandBuffer.class.getMethods()) {
            if (!"putComponent".equals(method.getName()) || method.getParameterCount() != 3) {
                continue;
            }
            try {
                method.invoke(commandBuffer, itemEntityRef, componentType, marker);
            } catch (Throwable throwable) {
            }
            return;
        }
    }
}