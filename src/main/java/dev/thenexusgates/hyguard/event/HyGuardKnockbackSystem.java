package dev.thenexusgates.hyguard.event;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent;
import com.hypixel.hytale.server.core.modules.entity.AllLegacyLivingEntityTypesQuery;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.player.KnockbackSimulation;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.thenexusgates.hyguard.HyGuardPlugin;
import dev.thenexusgates.hyguard.core.region.Region;
import dev.thenexusgates.hyguard.core.region.RegionFlag;
import dev.thenexusgates.hyguard.core.region.RegionFlagValue;
import dev.thenexusgates.hyguard.core.region.RegionRole;
import dev.thenexusgates.hyguard.util.BlockPos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@SuppressWarnings("deprecation")
public final class HyGuardKnockbackSystem extends EntityTickingSystem<EntityStore> {

    private static final Query<EntityStore> QUERY = Query.and(
            AllLegacyLivingEntityTypesQuery.INSTANCE,
            TransformComponent.getComponentType()
    );

    private final HyGuardPlugin plugin;

    public HyGuardKnockbackSystem(HyGuardPlugin plugin) {
        this.plugin = plugin;
    }

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public void tick(float dt,
                     int index,
                     @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        TransformComponent transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
        if (transform == null || transform.getPosition() == null) {
            return;
        }

        Ref<EntityStore> victimRef = archetypeChunk.getReferenceTo(index);
        KnockbackComponent knockbackComponent = commandBuffer.getComponent(victimRef, KnockbackComponent.getComponentType());
        KnockbackSimulation knockbackSimulation = commandBuffer.getComponent(victimRef, KnockbackSimulation.getComponentType());
        Velocity velocity = commandBuffer.getComponent(victimRef, Velocity.getComponentType());
        HyGuardKnockbackBypassMarker bypassMarker = HyGuardKnockbackBypassMarker.getComponentType() == null
                ? null
                : commandBuffer.getComponent(victimRef, HyGuardKnockbackBypassMarker.getComponentType());

        if (knockbackComponent == null
                && knockbackSimulation == null
                && (velocity == null || velocity.getInstructions().isEmpty())
                && bypassMarker == null) {
            return;
        }

        EntityStore entityStore = store.getExternalData();
        World world = entityStore != null ? entityStore.getWorld() : null;
        if (world == null) {
            return;
        }

        BlockPos position = new BlockPos(
                (int) Math.floor(transform.getPosition().getX()),
                (int) Math.floor(transform.getPosition().getY()),
                (int) Math.floor(transform.getPosition().getZ())
        );
        if (!isKnockbackSuppressed(plugin, victimRef, world.getName(), position, commandBuffer)) {
            clearBypassMarker(victimRef, commandBuffer, bypassMarker);
            return;
        }

        if (bypassMarker != null) {
            clearBypassMarker(victimRef, commandBuffer, bypassMarker);
            return;
        }

        suppressKnockbackState(victimRef, knockbackComponent, knockbackSimulation, velocity, commandBuffer);
    }

    public static boolean isKnockbackSuppressed(HyGuardPlugin plugin,
                                                Ref<EntityStore> victimRef,
                                                String worldId,
                                                BlockPos position,
                                                CommandBuffer<EntityStore> commandBuffer) {
        if (plugin == null || victimRef == null || worldId == null || position == null) {
            return false;
        }

        PlayerRef victimPlayerRef = commandBuffer.getComponent(victimRef, PlayerRef.getComponentType());
        if (victimPlayerRef != null && plugin.canBypassProtection(victimPlayerRef)) {
            return false;
        }

        List<Region> regions = plugin.getRegionsAt(worldId, position);
        for (Region region : regions) {
            RegionFlagValue flagValue = region.getFlags().get(RegionFlag.KNOCKBACK);
            if (flagValue == null || flagValue.getMode() == RegionFlagValue.Mode.INHERIT) {
                continue;
            }

            RegionRole role = victimPlayerRef == null || victimPlayerRef.getUuid() == null
                    ? null
                    : region.getRoleFor(victimPlayerRef.getUuid().toString());
            return switch (flagValue.getMode()) {
                case ALLOW -> false;
                case DENY -> true;
                case ALLOW_MEMBERS -> role == null || !role.isAtLeast(RegionRole.MEMBER);
                case ALLOW_TRUSTED -> role == null || !role.isAtLeast(RegionRole.TRUSTED);
                case INHERIT -> false;
            };
        }

        return false;
    }

    public static void markKnockbackBypass(Ref<EntityStore> victimRef,
                                           CommandBuffer<EntityStore> commandBuffer) {
        if (victimRef == null || !victimRef.isValid() || HyGuardKnockbackBypassMarker.getComponentType() == null) {
            return;
        }
        commandBuffer.putComponent(victimRef, HyGuardKnockbackBypassMarker.getComponentType(), new HyGuardKnockbackBypassMarker());
    }

    public static void suppressKnockbackState(Ref<EntityStore> victimRef,
                                              @Nullable KnockbackComponent knockbackComponent,
                                              @Nullable KnockbackSimulation knockbackSimulation,
                                              @Nullable Velocity velocity,
                                              CommandBuffer<EntityStore> commandBuffer) {
        if (knockbackComponent != null) {
            try {
                knockbackComponent.setVelocity(new Vector3d(0.0, 0.0, 0.0));
                knockbackComponent.setDuration(0.0f);
                knockbackComponent.setTimer(0.0f);
                knockbackComponent.setVelocityConfig(null);
            } catch (Throwable ignored) {
            }
            commandBuffer.tryRemoveComponent(victimRef, KnockbackComponent.getComponentType());
        }

        if (velocity != null && !velocity.getInstructions().isEmpty()) {
            velocity.getInstructions().clear();
        }

        if (knockbackSimulation != null) {
            try {
                knockbackSimulation.getRequestedVelocity().assign(0.0);
                knockbackSimulation.getRelativeMovement().assign(0.0);
                knockbackSimulation.getSimVelocity().assign(0.0);
                knockbackSimulation.getMovementOffset().assign(0.0);
                knockbackSimulation.setRequestedVelocityChangeType(null);
                knockbackSimulation.setRemainingTime(0.0f);
            } catch (Throwable ignored) {
            }
            commandBuffer.tryRemoveComponent(victimRef, KnockbackSimulation.getComponentType());
        }
    }

    private static void clearBypassMarker(Ref<EntityStore> victimRef,
                                          CommandBuffer<EntityStore> commandBuffer,
                                          @Nullable HyGuardKnockbackBypassMarker bypassMarker) {
        if (bypassMarker == null || HyGuardKnockbackBypassMarker.getComponentType() == null) {
            return;
        }
        commandBuffer.tryRemoveComponent(victimRef, HyGuardKnockbackBypassMarker.getComponentType());
    }
}