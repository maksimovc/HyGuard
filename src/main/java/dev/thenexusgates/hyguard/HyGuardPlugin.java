package dev.thenexusgates.hyguard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
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
import dev.thenexusgates.hyguard.core.selection.SelectionSession;
import dev.thenexusgates.hyguard.core.selection.SelectionService;
import dev.thenexusgates.hyguard.core.selection.WandItem;
import dev.thenexusgates.hyguard.event.HyGuardBreakBlockSystem;
import dev.thenexusgates.hyguard.event.HyGuardChangeGameModeSystem;
import dev.thenexusgates.hyguard.event.DisconnectCleanupSystem;
import dev.thenexusgates.hyguard.event.HyGuardDamageBlockSystem;
import dev.thenexusgates.hyguard.event.HyGuardEntityDamageSystem;
import dev.thenexusgates.hyguard.event.HyGuardItemSystem;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class HyGuardPlugin extends JavaPlugin {

    private static volatile HyGuardPlugin instance;

    public enum RegionUpdateResult {
        SUCCESS,
        SELECTION_INCOMPLETE,
        WORLD_MISMATCH,
        OVERLAP_CONFLICT
    }

    public record PlayerIdentity(String uuid, String username) {
    }

    private record WandLeftClickState(String worldId, BlockPos position, long timestampMs) {
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger STORAGE_LOGGER = Logger.getLogger("HyGuard");
    private static final String SERVER_REGION_OWNER_UUID = "__server__";
    private static final String SERVER_REGION_OWNER_NAME = "Server";
    private static final long WAND_LEFT_CLICK_DEBOUNCE_MS = 1500L;

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
    private DisconnectCleanupSystem disconnectCleanupSystem;
    private final ConcurrentHashMap<String, WandLeftClickState> recentWandLeftClicks = new ConcurrentHashMap<>();

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
        playerMoveSystem = new PlayerMoveSystem(this, HytaleServer.SCHEDULED_EXECUTOR, enterExitMessageRenderer);
        disconnectCleanupSystem = new DisconnectCleanupSystem(this);

        getCodecRegistry(Interaction.CODEC).register(
            HyGuardWandInteraction.ID,
            HyGuardWandInteraction.class,
            HyGuardWandInteraction.CODEC
        );
        getCommandRegistry().registerCommand(new GuardCommand(this));
        getEventRegistry().registerGlobal(com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent.class, disconnectCleanupSystem::handle);

        if (EntityModule.get() != null) {
            getEntityStoreRegistry().registerSystem(new HyGuardDamageBlockSystem(this));
            getEntityStoreRegistry().registerSystem(new HyGuardBreakBlockSystem(this));
            getEntityStoreRegistry().registerSystem(new HyGuardPlaceBlockSystem(this));
            getEntityStoreRegistry().registerSystem(new HyGuardUseBlockSystem(this));
            getEntityStoreRegistry().registerSystem(new HyGuardChangeGameModeSystem(this));
            getEntityStoreRegistry().registerSystem(new HyGuardEntityDamageSystem(this));
            getEntityStoreRegistry().registerSystem(new HyGuardItemSystem.DropSystem(this));
            getEntityStoreRegistry().registerSystem(new HyGuardItemSystem.PickupSystem(this));
            getLogger().at(Level.INFO).log(HyGuardMobSpawnSystem.NO_API_REASON);
        }
        if (playerMoveSystem != null) {
            playerMoveSystem.start();
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
                && (hasAdminPermission(playerRef) || bypassHandler.isBypassing(playerRef.getUuid().toString()));
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

    public Region createRegion(PlayerRef playerRef, String worldId, String name) {
        return createRegion(playerRef, worldId, name, playerRef.getUuid().toString(), playerRef.getUsername());
    }

    public Region createServerRegion(PlayerRef playerRef, String worldId, String name) {
        return createRegion(playerRef, worldId, name, SERVER_REGION_OWNER_UUID, SERVER_REGION_OWNER_NAME);
    }

    private Region createRegion(PlayerRef playerRef, String worldId, String name, String ownerUuid, String ownerName) {
        String playerUuid = playerRef.getUuid().toString();
        var session = selectionService.get(playerUuid);
        if (session == null || !session.isComplete() || !worldId.equalsIgnoreCase(session.getWorldId())) {
            return null;
        }

        if (hasOverlapConflict(worldId, ownerUuid, session.getFirstPoint().getPosition(), session.getSecondPoint().getPosition(), null)) {
            return null;
        }

        Region region = Region.create(
                name,
                ownerUuid,
                ownerName,
                worldId,
                session.getFirstPoint().getPosition(),
                session.getSecondPoint().getPosition()
        );
        region.putFlag(RegionFlag.BLOCK_BREAK, new RegionFlagValue(RegionFlagValue.Mode.valueOf(config.defaults.blockBreak)));
        region.putFlag(RegionFlag.BLOCK_PLACE, new RegionFlagValue(RegionFlagValue.Mode.valueOf(config.defaults.blockPlace)));
        region.putFlag(RegionFlag.BLOCK_INTERACT, new RegionFlagValue(RegionFlagValue.Mode.valueOf(config.defaults.blockInteract)));

        regionCache.put(region);
        regionRepository.saveRegionAsync(region);
        return region;
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

    public List<Region> getWorldRegions(String worldId) {
        return regionCache.allRegions().stream()
                .filter(region -> region.getWorldId() != null && region.getWorldId().equalsIgnoreCase(worldId))
                .sorted(Comparator.comparing(Region::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<Region> getRegionsAt(String worldId, BlockPos position) {
        return regionCache.getRegionsAt(worldId, position);
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
        if (playerRef == null || playerRef.getUuid() == null) {
            return;
        }

        String playerUuid = playerRef.getUuid().toString();
        SelectionSession session = selectionService.get(playerUuid);
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

    public void refreshSelectionVisualization(PlayerRef playerRef) {
        if (selectionVisualizer == null || playerRef == null || playerRef.getUuid() == null) {
            return;
        }
        SelectionSession session = selectionService.get(playerRef.getUuid().toString());
        if (session == null || !session.hasAnyPoint()) {
            selectionVisualizer.clearPlayer(playerRef.getUuid().toString());
            return;
        }
        boolean hasConflict = session.isComplete() && hasOverlapConflict(
            session.getWorldId(),
            playerRef.getUuid().toString(),
            session.getFirstPoint().getPosition(),
            session.getSecondPoint().getPosition(),
            null
        );
        selectionVisualizer.updateVisualization(playerRef, session, hasConflict);
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
        if (hasOverlapConflict(region.getWorldId(), region.getOwnerUuid(), session.getFirstPoint().getPosition(), session.getSecondPoint().getPosition(), region.getId())) {
            return RegionUpdateResult.OVERLAP_CONFLICT;
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

        List<Region> regions = getRegionsAt(worldId, position);
        applyRegionGameMode(store, entityRef, playerRef, regions);
        applyRegionFly(store, entityRef, playerRef, regions);
    }

    public ProtectionResult evaluate(PlayerRef playerRef, String worldId, BlockPos position, ProtectionAction action) {
        if (playerRef == null) {
            return ProtectionResult.allow();
        }
        boolean admin = hasAdminPermission(playerRef);
        return protectionEngine.evaluate(new ProtectionQuery(playerRef.getUuid().toString(), worldId, position, action), admin);
    }

    public boolean isFlagAllowed(PlayerRef playerRef,
                                 String worldId,
                                 BlockPos position,
                                 RegionFlag flag,
                                 RegionFlagValue.Mode defaultMode) {
        if (worldId == null || position == null) {
            return defaultMode == RegionFlagValue.Mode.ALLOW;
        }
        return isFlagAllowed(playerRef, getRegionsAt(worldId, position), flag, defaultMode);
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
        if (currentWorld == null) {
            transform.teleportPosition(destination);
            return true;
        }
        store.putComponent(entityRef, Teleport.getComponentType(), Teleport.createForPlayer(currentWorld, destination, transform.getRotation()));
        return true;
    }

    public boolean teleportToPosition(Store<EntityStore> store, Ref<EntityStore> entityRef, BlockPos position) {
        if (store == null || entityRef == null || position == null) {
            return false;
        }
        TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
        if (transform == null) {
            return false;
        }
        Vector3d destination = new Vector3d(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
        World currentWorld = store.getExternalData() == null ? null : store.getExternalData().getWorld();
        if (currentWorld == null) {
            transform.teleportPosition(destination);
            return true;
        }
        store.putComponent(entityRef, Teleport.getComponentType(), Teleport.createForPlayer(currentWorld, destination, transform.getRotation()));
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
        Transform transform = playerRef == null ? null : playerRef.getTransform();
        if (world == null || transform == null || transform.getPosition() == null) {
            return null;
        }
        BlockPos position = new BlockPos(
                (int) Math.floor(transform.getPosition().getX()),
                (int) Math.floor(transform.getPosition().getY()),
                (int) Math.floor(transform.getPosition().getZ())
        );
        List<Region> regions = regionCache.getRegionsAt(world.getName(), position);
        return regions.isEmpty() ? null : regions.getFirst();
    }

    public void openRegionBrowser(Store<EntityStore> store, Ref<EntityStore> entityRef, PlayerRef playerRef, World world) {
        if (store == null || entityRef == null || playerRef == null || world == null) {
            return;
        }
        Player player = store.getComponent(entityRef, Player.getComponentType());
        if (player == null) {
            return;
        }
        player.getPageManager().openCustomPage(entityRef, store, new RegionBrowserPage(playerRef, this, world.getName()));
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

    private boolean hasOverlapConflict(String worldId, String ownerUuid, BlockPos first, BlockPos second, String ignoredRegionId) {
        Region candidate = Region.create("candidate", ownerUuid, ownerUuid, worldId, first, second);
        for (Region existing : regionCache.allRegions()) {
            if (ignoredRegionId != null && ignoredRegionId.equals(existing.getId())) {
                continue;
            }
            if (!existing.getWorldId().equalsIgnoreCase(worldId)) {
                continue;
            }
            if (!GeometryUtils.intersects(existing, candidate)) {
                continue;
            }
            if (GeometryUtils.contains(existing, candidate) || GeometryUtils.contains(candidate, existing)) {
                continue;
            }
            if (ownerUuid == null || existing.getOwnerUuid() == null || !existing.getOwnerUuid().equalsIgnoreCase(ownerUuid)) {
                return true;
            }
        }
        return false;
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

        if (movementManager.getSettings().canFly != desiredCanFly) {
            movementManager.getSettings().canFly = desiredCanFly;
            movementManager.update(playerRef.getPacketHandler());
        }
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