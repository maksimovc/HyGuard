package dev.thenexusgates.hyguard.event;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.ChangeGameModeEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.thenexusgates.hyguard.HyGuardPlugin;
import dev.thenexusgates.hyguard.util.BlockPos;

import java.util.Collections;
import java.util.Set;

public final class HyGuardChangeGameModeSystem extends EntityEventSystem<EntityStore, ChangeGameModeEvent> {

    private final HyGuardPlugin plugin;

    public HyGuardChangeGameModeSystem(HyGuardPlugin plugin) {
        super(ChangeGameModeEvent.class);
        this.plugin = plugin;
    }

    @Override
    public void handle(int index,
                       ArchetypeChunk<EntityStore> archetypeChunk,
                       Store<EntityStore> store,
                       CommandBuffer<EntityStore> commandBuffer,
                       ChangeGameModeEvent event) {
        if (event == null || event.isCancelled()) {
            return;
        }

        Player player = store.getComponent(archetypeChunk.getReferenceTo(index), Player.getComponentType());
        PlayerRef playerRef = store.getComponent(archetypeChunk.getReferenceTo(index), PlayerRef.getComponentType());
        TransformComponent transform = store.getComponent(archetypeChunk.getReferenceTo(index), TransformComponent.getComponentType());
        EntityStore entityStore = store.getExternalData();
        World world = entityStore != null ? entityStore.getWorld() : null;
        if (player == null || playerRef == null || transform == null || transform.getPosition() == null || world == null) {
            return;
        }

        BlockPos position = new BlockPos(
                (int) Math.floor(transform.getPosition().getX()),
                (int) Math.floor(transform.getPosition().getY()),
                (int) Math.floor(transform.getPosition().getZ())
        );
        GameMode enforced = plugin.resolveEnforcedGameMode(playerRef, world.getName(), position);
        if (enforced != null && enforced != event.getGameMode()) {
            event.setGameMode(enforced);
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Collections.singleton(RootDependency.last());
    }
}