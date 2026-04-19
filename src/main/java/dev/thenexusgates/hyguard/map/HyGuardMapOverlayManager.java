package dev.thenexusgates.hyguard.map;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.FormattedMessage;
import com.hypixel.hytale.protocol.packets.worldmap.MapMarker;
import com.hypixel.hytale.protocol.packets.worldmap.MapMarkerComponent;
import com.hypixel.hytale.protocol.packets.worldmap.MapImage;
import com.hypixel.hytale.protocol.packets.worldmap.PlacedByMarkerComponent;
import com.hypixel.hytale.protocol.packets.worldmap.TintComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.map.WorldMap;
import com.hypixel.hytale.server.core.universe.world.chunk.palette.BitFieldArr;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager;
import com.hypixel.hytale.server.core.universe.world.worldmap.provider.IWorldMapProvider;
import com.hypixel.hytale.server.core.util.PositionUtil;
import dev.thenexusgates.hyguard.HyGuardPlugin;
import dev.thenexusgates.hyguard.asset.HyGuardAssetPack;
import dev.thenexusgates.hyguard.core.region.Region;
import dev.thenexusgates.hyguard.core.region.RegionFlag;
import dev.thenexusgates.hyguard.core.region.RegionFlagValue;
import dev.thenexusgates.hyguard.core.region.RegionMember;
import dev.thenexusgates.hyguard.core.region.RegionRole;
import dev.thenexusgates.hyguard.storage.RegionCache;
import dev.thenexusgates.hyguard.util.BlockPos;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class HyGuardMapOverlayManager {

    private static final String CLAIM_MARKER_PREFIX = "HyGuardClaim-";

    private static volatile boolean codecRegistered;

    private final HyGuardPlugin plugin;
    private final SimpleClaimsMapCompat simpleClaimsCompat;

    public HyGuardMapOverlayManager(HyGuardPlugin plugin) {
        this.plugin = plugin;
        this.simpleClaimsCompat = new SimpleClaimsMapCompat();
    }

    public void registerCodec() {
        if (codecRegistered) {
            return;
        }

        synchronized (HyGuardMapOverlayManager.class) {
            if (codecRegistered) {
                return;
            }

            try {
                IWorldMapProvider.CODEC.register(
                        HyGuardWorldMapProvider.ID,
                        HyGuardWorldMapProvider.class,
                        HyGuardWorldMapProvider.CODEC
                );
            } catch (IllegalArgumentException ignored) {
            }
            codecRegistered = true;
        }
    }

    public void install(World world) {
        if (world == null || world.getWorldConfig() == null || world.getWorldConfig().isDeleteOnRemove()) {
            return;
        }

        world.getWorldConfig().setWorldMapProvider(new HyGuardWorldMapProvider());
        invalidateWorld(world.getName());
    }

    public void shutdown() {
    }

    public String describeMode() {
        return simpleClaimsCompat.describeMode();
    }

    public void invalidateRegion(Region region) {
        if (region == null || region.isGlobal()) {
            return;
        }
        invalidateBounds(region.getWorldId(), region.getMin(), region.getMax());
    }

    public void invalidateRegionBounds(String worldId,
                                       BlockPos oldMin,
                                       BlockPos oldMax,
                                       BlockPos newMin,
                                       BlockPos newMax) {
        LongOpenHashSet chunkIndexes = new LongOpenHashSet();
        addChunkRange(chunkIndexes, oldMin, oldMax);
        addChunkRange(chunkIndexes, newMin, newMax);
        invalidateChunks(worldId, chunkIndexes);
    }

    public void invalidateWorld(String worldId) {
        if (worldId == null || worldId.isBlank()) {
            return;
        }

        LongOpenHashSet chunkIndexes = new LongOpenHashSet();
        for (Region region : plugin.getWorldRegions(worldId)) {
            if (!region.isGlobal()) {
                addChunkRange(chunkIndexes, region.getMin(), region.getMax());
            }
        }
        invalidateChunks(worldId, chunkIndexes);
    }

    public void invalidateAllRegions() {
        Map<String, LongOpenHashSet> chunksByWorld = new ConcurrentHashMap<>();
        for (Region region : plugin.getRegionCache().allRegions()) {
            if (region == null || region.isGlobal()) {
                continue;
            }
            chunksByWorld.computeIfAbsent(region.getWorldId(), ignored -> new LongOpenHashSet());
            addChunkRange(chunksByWorld.get(region.getWorldId()), region.getMin(), region.getMax());
        }
        for (Map.Entry<String, LongOpenHashSet> entry : chunksByWorld.entrySet()) {
            invalidateChunks(entry.getKey(), entry.getValue());
        }
    }

    public void applyOverlays(World world, WorldMap worldMap) {
        if (world == null || worldMap == null) {
            return;
        }

        Long2ObjectMap<MapImage> chunks = worldMap.getChunks();
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        String worldId = world.getName();
        for (Long2ObjectMap.Entry<MapImage> entry : chunks.long2ObjectEntrySet()) {
            renderChunk(worldId, entry.getLongKey(), entry.getValue());
        }
    }

    public Map<String, MapMarker> generatePointsOfInterest(World world) {
        if (world == null) {
            return Map.of();
        }

        HashMap<String, MapMarker> markers = new HashMap<>();
        for (Region region : plugin.getWorldRegions(world.getName())) {
            MapMarker marker = createRegionMarker(region);
            if (marker != null) {
                markers.put(marker.id, marker);
            }
        }
        return markers;
    }

    private void renderChunk(String worldId, long chunkIndex, MapImage image) {
        int width = image == null ? 0 : image.width;
        int height = image == null ? 0 : image.height;
        int[] pixels = decodePixels(image, width, height);
        if (pixels == null || width <= 0 || height <= 0) {
            return;
        }

        int chunkX = ChunkUtil.xOfChunkIndex(chunkIndex);
        int chunkZ = ChunkUtil.zOfChunkIndex(chunkIndex);
        RegionCache regionCache = plugin.getRegionCache();
        List<Region> candidates = regionCache.getRegionsOverlappingChunk(worldId, chunkX, chunkZ).stream()
                .filter(HyGuardRegionMapStyle::isVisible)
                .toList();
        SimpleClaimsMapCompat.ChunkOverlay simpleClaimsOverlay = simpleClaimsCompat.resolveChunkOverlay(worldId, chunkX, chunkZ);
        if (candidates.isEmpty() && !simpleClaimsOverlay.visible()) {
            return;
        }

        int minBlockX = ChunkUtil.minBlock(chunkX);
        int maxBlockX = ChunkUtil.maxBlock(chunkX);
        int minBlockZ = ChunkUtil.minBlock(chunkZ);
        int maxBlockZ = ChunkUtil.maxBlock(chunkZ);
        int blockSpanX = Math.max(1, maxBlockX - minBlockX + 1);
        int blockSpanZ = Math.max(1, maxBlockZ - minBlockZ + 1);
        int borderStepX = Math.max(1, (int) Math.ceil((double) blockSpanX / width));
        int borderStepZ = Math.max(1, (int) Math.ceil((double) blockSpanZ / height));
        boolean changed = applySimpleClaimsOverlay(pixels, width, height, simpleClaimsOverlay);

        for (int pixelY = 0; pixelY < height; pixelY++) {
            int worldZ = minBlockZ + Math.min(blockSpanZ - 1, (int) ((long) pixelY * blockSpanZ / height));
            int rowOffset = pixelY * width;
            for (int pixelX = 0; pixelX < width; pixelX++) {
                int worldX = minBlockX + Math.min(blockSpanX - 1, (int) ((long) pixelX * blockSpanX / width));
                Region region = resolveRegionAt(candidates, worldX, worldZ);
                if (region == null) {
                    continue;
                }

                int overlayColor = HyGuardRegionMapStyle.resolveColor(region);
                float overlayAlpha = HyGuardRegionMapStyle.resolveOpacity(region);
                if (isBorderPixel(region, worldX, worldZ, borderStepX, borderStepZ)) {
                    overlayColor = darken(overlayColor, 0.72F);
                    overlayAlpha = Math.min(0.95F, overlayAlpha + 0.22F);
                }

                int pixelIndex = rowOffset + pixelX;
                int blended = blendArgb(pixels[pixelIndex], overlayColor, overlayAlpha);
                if (blended != pixels[pixelIndex]) {
                    pixels[pixelIndex] = blended;
                    changed = true;
                }
            }
        }

        if (changed) {
            writePixels(image, pixels, width, height);
        }
    }

    private static boolean applySimpleClaimsOverlay(int[] pixels,
                                                    int width,
                                                    int height,
                                                    SimpleClaimsMapCompat.ChunkOverlay overlay) {
        if (pixels == null || width <= 0 || height <= 0 || overlay == null || !overlay.visible()) {
            return false;
        }

        int borderThicknessX = Math.max(1, Math.min(2, width));
        int borderThicknessY = Math.max(1, Math.min(2, height));
        boolean changed = false;
        for (int pixelY = 0; pixelY < height; pixelY++) {
            boolean northBorder = overlay.northBorder() && pixelY < borderThicknessY;
            boolean southBorder = overlay.southBorder() && pixelY >= height - borderThicknessY;
            int rowOffset = pixelY * width;
            for (int pixelX = 0; pixelX < width; pixelX++) {
                boolean border = northBorder
                        || southBorder
                        || (overlay.westBorder() && pixelX < borderThicknessX)
                        || (overlay.eastBorder() && pixelX >= width - borderThicknessX);
                float alpha = border ? 0.75F : 0.4F;
                int pixelIndex = rowOffset + pixelX;
                int blended = blendArgb(pixels[pixelIndex], overlay.color(), alpha);
                if (blended != pixels[pixelIndex]) {
                    pixels[pixelIndex] = blended;
                    changed = true;
                }
            }
        }
        return changed;
    }

    private MapMarker createRegionMarker(Region region) {
        if (region == null || region.isGlobal() || !HyGuardRegionMapStyle.isVisible(region)
                || region.getMin() == null || region.getMax() == null) {
            return null;
        }

        Transform markerTransform = new Transform(
                new Vector3d(resolveMarkerCenterX(region), resolveMarkerCenterY(region), resolveMarkerCenterZ(region)),
                Vector3f.ZERO
        );

        String markerId = CLAIM_MARKER_PREFIX + region.getId();
        FormattedMessage markerName = null;
        FormattedMessage markerDetails = createFormattedMessage(buildMarkerDetails(region));
        ArrayList<MapMarkerComponent> components = new ArrayList<>(2);
        components.add(new TintComponent(toProtocolColor(HyGuardRegionMapStyle.resolveColor(region))));
        if (markerDetails != null) {
            components.add(new PlacedByMarkerComponent(markerDetails, null));
        }

        return new MapMarker(
                markerId,
                markerName,
                HyGuardAssetPack.getClaimMarkerAssetPath(),
                PositionUtil.toTransformPacket(markerTransform),
                null,
                components.toArray(new MapMarkerComponent[0])
        );
    }

    private String buildMarkerDetails(Region region) {
        long width = (long) region.getMax().getX() - region.getMin().getX() + 1L;
        long height = (long) region.getMax().getY() - region.getMin().getY() + 1L;
        long depth = (long) region.getMax().getZ() - region.getMin().getZ() + 1L;

        StringBuilder details = new StringBuilder();
        details.append(region.getName());
        details.append('\n').append("Owner: ").append(blankTo(region.getOwnerName(), "Unknown"));
        details.append('\n').append("Roles: ").append(describeRoleHierarchy(region));
        details.append('\n').append("Bounds: X ").append(region.getMin().getX()).append("..").append(region.getMax().getX())
                .append(" | Y ").append(region.getMin().getY()).append("..").append(region.getMax().getY())
                .append(" | Z ").append(region.getMin().getZ()).append("..").append(region.getMax().getZ());
        details.append('\n').append("Size: ").append(width).append('x').append(height).append('x').append(depth);
        details.append('\n').append("PvP: ").append(describePvp(region));
        return details.toString();
    }

    private String describeRoleHierarchy(Region region) {
        EnumMap<RegionRole, List<String>> namesByRole = new EnumMap<>(RegionRole.class);
        for (RegionRole role : RegionRole.values()) {
            namesByRole.put(role, new ArrayList<>());
        }
        namesByRole.get(RegionRole.OWNER).add(blankTo(region.getOwnerName(), "Unknown"));

        for (RegionMember member : region.getMembers().values()) {
            if (member == null || member.getRole() == null) {
                continue;
            }
            namesByRole.computeIfAbsent(member.getRole(), ignored -> new ArrayList<>())
                    .add(blankTo(member.getName(), member.getUuid()));
        }

        StringBuilder summary = new StringBuilder();
        appendRoleSummary(summary, "Owner", namesByRole.get(RegionRole.OWNER));
        appendRoleSummary(summary, "Co-owner", namesByRole.get(RegionRole.CO_OWNER));
        appendRoleSummary(summary, "Manager", namesByRole.get(RegionRole.MANAGER));
        appendRoleSummary(summary, "Member", namesByRole.get(RegionRole.MEMBER));
        appendRoleSummary(summary, "Trusted", namesByRole.get(RegionRole.TRUSTED));
        appendRoleSummary(summary, "Visitor", namesByRole.get(RegionRole.VISITOR));
        return summary.toString();
    }

    private static void appendRoleSummary(StringBuilder summary, String label, List<String> names) {
        if (summary.length() > 0) {
            summary.append(" | ");
        }
        summary.append(label).append(':').append(' ');
        if (names == null || names.isEmpty()) {
            summary.append('-');
            return;
        }

        int limit = Math.min(3, names.size());
        for (int index = 0; index < limit; index++) {
            if (index > 0) {
                summary.append(", ");
            }
            summary.append(names.get(index));
        }
        if (names.size() > limit) {
            summary.append(" +").append(names.size() - limit);
        }
    }

    private String describePvp(Region region) {
        RegionFlagValue.Mode effectiveMode = resolveEffectiveFlagMode(region, RegionFlag.PVP, RegionFlagValue.Mode.DENY);
        return switch (effectiveMode) {
            case ALLOW -> "On";
            case DENY -> "Off";
            case ALLOW_MEMBERS -> "Members only";
            case ALLOW_TRUSTED -> "Trusted+ only";
            case INHERIT -> "Off";
        };
    }

    private RegionFlagValue.Mode resolveEffectiveFlagMode(Region region,
                                                          RegionFlag flag,
                                                          RegionFlagValue.Mode defaultMode) {
        Set<String> visited = new HashSet<>();
        Region current = region;
        while (current != null && current.getId() != null && visited.add(current.getId())) {
            RegionFlagValue direct = current.getFlags().get(flag);
            if (direct != null && direct.getMode() != RegionFlagValue.Mode.INHERIT) {
                return direct.getMode();
            }

            String parentRegionId = current.getParentRegionId();
            if (parentRegionId == null || parentRegionId.isBlank()) {
                break;
            }
            current = plugin.getRegionCache().findById(parentRegionId);
        }

        for (Region candidate : plugin.getWorldRegions(region.getWorldId())) {
            if (candidate == null || !candidate.isGlobal()) {
                continue;
            }
            RegionFlagValue global = candidate.getFlags().get(flag);
            if (global != null && global.getMode() != RegionFlagValue.Mode.INHERIT) {
                return global.getMode();
            }
        }

        return defaultMode;
    }

    private static FormattedMessage createFormattedMessage(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        FormattedMessage message = new FormattedMessage();
        message.rawText = text;
        return message;
    }

    private static Color toProtocolColor(int rgb) {
        return new Color(
                (byte) ((rgb >> 16) & 0xFF),
                (byte) ((rgb >> 8) & 0xFF),
                (byte) (rgb & 0xFF)
        );
    }

    private static double resolveMarkerCenterX(Region region) {
        return ((double) region.getMin().getX() + region.getMax().getX() + 1.0d) / 2.0d;
    }

    private static double resolveMarkerCenterY(Region region) {
        return ((double) region.getMin().getY() + region.getMax().getY()) / 2.0d;
    }

    private static double resolveMarkerCenterZ(Region region) {
        return ((double) region.getMin().getZ() + region.getMax().getZ() + 1.0d) / 2.0d;
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static Region resolveRegionAt(List<Region> candidates, int worldX, int worldZ) {
        for (Region candidate : candidates) {
            if (containsXZ(candidate, worldX, worldZ)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean containsXZ(Region region, int worldX, int worldZ) {
        return region != null
                && !region.isGlobal()
                && region.getMin() != null
                && region.getMax() != null
                && worldX >= region.getMin().getX()
                && worldX <= region.getMax().getX()
                && worldZ >= region.getMin().getZ()
                && worldZ <= region.getMax().getZ();
    }

    private static boolean isBorderPixel(Region region,
                                         int worldX,
                                         int worldZ,
                                         int borderStepX,
                                         int borderStepZ) {
        return worldX - region.getMin().getX() < borderStepX
                || region.getMax().getX() - worldX < borderStepX
                || worldZ - region.getMin().getZ() < borderStepZ
                || region.getMax().getZ() - worldZ < borderStepZ;
    }

    private boolean drawRegionLabels(List<Region> candidates,
                                     int chunkX,
                                     int chunkZ,
                                     int minBlockX,
                                     int minBlockZ,
                                     int blockSpanX,
                                     int blockSpanZ,
                                     int width,
                                     int height,
                                     int[] pixels) {
        List<Region> labeledRegions = candidates.stream()
                .filter(HyGuardRegionMapStyle::isLabelVisible)
                .filter(region -> shouldDrawLabelInChunk(region, chunkX, chunkZ))
                .toList();
        if (labeledRegions.isEmpty()) {
            return false;
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, pixels, 0, width);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            for (Region region : labeledRegions) {
                drawLabelForRegion(graphics, region, minBlockX, minBlockZ, blockSpanX, blockSpanZ, width, height);
            }
        } finally {
            graphics.dispose();
        }

        image.getRGB(0, 0, width, height, pixels, 0, width);
        return true;
    }

    private void drawLabelForRegion(Graphics2D graphics,
                                    Region region,
                                    int minBlockX,
                                    int minBlockZ,
                                    int blockSpanX,
                                    int blockSpanZ,
                                    int width,
                                    int height) {
        String label = abbreviateLabel(blankTo(region.getName(), "Claim"));
        int centerPixelX = (int) Math.round(((resolveMarkerCenterX(region) - minBlockX) / blockSpanX) * width);
        int centerPixelY = (int) Math.round(((resolveMarkerCenterZ(region) - minBlockZ) / blockSpanZ) * height);

        int regionPixelWidth = Math.max(18, (int) Math.round((((double) region.getMax().getX() - region.getMin().getX()) + 1.0d) / blockSpanX * width));
        int fontSize = Math.max(9, Math.min(14, regionPixelWidth / Math.max(3, label.length())));
        graphics.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        FontMetrics metrics = graphics.getFontMetrics();

        int availableWidth = Math.max(22, Math.min(width - 8, regionPixelWidth + 18));
        while (metrics.stringWidth(label) > availableWidth && label.length() > 4) {
            label = abbreviateLabel(label.substring(0, label.length() - 1));
        }

        int textWidth = metrics.stringWidth(label);
        int boxWidth = Math.min(width - 4, textWidth + 10);
        int boxHeight = metrics.getHeight() + 4;
        int boxX = clamp(centerPixelX - boxWidth / 2, 2, Math.max(2, width - boxWidth - 2));
        int boxY = clamp(centerPixelY - boxHeight / 2, 2, Math.max(2, height - boxHeight - 2));

        graphics.setColor(new java.awt.Color(12, 18, 26, 166));
        graphics.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 8, 8);
        graphics.setColor(new java.awt.Color(255, 255, 255, 226));
        graphics.drawString(label, boxX + (boxWidth - textWidth) / 2, boxY + metrics.getAscent() + 1);
    }

    private static boolean shouldDrawLabelInChunk(Region region, int chunkX, int chunkZ) {
        if (region == null || region.getMin() == null || region.getMax() == null) {
            return false;
        }

        int centerChunkX = ChunkUtil.chunkCoordinate((int) Math.floor(resolveMarkerCenterX(region)));
        int centerChunkZ = ChunkUtil.chunkCoordinate((int) Math.floor(resolveMarkerCenterZ(region)));
        return centerChunkX == chunkX && centerChunkZ == chunkZ;
    }

    private static String abbreviateLabel(String text) {
        if (text == null || text.isBlank()) {
            return "Claim";
        }
        if (text.length() <= 18) {
            return text;
        }
        return text.substring(0, 15) + "...";
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void invalidateBounds(String worldId, BlockPos min, BlockPos max) {
        LongOpenHashSet chunkIndexes = new LongOpenHashSet();
        addChunkRange(chunkIndexes, min, max);
        invalidateChunks(worldId, chunkIndexes);
    }

    private void invalidateChunks(String worldId, LongSet chunkIndexes) {
        if (worldId == null || worldId.isBlank() || chunkIndexes == null || chunkIndexes.isEmpty()) {
            return;
        }

        World world = plugin.getKnownWorld(worldId);
        if (world == null) {
            return;
        }

        LongOpenHashSet snapshot = new LongOpenHashSet(chunkIndexes);
        world.execute(() -> {
            WorldMapManager worldMapManager = world.getWorldMapManager();
            if (worldMapManager != null) {
                worldMapManager.clearImagesInChunks(snapshot);
            }

            EntityStore entityStore = world.getEntityStore();
            Store<EntityStore> store = entityStore == null ? null : entityStore.getStore();
            if (store == null) {
                return;
            }

            for (PlayerRef playerRef : world.getPlayerRefs()) {
                if (playerRef == null) {
                    continue;
                }

                Player player = store.getComponent(playerRef.getReference(), Player.getComponentType());
                if (player != null && player.getWorldMapTracker() != null) {
                    player.getWorldMapTracker().clearChunks(snapshot);
                }
            }
        });
    }

    private static void addChunkRange(LongSet target, BlockPos min, BlockPos max) {
        if (target == null || min == null || max == null) {
            return;
        }

        int minChunkX = ChunkUtil.chunkCoordinate(min.getX());
        int maxChunkX = ChunkUtil.chunkCoordinate(max.getX());
        int minChunkZ = ChunkUtil.chunkCoordinate(min.getZ());
        int maxChunkZ = ChunkUtil.chunkCoordinate(max.getZ());
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                target.add(ChunkUtil.indexChunk(chunkX, chunkZ));
            }
        }
    }

    private static int blendArgb(int baseColor, int overlayRgb, float alpha) {
        float clampedAlpha = Math.max(0F, Math.min(1F, alpha));
        int baseAlpha = (baseColor >>> 24) & 0xFF;
        int baseRed = (baseColor >> 16) & 0xFF;
        int baseGreen = (baseColor >> 8) & 0xFF;
        int baseBlue = baseColor & 0xFF;

        int overlayRed = (overlayRgb >> 16) & 0xFF;
        int overlayGreen = (overlayRgb >> 8) & 0xFF;
        int overlayBlue = overlayRgb & 0xFF;

        int red = Math.round(baseRed * (1F - clampedAlpha) + overlayRed * clampedAlpha);
        int green = Math.round(baseGreen * (1F - clampedAlpha) + overlayGreen * clampedAlpha);
        int blue = Math.round(baseBlue * (1F - clampedAlpha) + overlayBlue * clampedAlpha);
        return (baseAlpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static int darken(int rgb, float factor) {
        float clampedFactor = Math.max(0F, Math.min(1F, factor));
        int red = Math.round(((rgb >> 16) & 0xFF) * clampedFactor);
        int green = Math.round(((rgb >> 8) & 0xFF) * clampedFactor);
        int blue = Math.round((rgb & 0xFF) * clampedFactor);
        return (red << 16) | (green << 8) | blue;
    }

    private static int[] decodePixels(MapImage image, int width, int height) {
        if (image == null || width <= 0 || height <= 0) {
            return null;
        }

        int pixelCount = safePixelCount(width, height);
        if (pixelCount <= 0) {
            return null;
        }

        int[] palette = image.palette;
        if (palette == null || palette.length == 0) {
            return null;
        }

        byte[] packedIndices = image.packedIndices;
        int bitsPerIndex = Byte.toUnsignedInt(image.bitsPerIndex);
        if (packedIndices == null || packedIndices.length == 0 || bitsPerIndex <= 0) {
            if (palette.length == pixelCount) {
                return Arrays.copyOf(palette, palette.length);
            }
            return null;
        }

        BitFieldArr bitFieldArr = new BitFieldArr(bitsPerIndex, pixelCount);
        bitFieldArr.set(packedIndices);

        int[] pixels = new int[pixelCount];
        for (int pixelIndex = 0; pixelIndex < pixelCount; pixelIndex++) {
            int paletteIndex = bitFieldArr.get(pixelIndex);
            if (paletteIndex < 0 || paletteIndex >= palette.length) {
                paletteIndex = 0;
            }
            pixels[pixelIndex] = palette[paletteIndex];
        }
        return pixels;
    }

    private static void writePixels(MapImage image, int[] pixels, int width, int height) {
        if (image == null || pixels == null) {
            return;
        }

        EncodedMapImage encoded = encodePixels(pixels, width, height);
        image.palette = encoded.palette();
        image.bitsPerIndex = (byte) encoded.bitsPerIndex();
        image.packedIndices = encoded.packedIndices();
    }

    private static EncodedMapImage encodePixels(int[] pixels, int width, int height) {
        int pixelCount = safePixelCount(width, height);
        if (pixelCount <= 0 || pixels.length != pixelCount) {
            throw new IllegalArgumentException("Unexpected world map pixel buffer size.");
        }

        int[] paletteBuffer = new int[pixelCount];
        int[] paletteIndexes = new int[pixelCount];
        LinkedHashMap<Integer, Integer> paletteIndexByColor = new LinkedHashMap<>();
        int paletteSize = 0;
        for (int pixelIndex = 0; pixelIndex < pixelCount; pixelIndex++) {
            Integer paletteIndex = paletteIndexByColor.get(pixels[pixelIndex]);
            if (paletteIndex == null) {
                paletteIndex = paletteSize;
                paletteIndexByColor.put(pixels[pixelIndex], paletteIndex);
                paletteBuffer[paletteSize++] = pixels[pixelIndex];
            }
            paletteIndexes[pixelIndex] = paletteIndex;
        }

        int bitsPerIndex = calculateBitsRequired(paletteSize);
        BitFieldArr bitFieldArr = new BitFieldArr(bitsPerIndex, pixelCount);
        for (int pixelIndex = 0; pixelIndex < pixelCount; pixelIndex++) {
            bitFieldArr.set(pixelIndex, paletteIndexes[pixelIndex]);
        }

        return new EncodedMapImage(
                Arrays.copyOf(paletteBuffer, paletteSize),
                bitsPerIndex,
                bitFieldArr.get()
        );
    }

    private static int calculateBitsRequired(int paletteSize) {
        if (paletteSize <= 16) {
            return 4;
        }
        if (paletteSize <= 256) {
            return 8;
        }
        if (paletteSize <= 4096) {
            return 12;
        }
        return 16;
    }

    private static int safePixelCount(int width, int height) {
        long pixelCount = (long) width * height;
        if (pixelCount <= 0L || pixelCount > Integer.MAX_VALUE) {
            return -1;
        }
        return (int) pixelCount;
    }

    private static final class EncodedMapImage {

        private final int[] palette;
        private final int bitsPerIndex;
        private final byte[] packedIndices;

        private EncodedMapImage(int[] palette, int bitsPerIndex, byte[] packedIndices) {
            this.palette = palette;
            this.bitsPerIndex = bitsPerIndex;
            this.packedIndices = packedIndices;
        }

        private int[] palette() {
            return this.palette;
        }

        private int bitsPerIndex() {
            return this.bitsPerIndex;
        }

        private byte[] packedIndices() {
            return this.packedIndices;
        }
    }
}