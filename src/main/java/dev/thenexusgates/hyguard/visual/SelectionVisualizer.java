package dev.thenexusgates.hyguard.visual;

import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.Vector3f;
import com.hypixel.hytale.protocol.packets.buildertools.BuilderToolHideAnchors;
import com.hypixel.hytale.protocol.packets.interface_.EditorBlocksChange;
import com.hypixel.hytale.protocol.packets.player.ClearDebugShapes;
import com.hypixel.hytale.protocol.packets.player.DisplayDebug;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.thenexusgates.hyguard.core.selection.SelectionPoint;
import dev.thenexusgates.hyguard.core.selection.SelectionSession;
import dev.thenexusgates.hyguard.util.BlockPos;
import dev.thenexusgates.hyguard.util.BlockPosUtils;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public final class SelectionVisualizer {

    private static final Logger LOGGER = Logger.getLogger("HyGuard");
    private static final long DEBUG_REFRESH_MS = 900L;
    private static final float DEBUG_DISPLAY_TIME = 1.8f;
    private static final float OUTER_PADDING = 0.025f;
    private static final float GROUND_LIFT = 0.06f;
    private static final float GROUND_RAIL_THICKNESS = 0.18f;
    private static final float TOP_RAIL_THICKNESS = 0.08f;
    private static final float PILLAR_WIDTH = 0.16f;
    private static final float BOTTOM_ORB_SIZE = 0.34f;
    private static final float TOP_ORB_SIZE = 0.24f;
    private static final byte FLAG_SOLID_ONLY = (byte) DebugUtils.FLAG_NO_WIREFRAME;

    private static final Palette VALID_PALETTE = new Palette(
            new Vector3f(0.13f, 0.35f, 0.95f),
            new Vector3f(1.0f, 0.73f, 0.24f),
            new Vector3f(0.88f, 0.96f, 1.0f)
    );
    private static final Palette CONFLICT_PALETTE = new Palette(
            new Vector3f(0.93f, 0.24f, 0.30f),
            new Vector3f(1.0f, 0.55f, 0.18f),
            new Vector3f(1.0f, 0.88f, 0.75f)
    );

    private final ConcurrentHashMap<String, RenderState> activeRenders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PlayerRef> viewersByPlayer = new ConcurrentHashMap<>();
    private final VisualScheduler visualScheduler;
    private ScheduledFuture<?> refreshTask;

    public SelectionVisualizer(VisualScheduler visualScheduler) {
        this.visualScheduler = visualScheduler;
        if (visualScheduler != null) {
            this.refreshTask = visualScheduler.scheduleAtFixedRate(this::refreshDebugFrames, DEBUG_REFRESH_MS, DEBUG_REFRESH_MS, TimeUnit.MILLISECONDS);
        }
    }

    public void shutdown() {
        if (visualScheduler != null && refreshTask != null) {
            visualScheduler.cancel(refreshTask);
            refreshTask = null;
        }
        for (PlayerRef viewer : viewersByPlayer.values()) {
            clearDebugShapes(viewer);
            clearLegacySelectionPackets(viewer);
        }
        viewersByPlayer.clear();
        activeRenders.clear();
    }

    public boolean isSupported() {
        return true;
    }

    public void updateVisualization(PlayerRef viewer, SelectionSession selectionSession, boolean hasConflict) {
        if (viewer == null || viewer.getUuid() == null) {
            return;
        }

        String playerUuid = viewer.getUuid().toString();
        viewersByPlayer.put(playerUuid, viewer);
        if (selectionSession == null || !selectionSession.hasAnyPoint()) {
            clearPlayer(playerUuid);
            return;
        }

        SelectionPoint firstPoint = selectionSession.getFirstPoint();
        if (firstPoint == null) {
            clearPlayer(playerUuid);
            return;
        }

        BlockPos first = firstPoint.getPosition();
        BlockPos second = selectionSession.getSecondPoint() == null
                ? first
                : selectionSession.getSecondPoint().getPosition();
        RenderState nextState = new RenderState(
                BlockPosUtils.min(first, second),
                BlockPosUtils.max(first, second),
                hasConflict
        );
        RenderState current = activeRenders.put(playerUuid, nextState);
        if (nextState.equals(current)) {
            return;
        }

        clearLegacySelectionPackets(viewer);
        sendTerritoryOverlay(viewer, nextState);
    }

    public void clearPlayer(String playerUuid) {
        if (playerUuid == null) {
            return;
        }

        activeRenders.remove(playerUuid);
        PlayerRef viewer = viewersByPlayer.remove(playerUuid);
        if (viewer != null) {
            clearDebugShapes(viewer);
            clearLegacySelectionPackets(viewer);
        }
    }

    private void refreshDebugFrames() {
        if (activeRenders.isEmpty()) {
            return;
        }

        for (Map.Entry<String, RenderState> entry : activeRenders.entrySet()) {
            PlayerRef viewer = viewersByPlayer.get(entry.getKey());
            if (viewer == null) {
                continue;
            }

            try {
                sendTerritoryOverlay(viewer, entry.getValue());
            } catch (Throwable throwable) {
                LOGGER.warning("[HyGuard] selection refresh failed for " + viewer.getUsername() + ": " + throwable);
            }
        }
    }

    private void sendTerritoryOverlay(PlayerRef viewer, RenderState state) {
        if (viewer == null || state == null) {
            return;
        }

        clearDebugShapes(viewer);

        Palette palette = state.hasConflict() ? CONFLICT_PALETTE : VALID_PALETTE;
        float minX = state.min().getX() - OUTER_PADDING;
        float minY = state.min().getY() - OUTER_PADDING;
        float minZ = state.min().getZ() - OUTER_PADDING;
        float maxX = state.max().getX() + 1.0f + OUTER_PADDING;
        float maxY = state.max().getY() + 1.0f + OUTER_PADDING;
        float maxZ = state.max().getZ() + 1.0f + OUTER_PADDING;

        float groundY = minY + GROUND_LIFT;
        float topY = maxY - GROUND_LIFT;
        sendPerimeterRails(viewer, minX, maxX, minZ, maxZ, groundY, GROUND_RAIL_THICKNESS, palette.ground(), 0.31f);
        sendPerimeterRails(viewer, minX, maxX, minZ, maxZ, topY, TOP_RAIL_THICKNESS, palette.top(), 0.18f);
        sendCornerPillars(viewer, minX, maxX, minY, maxY, minZ, maxZ, PILLAR_WIDTH, palette.pillar(), 0.19f);
        sendCornerOrbs(viewer, minX, maxX, minY, maxY, minZ, maxZ, palette.ground(), palette.top());
    }

    private void sendPerimeterRails(PlayerRef viewer,
                                    float minX,
                                    float maxX,
                                    float minZ,
                                    float maxZ,
                                    float y,
                                    float thickness,
                                    Vector3f color,
                                    float opacity) {
        float centerX = (minX + maxX) * 0.5f;
        float centerZ = (minZ + maxZ) * 0.5f;
        float spanX = maxX - minX;
        float spanZ = maxZ - minZ;
        sendShape(viewer, DebugShape.Cube, centerX, y, minZ, spanX, thickness, thickness, color, opacity, FLAG_SOLID_ONLY);
        sendShape(viewer, DebugShape.Cube, centerX, y, maxZ, spanX, thickness, thickness, color, opacity, FLAG_SOLID_ONLY);
        sendShape(viewer, DebugShape.Cube, minX, y, centerZ, thickness, thickness, spanZ, color, opacity, FLAG_SOLID_ONLY);
        sendShape(viewer, DebugShape.Cube, maxX, y, centerZ, thickness, thickness, spanZ, color, opacity, FLAG_SOLID_ONLY);
    }

    private void sendCornerPillars(PlayerRef viewer,
                                   float minX,
                                   float maxX,
                                   float minY,
                                   float maxY,
                                   float minZ,
                                   float maxZ,
                                   float width,
                                   Vector3f color,
                                   float opacity) {
        float centerY = (minY + maxY) * 0.5f;
        float height = maxY - minY;
        sendShape(viewer, DebugShape.Cylinder, minX, centerY, minZ, width, height, width, color, opacity, FLAG_SOLID_ONLY);
        sendShape(viewer, DebugShape.Cylinder, minX, centerY, maxZ, width, height, width, color, opacity, FLAG_SOLID_ONLY);
        sendShape(viewer, DebugShape.Cylinder, maxX, centerY, minZ, width, height, width, color, opacity, FLAG_SOLID_ONLY);
        sendShape(viewer, DebugShape.Cylinder, maxX, centerY, maxZ, width, height, width, color, opacity, FLAG_SOLID_ONLY);
    }

    private void sendCornerOrbs(PlayerRef viewer,
                                float minX,
                                float maxX,
                                float minY,
                                float maxY,
                                float minZ,
                                float maxZ,
                                Vector3f groundColor,
                                Vector3f topColor) {
        float bottomY = minY + 0.08f;
        float topY = maxY - 0.08f;

        sendShape(viewer, DebugShape.Sphere, minX, bottomY, minZ, BOTTOM_ORB_SIZE, BOTTOM_ORB_SIZE, BOTTOM_ORB_SIZE, groundColor, 0.84f, FLAG_SOLID_ONLY);
        sendShape(viewer, DebugShape.Sphere, minX, bottomY, maxZ, BOTTOM_ORB_SIZE, BOTTOM_ORB_SIZE, BOTTOM_ORB_SIZE, groundColor, 0.84f, FLAG_SOLID_ONLY);
        sendShape(viewer, DebugShape.Sphere, maxX, bottomY, minZ, BOTTOM_ORB_SIZE, BOTTOM_ORB_SIZE, BOTTOM_ORB_SIZE, groundColor, 0.84f, FLAG_SOLID_ONLY);
        sendShape(viewer, DebugShape.Sphere, maxX, bottomY, maxZ, BOTTOM_ORB_SIZE, BOTTOM_ORB_SIZE, BOTTOM_ORB_SIZE, groundColor, 0.84f, FLAG_SOLID_ONLY);

        sendShape(viewer, DebugShape.Sphere, minX, topY, minZ, TOP_ORB_SIZE, TOP_ORB_SIZE, TOP_ORB_SIZE, topColor, 0.56f, FLAG_SOLID_ONLY);
        sendShape(viewer, DebugShape.Sphere, minX, topY, maxZ, TOP_ORB_SIZE, TOP_ORB_SIZE, TOP_ORB_SIZE, topColor, 0.56f, FLAG_SOLID_ONLY);
        sendShape(viewer, DebugShape.Sphere, maxX, topY, minZ, TOP_ORB_SIZE, TOP_ORB_SIZE, TOP_ORB_SIZE, topColor, 0.56f, FLAG_SOLID_ONLY);
        sendShape(viewer, DebugShape.Sphere, maxX, topY, maxZ, TOP_ORB_SIZE, TOP_ORB_SIZE, TOP_ORB_SIZE, topColor, 0.56f, FLAG_SOLID_ONLY);
    }

    private void sendShape(PlayerRef viewer,
                           DebugShape shape,
                           float x,
                           float y,
                           float z,
                           float sizeX,
                           float sizeY,
                           float sizeZ,
                           Vector3f color,
                           float opacity,
                           byte flags) {
        PacketHandler packetHandler = viewer.getPacketHandler();
        if (packetHandler == null) {
            return;
        }

        float[] transform = new float[] {
                sizeX, 0.0f, 0.0f, 0.0f,
                0.0f, sizeY, 0.0f, 0.0f,
                0.0f, 0.0f, sizeZ, 0.0f,
                x, y, z, 1.0f
        };
        packetHandler.writeNoCache(new DisplayDebug(shape, transform, color, DEBUG_DISPLAY_TIME, flags, null, opacity));
    }

    private void clearLegacySelectionPackets(PlayerRef viewer) {
        sendPackets(viewer, new BuilderToolHideAnchors(), new EditorBlocksChange());
    }

    private void clearDebugShapes(PlayerRef viewer) {
        if (viewer == null) {
            return;
        }

        PacketHandler packetHandler = viewer.getPacketHandler();
        if (packetHandler == null) {
            return;
        }

        try {
            packetHandler.writeNoCache(new ClearDebugShapes());
        } catch (Throwable throwable) {
            LOGGER.warning("[HyGuard] clearDebugShapes failed for " + viewer.getUsername() + ": " + throwable);
        }
    }

    private void sendPackets(PlayerRef viewer, ToClientPacket... packets) {
        if (viewer == null || packets == null || packets.length == 0) {
            return;
        }

        PacketHandler packetHandler = viewer.getPacketHandler();
        if (packetHandler == null) {
            return;
        }

        try {
            for (ToClientPacket packet : packets) {
                if (packet != null) {
                    packetHandler.write(packet);
                }
            }
        } catch (Throwable throwable) {
            LOGGER.warning("[HyGuard] sendPackets failed for " + viewer.getUsername() + ": " + throwable);
        }
    }

    private record Palette(Vector3f pillar, Vector3f ground, Vector3f top) {
    }

    private record RenderState(BlockPos min, BlockPos max, boolean hasConflict) {

        private RenderState {
            min = Objects.requireNonNull(min, "min").copy();
            max = Objects.requireNonNull(max, "max").copy();
        }
    }
}