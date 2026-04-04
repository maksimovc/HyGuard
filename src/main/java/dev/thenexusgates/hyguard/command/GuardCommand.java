package dev.thenexusgates.hyguard.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.thenexusgates.hyguard.HyGuardPlugin;
import dev.thenexusgates.hyguard.core.region.Region;
import dev.thenexusgates.hyguard.core.region.RegionFlag;
import dev.thenexusgates.hyguard.core.region.RegionFlagValue;
import dev.thenexusgates.hyguard.core.region.RegionMember;
import dev.thenexusgates.hyguard.core.region.RegionRole;
import dev.thenexusgates.hyguard.core.selection.SelectionSession;
import dev.thenexusgates.hyguard.util.BlockPos;
import dev.thenexusgates.hyguard.util.BlockPosUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class GuardCommand extends AbstractPlayerCommand {

    private record HelpTopic(String command, String usage, String descriptionKey, String example, String permissionKey) {
    }

    private enum SelectionEditMode {
        EXPAND,
        CONTRACT,
        SHIFT
    }

    private enum SelectionDirection {
        NORTH,
        SOUTH,
        EAST,
        WEST,
        UP,
        DOWN;

        static SelectionDirection parse(String raw) {
            try {
                return SelectionDirection.valueOf(raw.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException illegalArgumentException) {
                return null;
            }
        }
    }

    private static final String GLOBAL_REGION_NAME = "__global__";
    private static final int HELP_PAGE_SIZE = 8;
    private static final int MEMBER_PAGE_SIZE = 10;
    private static final int FLAG_PAGE_SIZE = 10;
    private static final Map<String, HelpTopic> HELP_TOPICS = createHelpTopics();

    private final HyGuardPlugin plugin;
    private final FlagArg confirmArg;
    private final FlagArg serverArg;

    public GuardCommand(HyGuardPlugin plugin) {
        super("hg", "HyGuard command");
        this.plugin = plugin;
        this.confirmArg = withFlagArg("confirm", "Confirm destructive actions");
        this.serverArg = withFlagArg("server", "Create a server-owned region");
        requirePermission(plugin.getConfigSnapshot().general.usePermission);
        setAllowsExtraArguments(true);
        addAliases("guard");
    }

    @Override
    protected void execute(CommandContext context,
                           Store<EntityStore> store,
                           Ref<EntityStore> entityRef,
                           PlayerRef playerRef,
                           World world) {
        plugin.rememberPlayer(playerRef);
        String[] args = parseArgs(context.getInputString());
        if (!plugin.hasUsePermission(playerRef)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
            return;
        }
        if (args.length == 0) {
            sendHelp(playerRef, new String[] { "help" });
            return;
        }

        if ("help".equalsIgnoreCase(args[0])) {
            sendHelp(playerRef, args);
            return;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        boolean confirmRequested = Boolean.TRUE.equals(confirmArg.get(context));
        boolean serverRequested = Boolean.TRUE.equals(serverArg.get(context));
        switch (subcommand) {
            case "wand" -> handleWand(store, entityRef, playerRef);
            case "create" -> handleCreate(playerRef, world, args, serverRequested);
            case "delete" -> handleDelete(playerRef, world, args, confirmRequested);
            case "info" -> handleInfo(playerRef, world, args);
            case "list" -> handleList(playerRef, world);
            case "select" -> handleSelect(playerRef, world, args);
            case "redefine" -> handleRedefine(playerRef, world, args);
            case "expand" -> handleSelectionEdit(playerRef, world, args, SelectionEditMode.EXPAND);
            case "contract" -> handleSelectionEdit(playerRef, world, args, SelectionEditMode.CONTRACT);
            case "shift" -> handleSelectionEdit(playerRef, world, args, SelectionEditMode.SHIFT);
            case "priority" -> handlePriority(playerRef, world, args);
            case "flag" -> handleFlag(playerRef, world, args);
            case "flags" -> handleFlags(playerRef, world, args);
            case "member" -> handleMember(playerRef, world, args, confirmRequested);
            case "tp" -> handleTeleport(store, entityRef, playerRef, world, args);
            case "setspawn" -> handleSetSpawn(playerRef, world, args);
            case "gui" -> handleGui(store, entityRef, playerRef, world, args);
            case "backup" -> handleBackup(playerRef);
            case "debug" -> handleDebug(playerRef, world, args);
            case "admin-override", "adminoverride", "bypass" -> handleAdminOverride(playerRef);
            case "save" -> handleSave(playerRef);
            case "reload" -> handleReload(playerRef);
            default -> sendHelp(playerRef, new String[] { "help", subcommand });
        }
    }

    private void handleWand(Store<EntityStore> store, Ref<EntityStore> entityRef, PlayerRef playerRef) {
        if (!requirePermission(playerRef, plugin.getConfigSnapshot().general.wandPermission, false)) {
            return;
        }
        if (plugin.giveWand(store, entityRef, playerRef)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.wandGiven);
            plugin.playSuccessSound(playerRef);
        }
    }

    private void handleCreate(PlayerRef playerRef, World world, String[] args, boolean serverRequested) {
        if (!requirePermission(playerRef, plugin.getConfigSnapshot().general.createPermission, true)) {
            return;
        }
        if (args.length < 2) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidRegionName);
            return;
        }
        String name = validateRegionName(playerRef, args[1]);
        if (name == null) {
            return;
        }
        boolean explicitServerOwned = serverRequested || hasServerFlag(args);
        boolean inheritedServerOwned = !explicitServerOwned && plugin.selectionInheritsServerOwnership(playerRef, world.getName());
        boolean serverOwned = explicitServerOwned || inheritedServerOwned;
        if (GLOBAL_REGION_NAME.equalsIgnoreCase(name)) {
            if (!plugin.hasAdminPermission(playerRef)) {
                plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
                return;
            }
            if (plugin.findRegionByName(world.getName(), GLOBAL_REGION_NAME) != null) {
                plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionAlreadyExists);
                return;
            }
            Region region = plugin.createGlobalRegion(playerRef, world.getName(), GLOBAL_REGION_NAME);
            plugin.getLogger().at(java.util.logging.Level.INFO).log("[HyGuard] region created: world=%s name=%s actor=%s global=true serverOwned=true", world.getName(), region.getName(), playerRef.getUsername());
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionCreated, Map.of("name", region.getName()));
            plugin.playSuccessSound(playerRef);
            return;
        }
        if (plugin.findRegionByName(world.getName(), name) != null) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionAlreadyExists);
            return;
        }
        if (explicitServerOwned && !plugin.hasAdminPermission(playerRef)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
            return;
        }
        if (!serverOwned
                && !plugin.hasAdminPermission(playerRef)
                && plugin.countOwnedRegions(playerRef.getUuid().toString()) >= plugin.getConfigSnapshot().limits.maxRegionsPerPlayer) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionLimitReached, Map.of(
                    "limit", Integer.toString(plugin.getConfigSnapshot().limits.maxRegionsPerPlayer)
            ));
            return;
        }
        HyGuardPlugin.RegionCreationResult creationResult = serverOwned
                ? plugin.createServerRegion(playerRef, world.getName(), name)
                : plugin.createRegion(playerRef, world.getName(), name);
        if (!creationResult.isSuccess()) {
            sendRegionMutationFailure(playerRef, creationResult.result(), null);
            return;
        }
        Region region = creationResult.region();
        plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionCreated, Map.of("name", region.getName()));
        plugin.playSuccessSound(playerRef);
        plugin.getLogger().at(java.util.logging.Level.INFO).log("[HyGuard] region created: world=%s name=%s actor=%s global=false serverOwned=%s", world.getName(), region.getName(), playerRef.getUsername(), plugin.isServerOwnedRegion(region));
    }

    private void handleDelete(PlayerRef playerRef, World world, String[] args, boolean confirmRequested) {
        if (!requirePermission(playerRef, plugin.getConfigSnapshot().general.deletePermission, true)) {
            return;
        }
        if (args.length < 2) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionNotFound);
            return;
        }
        Region region = resolveRegionReference(playerRef, world, args[1]);
        if (region == null) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionNotFound);
            return;
        }
        if (!plugin.canManageRegion(playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
            return;
        }
        if (plugin.hasChildRegions(region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionDeleteHasChildren, Map.of("name", region.getName()));
            return;
        }
        if (!confirmRequested && !hasConfirmFlag(args)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionDeleteConfirmRequired, Map.of("name", region.getName()));
            return;
        }
        if (plugin.deleteRegion(region)) {
            plugin.getLogger().at(java.util.logging.Level.INFO).log("[HyGuard] region deleted: world=%s name=%s actor=%s", world.getName(), region.getName(), playerRef.getUsername());
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionDeleted, Map.of("name", region.getName()));
            plugin.playDeleteSound(playerRef);
        }
    }

    private void handleInfo(PlayerRef playerRef, World world, String[] args) {
        if (!requirePermission(playerRef, plugin.getConfigSnapshot().general.infoPermission, false)) {
            return;
        }
        Region region = args.length >= 2 ? plugin.findRegionByName(world.getName(), args[1]) : plugin.regionAt(world, playerRef);
        if (region == null) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionNotFound);
            return;
        }
        Map<String, String> replacements = new HashMap<>();
        replacements.put("name", region.getName());
        replacements.put("owner", region.getOwnerName());
        replacements.put("world", region.getWorldId());
        replacements.put("min", region.isGlobal() || region.getMin() == null ? "global" : region.getMin().toString());
        replacements.put("max", region.isGlobal() || region.getMax() == null ? "global" : region.getMax().toString());
        plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionInfo, replacements);
    }

    private void handleFlags(PlayerRef playerRef, World world, String[] args) {
        if (!requirePermission(playerRef, plugin.getConfigSnapshot().general.flagsViewPermission, false)) {
            return;
        }
        Region region = requireExistingRegion(playerRef, world, args);
        if (region == null) {
            return;
        }

        int page = parseOptionalPage(playerRef, args, 2);
        if (page < 1) {
            return;
        }

        List<String> lines = new ArrayList<>(RegionFlag.visibleValues().size());
        for (RegionFlag flag : RegionFlag.visibleValues()) {
            RegionFlagValue flagValue = region.getFlags().get(flag);
            String value = formatFlagValue(flagValue);
            lines.add(plugin.message(playerRef, plugin.getConfigSnapshot().messages.flagListEntry, Map.of(
                    "flag", flag.name(),
                    "value", value
            )));
        }

        sendPaged(playerRef,
                plugin.getConfigSnapshot().messages.flagsHeader,
                Map.of("name", region.getName()),
                lines,
                page,
                FLAG_PAGE_SIZE);
    }

    private void handleList(PlayerRef playerRef, World world) {
        if (!requirePermission(playerRef, plugin.getConfigSnapshot().general.listPermission, false)) {
            return;
        }
        List<Region> regions = new ArrayList<>(plugin.getWorldRegions(world.getName()));
        if (regions.isEmpty()) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionListEmpty);
            return;
        }

        plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionList, Map.of(
                "world", world.getName(),
                "regions", Integer.toString(regions.size())
        ));

        for (Region rootRegion : plugin.getDisplayRootRegions(world.getName())) {
            sendRegionTree(playerRef, rootRegion, 0);
        }
    }

    private void sendRegionTree(PlayerRef playerRef, Region region, int depth) {
        plugin.sendRaw(playerRef, formatRegionTreeLine(region, depth));
        for (Region childRegion : plugin.getDisplayChildRegions(region)) {
            sendRegionTree(playerRef, childRegion, depth + 1);
        }
    }

    private String formatRegionTreeLine(Region region, int depth) {
        String prefix = depth <= 0 ? "- " : "  ".repeat(depth) + "- ";
        StringBuilder line = new StringBuilder(prefix)
                .append(region.getName())
                .append(" [owner: ")
                .append(region.getOwnerName())
                .append(", priority: ")
                .append(region.getPriority());

        if (region.isGlobal()) {
            line.append(", global");
        } else if (region.getParentRegionId() != null && !region.getParentRegionId().isBlank()) {
            line.append(", parent: ")
                    .append(plugin.getRegionNameById(region.getParentRegionId(), region.getWorldId()));
        }

        int childCount = plugin.getDisplayChildRegions(region).size();
        if (childCount > 0) {
            line.append(", children: ").append(childCount);
        }

        line.append(']');
        return line.toString();
    }

    private void handleSelect(PlayerRef playerRef, World world, String[] args) {
        if (!requirePermission(playerRef, plugin.getConfigSnapshot().general.selectPermission, true)) {
            return;
        }
        Region region = requireManageableRegion(playerRef, world, args);
        if (region == null) {
            return;
        }
        if (region.isGlobal()) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.globalSelectionUnsupported);
            return;
        }
        if (plugin.loadRegionSelection(playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.selectionLoaded, Map.of("name", region.getName()));
        }
    }

    private void handleRedefine(PlayerRef playerRef, World world, String[] args) {
        if (!requirePermission(playerRef, plugin.getConfigSnapshot().general.redefinePermission, true)) {
            return;
        }
        Region region = requireManageableRegion(playerRef, world, args);
        if (region == null) {
            return;
        }
        if (region.isGlobal()) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.globalSelectionUnsupported);
            return;
        }

        HyGuardPlugin.RegionUpdateResult redefineResult = plugin.redefineRegion(playerRef, world.getName(), region);
        switch (redefineResult) {
            case SUCCESS -> {
                plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionRedefined, Map.of("name", region.getName()));
                plugin.playSuccessSound(playerRef);
            }
            default -> sendRegionMutationFailure(playerRef, redefineResult, region);
        }
    }

    private void handlePriority(PlayerRef playerRef, World world, String[] args) {
        if (!requirePermission(playerRef, plugin.getConfigSnapshot().general.priorityPermission, true)) {
            return;
        }
        if (args.length < 3) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidPriority);
            return;
        }

        Region region = requireManageableRegion(playerRef, world, args);
        if (region == null) {
            return;
        }

        int priority;
        try {
            priority = Integer.parseInt(args[2]);
        } catch (NumberFormatException numberFormatException) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidPriority);
            return;
        }
        if (priority < 0 || priority > plugin.getConfigSnapshot().limits.maxPriority) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidPriority);
            return;
        }

        region.setPriority(priority);
        plugin.saveRegion(region);
        plugin.getLogger().at(java.util.logging.Level.INFO).log("[HyGuard] priority set: region=%s actor=%s priority=%s", region.getName(), playerRef.getUsername(), priority);
        plugin.send(playerRef, plugin.getConfigSnapshot().messages.priorityUpdated, Map.of(
                "name", region.getName(),
                "priority", Integer.toString(priority)
        ));
        plugin.playSuccessSound(playerRef);
    }

    private void handleSelectionEdit(PlayerRef playerRef, World world, String[] args, SelectionEditMode mode) {
        if (!requirePermission(playerRef, plugin.getConfigSnapshot().general.selectionEditPermission, false)) {
            return;
        }
        if (args.length < 3) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidSelectionEdit);
            return;
        }

        SelectionSession session = playerRef == null ? null : plugin.getSelectionService().get(playerRef.getUuid().toString());
        if (session == null || !session.isComplete()) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.selectionIncomplete);
            return;
        }
        if (!world.getName().equalsIgnoreCase(session.getWorldId())) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.selectionWorldMismatch);
            return;
        }

        SelectionDirection direction = SelectionDirection.parse(args[1]);
        if (direction == null) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidSelectionEdit);
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException numberFormatException) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidSelectionAmount);
            return;
        }
        if (amount <= 0 || amount > plugin.getConfigSnapshot().limits.maxSelectionEditAmount) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidSelectionAmount);
            return;
        }

        BlockPos min = BlockPosUtils.min(session.getFirstPoint().getPosition(), session.getSecondPoint().getPosition());
        BlockPos max = BlockPosUtils.max(session.getFirstPoint().getPosition(), session.getSecondPoint().getPosition());

        boolean valid = switch (mode) {
            case EXPAND -> applyExpand(min, max, direction, amount);
            case CONTRACT -> applyContract(min, max, direction, amount);
            case SHIFT -> applyShift(min, max, direction, amount);
        };
        if (!valid) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidSelectionAmount);
            return;
        }

        String playerUuid = playerRef.getUuid().toString();
        plugin.getSelectionService().setFirstPoint(playerUuid, session.getWorldId(), min);
        plugin.getSelectionService().setSecondPoint(playerUuid, session.getWorldId(), max);
        plugin.refreshSelectionVisualization(playerRef);
        plugin.send(playerRef, plugin.getConfigSnapshot().messages.selectionUpdated, Map.of(
                "min", min.toString(),
            "max", max.toString(),
            "size", plugin.formatSelectionSize(plugin.getSelectionService().get(playerUuid))
        ));
        plugin.playSuccessSound(playerRef);
    }

    private void handleMember(PlayerRef playerRef, World world, String[] args, boolean confirmRequested) {
        if (!requirePermission(playerRef, plugin.getConfigSnapshot().general.memberPermission, true)) {
            return;
        }
        if (args.length < 2) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.helpUnknownTopic);
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "add" -> handleMemberAdd(playerRef, world, args, confirmRequested);
            case "remove" -> handleMemberRemove(playerRef, world, args);
            case "role" -> handleMemberRole(playerRef, world, args);
            case "list" -> handleMemberList(playerRef, world, args);
            default -> plugin.send(playerRef, plugin.getConfigSnapshot().messages.helpUnknownTopic);
        }
    }

    private void handleMemberAdd(PlayerRef playerRef, World world, String[] args, boolean confirmRequested) {
        if (args.length < 4) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.helpUnknownTopic);
            return;
        }

        Region region = requireMembershipManagedRegion(playerRef, world, args, 2);
        if (region == null) {
            return;
        }

        HyGuardPlugin.PlayerIdentity target = plugin.resolvePlayerIdentity(args[3]);
        if (target == null) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.playerLookupFailed);
            return;
        }

        RegionRole role = RegionRole.MEMBER;
        if (args.length >= 5 && !"--confirm".equalsIgnoreCase(args[4])) {
            role = parseRole(playerRef, args[4]);
            if (role == null) {
                return;
            }
        }
        if (role == RegionRole.OWNER) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.cannotAssignOwner);
            return;
        }

        RegionMember existing = region.getMember(target.uuid());
        if (existing == null && region.getMembers().size() >= plugin.getConfigSnapshot().limits.maxMembersPerRegion) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.memberLimitReached, Map.of(
                "name", region.getName(),
                "limit", Integer.toString(plugin.getConfigSnapshot().limits.maxMembersPerRegion)
            ));
            return;
        }
        boolean confirm = confirmRequested || hasConfirmFlag(args);
        if (existing != null && !confirm) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.memberReplaceConfirm, Map.of(
                    "player", existing.getName(),
                    "role", existing.getRole().name()
            ));
            return;
        }

        RegionMember updated = new RegionMember(target.uuid(), target.username(), role);
        region.addMember(updated);
        plugin.saveRegion(region);
        plugin.getLogger().at(java.util.logging.Level.INFO).log("[HyGuard] member upsert: region=%s actor=%s target=%s role=%s", region.getName(), playerRef.getUsername(), updated.getName(), updated.getRole().name());
        plugin.send(playerRef,
                existing == null ? plugin.getConfigSnapshot().messages.memberAdded : plugin.getConfigSnapshot().messages.memberRoleUpdated,
                Map.of(
                        "player", updated.getName(),
                        "name", region.getName(),
                        "role", updated.getRole().name()
                ));
        if (existing == null) {
            plugin.playMemberAddedSound(playerRef);
        } else {
            plugin.playSuccessSound(playerRef);
        }
    }

    private void handleMemberRemove(PlayerRef playerRef, World world, String[] args) {
        if (args.length < 4) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.helpUnknownTopic);
            return;
        }

        Region region = requireMembershipManagedRegion(playerRef, world, args, 2);
        if (region == null) {
            return;
        }

        RegionMember target = resolveExistingMember(region, args[3]);
        if (target == null) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.memberNotFound, Map.of("name", region.getName()));
            return;
        }
        if (target.getUuid().equals(region.getOwnerUuid())) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.cannotRemoveOwner);
            return;
        }

        if (region.removeMember(target.getUuid())) {
            plugin.saveRegion(region);
            plugin.getLogger().at(java.util.logging.Level.INFO).log("[HyGuard] member removed: region=%s actor=%s target=%s", region.getName(), playerRef.getUsername(), target.getName());
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.memberRemoved, Map.of(
                    "player", target.getName(),
                    "name", region.getName()
            ));
            plugin.playMemberRemovedSound(playerRef);
        }
    }

    private void handleMemberRole(PlayerRef playerRef, World world, String[] args) {
        if (args.length < 5) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.helpUnknownTopic);
            return;
        }

        Region region = requireMembershipManagedRegion(playerRef, world, args, 2);
        if (region == null) {
            return;
        }

        RegionMember target = resolveExistingMember(region, args[3]);
        if (target == null) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.memberNotFound, Map.of("name", region.getName()));
            return;
        }

        RegionRole newRole = parseRole(playerRef, args[4]);
        if (newRole == null) {
            return;
        }
        if (newRole == RegionRole.OWNER) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.cannotAssignOwner);
            return;
        }

        region.addMember(new RegionMember(target.getUuid(), target.getName(), newRole));
        plugin.saveRegion(region);
        plugin.getLogger().at(java.util.logging.Level.INFO).log("[HyGuard] member role updated: region=%s actor=%s target=%s role=%s", region.getName(), playerRef.getUsername(), target.getName(), newRole.name());
        plugin.send(playerRef, plugin.getConfigSnapshot().messages.memberRoleUpdated, Map.of(
                "player", target.getName(),
                "name", region.getName(),
                "role", newRole.name()
        ));
        plugin.playSuccessSound(playerRef);
    }

    private void handleMemberList(PlayerRef playerRef, World world, String[] args) {
        if (args.length < 3) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionNotFound);
            return;
        }

        Region region = requireMembershipManagedRegion(playerRef, world, args, 2);
        if (region == null) {
            return;
        }

        List<RegionMember> members = new ArrayList<>(region.getMembers().values());
        members.sort(Comparator.comparing(RegionMember::getName, String.CASE_INSENSITIVE_ORDER));
        if (members.isEmpty()) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.memberListEmpty, Map.of("name", region.getName()));
            return;
        }

        int page = parseOptionalPage(playerRef, args, 4);
        if (page < 1) {
            return;
        }

        List<String> lines = new ArrayList<>(members.size());
        for (RegionMember member : members) {
            lines.add(plugin.message(playerRef, plugin.getConfigSnapshot().messages.memberListEntry, Map.of(
                    "role", member.getRole().name(),
                    "player", member.getName()
            )));
        }
        sendPaged(playerRef,
                plugin.getConfigSnapshot().messages.memberListHeader,
                Map.of("name", region.getName()),
                lines,
                page,
                MEMBER_PAGE_SIZE);
    }

    private void handleFlag(PlayerRef playerRef, World world, String[] args) {
        if (!requirePermission(playerRef, plugin.getConfigSnapshot().general.flagEditPermission, true)) {
            return;
        }
        if (args.length < 4) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidFlagValue);
            return;
        }

        Region region = requireManageableRegion(playerRef, world, args);
        if (region == null) {
            return;
        }

        RegionFlag flag;
        try {
            flag = RegionFlag.valueOf(args[2].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException illegalArgumentException) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidFlag);
            return;
        }
        if (!flag.isUserVisible()) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidFlag);
            return;
        }

        if ("clear".equalsIgnoreCase(args[3]) || "reset".equalsIgnoreCase(args[3]) || "--reset".equalsIgnoreCase(args[3])) {
            region.removeFlag(flag);
            plugin.saveRegion(region);
            plugin.getLogger().at(java.util.logging.Level.INFO).log("[HyGuard] flag cleared: region=%s actor=%s flag=%s", region.getName(), playerRef.getUsername(), flag.name());
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.flagCleared, Map.of(
                    "name", region.getName(),
                    "flag", flag.name()
            ));
                plugin.playSuccessSound(playerRef);
            return;
        }

        RegionFlagValue.Mode mode;
        try {
            mode = RegionFlagValue.Mode.valueOf(args[3].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException illegalArgumentException) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidFlagValue);
            return;
        }

        String textValue = args.length > 4 ? String.join(" ", slice(args, 4)) : null;
        if (flag == RegionFlag.GAME_MODE && (textValue == null || plugin.parseConfiguredGameMode(textValue) == null)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidFlagValue);
            return;
        }
        if (flag == RegionFlag.FLY) {
            textValue = null;
        }
        region.putFlag(flag, textValue == null || textValue.isBlank()
                ? new RegionFlagValue(mode)
                : new RegionFlagValue(mode, textValue));
        plugin.saveRegion(region);
        plugin.getLogger().at(java.util.logging.Level.INFO).log("[HyGuard] flag updated: region=%s actor=%s flag=%s value=%s", region.getName(), playerRef.getUsername(), flag.name(), textValue == null || textValue.isBlank() ? mode.name() : mode.name() + ":" + textValue);
        plugin.send(playerRef, plugin.getConfigSnapshot().messages.flagUpdated, Map.of(
                "name", region.getName(),
                "flag", flag.name(),
                "value", mode.name()
        ));
        plugin.playSuccessSound(playerRef);
    }

    private void handleTeleport(Store<EntityStore> store, Ref<EntityStore> entityRef, PlayerRef playerRef, World world, String[] args) {
        if (!requirePermission(playerRef, plugin.getConfigSnapshot().general.teleportPermission, true)) {
            return;
        }
        Region region = requireExistingRegion(playerRef, world, args, true);
        if (region == null) {
            return;
        }
        if (!plugin.canTeleportToRegion(playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.tpDenied);
            return;
        }
        if (!plugin.teleportToRegion(store, entityRef, playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.teleportFailed);
            return;
        }
        plugin.getLogger().at(java.util.logging.Level.INFO).log("[HyGuard] region teleport: world=%s name=%s actor=%s", world.getName(), region.getName(), playerRef.getUsername());
        plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionTeleported, Map.of("name", region.getName()));
        plugin.playSuccessSound(playerRef);
    }

    private void handleSetSpawn(PlayerRef playerRef, World world, String[] args) {
        if (!requirePermission(playerRef, plugin.getConfigSnapshot().general.setSpawnPermission, true)) {
            return;
        }
        Region region = requireExistingRegion(playerRef, world, args, true);
        if (region == null) {
            return;
        }
        if (!plugin.canManageSpawn(playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
            return;
        }
        if (playerRef == null || playerRef.getTransform() == null || playerRef.getTransform().getPosition() == null) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.teleportFailed);
            return;
        }
        BlockPos spawnPoint = new BlockPos(
                (int) Math.floor(playerRef.getTransform().getPosition().getX()),
                (int) Math.floor(playerRef.getTransform().getPosition().getY()),
                (int) Math.floor(playerRef.getTransform().getPosition().getZ())
        );
        region.setSpawnPoint(spawnPoint);
        plugin.saveRegion(region);
        plugin.getLogger().at(java.util.logging.Level.INFO).log("[HyGuard] region spawn set: world=%s name=%s actor=%s pos=%s", world.getName(), region.getName(), playerRef.getUsername(), spawnPoint.toString());
        plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionSpawnSet, Map.of(
                "name", region.getName(),
                "pos", spawnPoint.toString()
        ));
        plugin.playSuccessSound(playerRef);
    }

    private void handleGui(Store<EntityStore> store, Ref<EntityStore> entityRef, PlayerRef playerRef, World world, String[] args) {
        if (!requirePermission(playerRef, plugin.getConfigSnapshot().general.guiPermission, false)) {
            return;
        }
        if (args.length >= 2) {
            Region region = resolveRegionReference(playerRef, world, args[1]);
            if (region == null) {
                plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionNotFound);
                return;
            }
            plugin.openRegionDetail(store, entityRef, playerRef, world.getName(), region.getName());
            return;
        }
        plugin.openRegionBrowser(store, entityRef, playerRef, world);
    }

    private void handleBackup(PlayerRef playerRef) {
        if (!requirePermission(playerRef, plugin.getConfigSnapshot().general.backupPermission, true)) {
            return;
        }
        plugin.send(playerRef, plugin.getConfigSnapshot().messages.backupStarted);
        plugin.performManualBackup(playerRef);
    }

    private void handleDebug(PlayerRef playerRef, World world, String[] args) {
        if (!requirePermission(playerRef, plugin.getConfigSnapshot().general.debugPermission, true)) {
            return;
        }
        if (args.length < 2 || !"pos".equalsIgnoreCase(args[1])) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidDebugCommand);
            return;
        }
        if (playerRef == null || playerRef.getTransform() == null || playerRef.getTransform().getPosition() == null) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.debugPosNone);
            return;
        }

        BlockPos position = new BlockPos(
                (int) Math.floor(playerRef.getTransform().getPosition().getX()),
                (int) Math.floor(playerRef.getTransform().getPosition().getY()),
                (int) Math.floor(playerRef.getTransform().getPosition().getZ())
        );
        List<Region> regions = plugin.getRegionsAt(world.getName(), position);
        if (regions.isEmpty()) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.debugPosNone);
            return;
        }

        plugin.send(playerRef, plugin.getConfigSnapshot().messages.debugPosHeader);
        for (Region region : regions) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.debugPosEntry, Map.of(
                    "name", region.getName(),
                    "priority", Integer.toString(region.getPriority()),
                    "owner", region.getOwnerName(),
                    "flags", describeActiveFlags(region)
            ));
        }
    }

    private void handleAdminOverride(PlayerRef playerRef) {
        if (!requirePermission(playerRef, plugin.getConfigSnapshot().general.bypassTogglePermission, true)) {
            return;
        }
        boolean enabled = plugin.toggleAdminOverride(playerRef);
        plugin.send(playerRef, enabled ? plugin.getConfigSnapshot().messages.bypassEnabled : plugin.getConfigSnapshot().messages.bypassDisabled);
        plugin.playSuccessSound(playerRef);
    }

    private void handleSave(PlayerRef playerRef) {
        if (!requirePermission(playerRef, plugin.getConfigSnapshot().general.savePermission, true)) {
            return;
        }
        plugin.flushRegionSaves();
        plugin.send(playerRef, plugin.getConfigSnapshot().messages.saveCompleted);
        plugin.playSuccessSound(playerRef);
    }

    private void handleReload(PlayerRef playerRef) {
        if (!requirePermission(playerRef, plugin.getConfigSnapshot().general.reloadPermission, true)) {
            return;
        }
        plugin.reloadState();
        plugin.send(playerRef, plugin.getConfigSnapshot().messages.reloadCompleted);
        plugin.playSuccessSound(playerRef);
    }

    private void sendHelp(PlayerRef playerRef, String[] args) {
        if (args.length < 2) {
            sendHelpPage(playerRef, 1);
            return;
        }

        Integer numericPage = parsePositiveInt(args[1]);
        if (numericPage != null) {
            sendHelpPage(playerRef, numericPage);
            return;
        }

        HelpTopic topic = HELP_TOPICS.get(args[1].toLowerCase(Locale.ROOT));
        if (topic == null) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.helpUnknownTopic);
            return;
        }

        plugin.send(playerRef, plugin.getConfigSnapshot().messages.helpDetailHeader, Map.of("command", topic.command()));
        plugin.send(playerRef, plugin.getConfigSnapshot().messages.helpDetailUsage, Map.of("usage", topic.usage()));
        plugin.send(playerRef, plugin.getConfigSnapshot().messages.helpDetailDescription, Map.of("description", plugin.text(playerRef, topic.descriptionKey(), Map.of())));
        plugin.send(playerRef, plugin.getConfigSnapshot().messages.helpDetailExample, Map.of("example", topic.example()));
        plugin.send(playerRef, plugin.getConfigSnapshot().messages.helpDetailPermission, Map.of("permission", plugin.text(playerRef, topic.permissionKey(), Map.of())));
    }

    private Region requireManageableRegion(PlayerRef playerRef, World world, String[] args) {
        return requireManageableRegion(playerRef, world, args, 1);
    }

    private Region requireManageableRegion(PlayerRef playerRef, World world, String[] args, int nameIndex) {
        if (args.length <= nameIndex) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionNotFound);
            return null;
        }
        Region region = resolveRegionReference(playerRef, world, args[nameIndex]);
        if (region == null) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionNotFound);
            return null;
        }
        if (!plugin.canManageRegion(playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
            return null;
        }
        return region;
    }

    private Region requireExistingRegion(PlayerRef playerRef, World world, String[] args) {
        return requireExistingRegion(playerRef, world, args, false);
    }

    private Region requireExistingRegion(PlayerRef playerRef, World world, String[] args, boolean preferGlobalAlias) {
        if (args.length < 2) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionNotFound);
            return null;
        }
        Region region = resolveRegionReference(playerRef, world, args[1], preferGlobalAlias);
        if (region == null) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionNotFound);
            return null;
        }
        return region;
    }

    private Region requireMembershipManagedRegion(PlayerRef playerRef, World world, String[] args, int nameIndex) {
        if (args.length <= nameIndex) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionNotFound);
            return null;
        }
        Region region = resolveRegionReference(playerRef, world, args[nameIndex]);
        if (region == null) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionNotFound);
            return null;
        }
        if (!plugin.canManageMembership(playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
            return null;
        }
        return region;
    }

    private Region resolveRegionReference(PlayerRef playerRef, World world, String raw) {
        return resolveRegionReference(playerRef, world, raw, false);
    }

    private Region resolveRegionReference(PlayerRef playerRef, World world, String raw, boolean preferGlobalAlias) {
        if (world == null || raw == null || raw.isBlank()) {
            return null;
        }

        String lookup = raw.trim();
        if (!isSpecialRegionAlias(lookup)) {
            String validated = validateRegionName(playerRef, lookup);
            if (validated == null) {
                return null;
            }
            lookup = validated;
        }

        if (preferGlobalAlias && isSpecialRegionAlias(lookup)) {
            Region globalRegion = plugin.findRegionByName(world.getName(), GLOBAL_REGION_NAME);
            if (globalRegion != null) {
                return globalRegion;
            }
        }

        Region directMatch = plugin.findRegionByName(world.getName(), lookup);
        if (directMatch != null) {
            return directMatch;
        }
        if (!isSpecialRegionAlias(lookup)) {
            return null;
        }
        return plugin.findRegionByName(world.getName(), GLOBAL_REGION_NAME);
    }

    private boolean isSpecialRegionAlias(String raw) {
        return "spawn".equalsIgnoreCase(raw) || "global".equalsIgnoreCase(raw);
    }

    private static String[] slice(String[] values, int startIndex) {
        int length = values.length - startIndex;
        String[] slice = new String[length];
        System.arraycopy(values, startIndex, slice, 0, length);
        return slice;
    }

    private static String[] parseArgs(String input) {
        if (input == null || input.isBlank()) {
            return new String[0];
        }
        String[] raw = input.trim().split("\\s+");
        if (raw.length > 0 && ("hg".equalsIgnoreCase(raw[0]) || "guard".equalsIgnoreCase(raw[0]))) {
            String[] trimmed = new String[raw.length - 1];
            System.arraycopy(raw, 1, trimmed, 0, trimmed.length);
            return trimmed;
        }
        return raw;
    }

    private static boolean applyExpand(BlockPos min, BlockPos max, SelectionDirection direction, int amount) {
        switch (direction) {
            case NORTH -> min.setZ(min.getZ() - amount);
            case SOUTH -> max.setZ(max.getZ() + amount);
            case EAST -> max.setX(max.getX() + amount);
            case WEST -> min.setX(min.getX() - amount);
            case UP -> max.setY(max.getY() + amount);
            case DOWN -> min.setY(min.getY() - amount);
        }
        return true;
    }

    private static boolean applyContract(BlockPos min, BlockPos max, SelectionDirection direction, int amount) {
        switch (direction) {
            case NORTH -> {
                if (min.getZ() + amount > max.getZ()) {
                    return false;
                }
                min.setZ(min.getZ() + amount);
            }
            case SOUTH -> {
                if (max.getZ() - amount < min.getZ()) {
                    return false;
                }
                max.setZ(max.getZ() - amount);
            }
            case EAST -> {
                if (max.getX() - amount < min.getX()) {
                    return false;
                }
                max.setX(max.getX() - amount);
            }
            case WEST -> {
                if (min.getX() + amount > max.getX()) {
                    return false;
                }
                min.setX(min.getX() + amount);
            }
            case UP -> {
                if (max.getY() - amount < min.getY()) {
                    return false;
                }
                max.setY(max.getY() - amount);
            }
            case DOWN -> {
                if (min.getY() + amount > max.getY()) {
                    return false;
                }
                min.setY(min.getY() + amount);
            }
        }
        return true;
    }

    private static boolean applyShift(BlockPos min, BlockPos max, SelectionDirection direction, int amount) {
        switch (direction) {
            case NORTH -> {
                min.setZ(min.getZ() - amount);
                max.setZ(max.getZ() - amount);
            }
            case SOUTH -> {
                min.setZ(min.getZ() + amount);
                max.setZ(max.getZ() + amount);
            }
            case EAST -> {
                min.setX(min.getX() + amount);
                max.setX(max.getX() + amount);
            }
            case WEST -> {
                min.setX(min.getX() - amount);
                max.setX(max.getX() - amount);
            }
            case UP -> {
                min.setY(min.getY() + amount);
                max.setY(max.getY() + amount);
            }
            case DOWN -> {
                min.setY(min.getY() - amount);
                max.setY(max.getY() - amount);
            }
        }
        return true;
    }

    private void sendHelpPage(PlayerRef playerRef, int page) {
        List<HelpTopic> topics = new ArrayList<>(HELP_TOPICS.values());
        List<String> lines = new ArrayList<>(topics.size());
        for (HelpTopic topic : topics) {
            lines.add(plugin.message(playerRef, plugin.getConfigSnapshot().messages.helpEntry, Map.of(
                    "usage", topic.usage(),
                    "description", plugin.text(playerRef, topic.descriptionKey(), Map.of())
            )));
        }
        sendPaged(playerRef, plugin.getConfigSnapshot().messages.helpPageHeader, Map.of(), lines, page, HELP_PAGE_SIZE);
    }

    private void sendPaged(PlayerRef playerRef,
                           String headerTemplate,
                           Map<String, String> replacements,
                           List<String> lines,
                           int page,
                           int pageSize) {
        if (page < 1) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidPage);
            return;
        }
        int totalPages = Math.max(1, (lines.size() + pageSize - 1) / pageSize);
        if (page > totalPages) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidPage);
            return;
        }

        Map<String, String> headerReplacements = new HashMap<>(replacements);
        headerReplacements.put("page", Integer.toString(page));
        headerReplacements.put("pages", Integer.toString(totalPages));
        plugin.send(playerRef, headerTemplate, headerReplacements);

        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(lines.size(), fromIndex + pageSize);
        for (String line : lines.subList(fromIndex, toIndex)) {
            plugin.sendRaw(playerRef, line);
        }
    }

    private int parseOptionalPage(PlayerRef playerRef, String[] args, int startIndex) {
        for (int index = startIndex; index < args.length; index++) {
            if ("--confirm".equalsIgnoreCase(args[index])) {
                continue;
            }
            Integer parsed = parsePositiveInt(args[index]);
            if (parsed == null) {
                plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidPage);
                return -1;
            }
            return parsed;
        }
        return 1;
    }

    private RegionRole parseRole(PlayerRef playerRef, String raw) {
        try {
            RegionRole role = RegionRole.valueOf(raw.toUpperCase(Locale.ROOT));
            if (!EnumSet.of(RegionRole.CO_OWNER, RegionRole.MANAGER, RegionRole.MEMBER, RegionRole.TRUSTED, RegionRole.VISITOR, RegionRole.OWNER).contains(role)) {
                plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidRole);
                return null;
            }
            return role;
        } catch (IllegalArgumentException illegalArgumentException) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidRole);
            return null;
        }
    }

    private RegionMember resolveExistingMember(Region region, String playerNameOrUuid) {
        HyGuardPlugin.PlayerIdentity identity = plugin.resolvePlayerIdentity(playerNameOrUuid);
        if (identity != null) {
            RegionMember member = region.getMember(identity.uuid());
            if (member != null) {
                return member;
            }
        }

        for (RegionMember member : region.getMembers().values()) {
            if (member.getName() != null && member.getName().equalsIgnoreCase(playerNameOrUuid)) {
                return member;
            }
            if (member.getUuid() != null && member.getUuid().equalsIgnoreCase(playerNameOrUuid)) {
                return member;
            }
        }
        return null;
    }

    private String describeActiveFlags(Region region) {
        if (region.getFlags().isEmpty()) {
            return "none";
        }
        List<String> entries = new ArrayList<>();
        region.getFlags().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    if (entry.getValue() != null && entry.getValue().getMode() != RegionFlagValue.Mode.INHERIT) {
                        entries.add(entry.getKey().name() + "=" + formatFlagValue(entry.getValue()));
                    }
                });
        return entries.isEmpty() ? "none" : String.join(", ", entries);
    }

    private String formatFlagValue(RegionFlagValue flagValue) {
        if (flagValue == null) {
            return RegionFlagValue.Mode.INHERIT.name();
        }
        String base = flagValue.getMode().name();
        return flagValue.getTextValue() == null || flagValue.getTextValue().isBlank()
                ? base
                : base + " (" + flagValue.getTextValue() + ")";
    }

    private static boolean hasConfirmFlag(String[] args) {
        for (String arg : args) {
            if ("--confirm".equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasServerFlag(String[] args) {
        for (String arg : args) {
            if ("--server".equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return false;
    }

    private static Integer parsePositiveInt(String raw) {
        try {
            int value = Integer.parseInt(raw);
            return value > 0 ? value : null;
        } catch (NumberFormatException numberFormatException) {
            return null;
        }
    }

    private String validateRegionName(PlayerRef playerRef, String raw) {
        if (raw == null || !plugin.isValidRegionName(raw)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidRegionName, Map.of(
                    "min", Integer.toString(plugin.getConfigSnapshot().limits.regionNameMinLength),
                    "max", Integer.toString(plugin.getConfigSnapshot().limits.regionNameMaxLength)
            ));
            return null;
        }
        return raw;
    }

    private void sendRegionMutationFailure(PlayerRef playerRef, HyGuardPlugin.RegionUpdateResult result, Region region) {
        switch (result) {
            case WORLD_MISMATCH -> plugin.send(playerRef, plugin.getConfigSnapshot().messages.selectionWorldMismatch);
            case OVERLAP_CONFLICT -> plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionOverlapConflict);
            case HIERARCHY_CONFLICT -> plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionHierarchyConflict);
            case CHILD_LIMIT_REACHED -> plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionChildLimitReached, Map.of(
                    "name", region == null ? "parent" : region.getName(),
                    "limit", Integer.toString(plugin.getConfigSnapshot().limits.maxChildRegionsPerParent)
            ));
            case SELECTION_INCOMPLETE -> plugin.send(playerRef, plugin.getConfigSnapshot().messages.selectionIncomplete);
            case SUCCESS -> {
            }
        }
    }

    private static Map<String, HelpTopic> createHelpTopics() {
        LinkedHashMap<String, HelpTopic> topics = new LinkedHashMap<>();
        registerHelp(topics, new HelpTopic("wand", "/hg wand", "hyguard.help.topic.wand.description", "/hg wand", "hyguard.help.permission.player"));
        registerHelp(topics, new HelpTopic("create", "/hg create <name> [--server]", "hyguard.help.topic.create.description", "/hg create spawn --server", "hyguard.help.permission.create"));
        registerHelp(topics, new HelpTopic("delete", "/hg delete <name> --confirm", "hyguard.help.topic.delete.description", "/hg delete spawn --confirm", "hyguard.help.permission.manager_or_admin"));
        registerHelp(topics, new HelpTopic("info", "/hg info [name]", "hyguard.help.topic.info.description", "/hg info spawn", "hyguard.help.permission.player"));
        registerHelp(topics, new HelpTopic("list", "/hg list", "hyguard.help.topic.list.description", "/hg list", "hyguard.help.permission.player"));
        registerHelp(topics, new HelpTopic("select", "/hg select <name>", "hyguard.help.topic.select.description", "/hg select spawn", "hyguard.help.permission.manager_or_admin"));
        registerHelp(topics, new HelpTopic("redefine", "/hg redefine <name>", "hyguard.help.topic.redefine.description", "/hg redefine spawn", "hyguard.help.permission.manager_or_admin"));
        registerHelp(topics, new HelpTopic("expand", "/hg expand <dir> <amount>", "hyguard.help.topic.expand.description", "/hg expand north 5", "hyguard.help.permission.player"));
        registerHelp(topics, new HelpTopic("contract", "/hg contract <dir> <amount>", "hyguard.help.topic.contract.description", "/hg contract up 2", "hyguard.help.permission.player"));
        registerHelp(topics, new HelpTopic("shift", "/hg shift <dir> <amount>", "hyguard.help.topic.shift.description", "/hg shift east 16", "hyguard.help.permission.player"));
        registerHelp(topics, new HelpTopic("priority", "/hg priority <name> <value>", "hyguard.help.topic.priority.description", "/hg priority spawn 10", "hyguard.help.permission.manager_or_admin"));
        registerHelp(topics, new HelpTopic("flag", "/hg flag <name> <flag> <value|clear> [text]", "hyguard.help.topic.flag.description", "/hg flag spawn PVP ALLOW", "hyguard.help.permission.manager_or_admin"));
        registerHelp(topics, new HelpTopic("flags", "/hg flags <name> [page]", "hyguard.help.topic.flags.description", "/hg flags spawn", "hyguard.help.permission.player"));
        registerHelp(topics, new HelpTopic("member", "/hg member <add|remove|role|list> ...", "hyguard.help.topic.member.description", "/hg member add spawn Player2 TRUSTED", "hyguard.help.permission.owner_coowner_admin"));
        registerHelp(topics, new HelpTopic("tp", "/hg tp <name>", "hyguard.help.topic.tp.description", "/hg tp spawn", "hyguard.help.permission.member_admin_access"));
        registerHelp(topics, new HelpTopic("setspawn", "/hg setspawn <name>", "hyguard.help.topic.setspawn.description", "/hg setspawn spawn", "hyguard.help.permission.owner_coowner_admin"));
        registerHelp(topics, new HelpTopic("gui", "/hg gui [name]", "hyguard.help.topic.gui.description", "/hg gui spawn", "hyguard.help.permission.player"));
        registerHelp(topics, new HelpTopic("backup", "/hg backup", "hyguard.help.topic.backup.description", "/hg backup", "hyguard.help.permission.admin"));
        registerHelp(topics, new HelpTopic("debug", "/hg debug pos", "hyguard.help.topic.debug.description", "/hg debug pos", "hyguard.help.permission.admin"));
        registerHelp(topics, new HelpTopic("admin-override", "/hg admin-override", "hyguard.help.topic.admin_override.description", "/hg admin-override", "hyguard.help.permission.bypass_or_admin"));
        registerHelp(topics, new HelpTopic("save", "/hg save", "hyguard.help.topic.save.description", "/hg save", "hyguard.help.permission.admin"));
        registerHelp(topics, new HelpTopic("reload", "/hg reload", "hyguard.help.topic.reload.description", "/hg reload", "hyguard.help.permission.admin"));
        return topics;
    }

    private static void registerHelp(Map<String, HelpTopic> topics, HelpTopic topic) {
        topics.put(topic.command(), topic);
    }

    private boolean requirePermission(PlayerRef playerRef, String permission, boolean allowAdminOverride) {
        boolean allowed = allowAdminOverride
                ? plugin.hasPermissionOrAdmin(playerRef, permission)
                : plugin.hasPermission(playerRef, permission);
        if (!allowed) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
        }
        return allowed;
    }
}