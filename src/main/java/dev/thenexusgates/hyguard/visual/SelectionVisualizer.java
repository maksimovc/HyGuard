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
import dev.thenexusgates.hyguard.core.region.Region;
import dev.thenexusgates.hyguard.util.BlockPos;
import dev.thenexusgates.hyguard.util.BlockPosUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public final class SelectionVisualizer {

    public enum PreviewState {
        NEUTRAL,
        CLAIMED,
        AVAILABLE,
        VALID,
        INVALID
    }

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

    private static final Theme YELLOW_THEME = new Theme(new Vector3f(0.96f, 0.79f, 0.16f), 0.34f, 0.23f, 0.82f);
    private static final Theme GREEN_THEME = new Theme(new Vector3f(0.20f, 0.84f, 0.36f), 0.34f, 0.23f, 0.82f);
    private static final Theme RED_THEME = new Theme(new Vector3f(0.94f, 0.26f, 0.31f), 0.34f, 0.23f, 0.82f);
    private static final Theme BLUE_THEME = new Theme(new Vector3f(0.22f, 0.58f, 0.96f), 0.28f, 0.19f, 0.72f);

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

    public void updateVisualization(PlayerRef viewer,
                                    SelectionSession selectionSession,
                                    PreviewState previewState,
                                    List<Region> childRegions) {
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
                previewState == null ? PreviewState.NEUTRAL : previewState,
                buildOverlayRegions(childRegions)
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

        renderOutline(viewer, state.min(), state.max(), themeFor(state.previewState()));
        for (OverlayRegion overlayRegion : state.overlayRegions()) {
            renderOutline(viewer, overlayRegion.min(), overlayRegion.max(), BLUE_THEME);
        }
    }

    private void renderOutline(PlayerRef viewer, BlockPos min, BlockPos max, Theme theme) {
        if (viewer == null || min == null || max == null || theme == null) {
            return;
        }

        float minX = min.getX() - OUTER_PADDING;
        float minY = min.getY() - OUTER_PADDING;
        float minZ = min.getZ() - OUTER_PADDING;
        float maxX = max.getX() + 1.0f + OUTER_PADDING;
        float maxY = max.getY() + 1.0f + OUTER_PADDING;
        float maxZ = max.getZ() + 1.0f + OUTER_PADDING;

        float groundY = minY + GROUND_LIFT;
        float topY = maxY - GROUND_LIFT;
        sendPerimeterRails(viewer, minX, maxX, minZ, maxZ, groundY, GROUND_RAIL_THICKNESS, theme.color(), theme.railOpacity());
        sendPerimeterRails(viewer, minX, maxX, minZ, maxZ, topY, TOP_RAIL_THICKNESS, theme.color(), theme.railOpacity());
        sendCornerPillars(viewer, minX, maxX, minY, maxY, minZ, maxZ, PILLAR_WIDTH, theme.color(), theme.pillarOpacity());
        sendVerticalConnectors(viewer, minX, maxX, minY, maxY, minZ, maxZ, theme.color(), theme.pillarOpacity());
        sendCornerOrbs(viewer, minX, maxX, minY, maxY, minZ, maxZ, theme.color(), theme.orbOpacity());
    }

    private Theme themeFor(PreviewState previewState) {
        return switch (previewState) {
            case CLAIMED -> GREEN_THEME;
            case INVALID -> RED_THEME;
            case AVAILABLE, VALID, NEUTRAL -> YELLOW_THEME;
        };
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

    private void sendVerticalConnectors(PlayerRef viewer,
                                        float minX,
                                        float maxX,
                                        float minY,
                                        float maxY,
                                        float minZ,
                                        float maxZ,
                                        Vector3f color,
                                        float opacity) {
        float centerY = (minY + maxY) * 0.5f;
        float height = maxY - minY;
        for (ConnectorPoint connectorPoint : collectConnectorPoints(minX, maxX, minZ, maxZ)) {
            sendShape(viewer, DebugShape.Cylinder, connectorPoint.x(), centerY, connectorPoint.z(), PILLAR_WIDTH, height, PILLAR_WIDTH, color, opacity, FLAG_SOLID_ONLY);
        }
    }

    private void sendCornerOrbs(PlayerRef viewer,
                                float minX,
                                float maxX,
                                float minY,
                                float maxY,
                                float minZ,
                                float maxZ,
                                Vector3f color,
                                float opacity) {
        float bottomY = minY + 0.08f;
        float topY = maxY - 0.08f;

        sendShape(viewer, DebugShape.Sphere, minX, bottomY, minZ, BOTTOM_ORB_SIZE, BOTTOM_ORB_SIZE, BOTTOM_ORB_SIZE, color, opacity, FLAG_SOLID_ONLY);
        sendShape(viewer, DebugShape.Sphere, minX, bottomY, maxZ, BOTTOM_ORB_SIZE, BOTTOM_ORB_SIZE, BOTTOM_ORB_SIZE, color, opacity, FLAG_SOLID_ONLY);
        sendShape(viewer, DebugShape.Sphere, maxX, bottomY, minZ, BOTTOM_ORB_SIZE, BOTTOM_ORB_SIZE, BOTTOM_ORB_SIZE, color, opacity, FLAG_SOLID_ONLY);
        sendShape(viewer, DebugShape.Sphere, maxX, bottomY, maxZ, BOTTOM_ORB_SIZE, BOTTOM_ORB_SIZE, BOTTOM_ORB_SIZE, color, opacity, FLAG_SOLID_ONLY);

        sendShape(viewer, DebugShape.Sphere, minX, topY, minZ, TOP_ORB_SIZE, TOP_ORB_SIZE, TOP_ORB_SIZE, color, opacity, FLAG_SOLID_ONLY);
        sendShape(viewer, DebugShape.Sphere, minX, topY, maxZ, TOP_ORB_SIZE, TOP_ORB_SIZE, TOP_ORB_SIZE, color, opacity, FLAG_SOLID_ONLY);
        sendShape(viewer, DebugShape.Sphere, maxX, topY, minZ, TOP_ORB_SIZE, TOP_ORB_SIZE, TOP_ORB_SIZE, color, opacity, FLAG_SOLID_ONLY);
        sendShape(viewer, DebugShape.Sphere, maxX, topY, maxZ, TOP_ORB_SIZE, TOP_ORB_SIZE, TOP_ORB_SIZE, color, opacity, FLAG_SOLID_ONLY);
    }

    private List<OverlayRegion> buildOverlayRegions(List<Region> childRegions) {
        if (childRegions == null || childRegions.isEmpty()) {
            return List.of();
        }

        ArrayList<OverlayRegion> overlays = new ArrayList<>();
        for (Region childRegion : childRegions) {
            if (childRegion == null || childRegion.isGlobal() || childRegion.getMin() == null || childRegion.getMax() == null) {
                continue;
            }
            overlays.add(new OverlayRegion(childRegion.getMin(), childRegion.getMax()));
        }
        return List.copyOf(overlays);
    }

    private List<ConnectorPoint> collectConnectorPoints(float minX, float maxX, float minZ, float maxZ) {
        Set<ConnectorPoint> connectorPoints = new LinkedHashSet<>();
        addConnectorRange(connectorPoints, minX, maxX, minZ, true);
        addConnectorRange(connectorPoints, minX, maxX, maxZ, true);
        addConnectorRange(connectorPoints, minZ, maxZ, minX, false);
        addConnectorRange(connectorPoints, minZ, maxZ, maxX, false);
        return List.copyOf(connectorPoints);
    }

    private void addConnectorRange(Set<ConnectorPoint> connectorPoints,
                                   float start,
                                   float end,
                                   float fixed,
                                   boolean alongX) {
        float span = end - start;
        if (span <= 2.0f) {
            return;
        }

        float quarter = start + span * 0.25f;
        float center = start + span * 0.5f;
        float threeQuarter = start + span * 0.75f;
        addConnector(connectorPoints, alongX ? quarter : fixed, alongX ? fixed : quarter);
        addConnector(connectorPoints, alongX ? center : fixed, alongX ? fixed : center);
        addConnector(connectorPoints, alongX ? threeQuarter : fixed, alongX ? fixed : threeQuarter);
    }

    private void addConnector(Set<ConnectorPoint> connectorPoints, float x, float z) {
        connectorPoints.add(new ConnectorPoint(roundCoordinate(x), roundCoordinate(z)));
    }

    private float roundCoordinate(float value) {
        return Math.round(value * 1000.0f) / 1000.0f;
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

    private record Theme(Vector3f color, float railOpacity, float pillarOpacity, float orbOpacity) {
    }

    private record OverlayRegion(BlockPos min, BlockPos max) {

        private OverlayRegion {
            min = Objects.requireNonNull(min, "min").copy();
            max = Objects.requireNonNull(max, "max").copy();
        }
    }

    private record ConnectorPoint(float x, float z) {
    }

    private record RenderState(BlockPos min, BlockPos max, PreviewState previewState, List<OverlayRegion> overlayRegions) {

        private RenderState {
            min = Objects.requireNonNull(min, "min").copy();
            max = Objects.requireNonNull(max, "max").copy();
            previewState = Objects.requireNonNull(previewState, "previewState");
            overlayRegions = overlayRegions == null ? List.of() : List.copyOf(overlayRegions);
        }
    }
}