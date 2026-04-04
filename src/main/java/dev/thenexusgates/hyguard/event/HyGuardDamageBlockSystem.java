package dev.thenexusgates.hyguard.event;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.thenexusgates.hyguard.HyGuardPlugin;
import dev.thenexusgates.hyguard.core.protection.ProtectionAction;
import dev.thenexusgates.hyguard.util.BlockPos;

import java.util.Collections;
import java.util.Set;

public final class HyGuardDamageBlockSystem extends EntityEventSystem<EntityStore, DamageBlockEvent> {

    private final HyGuardPlugin plugin;

    public HyGuardDamageBlockSystem(HyGuardPlugin plugin) {
        super(DamageBlockEvent.class);
        this.plugin = plugin;
    }

    @Override
    public void handle(int index,
                       ArchetypeChunk<EntityStore> archetypeChunk,
                       Store<EntityStore> store,
                       CommandBuffer<EntityStore> commandBuffer,
                       DamageBlockEvent event) {
        if (event == null || event.isCancelled() || event.getTargetBlock() == null) {
            return;
        }

        Player player = store.getComponent(archetypeChunk.getReferenceTo(index), Player.getComponentType());
        PlayerRef playerRef = store.getComponent(archetypeChunk.getReferenceTo(index), PlayerRef.getComponentType());
        EntityStore entityStore = store.getExternalData();
        World world = entityStore != null ? entityStore.getWorld() : null;
        if (player == null || playerRef == null || world == null) {
            return;
        }

        BlockPos position = BlockPos.fromVector(event.getTargetBlock());
        if (plugin.isWand(player)) {
            event.setCancelled(true);
            return;
        }

        if (!plugin.evaluate(playerRef, world.getName(), position, ProtectionAction.BLOCK_BREAK).allowed()) {
            event.setCancelled(true);
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.protectionDenied);
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Collections.singleton(RootDependency.first());
    }
}