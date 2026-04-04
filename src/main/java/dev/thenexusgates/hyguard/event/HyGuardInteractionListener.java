package dev.thenexusgates.hyguard.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PickupItemComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.thenexusgates.hyguard.HyGuardPlugin;
import dev.thenexusgates.hyguard.core.protection.ProtectionAction;
import dev.thenexusgates.hyguard.util.BlockPos;

public final class HyGuardInteractionListener {

    private final HyGuardPlugin plugin;

    public HyGuardInteractionListener(HyGuardPlugin plugin) {
        this.plugin = plugin;
    }

    public void handle(PlayerInteractEvent event) {
        if (event == null || event.isCancelled()) {
            return;
        }

        Ref<EntityStore> playerEntityRef = event.getPlayerRef();
        if (playerEntityRef == null || !playerEntityRef.isValid()) {
            return;
        }

        Store<EntityStore> playerStore;
        try {
            playerStore = playerEntityRef.getStore();
        } catch (Throwable throwable) {
            return;
        }
        if (playerStore == null) {
            return;
        }

        PlayerRef playerRef = playerStore.getComponent(playerEntityRef, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        if (plugin.isWand(event.getItemInHand())) {
            return;
        }

        if (handleItemPickupInteraction(event, playerRef)) {
            return;
        }

        if (event.getTargetRef() != null || !isBlockInteractionType(event.getActionType())) {
            return;
        }

        Vector3i targetBlock = event.getTargetBlock();
        World world = resolveWorld(event.getTargetEntity(), playerStore);
        if (targetBlock == null || world == null) {
            return;
        }

        if (!plugin.evaluate(playerRef, world.getName(), BlockPos.fromVector(targetBlock), ProtectionAction.BLOCK_INTERACT).allowed()) {
            event.setCancelled(true);
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.protectionDenied);
        }
    }

    private boolean handleItemPickupInteraction(PlayerInteractEvent event, PlayerRef playerRef) {
        if (!isPickupInteractionType(event.getActionType()) || PickupItemComponent.getComponentType() == null) {
            return false;
        }

        Ref<EntityStore> targetRef = event.getTargetRef();
        if (targetRef == null || !targetRef.isValid()) {
            return false;
        }

        Store<EntityStore> targetStore;
        try {
            targetStore = targetRef.getStore();
        } catch (Throwable throwable) {
            return false;
        }
        if (targetStore == null || targetStore.getComponent(targetRef, PickupItemComponent.getComponentType()) == null) {
            return false;
        }

        TransformComponent transform = targetStore.getComponent(targetRef, TransformComponent.getComponentType());
        World world = resolveWorld(event.getTargetEntity(), targetStore);
        if (transform == null || transform.getPosition() == null || world == null) {
            return false;
        }

        BlockPos position = new BlockPos(
                (int) Math.floor(transform.getPosition().getX()),
                (int) Math.floor(transform.getPosition().getY()),
                (int) Math.floor(transform.getPosition().getZ())
        );
        if (plugin.evaluate(playerRef, world.getName(), position, ProtectionAction.PLAYER_ITEM_PICKUP).allowed()) {
            return false;
        }

        event.setCancelled(true);
        plugin.send(playerRef, plugin.getConfigSnapshot().messages.protectionDenied);
        return true;
    }

    private boolean isBlockInteractionType(InteractionType interactionType) {
        return interactionType == InteractionType.Use
                || interactionType == InteractionType.Secondary
                || interactionType == InteractionType.Held
                || interactionType == InteractionType.HeldOffhand;
    }

    private boolean isPickupInteractionType(InteractionType interactionType) {
        return interactionType == InteractionType.Pickup
                || interactionType == InteractionType.Pick
                || interactionType == InteractionType.Use
                || interactionType == InteractionType.Held;
    }

    private World resolveWorld(Entity targetEntity, Store<EntityStore> store) {
        if (targetEntity != null && targetEntity.getWorld() != null) {
            return targetEntity.getWorld();
        }
        if (store != null && store.getExternalData() != null) {
            return store.getExternalData().getWorld();
        }
        return null;
    }
}