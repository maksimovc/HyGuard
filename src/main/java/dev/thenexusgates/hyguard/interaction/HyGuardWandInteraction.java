package dev.thenexusgates.hyguard.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.thenexusgates.hyguard.HyGuardPlugin;
import dev.thenexusgates.hyguard.util.BlockPos;

public final class HyGuardWandInteraction extends SimpleInstantInteraction {

    public static final String ID = "HyGuard_WandInteraction";

    public static final BuilderCodec<HyGuardWandInteraction> CODEC = BuilderCodec.builder(
            HyGuardWandInteraction.class,
            HyGuardWandInteraction::new,
            SimpleInstantInteraction.CODEC
    ).documentation("Handles HyGuard wand primary selection and secondary clear or restore input.").build();

    public HyGuardWandInteraction(String id) {
        super(id);
    }

    protected HyGuardWandInteraction() {
    }

    @Override
    protected void firstRun(
            InteractionType interactionType,
            InteractionContext interactionContext,
            CooldownHandler cooldownHandler
    ) {
        HyGuardPlugin plugin = HyGuardPlugin.getInstance();
        if (plugin == null) {
            return;
        }

        Player player = resolvePlayer(interactionContext);
        PlayerRef playerRef = resolvePlayerRef(interactionContext);
        if (player == null || playerRef == null || !plugin.isWand(player)) {
            fail(interactionContext);
            return;
        }

        if (interactionType == InteractionType.Secondary || interactionType == InteractionType.Use) {
            plugin.toggleSelectionClear(playerRef);
            fail(interactionContext);
            return;
        }

        if (interactionType != InteractionType.Primary) {
            return;
        }

        BlockPosition targetBlock = interactionContext != null ? interactionContext.getTargetBlock() : null;
        World world = player.getWorld();
        if (targetBlock == null || world == null) {
            fail(interactionContext);
            return;
        }

        plugin.handleWandLeftClick(
                playerRef,
                world.getName(),
                new BlockPos(targetBlock.x, targetBlock.y, targetBlock.z)
        );
        fail(interactionContext);
    }

    private static Player resolvePlayer(InteractionContext interactionContext) {
        if (interactionContext == null) {
            return null;
        }

        Ref<EntityStore> ref = interactionContext.getEntity();
        if (ref == null || !ref.isValid()) {
            return null;
        }

        Store<EntityStore> store = ref.getStore();
        if (store == null) {
            return null;
        }

        return store.getComponent(ref, Player.getComponentType());
    }

    private static PlayerRef resolvePlayerRef(InteractionContext interactionContext) {
        if (interactionContext == null) {
            return null;
        }

        Ref<EntityStore> ref = interactionContext.getEntity();
        if (ref == null || !ref.isValid()) {
            return null;
        }

        Store<EntityStore> store = ref.getStore();
        if (store == null) {
            return null;
        }

        return store.getComponent(ref, PlayerRef.getComponentType());
    }

    private static void fail(InteractionContext interactionContext) {
        if (interactionContext != null && interactionContext.getState() != null) {
            interactionContext.getState().state = InteractionState.Failed;
        }
    }
}