package dev.thenexusgates.hyguard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.builtin.weather.components.WeatherTracker;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.protocol.SavedMovementStates;
import com.hypixel.hytale.protocol.packets.world.ClearEditorTimeOverride;
import com.hypixel.hytale.protocol.packets.world.UpdateEditorTimeOverride;
import com.hypixel.hytale.protocol.packets.world.UpdateEditorWeatherOverride;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.RemovedPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.thenexusgates.hyguard.asset.HyGuardAssetPack;
import dev.thenexusgates.hyguard.command.GuardCommand;
import dev.thenexusgates.hyguard.config.HyGuardConfig;
import dev.thenexusgates.hyguard.core.protection.BypassHandler;
import dev.thenexusgates.hyguard.core.protection.ProtectionAction;
import dev.thenexusgates.hyguard.core.protection.ProtectionEngine;
import dev.thenexusgates.hyguard.core.protection.ProtectionQuery;
import dev.thenexusgates.hyguard.core.protection.ProtectionResult;
import dev.thenexusgates.hyguard.core.region.Region;
import dev.thenexusgates.hyguard.core.region.RegionFlag;
import dev.thenexusgates.hyguard.core.region.RegionFlagValue;
import dev.thenexusgates.hyguard.core.region.RegionRole;
import dev.thenexusgates.hyguard.core.selection.SelectionPoint;
import dev.thenexusgates.hyguard.core.selection.SelectionSession;
import dev.thenexusgates.hyguard.core.selection.SelectionService;
import dev.thenexusgates.hyguard.core.selection.WandItem;
import dev.thenexusgates.hyguard.event.HyGuardBreakBlockSystem;
import dev.thenexusgates.hyguard.event.HyGuardChangeGameModeSystem;
import dev.thenexusgates.hyguard.event.DisconnectCleanupSystem;
import dev.thenexusgates.hyguard.event.HyGuardDamageBlockSystem;
import dev.thenexusgates.hyguard.event.HyGuardEntityDamageSystem;
import dev.thenexusgates.hyguard.event.HyGuardBoundaryFluidSystem;
import dev.thenexusgates.hyguard.event.HyGuardCommandListener;
import dev.thenexusgates.hyguard.event.HyGuardInteractionListener;
import dev.thenexusgates.hyguard.event.HyGuardItemSystem;
import dev.thenexusgates.hyguard.event.HyGuardKnockbackBypassMarker;
import dev.thenexusgates.hyguard.event.HyGuardKnockbackSystem;
import dev.thenexusgates.hyguard.event.HyGuardMobSpawnSystem;
import dev.thenexusgates.hyguard.event.PlayerMoveSystem;
import dev.thenexusgates.hyguard.event.HyGuardPlaceBlockSystem;
import dev.thenexusgates.hyguard.event.HyGuardUseBlockSystem;
import dev.thenexusgates.hyguard.interaction.HyGuardWandInteraction;
import dev.thenexusgates.hyguard.i18n.HyGuardText;
import dev.thenexusgates.hyguard.permission.HyGuardPermissions;
import dev.thenexusgates.hyguard.sound.HyGuardSounds;
import dev.thenexusgates.hyguard.storage.BackupManager;
import dev.thenexusgates.hyguard.storage.JsonRegionRepository;
import dev.thenexusgates.hyguard.storage.PlayerDirectory;
import dev.thenexusgates.hyguard.storage.RegionCache;
import dev.thenexusgates.hyguard.storage.RegionRepository;
import dev.thenexusgates.hyguard.ui.EntryBlacklistPage;
import dev.thenexusgates.hyguard.ui.FlagEditorPage;
import dev.thenexusgates.hyguard.ui.MemberManagerPage;
import dev.thenexusgates.hyguard.ui.RegionBrowserPage;
import dev.thenexusgates.hyguard.ui.RegionDetailPage;
import dev.thenexusgates.hyguard.visual.EnterExitMessageRenderer;
import dev.thenexusgates.hyguard.visual.SelectionVisualizer;
import dev.thenexusgates.hyguard.visual.VisualScheduler;
import dev.thenexusgates.hyguard.util.BlockPos;
import dev.thenexusgates.hyguard.util.BlockPosUtils;
import dev.thenexusgates.hyguard.util.GeometryUtils;
import dev.thenexusgates.hyguard.util.TextFormatter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class HyGuardPlugin extends JavaPlugin {

    private static volatile HyGuardPlugin instance;

    public enum RegionUpdateResult {
        SUCCESS,
        SELECTION_INCOMPLETE,
        WORLD_MISMATCH,
        OVERLAP_CONFLICT,
        HIERARCHY_CONFLICT,
        CHILD_LIMIT_REACHED
    }

    public record RegionCreationResult(Region region, RegionUpdateResult result) {
        public static RegionCreationResult success(Region region) {
            return new RegionCreationResult(region, RegionUpdateResult.SUCCESS);
        }

        public static RegionCreationResult failure(RegionUpdateResult result) {
            return new RegionCreationResult(null, result);
        }

        public boolean isSuccess() {
            return region != null && result == RegionUpdateResult.SUCCESS;
        }
    }

    public record PlayerIdentity(String uuid, String username) {
    }

    private record WandLeftClickState(String worldId, BlockPos position, long timestampMs) {
    }

    private record RegionPlacementValidation(RegionUpdateResult result, Region parentRegion) {
        private boolean isSuccess() {
            return result == RegionUpdateResult.SUCCESS;
        }
    }

    private record SelectionVisualizationContext(SelectionVisualizer.PreviewState previewState,
                                                 List<Region> childRegions) {
        private SelectionVisualizationContext {
            childRegions = childRegions == null ? List.of() : List.copyOf(childRegions);
        }
    }

    private record SelectionSnapshot(String worldId,
                                     BlockPos first,
                                     BlockPos second,
                                     boolean nextPointIsFirst) {
        private SelectionSnapshot {
            first = first == null ? null : first.copy();
            second = second == null ? null : second.copy();
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger STORAGE_LOGGER = Logger.getLogger("HyGuard");
    private static final String SERVER_REGION_OWNER_UUID = "__server__";
    private static final String SERVER_REGION_OWNER_NAME = "Server";
    private static final long WAND_LEFT_CLICK_DEBOUNCE_MS = 1500L;
    private static final String SERIALIZED_PLAYER_IDENTITY_SEPARATOR = "\n";
    private static final String SERIALIZED_PLAYER_IDENTITY_DELIMITER = "\t";

    private Path dataDirectory;
    private Path configPath;
    private HyGuardAssetPack assetPack;
    private HyGuardConfig config;
    private HyGuardText text;
    private HyGuardPermissions permissions;
    private HyGuardSounds sounds;
    private RegionRepository regionRepository;
    private RegionCache regionCache;
    private SelectionService selectionService;
    private BypassHandler bypassHandler;
    private ProtectionEngine protectionEngine;
    private PlayerDirectory playerDirectory;
    private BackupManager backupManager;
    private VisualScheduler visualScheduler;
    private EnterExitMessageRenderer enterExitMessageRenderer;
    private SelectionVisualizer selectionVisualizer;
    private PlayerMoveSystem playerMoveSystem;
    private HyGuardBoundaryFluidSystem boundaryFluidSystem;
    private DisconnectCleanupSystem disconnectCleanupSystem;
    private final ConcurrentHashMap<String, WandLeftClickState> recentWandLeftClicks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SelectionSnapshot> lastClearedSelections = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, World> knownWorlds = new ConcurrentHashMap<>();

    public HyGuardPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        instance = this;
        assetPack = HyGuardAssetPack.initialize(STORAGE_LOGGER);
        dataDirectory = assetPack.getDataRoot();
        configPath = dataDirectory.resolve("config.json");
        ensureResourceFile("config.json", configPath);
        config = loadConfig();
        text = new HyGuardText(config.chat);
        permissions = new HyGuardPermissions(config.general);
        sounds = new HyGuardSounds(config.sounds, STORAGE_LOGGER);
        regionRepository = new JsonRegionRepository(dataDirectory, STORAGE_LOGGER);
        regionCache = new RegionCache();
        regionCache.loadAll(regionRepository.loadAll());
        selectionService = new SelectionService();
        bypassHandler = new BypassHandler();
        protectionEngine = new ProtectionEngine(regionCache, bypassHandler, config);
        playerDirectory = new PlayerDirectory(dataDirectory, STORAGE_LOGGER);
        playerDirectory.loadAll();
        backupManager = new BackupManager(dataDirectory, STORAGE_LOGGER);
        backupManager.start(config.general.autoBackupIntervalMinutes, config.general.maxBackups);
        visualScheduler = new VisualScheduler(HytaleServer.SCHEDULED_EXECUTOR);
        enterExitMessageRenderer = new EnterExitMessageRenderer(this);
        selectionVisualizer = new SelectionVisualizer(visualScheduler);
        playerMoveSystem = new PlayerMoveSystem(this, enterExitMessageRenderer);
        boundaryFluidSystem = new HyGuardBoundaryFluidSystem(this);
        disconnectCleanupSystem = new DisconnectCleanupSystem(this);

        getCodecRegistry(Interaction.CODEC).register(
            HyGuardWandInteraction.ID,
            HyGuardWandInteraction.class,
            HyGuardWandInteraction.CODEC
        );
        getCommandRegistry().registerCommand(new GuardCommand(this));
        getEventRegistry().registerGlobal(com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent.class, disconnectCleanupSystem::handle);
        getEventRegistry().registerGlobal(RemovedPlayerFromWorldEvent.class, disconnectCleanupSystem::handle);
        getEventRegistry().registerGlobal(DrainPlayerFromWorldEvent.class, disconnectCleanupSystem::handle);
        getEventRegistry().registerGlobal(EventPriority.FIRST, PlayerChatEvent.class, new HyGuardCommandListener(this)::handle);
        getEventRegistry().registerGlobal(EventPriority.FIRST, PlayerInteractEvent.class, new HyGuardInteractionListener(this)::handle);

        if (EntityModule.get() != null) {
            HyGuardKnockbackBypassMarker.register(
                getEntityStoreRegistry().registerComponent(
                    HyGuardKnockbackBypassMarker.class,
                    HyGuardKnockbackBypassMarker::new
                )
            );
            getEntityStoreRegistry().registerSystem(new HyGuardDamageBlockSystem(this));
            getEntityStoreRegistry().registerSystem(new HyGuardBreakBlockSystem(this));
            getEntityStoreRegistry().registerSystem(new HyGuardPlaceBlockSystem(this));
            getEntityStoreRegistry().registerSystem(new HyGuardUseBlockSystem(this));
            getEntityStoreRegistry().registerSystem(new HyGuardChangeGameModeSystem(this));
            getEntityStoreRegistry().registerSystem(new HyGuardEntityDamageSystem(this));
            getEntityStoreRegistry().registerSystem(new HyGuardKnockbackSystem(this));
            getEntityStoreRegistry().registerSystem(new HyGuardItemSystem.DropSystem(this));
            getEntityStoreRegistry().registerSystem(new HyGuardItemSystem.PickupGuardSystem(this));
            getEntityStoreRegistry().registerSystem(new HyGuardItemSystem.PickupSystem(this));
            getEntityStoreRegistry().registerSystem(playerMoveSystem);
            getEntityStoreRegistry().registerSystem(boundaryFluidSystem);
            getLogger().at(Level.INFO).log(HyGuardMobSpawnSystem.NO_API_REASON);
        }

        long worldCount = regionCache.allRegions().stream()
            .map(Region::getWorldId)
            .filter(worldId -> worldId != null && !worldId.isBlank())
            .distinct()
            .count();
        getLogger().at(Level.INFO).log("[HyGuard] Enabled. Regions loaded=%s worlds=%s selectionVisuals=%s dataRoot=%s packRoot=%s",
                regionCache.allRegions().size(),
                worldCount,
                selectionVisualizer.isSupported(),
                dataDirectory,
                assetPack.getPackRoot());
    }

    @Override
    protected void shutdown() {
        if (regionRepository != null) {
            regionRepository.shutdown();
        }
        if (playerDirectory != null) {
            playerDirectory.shutdown();
        }
        if (backupManager != null) {
            backupManager.stop();
            backupManager.shutdown();
        }
        if (selectionVisualizer != null) {
            selectionVisualizer.shutdown();
        }
        if (visualScheduler != null) {
            visualScheduler.cancelAll();
        }
        if (playerMoveSystem != null) {
            playerMoveSystem.stop();
        }
        if (instance == this) {
            instance = null;
        }
        getLogger().at(Level.INFO).log("[HyGuard] Shutdown complete.");
    }

    public static HyGuardPlugin getInstance() {
        return instance;
    }

    public HyGuardConfig getConfigSnapshot() {
        return config;
    }

    public void clearTransientPlayerState(PlayerRef playerRef) {
        if (playerRef == null || playerRef.getUuid() == null) {
            return;
        }
        String playerUuid = playerRef.getUuid().toString();
        selectionService.clear(playerUuid);
        bypassHandler.clear(playerUuid);
        recentWandLeftClicks.remove(playerUuid);
        lastClearedSelections.remove(playerUuid);
        if (selectionVisualizer != null) {
            selectionVisualizer.clearPlayer(playerUuid);
        }
        if (playerMoveSystem != null) {
            playerMoveSystem.clearPlayer(playerUuid);
        }
    }

    public SelectionService getSelectionService() {
        return selectionService;
    }

    public RegionCache getRegionCache() {
        return regionCache;
    }

    public boolean hasAdminPermission(PlayerRef playerRef) {
        return permissions.hasAdmin(playerRef);
    }

    public boolean hasBypassPermission(PlayerRef playerRef) {
        return permissions.hasBypass(playerRef);
    }

    public boolean canBypassProtection(PlayerRef playerRef) {
        return playerRef != null
                && playerRef.getUuid() != null
                && bypassHandler.isBypassing(playerRef.getUuid().toString());
    }

    public boolean hasUsePermission(PlayerRef playerRef) {
        return permissions.hasUse(playerRef);
    }

    public boolean hasPermission(PlayerRef playerRef, String permission) {
        return permissions.has(playerRef, permission);
    }

    public boolean hasPermissionOrAdmin(PlayerRef playerRef, String permission) {
        return hasAdminPermission(playerRef) || hasPermission(playerRef, permission);
    }

    public boolean toggleBypass(PlayerRef playerRef) {
        return bypassHandler.toggle(playerRef.getUuid().toString());
    }

    public boolean toggleAdminOverride(PlayerRef playerRef) {
        return toggleBypass(playerRef);
    }

    public String text(PlayerRef playerRef, String key, Map<String, String> replacements) {
        return text.text(playerRef, key, replacements);
    }

    public String message(PlayerRef playerRef, String key, Map<String, String> replacements) {
        return text(playerRef, key, replacements);
    }

    public String message(String template, Map<String, String> replacements) {
        return TextFormatter.format(template, replacements);
    }

    public void send(PlayerRef playerRef, String template) {
        send(playerRef, template, Map.of());
    }

    public void send(PlayerRef playerRef, String template, Map<String, String> replacements) {
        if (playerRef != null) {
            playerRef.sendMessage(text.chat(playerRef, template, replacements, true));
        }
    }

    public void sendRaw(PlayerRef playerRef, String rawText) {
        sendRaw(playerRef, rawText, false);
    }

    public void sendRaw(PlayerRef playerRef, String rawText, boolean includePrefix) {
        if (playerRef != null) {
            playerRef.sendMessage(text.rawChat(rawText, includePrefix));
        }
    }

    public Message rawMessage(String template, Map<String, String> replacements, boolean includePrefix) {
        return text.rawChat(TextFormatter.format(template, replacements), includePrefix);
    }

    public Region findRegionByName(String worldId, String name) {
        return regionCache.findByName(worldId, name);
    }

    public String getRegionNameById(String regionId, String worldId) {
        if (regionId == null || regionId.isBlank()) {
            return "unknown";
        }
        Region region = regionCache.findById(regionId);
        if (region == null) {
            return regionId;
        }
        if (worldId != null && !worldId.isBlank() && region.getWorldId() != null && !region.getWorldId().equalsIgnoreCase(worldId)) {
            return regionId;
        }
        return region.getName() == null || region.getName().isBlank() ? regionId : region.getName();
    }

    public Region findRegionById(String regionId) {
        return regionCache.findById(regionId);
    }

    public boolean isValidRegionName(String name) {
        if (name == null) {
            return false;
        }
        if (name.length() < config.limits.regionNameMinLength || name.length() > config.limits.regionNameMaxLength) {
            return false;
        }
        return name.matches(config.limits.regionNamePattern);
    }

    public int countOwnedRegions(String ownerUuid) {
        if (ownerUuid == null || ownerUuid.isBlank()) {
            return 0;
        }
        return (int) regionCache.allRegions().stream()
                .filter(region -> ownerUuid.equalsIgnoreCase(region.getOwnerUuid()))
                .count();
    }

    public RegionCreationResult createRegion(PlayerRef playerRef, String worldId, String name) {
        return createRegion(playerRef, worldId, name, playerRef.getUuid().toString(), playerRef.getUsername());
    }

    public RegionCreationResult createServerRegion(PlayerRef playerRef, String worldId, String name) {
        return createRegion(playerRef, worldId, name, SERVER_REGION_OWNER_UUID, SERVER_REGION_OWNER_NAME);
    }

    private RegionCreationResult createRegion(PlayerRef playerRef, String worldId, String name, String ownerUuid, String ownerName) {
        String playerUuid = playerRef.getUuid().toString();
        var session = selectionService.get(playerUuid);
        if (session == null || !session.isComplete() || !worldId.equalsIgnoreCase(session.getWorldId())) {
            return RegionCreationResult.failure(RegionUpdateResult.SELECTION_INCOMPLETE);
        }

        RegionPlacementValidation validation = validateRegionPlacement(
                playerRef,
                worldId,
                session.getFirstPoint().getPosition(),
                session.getSecondPoint().getPosition(),
                null
        );
        if (!validation.isSuccess()) {
            return RegionCreationResult.failure(validation.result());
        }

        Region parentRegion = validation.parentRegion();
        String effectiveOwnerUuid = ownerUuid;
        String effectiveOwnerName = ownerName;
        if (isServerOwned(parentRegion)) {
            effectiveOwnerUuid = parentRegion.getOwnerUuid();
            effectiveOwnerName = parentRegion.getOwnerName();
        }

        Region region = Region.create(
                name,
                effectiveOwnerUuid,
                effectiveOwnerName,
                worldId,
                session.getFirstPoint().getPosition(),
                session.getSecondPoint().getPosition()
        );
        region.putFlag(RegionFlag.BLOCK_BREAK, new RegionFlagValue(RegionFlagValue.Mode.valueOf(config.defaults.blockBreak)));
        region.putFlag(RegionFlag.BLOCK_PLACE, new RegionFlagValue(RegionFlagValue.Mode.valueOf(config.defaults.blockPlace)));
        region.putFlag(RegionFlag.BLOCK_INTERACT, new RegionFlagValue(RegionFlagValue.Mode.valueOf(config.defaults.blockInteract)));

        regionCache.put(region);
        regionRepository.saveRegionAsync(region);
        return RegionCreationResult.success(region);
    }

    public Region createGlobalRegion(PlayerRef playerRef, String worldId, String name) {
        BlockPos spawnPoint = resolvePlayerBlockPosition(playerRef);

        Region region = Region.createGlobal(name, SERVER_REGION_OWNER_UUID, SERVER_REGION_OWNER_NAME, worldId, spawnPoint);
        region.putFlag(RegionFlag.BLOCK_BREAK, new RegionFlagValue(RegionFlagValue.Mode.valueOf(config.defaults.blockBreak)));
        region.putFlag(RegionFlag.BLOCK_PLACE, new RegionFlagValue(RegionFlagValue.Mode.valueOf(config.defaults.blockPlace)));
        region.putFlag(RegionFlag.BLOCK_INTERACT, new RegionFlagValue(RegionFlagValue.Mode.valueOf(config.defaults.blockInteract)));
        regionCache.put(region);
        regionRepository.saveRegionAsync(region);
        return region;
    }

    public boolean deleteRegion(Region region) {
        if (region == null || hasChildRegions(region)) {
            return false;
        }
        Region removed = regionCache.remove(region.getId());
        if (removed == null) {
            return false;
        }
        regionRepository.deleteRegionAsync(removed);
        return true;
    }

    public void saveRegion(Region region) {
        if (region == null) {
            return;
        }
        region.touch();
        regionCache.put(region);
        regionRepository.saveRegionAsync(region);
    }

    public void flushRegionSaves() {
        regionRepository.flush();
    }

    public void reloadState() {
        config = loadConfig();
        text = new HyGuardText(config.chat);
        permissions = new HyGuardPermissions(config.general);
        sounds = new HyGuardSounds(config.sounds, STORAGE_LOGGER);
        regionCache.loadAll(regionRepository.loadAll());
        protectionEngine = new ProtectionEngine(regionCache, bypassHandler, config);
        playerDirectory.loadAll();
        backupManager.start(config.general.autoBackupIntervalMinutes, config.general.maxBackups);
    }

    public boolean canManageRegion(PlayerRef playerRef, Region region) {
        if (playerRef == null || region == null) {
            return false;
        }
        if (hasAdminPermission(playerRef)) {
            return true;
        }
        RegionRole role = region.getRoleFor(playerRef.getUuid().toString());
        return role != null && role.isAtLeast(RegionRole.MANAGER);
    }

    public boolean hasChildRegions(Region region) {
        return region != null && !region.getChildRegionIds().isEmpty();
    }

    public List<Region> getWorldRegions(String worldId) {
        return regionCache.allRegions().stream()
                .filter(region -> region.getWorldId() != null && region.getWorldId().equalsIgnoreCase(worldId))
                .sorted(Comparator.comparing(Region::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

        public List<String> getKnownWorldIds() {
        return regionCache.allRegions().stream()
            .map(Region::getWorldId)
            .filter(worldId -> worldId != null && !worldId.isBlank())
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
        }

        public List<Region> getAllRegions() {
        return regionCache.allRegions().stream()
            .sorted(Comparator
                .comparing(Region::getWorldId, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(Region::getName, String.CASE_INSENSITIVE_ORDER))
            .toList();
        }

    public List<Region> getDisplayRootRegions(String worldId) {
        return getWorldRegions(worldId).stream()
                .filter(region -> !isNestedDisplayChild(region))
                .toList();
    }

        public List<Region> getAllDisplayRootRegions() {
        return getAllRegions().stream()
            .filter(region -> !isNestedDisplayChild(region))
            .toList();
        }

    public List<Region> getDisplayChildRegions(Region parentRegion) {
        if (parentRegion == null || parentRegion.isGlobal()) {
            return List.of();
        }

        return parentRegion.getChildRegionIds().stream()
                .map(regionCache::findById)
                .filter(Objects::nonNull)
                .filter(region -> !region.isGlobal())
                .sorted(Comparator.comparing(Region::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<Region> getRegionsAt(String worldId, BlockPos position) {
        return regionCache.getRegionsAt(worldId, position);
    }

    public List<Region> getRegionsAt(String worldId, List<BlockPos> positions) {
        if (worldId == null || positions == null || positions.isEmpty()) {
            return List.of();
        }

        Map<String, Region> regionsById = new HashMap<>();
        for (BlockPos position : positions) {
            if (position == null) {
                continue;
            }
            for (Region region : regionCache.getRegionsAt(worldId, position)) {
                regionsById.putIfAbsent(region.getId(), region);
            }
        }

        ArrayList<Region> regions = new ArrayList<>(regionsById.values());
        regions.sort(Comparator
                .comparingLong(Region::getVolume)
                .thenComparing(Comparator.comparingInt(Region::getPriority).reversed())
                .thenComparing(Region::getName, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(regions);
    }

    public boolean loadRegionSelection(PlayerRef playerRef, Region region) {
        if (playerRef == null || region == null || region.isGlobal() || region.getMin() == null || region.getMax() == null) {
            return false;
        }
        String playerUuid = playerRef.getUuid().toString();
        selectionService.setFirstPoint(playerUuid, region.getWorldId(), region.getMin().copy());
        selectionService.setSecondPoint(playerUuid, region.getWorldId(), region.getMax().copy());
        refreshSelectionVisualization(playerRef);
        return true;
    }

    public void cycleSelectionPoint(PlayerRef playerRef, String worldId, BlockPos position) {
        if (playerRef == null || playerRef.getUuid() == null || worldId == null || position == null) {
            return;
        }

        String playerUuid = playerRef.getUuid().toString();
        boolean firstPoint = selectionService.setNextPoint(playerUuid, worldId, position);
        if (firstPoint) {
            send(playerRef, config.messages.selectionPointOneSet, Map.of("pos", position.toString()));
            sounds.play(playerRef, HyGuardSounds.Cue.SELECTION_POINT_ONE);
        } else {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("pos", position.toString());
            replacements.put("size", formatSelectionSize(selectionService.get(playerUuid)));
            send(playerRef, config.messages.selectionPointTwoSet, replacements);
            sounds.play(playerRef, HyGuardSounds.Cue.SELECTION_POINT_TWO);
        }
        refreshSelectionVisualization(playerRef);
    }

    public void handleWandLeftClick(PlayerRef playerRef, String worldId, BlockPos position) {
        if (playerRef == null || playerRef.getUuid() == null || worldId == null || position == null) {
            return;
        }

        String playerUuid = playerRef.getUuid().toString();
        long now = System.currentTimeMillis();
        WandLeftClickState previous = recentWandLeftClicks.get(playerUuid);
        if (previous != null
                && previous.worldId() != null
                && previous.worldId().equalsIgnoreCase(worldId)
                && previous.position().equals(position)
                && now - previous.timestampMs() < WAND_LEFT_CLICK_DEBOUNCE_MS) {
            return;
        }

        recentWandLeftClicks.put(playerUuid, new WandLeftClickState(worldId, position.copy(), now));
        cycleSelectionPoint(playerRef, worldId, position);
    }

    public void setSelectionPoint(PlayerRef playerRef, String worldId, BlockPos position, boolean firstPoint) {
        if (playerRef == null || playerRef.getUuid() == null || worldId == null || position == null) {
            return;
        }

        String playerUuid = playerRef.getUuid().toString();
        if (firstPoint) {
            selectionService.setFirstPoint(playerUuid, worldId, position);
            send(playerRef, config.messages.selectionPointOneSet, Map.of("pos", position.toString()));
            sounds.play(playerRef, HyGuardSounds.Cue.SELECTION_POINT_ONE);
        } else {
            selectionService.setSecondPoint(playerUuid, worldId, position);
            Map<String, String> replacements = new HashMap<>();
            replacements.put("pos", position.toString());
            replacements.put("size", formatSelectionSize(selectionService.get(playerUuid)));
            send(playerRef, config.messages.selectionPointTwoSet, replacements);
            sounds.play(playerRef, HyGuardSounds.Cue.SELECTION_POINT_TWO);
        }
        refreshSelectionVisualization(playerRef);
    }

    public void clearSelection(PlayerRef playerRef) {
        clearSelection(playerRef, true);
    }

    public void toggleSelectionClear(PlayerRef playerRef) {
        if (playerRef == null || playerRef.getUuid() == null) {
            return;
        }

        SelectionSession session = selectionService.get(playerRef.getUuid().toString());
        if (session != null && session.hasAnyPoint()) {
            clearSelection(playerRef, true);
            return;
        }

        restoreLastClearedSelection(playerRef);
    }

    private void clearSelection(PlayerRef playerRef, boolean rememberForRestore) {
        if (playerRef == null || playerRef.getUuid() == null) {
            return;
        }

        String playerUuid = playerRef.getUuid().toString();
        SelectionSession session = selectionService.get(playerUuid);
        if (rememberForRestore && session != null && session.hasAnyPoint()) {
            lastClearedSelections.put(playerUuid, snapshotSelection(session));
        }
        selectionService.clear(playerUuid);
        recentWandLeftClicks.remove(playerUuid);
        if (selectionVisualizer != null) {
            selectionVisualizer.clearPlayer(playerUuid);
        }
        if (session != null && session.hasAnyPoint()) {
            send(playerRef, config.messages.selectionCleared);
            sounds.play(playerRef, HyGuardSounds.Cue.DELETE);
        }
    }

    private void restoreLastClearedSelection(PlayerRef playerRef) {
        if (playerRef == null || playerRef.getUuid() == null) {
            return;
        }

        String playerUuid = playerRef.getUuid().toString();
        SelectionSnapshot snapshot = lastClearedSelections.remove(playerUuid);
        if (snapshot == null || snapshot.worldId() == null || (snapshot.first() == null && snapshot.second() == null)) {
            return;
        }

        selectionService.clear(playerUuid);
        if (snapshot.first() != null) {
            selectionService.setFirstPoint(playerUuid, snapshot.worldId(), snapshot.first());
        }
        if (snapshot.second() != null) {
            selectionService.setSecondPoint(playerUuid, snapshot.worldId(), snapshot.second());
        }
        SelectionSession restored = selectionService.getOrCreate(playerUuid);
        restored.setNextPointIsFirst(snapshot.nextPointIsFirst());
        refreshSelectionVisualization(playerRef);
        sendSelectionRestoreFeedback(playerRef, restored);
        sounds.play(playerRef, HyGuardSounds.Cue.SELECTION_POINT_TWO);
    }

    private SelectionSnapshot snapshotSelection(SelectionSession session) {
        if (session == null) {
            return null;
        }

        return new SelectionSnapshot(
                session.getWorldId(),
                session.getFirstPoint() == null ? null : session.getFirstPoint().getPosition(),
                session.getSecondPoint() == null ? null : session.getSecondPoint().getPosition(),
                session.isNextPointFirst()
        );
    }

    private void sendSelectionRestoreFeedback(PlayerRef playerRef, SelectionSession session) {
        if (playerRef == null || session == null || !session.hasAnyPoint()) {
            return;
        }

        if (session.isComplete()) {
            BlockPos min = BlockPosUtils.min(session.getFirstPoint().getPosition(), session.getSecondPoint().getPosition());
            BlockPos max = BlockPosUtils.max(session.getFirstPoint().getPosition(), session.getSecondPoint().getPosition());
            send(playerRef, config.messages.selectionUpdated, Map.of(
                    "min", min.toString(),
                    "max", max.toString(),
                    "size", formatSelectionSize(session)
            ));
            return;
        }

        SelectionPoint point = session.getFirstPoint() != null ? session.getFirstPoint() : session.getSecondPoint();
        if (point != null) {
            send(playerRef, config.messages.selectionPointOneSet, Map.of("pos", point.getPosition().toString()));
        }
    }

    public void refreshSelectionVisualization(PlayerRef playerRef) {
        if (selectionVisualizer == null || playerRef == null || playerRef.getUuid() == null) {
            return;
        }
        SelectionSession session = selectionService.get(playerRef.getUuid().toString());
        if (session == null || !session.hasAnyPoint()) {
            selectionVisualizer.clearPlayer(playerRef.getUuid().toString());
            return;
        }
        SelectionVisualizationContext context = buildSelectionVisualizationContext(playerRef, session);
        selectionVisualizer.updateVisualization(playerRef, session, context.previewState(), context.childRegions());
    }

    private SelectionVisualizationContext buildSelectionVisualizationContext(PlayerRef playerRef, SelectionSession session) {
        if (session == null || !session.isComplete()) {
            return new SelectionVisualizationContext(SelectionVisualizer.PreviewState.NEUTRAL, List.of());
        }

        BlockPos selectionMin = BlockPosUtils.min(session.getFirstPoint().getPosition(), session.getSecondPoint().getPosition());
        BlockPos selectionMax = BlockPosUtils.max(session.getFirstPoint().getPosition(), session.getSecondPoint().getPosition());
        Region exactRegion = findExactRegionByBounds(session.getWorldId(), selectionMin, selectionMax);
        if (exactRegion != null && !exactRegion.isGlobal() && canManageRegion(playerRef, exactRegion)) {
            List<Region> childRegions = isPrimaryRegion(exactRegion)
                    ? getDisplayChildRegions(exactRegion)
                    : List.of();
            return new SelectionVisualizationContext(SelectionVisualizer.PreviewState.CLAIMED, childRegions);
        }

        RegionPlacementValidation validation = validateRegionPlacement(
                playerRef,
                session.getWorldId(),
                session.getFirstPoint().getPosition(),
                session.getSecondPoint().getPosition(),
                null
        );
        return new SelectionVisualizationContext(
                validation.isSuccess() ? SelectionVisualizer.PreviewState.AVAILABLE : SelectionVisualizer.PreviewState.INVALID,
                List.of()
        );
    }

    private Region findExactRegionByBounds(String worldId, BlockPos min, BlockPos max) {
        if (worldId == null || min == null || max == null) {
            return null;
        }

        for (Region region : getWorldRegions(worldId)) {
            if (region.isGlobal() || region.getMin() == null || region.getMax() == null) {
                continue;
            }
            if (region.getMin().equals(min) && region.getMax().equals(max)) {
                return region;
            }
        }
        return null;
    }

    private boolean isNestedDisplayChild(Region region) {
        if (region == null) {
            return false;
        }

        String parentRegionId = region.getParentRegionId();
        if (parentRegionId == null || parentRegionId.isBlank()) {
            return false;
        }

        Region parent = regionCache.findById(parentRegionId);
        return parent != null && !parent.isGlobal();
    }

    public Region getSelectionPlacementParent(PlayerRef playerRef, String worldId) {
        if (playerRef == null || playerRef.getUuid() == null || worldId == null) {
            return null;
        }

        SelectionSession session = selectionService.get(playerRef.getUuid().toString());
        if (session == null || !session.isComplete() || session.getWorldId() == null || !worldId.equalsIgnoreCase(session.getWorldId())) {
            return null;
        }

        return validateRegionPlacement(
                playerRef,
                worldId,
                session.getFirstPoint().getPosition(),
                session.getSecondPoint().getPosition(),
                null
        ).parentRegion();
    }

    public boolean selectionInheritsServerOwnership(PlayerRef playerRef, String worldId) {
        return isServerOwned(getSelectionPlacementParent(playerRef, worldId));
    }

    public String formatSelectionSize(SelectionSession session) {
        if (session == null || !session.isComplete()) {
            return "incomplete";
        }
        BlockPos min = BlockPosUtils.min(session.getFirstPoint().getPosition(), session.getSecondPoint().getPosition());
        BlockPos max = BlockPosUtils.max(session.getFirstPoint().getPosition(), session.getSecondPoint().getPosition());
        long width = (long) max.getX() - min.getX() + 1L;
        long height = (long) max.getY() - min.getY() + 1L;
        long depth = (long) max.getZ() - min.getZ() + 1L;
        long volume = width * height * depth;
        return width + "x" + height + "x" + depth + " (" + volume + " blocks)";
    }

    public RegionUpdateResult redefineRegion(PlayerRef playerRef, String worldId, Region region) {
        if (playerRef == null || region == null || region.isGlobal()) {
            return RegionUpdateResult.SELECTION_INCOMPLETE;
        }

        var session = selectionService.get(playerRef.getUuid().toString());
        if (session == null || !session.isComplete()) {
            return RegionUpdateResult.SELECTION_INCOMPLETE;
        }
        if (!worldId.equalsIgnoreCase(session.getWorldId()) || !region.getWorldId().equalsIgnoreCase(session.getWorldId())) {
            return RegionUpdateResult.WORLD_MISMATCH;
        }
        RegionPlacementValidation validation = validateRegionPlacement(
                playerRef,
                region.getWorldId(),
                session.getFirstPoint().getPosition(),
                session.getSecondPoint().getPosition(),
                region.getId()
        );
        if (!validation.isSuccess()) {
            return validation.result();
        }

        region.setBounds(session.getFirstPoint().getPosition(), session.getSecondPoint().getPosition());
        saveRegion(region);
        return RegionUpdateResult.SUCCESS;
    }

    public boolean canManageMembership(PlayerRef playerRef, Region region) {
        if (playerRef == null || region == null) {
            return false;
        }
        if (hasAdminPermission(playerRef)) {
            return true;
        }
        RegionRole role = region.getRoleFor(playerRef.getUuid().toString());
        return role == RegionRole.OWNER || role == RegionRole.CO_OWNER;
    }

    public boolean canManageSpawn(PlayerRef playerRef, Region region) {
        return canManageMembership(playerRef, region);
    }

    public boolean canTeleportToRegion(PlayerRef playerRef, Region region) {
        if (playerRef == null || region == null) {
            return false;
        }
        if (hasAdminPermission(playerRef)) {
            return true;
        }
        RegionRole role = region.getRoleFor(playerRef.getUuid().toString());
        return role != null && role != RegionRole.VISITOR;
    }

    public void rememberPlayer(PlayerRef playerRef) {
        if (playerDirectory != null) {
            playerDirectory.remember(playerRef);
        }
    }

    public PlayerIdentity resolvePlayerIdentity(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return null;
        }

        Universe universe = Universe.get();
        if (universe != null) {
            PlayerRef online = universe.getPlayerByUsername(playerName, NameMatching.EXACT_IGNORE_CASE);
            if (online == null) {
                online = universe.getPlayerByUsername(playerName, NameMatching.STARTS_WITH_IGNORE_CASE);
            }
            if (online != null) {
                rememberPlayer(online);
                return new PlayerIdentity(online.getUuid().toString(), online.getUsername());
            }
        }

        PlayerDirectory.StoredPlayer storedPlayer = playerDirectory == null ? null : playerDirectory.findByName(playerName);
        return storedPlayer == null ? null : new PlayerIdentity(storedPlayer.uuid(), storedPlayer.username());
    }

    public List<PlayerIdentity> getKnownPlayerIdentities() {
        if (playerDirectory == null) {
            return List.of();
        }
        return playerDirectory.allPlayers().stream()
                .map(storedPlayer -> new PlayerIdentity(storedPlayer.uuid(), storedPlayer.username()))
                .sorted(Comparator.comparing(PlayerIdentity::username, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<PlayerIdentity> getEntryBlacklist(Region region) {
        if (region == null) {
            return List.of();
        }
        RegionFlagValue flagValue = region.getFlags().get(RegionFlag.ENTRY_BLACKLIST);
        if (flagValue == null || flagValue.getTextValue() == null || flagValue.getTextValue().isBlank()) {
            return List.of();
        }
        return decodePlayerIdentities(flagValue.getTextValue());
    }

    public boolean isEntryBlacklisted(Region region, PlayerRef playerRef) {
        if (region == null || playerRef == null || playerRef.getUuid() == null) {
            return false;
        }
        String playerUuid = playerRef.getUuid().toString();
        for (PlayerIdentity identity : getEntryBlacklist(region)) {
            if (identity.uuid() != null && identity.uuid().equalsIgnoreCase(playerUuid)) {
                return true;
            }
        }
        return false;
    }

    public void setEntryBlacklist(Region region, Collection<PlayerIdentity> identities) {
        if (region == null) {
            return;
        }

        String encoded = encodePlayerIdentities(identities);
        if (encoded.isBlank()) {
            region.removeFlag(RegionFlag.ENTRY_BLACKLIST);
            return;
        }

        region.putFlag(RegionFlag.ENTRY_BLACKLIST, new RegionFlagValue(RegionFlagValue.Mode.ALLOW, encoded));
    }

    public int countStoredPlayerIdentities(String raw) {
        return decodePlayerIdentities(raw).size();
    }

    public String sanitizeCommandBlacklist(String raw) {
        List<String> patterns = decodeCommandBlacklist(raw);
        return patterns.isEmpty() ? "" : String.join(", ", patterns);
    }

    public int countConfiguredCommandBlacklist(String raw) {
        return decodeCommandBlacklist(raw).size();
    }

    public boolean isCommandBlacklisted(PlayerRef playerRef, String worldId, BlockPos position, String rawCommand) {
        if (playerRef == null || worldId == null || position == null || rawCommand == null) {
            return false;
        }
        if (canBypassProtection(playerRef)) {
            return false;
        }

        String normalizedCommand = normalizeIssuedCommand(rawCommand);
        if (normalizedCommand.isBlank()) {
            return false;
        }

        for (Region region : getRegionsAt(worldId, position)) {
            RegionFlagValue flagValue = region.getFlags().get(RegionFlag.COMMAND_BLACKLIST);
            if (flagValue == null || flagValue.getMode() == RegionFlagValue.Mode.INHERIT) {
                continue;
            }
            for (String pattern : decodeCommandBlacklist(flagValue.getTextValue())) {
                if (matchesCommandBlacklistPattern(normalizedCommand, pattern)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String resolvePlayerWorldId(PlayerRef playerRef) {
        if (playerRef == null || playerRef.getWorldUuid() == null) {
            return null;
        }
        Universe universe = Universe.get();
        World world = universe == null ? null : universe.getWorld(playerRef.getWorldUuid());
        rememberWorld(world);
        return world == null ? null : world.getName();
    }

    public void rememberWorld(World world) {
        if (world == null || world.getName() == null || world.getName().isBlank()) {
            return;
        }
        knownWorlds.put(world.getName(), world);
    }

    public String resolveRegionWorldId(String worldId, BlockPos position) {
        if (position == null) {
            return worldId;
        }

        if (worldId != null && !worldId.isBlank() && !getRegionsAt(worldId, position).isEmpty()) {
            return worldId;
        }

        String resolved = null;
        for (String candidateWorldId : getKnownWorldIds()) {
            if (candidateWorldId == null || candidateWorldId.isBlank()) {
                continue;
            }
            if (worldId != null && candidateWorldId.equalsIgnoreCase(worldId)) {
                continue;
            }
            if (getRegionsAt(candidateWorldId, position).isEmpty()) {
                continue;
            }
            if (resolved != null) {
                return worldId;
            }
            resolved = candidateWorldId;
        }
        return resolved == null ? worldId : resolved;
    }

    public GameMode parseConfiguredGameMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return switch (raw.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "creative" -> GameMode.Creative;
            case "adventure", "survival" -> GameMode.Adventure;
            default -> null;
        };
    }

    public Integer parseConfiguredWeatherLock(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        return switch (raw.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "clear", "sunny" -> 0;
            case "rain", "rainy" -> 1;
            case "storm", "thunder", "thunderstorm" -> 2;
            default -> {
                try {
                    int index = Integer.parseInt(raw.trim());
                    yield index < 0 ? null : index;
                } catch (NumberFormatException numberFormatException) {
                    yield null;
                }
            }
        };
    }

    public LocalTime parseConfiguredTimeLock(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String trimmed = raw.trim();
        try {
            if (trimmed.contains(":")) {
                String[] parts = trimmed.split(":", 2);
                if (parts.length != 2) {
                    return null;
                }
                return LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            }
            return LocalTime.of(Integer.parseInt(trimmed), 0);
        } catch (RuntimeException runtimeException) {
            return null;
        }
    }

    public GameMode resolveEnforcedGameMode(PlayerRef playerRef, String worldId, BlockPos position) {
        if (playerRef == null || worldId == null || position == null) {
            return null;
        }
        return resolveEnforcedGameMode(playerRef, getRegionsAt(worldId, position));
    }

    public void applyRegionState(Store<EntityStore> store,
                                 Ref<EntityStore> entityRef,
                                 PlayerRef playerRef,
                                 String worldId,
                                 BlockPos position) {
        if (store == null || entityRef == null || playerRef == null || worldId == null || position == null) {
            return;
        }

        String resolvedWorldId = resolveRegionWorldId(worldId, position);
        List<Region> regions = getRegionsAt(resolvedWorldId, position);
        applyRegionGameMode(store, entityRef, playerRef, regions);
        applyRegionFly(store, entityRef, playerRef, regions);
        applyRegionTimeAndWeather(store, entityRef, playerRef, regions);
    }

    public ProtectionResult evaluate(PlayerRef playerRef, String worldId, BlockPos position, ProtectionAction action) {
        if (playerRef == null) {
            return ProtectionResult.allow();
        }
        return protectionEngine.evaluate(new ProtectionQuery(
                playerRef.getUuid().toString(),
                resolveRegionWorldId(worldId, position),
                position,
                action
        ));
    }

    public boolean isFlagAllowed(PlayerRef playerRef,
                                 String worldId,
                                 BlockPos position,
                                 RegionFlag flag,
                                 RegionFlagValue.Mode defaultMode) {
        if (worldId == null || position == null) {
            return defaultMode == RegionFlagValue.Mode.ALLOW;
        }
        return isFlagAllowed(playerRef, getRegionsAt(resolveRegionWorldId(worldId, position), position), flag, defaultMode);
    }

    public boolean isFlagAllowed(PlayerRef playerRef,
                                 Region region,
                                 RegionFlag flag,
                                 RegionFlagValue.Mode defaultMode) {
        if (region == null) {
            return defaultMode == RegionFlagValue.Mode.ALLOW;
        }
        return isFlagAllowed(playerRef, List.of(region), flag, defaultMode);
    }

    public boolean teleportToRegion(Store<EntityStore> store, Ref<EntityStore> entityRef, PlayerRef playerRef, Region region) {
        if (store == null || entityRef == null || playerRef == null || region == null) {
            return false;
        }
        if (region.isGlobal() && region.getSpawnPoint() == null) {
            return false;
        }
        TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
        if (transform == null) {
            return false;
        }
        Vector3d destination = resolveRegionTeleportPosition(region);
        World currentWorld = store.getExternalData() == null ? null : store.getExternalData().getWorld();
        rememberWorld(currentWorld);
        World targetWorld = resolveTargetWorld(region.getWorldId(), currentWorld);
        if (targetWorld == null && currentWorld == null) {
            transform.teleportPosition(destination);
            return true;
        }
        if (targetWorld == null) {
            return false;
        }
        store.putComponent(entityRef, Teleport.getComponentType(), Teleport.createForPlayer(targetWorld, destination, transform.getRotation()));
        return true;
    }

    public boolean teleportToRegion(CommandBuffer<EntityStore> commandBuffer,
                                    Store<EntityStore> store,
                                    Ref<EntityStore> entityRef,
                                    PlayerRef playerRef,
                                    Region region) {
        if (commandBuffer == null || store == null || entityRef == null || playerRef == null || region == null) {
            return false;
        }
        if (region.isGlobal() && region.getSpawnPoint() == null) {
            return false;
        }
        TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
        if (transform == null) {
            return false;
        }
        Vector3d destination = resolveRegionTeleportPosition(region);
        World currentWorld = store.getExternalData() == null ? null : store.getExternalData().getWorld();
        rememberWorld(currentWorld);
        World targetWorld = resolveTargetWorld(region.getWorldId(), currentWorld);
        if (targetWorld == null && currentWorld == null) {
            transform.teleportPosition(destination);
            return true;
        }
        if (targetWorld == null) {
            return false;
        }
        commandBuffer.putComponent(entityRef, Teleport.getComponentType(), Teleport.createForPlayer(targetWorld, destination, transform.getRotation()));
        return true;
    }

    public boolean teleportToPosition(Store<EntityStore> store, Ref<EntityStore> entityRef, BlockPos position) {
        World currentWorld = store == null || store.getExternalData() == null ? null : store.getExternalData().getWorld();
        return teleportToPosition(store, entityRef, currentWorld == null ? null : currentWorld.getName(), position);
    }

    public boolean teleportToPosition(Store<EntityStore> store,
                                      Ref<EntityStore> entityRef,
                                      String worldId,
                                      BlockPos position) {
        if (store == null || entityRef == null || position == null) {
            return false;
        }
        TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
        if (transform == null) {
            return false;
        }
        Vector3d destination = new Vector3d(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
        World currentWorld = store.getExternalData() == null ? null : store.getExternalData().getWorld();
        rememberWorld(currentWorld);
        World targetWorld = resolveTargetWorld(worldId, currentWorld);
        if (targetWorld == null && currentWorld == null) {
            transform.teleportPosition(destination);
            return true;
        }
        if (targetWorld == null) {
            return false;
        }
        store.putComponent(entityRef, Teleport.getComponentType(), Teleport.createForPlayer(targetWorld, destination, transform.getRotation()));
        return true;
    }

    public boolean teleportToPosition(CommandBuffer<EntityStore> commandBuffer,
                                      Store<EntityStore> store,
                                      Ref<EntityStore> entityRef,
                                      BlockPos position) {
        World currentWorld = store == null || store.getExternalData() == null ? null : store.getExternalData().getWorld();
        return teleportToPosition(commandBuffer, store, entityRef, currentWorld == null ? null : currentWorld.getName(), position);
    }

    public boolean teleportToPosition(CommandBuffer<EntityStore> commandBuffer,
                                      Store<EntityStore> store,
                                      Ref<EntityStore> entityRef,
                                      String worldId,
                                      BlockPos position) {
        if (commandBuffer == null || store == null || entityRef == null || position == null) {
            return false;
        }
        TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
        if (transform == null) {
            return false;
        }
        Vector3d destination = new Vector3d(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
        World currentWorld = store.getExternalData() == null ? null : store.getExternalData().getWorld();
        rememberWorld(currentWorld);
        World targetWorld = resolveTargetWorld(worldId, currentWorld);
        if (targetWorld == null && currentWorld == null) {
            transform.teleportPosition(destination);
            return true;
        }
        if (targetWorld == null) {
            return false;
        }
        commandBuffer.putComponent(entityRef, Teleport.getComponentType(), Teleport.createForPlayer(targetWorld, destination, transform.getRotation()));
        return true;
    }

    public void performManualBackup(PlayerRef requester) {
        if (backupManager == null) {
            return;
        }
        backupManager.performBackup("manual",
                path -> {
                    send(requester, config.messages.backupCompleted, Map.of("path", path.toString()));
                    sounds.play(requester, HyGuardSounds.Cue.SUCCESS);
                },
                throwable -> send(requester, config.messages.backupFailed));
    }

    public void playSuccessSound(PlayerRef playerRef) {
        sounds.play(playerRef, HyGuardSounds.Cue.SUCCESS);
    }

    public void playDeleteSound(PlayerRef playerRef) {
        sounds.play(playerRef, HyGuardSounds.Cue.DELETE);
    }

    public void playMemberAddedSound(PlayerRef playerRef) {
        sounds.play(playerRef, HyGuardSounds.Cue.MEMBER_ADDED);
    }

    public void playMemberRemovedSound(PlayerRef playerRef) {
        sounds.play(playerRef, HyGuardSounds.Cue.MEMBER_REMOVED);
    }

    public boolean giveWand(Store<EntityStore> store, Ref<EntityStore> entityRef, PlayerRef playerRef) {
        Vector3d position = playerRef != null && playerRef.getTransform() != null ? playerRef.getTransform().getPosition() : null;
        if (store == null || entityRef == null || playerRef == null || position == null || store.getExternalData() == null) {
            return false;
        }
        ItemUtils.interactivelyPickupItem(entityRef, new ItemStack(config.general.wandItemId, 1), position, store);
        return true;
    }

    public EnterExitMessageRenderer getEnterExitMessageRenderer() {
        return enterExitMessageRenderer;
    }

    public SelectionVisualizer getSelectionVisualizer() {
        return selectionVisualizer;
    }

    public boolean isWand(Player player) {
        if (player == null || player.getInventory() == null) {
            return false;
        }
        ItemStack active = player.getInventory().getActiveHotbarItem();
        return isWand(active);
    }

    public boolean isWand(Item item) {
        return item != null && WandItem.matches(item.getId(), config.general.wandItemId);
    }

    public boolean isWand(ItemStack itemStack) {
        return itemStack != null
                && !itemStack.isEmpty()
                && WandItem.matches(itemStack.getItemId(), config.general.wandItemId);
    }

    public Region regionAt(World world, PlayerRef playerRef) {
        List<BlockPos> positions = resolvePlayerRegionPositions(playerRef);
        if (world == null || positions.isEmpty()) {
            return null;
        }
        List<Region> regions = getRegionsAt(world.getName(), positions);
        return regions.isEmpty() ? null : regions.getFirst();
    }

    public BlockPos resolvePlayerRegionPosition(PlayerRef playerRef) {
        Transform transform = playerRef == null ? null : playerRef.getTransform();
        if (transform == null || transform.getPosition() == null) {
            return null;
        }
        return new BlockPos(
                (int) Math.floor(transform.getPosition().getX()),
                (int) Math.floor(transform.getPosition().getY() + 0.25D),
                (int) Math.floor(transform.getPosition().getZ())
        );
    }

    public List<BlockPos> resolvePlayerRegionPositions(PlayerRef playerRef) {
        Transform transform = playerRef == null ? null : playerRef.getTransform();
        if (transform == null || transform.getPosition() == null) {
            return List.of();
        }

        int blockX = (int) Math.floor(transform.getPosition().getX());
        int minBlockY = (int) Math.floor(transform.getPosition().getY());
        int maxBlockY = Math.max(minBlockY, (int) Math.floor(transform.getPosition().getY() + 1.8D));
        int blockZ = (int) Math.floor(transform.getPosition().getZ());

        ArrayList<BlockPos> positions = new ArrayList<>(maxBlockY - minBlockY + 1);
        for (int blockY = minBlockY; blockY <= maxBlockY; blockY++) {
            positions.add(new BlockPos(blockX, blockY, blockZ));
        }
        return List.copyOf(positions);
    }

    public void openRegionBrowser(Store<EntityStore> store, Ref<EntityStore> entityRef, PlayerRef playerRef) {
        if (store == null || entityRef == null || playerRef == null) {
            return;
        }
        Player player = store.getComponent(entityRef, Player.getComponentType());
        if (player == null) {
            return;
        }
        player.getPageManager().openCustomPage(entityRef, store, new RegionBrowserPage(playerRef, this));
    }

    public void openRegionBrowser(Store<EntityStore> store, Ref<EntityStore> entityRef, PlayerRef playerRef, World world) {
        openRegionBrowser(store, entityRef, playerRef);
    }

    public void openChildRegionBrowser(Store<EntityStore> store,
                                       Ref<EntityStore> entityRef,
                                       PlayerRef playerRef,
                                       String worldId,
                                       String parentRegionName) {
        if (store == null || entityRef == null || playerRef == null || worldId == null || parentRegionName == null) {
            return;
        }
        Player player = store.getComponent(entityRef, Player.getComponentType());
        if (player == null) {
            return;
        }
        player.getPageManager().openCustomPage(entityRef, store, new RegionBrowserPage(playerRef, this, worldId, parentRegionName));
    }

    public void openRegionDetail(Store<EntityStore> store, Ref<EntityStore> entityRef, PlayerRef playerRef, String worldId, String regionName) {
        if (store == null || entityRef == null || playerRef == null || worldId == null || regionName == null) {
            return;
        }
        Player player = store.getComponent(entityRef, Player.getComponentType());
        if (player == null) {
            return;
        }
        player.getPageManager().openCustomPage(entityRef, store, new RegionDetailPage(playerRef, this, worldId, regionName));
    }

    public void openMemberManager(Store<EntityStore> store, Ref<EntityStore> entityRef, PlayerRef playerRef, String worldId, String regionName) {
        if (store == null || entityRef == null || playerRef == null || worldId == null || regionName == null) {
            return;
        }
        Player player = store.getComponent(entityRef, Player.getComponentType());
        if (player == null) {
            return;
        }
        player.getPageManager().openCustomPage(entityRef, store, new MemberManagerPage(playerRef, this, worldId, regionName));
    }

    public void openEntryBlacklistManager(Store<EntityStore> store, Ref<EntityStore> entityRef, PlayerRef playerRef, String worldId, String regionName) {
        if (store == null || entityRef == null || playerRef == null || worldId == null || regionName == null) {
            return;
        }
        Player player = store.getComponent(entityRef, Player.getComponentType());
        if (player == null) {
            return;
        }
        player.getPageManager().openCustomPage(entityRef, store, new EntryBlacklistPage(playerRef, this, worldId, regionName));
    }

    public void openFlagEditor(Store<EntityStore> store, Ref<EntityStore> entityRef, PlayerRef playerRef, String worldId, String regionName) {
        if (store == null || entityRef == null || playerRef == null || worldId == null || regionName == null) {
            return;
        }
        Player player = store.getComponent(entityRef, Player.getComponentType());
        if (player == null) {
            return;
        }
        player.getPageManager().openCustomPage(entityRef, store, new FlagEditorPage(playerRef, this, worldId, regionName));
    }

    private List<PlayerIdentity> decodePlayerIdentities(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        LinkedHashMap<String, PlayerIdentity> identities = new LinkedHashMap<>();
        for (String line : raw.split(SERIALIZED_PLAYER_IDENTITY_SEPARATOR)) {
            if (line == null || line.isBlank()) {
                continue;
            }

            String[] parts = line.split(SERIALIZED_PLAYER_IDENTITY_DELIMITER, 2);
            String uuid = parts.length > 0 ? parts[0].trim() : "";
            String username = parts.length > 1 ? parts[1].trim() : uuid;
            if (uuid.isBlank() || username.isBlank()) {
                continue;
            }
            identities.put(uuid, new PlayerIdentity(uuid, username));
        }
        return List.copyOf(identities.values());
    }

    private List<String> decodeCommandBlacklist(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        LinkedHashSet<String> patterns = new LinkedHashSet<>();
        for (String token : raw.split("[,;\r\n]+")) {
            String normalized = normalizeCommandBlacklistPattern(token);
            if (!normalized.isBlank()) {
                patterns.add(normalized);
            }
        }
        return List.copyOf(patterns);
    }

    private String normalizeIssuedCommand(String rawCommand) {
        return normalizeCommandToken(rawCommand);
    }

    private String normalizeCommandBlacklistPattern(String rawPattern) {
        if (rawPattern == null) {
            return "";
        }

        String trimmed = rawPattern.trim();
        boolean wildcard = trimmed.endsWith("*");
        String normalized = normalizeCommandToken(wildcard ? trimmed.substring(0, trimmed.length() - 1) : trimmed);
        if (normalized.isBlank()) {
            return "";
        }
        return wildcard ? normalized + "*" : normalized;
    }

    private String normalizeCommandToken(String rawValue) {
        if (rawValue == null) {
            return "";
        }

        String normalized = rawValue.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1).trim();
        }
        normalized = normalized.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
        return normalized;
    }

    private boolean matchesCommandBlacklistPattern(String normalizedCommand, String pattern) {
        if (normalizedCommand == null || normalizedCommand.isBlank() || pattern == null || pattern.isBlank()) {
            return false;
        }
        if (pattern.endsWith("*")) {
            String prefix = pattern.substring(0, pattern.length() - 1).trim();
            return !prefix.isBlank() && normalizedCommand.startsWith(prefix);
        }
        return normalizedCommand.equals(pattern) || normalizedCommand.startsWith(pattern + " ");
    }

    private String encodePlayerIdentities(Collection<PlayerIdentity> identities) {
        if (identities == null || identities.isEmpty()) {
            return "";
        }

        return identities.stream()
                .filter(identity -> identity != null
                        && identity.uuid() != null
                        && !identity.uuid().isBlank()
                        && identity.username() != null
                        && !identity.username().isBlank())
                .collect(Collectors.toMap(
                        PlayerIdentity::uuid,
                        identity -> identity,
                        (left, right) -> right,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .sorted(Comparator.comparing(PlayerIdentity::username, String.CASE_INSENSITIVE_ORDER))
                .map(identity -> identity.uuid() + SERIALIZED_PLAYER_IDENTITY_DELIMITER + sanitizeSerializedPlayerField(identity.username()))
                .collect(Collectors.joining(SERIALIZED_PLAYER_IDENTITY_SEPARATOR));
    }

    private String sanitizeSerializedPlayerField(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace(SERIALIZED_PLAYER_IDENTITY_SEPARATOR, " ")
                .replace(SERIALIZED_PLAYER_IDENTITY_DELIMITER, " ")
                .trim();
    }

    private HyGuardConfig loadConfig() {
        try {
            Files.createDirectories(configPath.getParent());
            ensureResourceFile("config.json", configPath);
            try (Reader reader = Files.newBufferedReader(configPath)) {
                HyGuardConfig loaded = GSON.fromJson(reader, HyGuardConfig.class);
                if (loaded == null) {
                    loaded = new HyGuardConfig();
                }
                loaded.normalize();
                try (Writer writer = Files.newBufferedWriter(configPath)) {
                    GSON.toJson(loaded, writer);
                }
                return loaded;
            }
        } catch (Throwable throwable) {
            getLogger().at(Level.WARNING).withCause(throwable).log("[HyGuard] Failed to load config.json. Using defaults.");
            HyGuardConfig fallback = new HyGuardConfig().normalize();
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(fallback, writer);
            } catch (IOException ioException) {
                getLogger().at(Level.WARNING).withCause(ioException).log("[HyGuard] Failed to write fallback config.json.");
            }
            return fallback;
        }
    }

    private void ensureResourceFile(String resourceName, Path destination) {
        try {
            Files.createDirectories(destination.getParent());
            if (Files.exists(destination)) {
                return;
            }
            try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
                if (inputStream == null) {
                    return;
                }
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ioException) {
            getLogger().at(Level.WARNING).withCause(ioException).log("[HyGuard] Failed to prepare %s.", resourceName);
        }
    }

    private RegionPlacementValidation validateRegionPlacement(PlayerRef actor,
                                                              String worldId,
                                                              BlockPos first,
                                                              BlockPos second,
                                                              String ignoredRegionId) {
        if (actor == null || worldId == null || first == null || second == null) {
            return new RegionPlacementValidation(RegionUpdateResult.SELECTION_INCOMPLETE, null);
        }

        Region candidate = Region.create("candidate", actor.getUuid().toString(), actor.getUsername(), worldId, first, second);
        List<Region> worldRegions = regionCache.allRegions().stream()
                .filter(region -> region.getWorldId() != null && region.getWorldId().equalsIgnoreCase(worldId))
                .toList();

        Region parent = findPlacementParent(worldRegions, candidate, ignoredRegionId);
        if (parent != null && !parent.isGlobal()) {
            if (!canManageRegion(actor, parent)) {
                return new RegionPlacementValidation(RegionUpdateResult.OVERLAP_CONFLICT, parent);
            }
            if (!canHostChildPlots(parent)) {
                return new RegionPlacementValidation(RegionUpdateResult.HIERARCHY_CONFLICT, parent);
            }
            if (ignoredRegionId != null && hasDescendantRegions(ignoredRegionId)) {
                return new RegionPlacementValidation(RegionUpdateResult.HIERARCHY_CONFLICT, parent);
            }

            int childLimit = config.limits.maxChildRegionsPerParent;
            if (!isServerOwned(parent) && childLimit >= 0 && countDirectChildren(parent, ignoredRegionId) >= childLimit) {
                return new RegionPlacementValidation(RegionUpdateResult.CHILD_LIMIT_REACHED, parent);
            }
        }

        if (ignoredRegionId != null && !descendantsRemainInside(candidate, ignoredRegionId)) {
            return new RegionPlacementValidation(RegionUpdateResult.HIERARCHY_CONFLICT, parent);
        }

        for (Region existing : worldRegions) {
            if (ignoredRegionId != null && ignoredRegionId.equals(existing.getId())) {
                continue;
            }
            if (!GeometryUtils.intersects(existing, candidate)) {
                continue;
            }
            if (isAllowedAncestorOverlap(existing, candidate, parent)) {
                continue;
            }
            if (isAllowedDescendantOverlap(existing, candidate, ignoredRegionId)) {
                continue;
            }
            return new RegionPlacementValidation(resolvePlacementConflict(existing, candidate), parent);
        }

        return new RegionPlacementValidation(RegionUpdateResult.SUCCESS, parent);
    }

    private Region findPlacementParent(List<Region> worldRegions, Region candidate, String ignoredRegionId) {
        Region best = null;
        for (Region existing : worldRegions) {
            if ((ignoredRegionId != null && ignoredRegionId.equals(existing.getId())) || !GeometryUtils.contains(existing, candidate)) {
                continue;
            }
            if (best == null
                    || existing.getVolume() < best.getVolume()
                    || (existing.getVolume() == best.getVolume() && existing.getPriority() > best.getPriority())) {
                best = existing;
            }
        }
        return best;
    }

    private boolean isAllowedAncestorOverlap(Region existing, Region candidate, Region parent) {
        if (!GeometryUtils.contains(existing, candidate)) {
            return false;
        }
        if (existing.isGlobal()) {
            return true;
        }
        return parent != null && isAncestorOrSelf(existing, parent);
    }

    private boolean isAllowedDescendantOverlap(Region existing, Region candidate, String ignoredRegionId) {
        return ignoredRegionId != null
                && GeometryUtils.contains(candidate, existing)
                && isDescendantOf(existing, ignoredRegionId);
    }

    private RegionUpdateResult resolvePlacementConflict(Region existing, Region candidate) {
        if (GeometryUtils.contains(candidate, existing) || GeometryUtils.contains(existing, candidate)) {
            return RegionUpdateResult.HIERARCHY_CONFLICT;
        }
        return RegionUpdateResult.OVERLAP_CONFLICT;
    }

    private boolean canHostChildPlots(Region region) {
        return isPrimaryRegion(region);
    }

    private boolean isPrimaryRegion(Region region) {
        if (region == null || region.isGlobal()) {
            return false;
        }
        String parentRegionId = region.getParentRegionId();
        if (parentRegionId == null || parentRegionId.isBlank()) {
            return true;
        }
        Region parent = regionCache.findById(parentRegionId);
        return parent == null || parent.isGlobal();
    }

    private boolean isAncestorOrSelf(Region possibleAncestor, Region region) {
        if (possibleAncestor == null || region == null) {
            return false;
        }
        if (possibleAncestor.getId().equals(region.getId())) {
            return true;
        }

        String currentParentId = region.getParentRegionId();
        while (currentParentId != null && !currentParentId.isBlank()) {
            Region currentParent = regionCache.findById(currentParentId);
            if (currentParent == null) {
                return false;
            }
            if (possibleAncestor.getId().equals(currentParent.getId())) {
                return true;
            }
            currentParentId = currentParent.getParentRegionId();
        }
        return false;
    }

    private boolean isDescendantOf(Region region, String ancestorRegionId) {
        if (region == null || ancestorRegionId == null || ancestorRegionId.isBlank()) {
            return false;
        }

        String currentParentId = region.getParentRegionId();
        while (currentParentId != null && !currentParentId.isBlank()) {
            if (ancestorRegionId.equals(currentParentId)) {
                return true;
            }
            Region currentParent = regionCache.findById(currentParentId);
            if (currentParent == null) {
                return false;
            }
            currentParentId = currentParent.getParentRegionId();
        }
        return false;
    }

    private boolean hasDescendantRegions(String regionId) {
        for (Region existing : regionCache.allRegions()) {
            if (regionId.equals(existing.getId())) {
                continue;
            }
            if (isDescendantOf(existing, regionId)) {
                return true;
            }
        }
        return false;
    }

    private boolean descendantsRemainInside(Region candidate, String regionId) {
        for (Region existing : regionCache.allRegions()) {
            if (regionId.equals(existing.getId())) {
                continue;
            }
            if (isDescendantOf(existing, regionId) && !GeometryUtils.contains(candidate, existing)) {
                return false;
            }
        }
        return true;
    }

    private int countDirectChildren(Region parent, String ignoredRegionId) {
        int count = 0;
        for (String childRegionId : parent.getChildRegionIds()) {
            if (ignoredRegionId != null && ignoredRegionId.equals(childRegionId)) {
                continue;
            }
            Region child = regionCache.findById(childRegionId);
            if (child != null && !child.isGlobal()) {
                count++;
            }
        }
        return count;
    }

    private boolean isServerOwned(Region region) {
        return region != null && SERVER_REGION_OWNER_UUID.equalsIgnoreCase(region.getOwnerUuid());
    }

    public boolean isServerOwnedRegion(Region region) {
        return isServerOwned(region);
    }

    private BlockPos resolvePlayerBlockPosition(PlayerRef playerRef) {
        if (playerRef == null || playerRef.getTransform() == null || playerRef.getTransform().getPosition() == null) {
            return null;
        }
        return new BlockPos(
                (int) Math.floor(playerRef.getTransform().getPosition().getX()),
                (int) Math.floor(playerRef.getTransform().getPosition().getY()),
                (int) Math.floor(playerRef.getTransform().getPosition().getZ())
        );
    }

    private boolean isFlagAllowed(PlayerRef playerRef,
                                  List<Region> regions,
                                  RegionFlag flag,
                                  RegionFlagValue.Mode defaultMode) {
        if (playerRef == null || flag == null) {
            return defaultMode == RegionFlagValue.Mode.ALLOW;
        }
        for (Region region : regions) {
            RegionFlagValue flagValue = region.getFlags().get(flag);
            if (flagValue == null || flagValue.getMode() == RegionFlagValue.Mode.INHERIT) {
                continue;
            }
            return isFlagGranted(region, playerRef, flagValue);
        }
        return defaultMode == RegionFlagValue.Mode.ALLOW;
    }

    private Vector3d resolveRegionTeleportPosition(Region region) {
        BlockPos spawnPoint = region.getSpawnPoint();
        if (spawnPoint != null) {
            return new Vector3d(spawnPoint.getX() + 0.5, spawnPoint.getY(), spawnPoint.getZ() + 0.5);
        }
        if (region.isGlobal()) {
            return new Vector3d(0.5, 64.0, 0.5);
        }

        double centerX = (region.getMin().getX() + region.getMax().getX()) / 2.0 + 0.5;
        double centerZ = (region.getMin().getZ() + region.getMax().getZ()) / 2.0 + 0.5;
        double y = region.getMin().getY() + 1.0;
        return new Vector3d(centerX, y, centerZ);
    }

    private GameMode resolveEnforcedGameMode(PlayerRef playerRef, List<Region> regions) {
        for (Region region : regions) {
            RegionFlagValue flagValue = region.getFlags().get(RegionFlag.GAME_MODE);
            if (flagValue == null || flagValue.getMode() == RegionFlagValue.Mode.INHERIT) {
                continue;
            }

            GameMode configuredMode = parseConfiguredGameMode(flagValue.getTextValue());
            if (configuredMode == null) {
                continue;
            }
            return isFlagGranted(region, playerRef, flagValue) ? configuredMode : GameMode.Adventure;
        }
        return null;
    }

    private Boolean resolveEnforcedFly(PlayerRef playerRef, List<Region> regions) {
        for (Region region : regions) {
            RegionFlagValue flagValue = region.getFlags().get(RegionFlag.FLY);
            if (flagValue == null || flagValue.getMode() == RegionFlagValue.Mode.INHERIT) {
                continue;
            }
            return isFlagGranted(region, playerRef, flagValue);
        }
        return null;
    }

    private Integer resolveLockedWeather(PlayerRef playerRef, List<Region> regions) {
        for (Region region : regions) {
            RegionFlagValue flagValue = region.getFlags().get(RegionFlag.WEATHER_LOCK);
            if (flagValue == null || flagValue.getMode() == RegionFlagValue.Mode.INHERIT) {
                continue;
            }

            Integer configuredWeather = parseConfiguredWeatherLock(flagValue.getTextValue());
            if (configuredWeather == null) {
                continue;
            }
            return isFlagGranted(region, playerRef, flagValue) ? configuredWeather : null;
        }
        return null;
    }

    private LocalTime resolveLockedTime(PlayerRef playerRef, List<Region> regions) {
        for (Region region : regions) {
            RegionFlagValue flagValue = region.getFlags().get(RegionFlag.TIME_LOCK);
            if (flagValue == null || flagValue.getMode() == RegionFlagValue.Mode.INHERIT) {
                continue;
            }

            LocalTime configuredTime = parseConfiguredTimeLock(flagValue.getTextValue());
            if (configuredTime == null) {
                continue;
            }
            return isFlagGranted(region, playerRef, flagValue) ? configuredTime : null;
        }
        return null;
    }

    private World resolveTargetWorld(String worldId, World fallbackWorld) {
        if (fallbackWorld != null) {
            rememberWorld(fallbackWorld);
            if (worldId == null || worldId.isBlank() || worldId.equalsIgnoreCase(fallbackWorld.getName())) {
                return fallbackWorld;
            }
        }
        if (worldId == null || worldId.isBlank()) {
            return fallbackWorld;
        }
        return knownWorlds.get(worldId);
    }

    private void applyRegionGameMode(Store<EntityStore> store,
                                     Ref<EntityStore> entityRef,
                                     PlayerRef playerRef,
                                     List<Region> regions) {
        Player player = store.getComponent(entityRef, Player.getComponentType());
        if (player == null) {
            return;
        }

        GameMode enforced = resolveEnforcedGameMode(playerRef, regions);
        if (enforced != null && player.getGameMode() != enforced) {
            Player.setGameMode(entityRef, enforced, store);
        }
    }

    private void applyRegionFly(Store<EntityStore> store,
                                Ref<EntityStore> entityRef,
                                PlayerRef playerRef,
                                List<Region> regions) {
        Player player = store.getComponent(entityRef, Player.getComponentType());
        MovementManager movementManager = store.getComponent(entityRef, MovementManager.getComponentType());
        if (player == null || movementManager == null || movementManager.getSettings() == null || playerRef.getPacketHandler() == null) {
            return;
        }

        boolean desiredCanFly = player.getGameMode() == GameMode.Creative;
        Boolean enforcedFly = resolveEnforcedFly(playerRef, regions);
        if (enforcedFly != null) {
            desiredCanFly = enforcedFly;
        }

        boolean changed = false;
        if (movementManager.getDefaultSettings() != null && movementManager.getDefaultSettings().canFly != desiredCanFly) {
            movementManager.getDefaultSettings().canFly = desiredCanFly;
            changed = true;
        }

        if (movementManager.getSettings().canFly != desiredCanFly) {
            movementManager.getSettings().canFly = desiredCanFly;
            changed = true;
        }

        if (!desiredCanFly) {
            MovementStatesComponent movementStatesComponent = store.getComponent(entityRef, MovementStatesComponent.getComponentType());
            MovementStates currentStates = movementStatesComponent == null ? null : movementStatesComponent.getMovementStates();
            if (currentStates != null && currentStates.flying) {
                MovementStates updatedStates = new MovementStates(currentStates);
                updatedStates.flying = false;
                player.applyMovementStates(entityRef, new SavedMovementStates(false), updatedStates, store);
                changed = true;
            }
        }

        if (changed) {
            movementManager.update(playerRef.getPacketHandler());
        }
    }

    private void applyRegionTimeAndWeather(Store<EntityStore> store,
                                           Ref<EntityStore> entityRef,
                                           PlayerRef playerRef,
                                           List<Region> regions) {
        if (store == null || entityRef == null || playerRef == null || playerRef.getPacketHandler() == null) {
            return;
        }

        applyRegionWeather(store, entityRef, playerRef, regions);
        applyRegionTime(store, playerRef, regions);
    }

    private void applyRegionWeather(Store<EntityStore> store,
                                    Ref<EntityStore> entityRef,
                                    PlayerRef playerRef,
                                    List<Region> regions) {
        WeatherTracker weatherTracker = store.getComponent(entityRef, WeatherTracker.getComponentType());
        if (weatherTracker == null) {
            return;
        }

        Integer lockedWeather = resolveLockedWeather(playerRef, regions);
        if (lockedWeather != null) {
            int effectiveWeatherIndex = lockedWeather > 0 ? lockedWeather : weatherTracker.getWeatherIndex();
            playerRef.getPacketHandler().write(new UpdateEditorWeatherOverride(effectiveWeatherIndex));
            return;
        }

        playerRef.getPacketHandler().write(new UpdateEditorWeatherOverride(weatherTracker.getWeatherIndex()));
    }

    private void applyRegionTime(Store<EntityStore> store,
                                 PlayerRef playerRef,
                                 List<Region> regions) {
        WorldTimeResource worldTimeResource = store.getResource(WorldTimeResource.getResourceType());
        if (worldTimeResource == null || playerRef.getPacketHandler() == null) {
            return;
        }

        LocalTime lockedTime = resolveLockedTime(playerRef, regions);
        if (lockedTime != null) {
            LocalDateTime lockedDateTime = worldTimeResource.getGameDateTime()
                    .withHour(lockedTime.getHour())
                    .withMinute(lockedTime.getMinute())
                    .withSecond(0)
                    .withNano(0);
            playerRef.getPacketHandler().write(new UpdateEditorTimeOverride(
                    WorldTimeResource.instantToInstantData(lockedDateTime.toInstant(WorldTimeResource.ZONE_OFFSET)),
                    true
            ));
            return;
        }

        playerRef.getPacketHandler().write(new ClearEditorTimeOverride());
        worldTimeResource.sendTimePackets(playerRef);
    }

    private boolean isFlagGranted(Region region, PlayerRef playerRef, RegionFlagValue flagValue) {
        RegionRole role = region.getRoleFor(playerRef.getUuid().toString());
        return switch (flagValue.getMode()) {
            case ALLOW -> true;
            case DENY -> false;
            case ALLOW_MEMBERS -> role != null && role.isAtLeast(RegionRole.MEMBER);
            case ALLOW_TRUSTED -> role != null && role.isAtLeast(RegionRole.TRUSTED);
            case INHERIT -> false;
        };
    }
}