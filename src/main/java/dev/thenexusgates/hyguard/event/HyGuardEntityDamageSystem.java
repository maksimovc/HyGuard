package dev.thenexusgates.hyguard.event;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.thenexusgates.hyguard.HyGuardPlugin;
import dev.thenexusgates.hyguard.core.protection.ProtectionAction;
import dev.thenexusgates.hyguard.util.BlockPos;

import javax.annotation.Nullable;

@SuppressWarnings("deprecation")
public final class HyGuardEntityDamageSystem extends DamageEventSystem {

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
        return Player.getComponentType();
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
        Player victimPlayer = commandBuffer.getComponent(victimRef, Player.getComponentType());
        PlayerRef victimPlayerRef = commandBuffer.getComponent(victimRef, PlayerRef.getComponentType());
        TransformComponent victimTransform = commandBuffer.getComponent(victimRef, TransformComponent.getComponentType());
        EntityStore entityStore = store.getExternalData();
        World world = entityStore != null ? entityStore.getWorld() : null;
        if (victimPlayer == null || victimPlayerRef == null || victimTransform == null || victimTransform.getPosition() == null || world == null) {
            return;
        }

        BlockPos position = new BlockPos(
                (int) Math.floor(victimTransform.getPosition().getX()),
                (int) Math.floor(victimTransform.getPosition().getY()),
                (int) Math.floor(victimTransform.getPosition().getZ())
        );

        if (damage.getCause() == DamageCause.FALL
                && !plugin.evaluate(victimPlayerRef, world.getName(), position, ProtectionAction.PLAYER_FALL_DAMAGE).allowed()) {
            damage.setCancelled(true);
            plugin.send(victimPlayerRef, plugin.getConfigSnapshot().messages.protectionDenied);
            return;
        }

        PlayerRef attackerPlayerRef = resolveAttackerPlayerRef(damage, commandBuffer);
        if (attackerPlayerRef != null
                && !plugin.evaluate(attackerPlayerRef, world.getName(), position, ProtectionAction.PVP).allowed()) {
            damage.setCancelled(true);
            plugin.send(victimPlayerRef, plugin.getConfigSnapshot().messages.protectionDenied);
            return;
        }

        if (isMobDamage(damage, commandBuffer)
                && !plugin.evaluate(victimPlayerRef, world.getName(), position, ProtectionAction.MOB_DAMAGE_PLAYERS).allowed()) {
            damage.setCancelled(true);
            plugin.send(victimPlayerRef, plugin.getConfigSnapshot().messages.protectionDenied);
            return;
        }

        if (!plugin.evaluate(victimPlayerRef, world.getName(), position, ProtectionAction.PLAYER_DAMAGE).allowed()) {
            damage.setCancelled(true);
            plugin.send(victimPlayerRef, plugin.getConfigSnapshot().messages.protectionDenied);
        }
    }

    private PlayerRef resolveAttackerPlayerRef(Damage damage, CommandBuffer<EntityStore> commandBuffer) {
        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            return null;
        }

        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (attackerRef == null || !attackerRef.isValid()) {
            return null;
        }

        Player attackerPlayer = commandBuffer.getComponent(attackerRef, Player.getComponentType());
        if (attackerPlayer == null) {
            return null;
        }
        return commandBuffer.getComponent(attackerRef, PlayerRef.getComponentType());
    }

    private boolean isMobDamage(Damage damage, CommandBuffer<EntityStore> commandBuffer) {
        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            return false;
        }

        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (attackerRef == null || !attackerRef.isValid()) {
            return false;
        }

        return commandBuffer.getComponent(attackerRef, Player.getComponentType()) == null;
    }
}