package dev.thenexusgates.hyguard.event;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.AllLegacyLivingEntityTypesQuery;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.player.KnockbackSimulation;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.thenexusgates.hyguard.HyGuardPlugin;
import dev.thenexusgates.hyguard.core.protection.ProtectionAction;
import dev.thenexusgates.hyguard.core.region.RegionFlag;
import dev.thenexusgates.hyguard.core.region.RegionFlagValue;
import dev.thenexusgates.hyguard.util.BlockPos;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

@SuppressWarnings("deprecation")
public final class HyGuardEntityDamageSystem extends DamageEventSystem {

    private static final Query<EntityStore> QUERY = Query.and(
            AllLegacyLivingEntityTypesQuery.INSTANCE,
            TransformComponent.getComponentType()
    );

    private final HyGuardPlugin plugin;

    public HyGuardEntityDamageSystem(HyGuardPlugin plugin) {
        this.plugin = plugin;
    }

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Collections.singleton(RootDependency.first());
    }

    @Override
    public void handle(int index,
                       ArchetypeChunk<EntityStore> archetypeChunk,
                       Store<EntityStore> store,
                       CommandBuffer<EntityStore> commandBuffer,
                       Damage damage) {
        if (damage == null || damage.isCancelled()) {
            return;
        }

        Ref<EntityStore> victimRef = archetypeChunk.getReferenceTo(index);
        TransformComponent victimTransform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
        EntityStore entityStore = store.getExternalData();
        World world = entityStore != null ? entityStore.getWorld() : null;
        if (victimTransform == null || victimTransform.getPosition() == null || world == null) {
            return;
        }

        Player victimPlayer = store.getComponent(victimRef, Player.getComponentType());
        PlayerRef victimPlayerRef = store.getComponent(victimRef, PlayerRef.getComponentType());
        PlayerRef attackerPlayerRef = resolveAttackerPlayerRef(damage, store);

        BlockPos position = new BlockPos(
                (int) Math.floor(victimTransform.getPosition().getX()),
                (int) Math.floor(victimTransform.getPosition().getY()),
                (int) Math.floor(victimTransform.getPosition().getZ())
        );

        if (victimPlayer == null || victimPlayerRef == null) {
            if (attackerPlayerRef != null
                    && !plugin.evaluate(attackerPlayerRef, world.getName(), position, ProtectionAction.ENTITY_DAMAGE).allowed()) {
                damage.setCancelled(true);
                plugin.send(attackerPlayerRef, plugin.getConfigSnapshot().messages.protectionDenied);
            }
            applyKnockbackProtection(victimRef, victimPlayerRef, attackerPlayerRef, world.getName(), position, commandBuffer, damage);
            return;
        }

        if (plugin.isFlagAllowed(victimPlayerRef, world.getName(), position, RegionFlag.INVINCIBLE, RegionFlagValue.Mode.DENY)) {
            damage.setCancelled(true);
            return;
        }

        if (damage.getCause() == DamageCause.FALL
                && !plugin.evaluate(victimPlayerRef, world.getName(), position, ProtectionAction.PLAYER_FALL_DAMAGE).allowed()) {
            damage.setCancelled(true);
            plugin.send(victimPlayerRef, plugin.getConfigSnapshot().messages.protectionDenied);
            return;
        }

        if (attackerPlayerRef != null
                && !plugin.evaluate(attackerPlayerRef, world.getName(), position, ProtectionAction.PVP).allowed()) {
            damage.setCancelled(true);
            plugin.send(attackerPlayerRef, plugin.getConfigSnapshot().messages.protectionDenied);
            return;
        }

        if (isMobDamage(damage, store)
                && !plugin.evaluate(victimPlayerRef, world.getName(), position, ProtectionAction.MOB_DAMAGE_PLAYERS).allowed()) {
            damage.setCancelled(true);
            plugin.send(victimPlayerRef, plugin.getConfigSnapshot().messages.protectionDenied);
            return;
        }

        if (!plugin.evaluate(victimPlayerRef, world.getName(), position, ProtectionAction.PLAYER_DAMAGE).allowed()) {
            damage.setCancelled(true);
            plugin.send(victimPlayerRef, plugin.getConfigSnapshot().messages.protectionDenied);
        }

        applyKnockbackProtection(victimRef, victimPlayerRef, attackerPlayerRef, world.getName(), position, commandBuffer, damage);
    }

    private void applyKnockbackProtection(Ref<EntityStore> victimRef,
                                          PlayerRef victimPlayerRef,
                                          PlayerRef attackerPlayerRef,
                                          String worldId,
                                          BlockPos position,
                                          CommandBuffer<EntityStore> commandBuffer,
                                          Damage damage) {
        boolean knockbackSuppressed = damage.isCancelled()
                || HyGuardKnockbackSystem.isKnockbackSuppressed(plugin, victimRef, worldId, position, commandBuffer);
        if (!knockbackSuppressed) {
            return;
        }

        if (plugin.canBypassProtection(victimPlayerRef) || plugin.canBypassProtection(attackerPlayerRef)) {
            HyGuardKnockbackSystem.markKnockbackBypass(victimRef, commandBuffer);
            return;
        }

        KnockbackComponent knockbackComponent = commandBuffer.getComponent(victimRef, KnockbackComponent.getComponentType());
        KnockbackSimulation knockbackSimulation = commandBuffer.getComponent(victimRef, KnockbackSimulation.getComponentType());
        Velocity velocity = commandBuffer.getComponent(victimRef, Velocity.getComponentType());
        HyGuardKnockbackSystem.suppressKnockbackState(victimRef, knockbackComponent, knockbackSimulation, velocity, commandBuffer);
    }

    private PlayerRef resolveAttackerPlayerRef(Damage damage, Store<EntityStore> store) {
        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            return null;
        }

        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (attackerRef == null || !attackerRef.isValid()) {
            return null;
        }

        Player attackerPlayer = store.getComponent(attackerRef, Player.getComponentType());
        if (attackerPlayer == null) {
            return null;
        }
        return store.getComponent(attackerRef, PlayerRef.getComponentType());
    }

    private boolean isMobDamage(Damage damage, Store<EntityStore> store) {
        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            return false;
        }

        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (attackerRef == null || !attackerRef.isValid()) {
            return false;
        }

        return store.getComponent(attackerRef, Player.getComponentType()) == null;
    }
}