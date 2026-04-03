package dev.thenexusgates.hyguard.visual;

import com.hypixel.hytale.protocol.packets.interface_.EditorBlocksChange;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.thenexusgates.hyguard.core.selection.SelectionSession;
import dev.thenexusgates.hyguard.util.BlockPos;
import dev.thenexusgates.hyguard.util.BlockPosUtils;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class SelectionVisualizer {

    private static final long REFRESH_INTERVAL_MS = 350L;

    private final ParticleAdapter particleAdapter;
    private final VisualScheduler visualScheduler;
    private final ConcurrentHashMap<String, RenderHandle> activeRenders = new ConcurrentHashMap<>();
    private volatile boolean nativeOverlaySupported = true;
    private volatile ScheduledFuture<?> refreshTask;

    public SelectionVisualizer(ParticleAdapter particleAdapter, VisualScheduler visualScheduler) {
        this.particleAdapter = particleAdapter;
        this.visualScheduler = visualScheduler;
    }

    public boolean isSupported() {
        return nativeOverlaySupported || particleAdapter.supportsParticles();
    }

    public void updateVisualization(PlayerRef viewer, SelectionSession selectionSession, boolean hasConflict) {
        if (viewer == null || viewer.getUuid() == null) {
            return;
        }

        String playerUuid = viewer.getUuid().toString();
        if (!isSupported() || selectionSession == null || !selectionSession.hasAnyPoint()) {
            clearPlayer(playerUuid);
            return;
        }

        BlockPos first = selectionSession.getFirstPoint() != null ? selectionSession.getFirstPoint().getPosition() : null;
        BlockPos second = selectionSession.getSecondPoint() != null ? selectionSession.getSecondPoint().getPosition() : null;
        BlockPos anchor = first != null ? first : second;
        BlockPos extent = second != null ? second : anchor;
        BlockPos min = BlockPosUtils.min(anchor, extent);
        BlockPos max = BlockPosUtils.max(anchor, extent);
        RenderState nextState = new RenderState(min, max, hasConflict);
        RenderHandle current = activeRenders.get(playerUuid);
        if (current != null && current.state().equals(nextState)) {
            return;
        }

        activeRenders.put(playerUuid, new RenderHandle(viewer, nextState));
        drawSelection(viewer, nextState);
        ensureRefreshTask();
    }

    public void clearPlayer(String playerUuid) {
        removePlayer(playerUuid, true);
    }

    private void removePlayer(String playerUuid, boolean clearOverlay) {
        if (playerUuid == null) {
            return;
        }
        RenderHandle removed = activeRenders.remove(playerUuid);
        if (removed != null) {
            if (clearOverlay) {
                clearSelection(removed.viewer());
            }
        }
        stopRefreshTaskIfIdle();
    }

    private synchronized void ensureRefreshTask() {
        if (refreshTask != null && !refreshTask.isCancelled()) {
            return;
        }
        refreshTask = visualScheduler.scheduleAtFixedRate(
                this::redrawAll,
                REFRESH_INTERVAL_MS,
                REFRESH_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

    private synchronized void stopRefreshTaskIfIdle() {
        if (!activeRenders.isEmpty()) {
            return;
        }
        if (refreshTask != null) {
            visualScheduler.cancel(refreshTask);
            refreshTask = null;
        }
    }

    private void redrawAll() {
        for (RenderHandle renderHandle : activeRenders.values()) {
            drawSelection(renderHandle.viewer(), renderHandle.state());
        }
    }

    private void drawSelection(PlayerRef viewer, RenderState state) {
        if (viewer == null || state == null) {
            return;
        }

        boolean shouldUseNativeOverlay = shouldUseNativeOverlay(viewer);
        boolean drewNativeOverlay = shouldUseNativeOverlay && sendSelection(viewer, state);
        if (!shouldUseNativeOverlay) {
            clearSelection(viewer);
        }
        if (state.hasConflict() && particleAdapter.supportsParticles()) {
            particleAdapter.drawCuboid(viewer, state.min(), state.max(), ParticleAdapter.ParticleStyle.SELECTION_CONFLICT);
            return;
        }
        if (drewNativeOverlay || !particleAdapter.supportsParticles()) {
            return;
        }

        particleAdapter.drawCuboid(viewer, state.min(), state.max(), ParticleAdapter.ParticleStyle.SELECTION_VALID);
    }

    private boolean sendSelection(PlayerRef viewer, RenderState state) {
        if (!nativeOverlaySupported || viewer == null || state == null) {
            return false;
        }

        PacketHandler packetHandler = viewer.getPacketHandler();
        if (packetHandler == null) {
            return false;
        }

        try {
            packetHandler.write(buildSelectionPacket(state.min(), state.max()));
            return true;
        } catch (Throwable throwable) {
            nativeOverlaySupported = false;
            return false;
        }
    }

    private void clearSelection(PlayerRef viewer) {
        if (!nativeOverlaySupported || viewer == null) {
            return;
        }

        PacketHandler packetHandler = viewer.getPacketHandler();
        if (packetHandler == null) {
            return;
        }

        try {
            packetHandler.write(new EditorBlocksChange());
        } catch (Throwable throwable) {
            nativeOverlaySupported = false;
        }
    }

    private EditorBlocksChange buildSelectionPacket(BlockPos min, BlockPos max) {
        BlockSelection selection = new BlockSelection();
        selection.setSelectionArea(min.toVector(), max.toVector());
        return selection.toSelectionPacket();
    }

    private boolean shouldUseNativeOverlay(PlayerRef viewer) {
        if (viewer == null) {
            return false;
        }

        Ref<EntityStore> viewerRef = viewer.getReference();
        if (viewerRef == null || !viewerRef.isValid()) {
            return false;
        }

        Store<EntityStore> store = viewerRef.getStore();
        if (store == null) {
            return false;
        }

        Player player = store.getComponent(viewerRef, Player.getComponentType());
        return player != null && player.getGameMode() == GameMode.Creative;
    }

    public VisualScheduler getVisualScheduler() {
        return visualScheduler;
    }

    private record RenderHandle(PlayerRef viewer, RenderState state) {
    }

    private static final class RenderState {
        private final BlockPos min;
        private final BlockPos max;
        private final boolean hasConflict;

        private RenderState(BlockPos min, BlockPos max, boolean hasConflict) {
            this.min = min.copy();
            this.max = max.copy();
            this.hasConflict = hasConflict;
        }

        private BlockPos min() {
            return min;
        }

        private BlockPos max() {
            return max;
        }

        private boolean hasConflict() {
            return hasConflict;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof RenderState renderState)) {
                return false;
            }
            return hasConflict == renderState.hasConflict
                    && Objects.equals(min, renderState.min)
                    && Objects.equals(max, renderState.max);
        }

        @Override
        public int hashCode() {
            return Objects.hash(min, max, hasConflict);
        }
    }
}