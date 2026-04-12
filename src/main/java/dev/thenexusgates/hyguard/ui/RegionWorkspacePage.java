package dev.thenexusgates.hyguard.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.thenexusgates.hyguard.HyGuardPlugin;
import dev.thenexusgates.hyguard.core.region.Region;
import dev.thenexusgates.hyguard.map.HyGuardRegionMapStyle;
import dev.thenexusgates.hyguard.util.BlockPos;

import java.util.Locale;
import java.util.Map;

public final class RegionWorkspacePage extends InteractiveCustomUIPage<RegionWorkspacePage.PageData> {

    public enum WorkspaceTab {
        OVERVIEW,
        ACCESS,
        RULES,
        MAP;

        static WorkspaceTab fromName(String raw) {
            if (raw == null || raw.isBlank()) {
                return OVERVIEW;
            }
            try {
                return WorkspaceTab.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return OVERVIEW;
            }
        }
    }

    public enum AccessMode {
        MEMBERS,
        BLACKLIST;

        static AccessMode fromName(String raw) {
            if (raw == null || raw.isBlank()) {
                return MEMBERS;
            }
            try {
                return AccessMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return MEMBERS;
            }
        }
    }

    private enum StatusTone {
        INFO,
        SUCCESS,
        WARNING,
        ERROR
    }

    public static final class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
            .append(new KeyedCodec<>("Action", Codec.STRING), (data, value) -> data.action = value, data -> data.action)
            .add()
            .build();

        String action;
    }

    private static final String UI_PAGE = "Pages/HyGuardRegionWorkspace.ui";

    private final HyGuardPlugin plugin;
    private final String worldName;
    private final String regionName;
    private WorkspaceTab activeTab;
    private AccessMode accessMode;
    private boolean deleteArmed;
    private String statusMessage;
    private StatusTone statusTone = StatusTone.INFO;

    public RegionWorkspacePage(PlayerRef playerRef,
                               HyGuardPlugin plugin,
                               String worldName,
                               String regionName,
                               WorkspaceTab initialTab,
                               AccessMode initialAccessMode) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.plugin = plugin;
        this.worldName = worldName;
        this.regionName = regionName;
        this.activeTab = initialTab == null ? WorkspaceTab.OVERVIEW : initialTab;
        this.accessMode = initialAccessMode == null ? AccessMode.MEMBERS : initialAccessMode;
        this.statusMessage = t("Manage this region.", "Керуйте цим регіоном.");
    }

    @Override
    public void build(Ref<EntityStore> entityRef,
                      UICommandBuilder cmd,
                      UIEventBuilder evt,
                      Store<EntityStore> store) {
        render(entityRef, store, cmd, evt);
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> entityRef,
                                Store<EntityStore> store,
                                PageData data) {
        if (data == null || data.action == null) {
            return;
        }

        Region region = plugin.findRegionByName(worldName, regionName);
        if (region == null) {
            close();
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionNotFound);
            return;
        }

        switch (data.action) {
            case "Back" -> {
                plugin.openRegionBrowser(store, entityRef, playerRef);
                return;
            }
            case "Close" -> {
                close();
                return;
            }
            case "SwitchTab:Overview" -> switchTab(WorkspaceTab.OVERVIEW, null);
            case "SwitchTab:Access" -> switchTab(WorkspaceTab.ACCESS, null);
            case "SwitchTab:Rules" -> switchTab(WorkspaceTab.RULES, null);
            case "SwitchTab:Map" -> switchTab(WorkspaceTab.MAP, null);
            case "SwitchAccess:Members" -> switchTab(WorkspaceTab.ACCESS, AccessMode.MEMBERS);
            case "SwitchAccess:Blacklist" -> switchTab(WorkspaceTab.ACCESS, AccessMode.BLACKLIST);
            case "Teleport" -> teleport(region, entityRef, store);
            case "Select" -> select(region);
            case "SetSpawn" -> setSpawn(region);
            case "Delete" -> delete(region, entityRef, store);
            case "OpenMembers" -> {
                plugin.openMemberManager(store, entityRef, playerRef, worldName, regionName);
                return;
            }
            case "OpenBlacklist" -> {
                plugin.openEntryBlacklistManager(store, entityRef, playerRef, worldName, regionName);
                return;
            }
            case "OpenRules" -> {
                plugin.openFlagEditor(store, entityRef, playerRef, worldName, regionName);
                return;
            }
            case "OpenMap" -> {
                plugin.openRegionMapSettings(store, entityRef, playerRef, worldName, regionName);
                return;
            }
            default -> {
                return;
            }
        }

        refresh(entityRef, store);
    }

    private void switchTab(WorkspaceTab nextTab, AccessMode nextMode) {
        activeTab = nextTab == null ? WorkspaceTab.OVERVIEW : nextTab;
        if (nextMode != null) {
            accessMode = nextMode;
        }
        deleteArmed = false;
        statusTone = StatusTone.INFO;
        statusMessage = switch (activeTab) {
            case OVERVIEW -> t("Manage this region.", "Керуйте цим регіоном.");
            case ACCESS -> accessMode == AccessMode.BLACKLIST
                ? t("Review blocked players or open the full editor.", "Перегляньте заблокованих гравців або відкрийте повний редактор.")
                : t("Review access or open the full editor.", "Перегляньте доступ або відкрийте повний редактор.");
            case RULES -> t("Review rules or open the full editor.", "Перегляньте правила або відкрийте повний редактор.");
            case MAP -> t("Review map state or open the full editor.", "Перегляньте стан мапи або відкрийте повний редактор.");
        };
    }

    private void teleport(Region region, Ref<EntityStore> entityRef, Store<EntityStore> store) {
        if (!plugin.canTeleportToRegion(playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.tpDenied);
            setStatus(StatusTone.ERROR, t("You cannot teleport to this region.", "Ви не можете телепортуватися в цей регіон."));
        } else if (!plugin.teleportToRegion(store, entityRef, playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.teleportFailed);
            setStatus(StatusTone.ERROR, t("Teleport failed. The target location may be invalid.", "Телепортація не вдалася. Цільова точка може бути недійсною."));
        } else {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionTeleported, Map.of("name", region.getName()));
            plugin.playSuccessSound(playerRef);
            setStatus(StatusTone.SUCCESS, f("Teleported to %s.", "Телепортовано до %s.", region.getName()));
        }
    }

    private void select(Region region) {
        if (region.isGlobal()) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.globalSelectionUnsupported);
            setStatus(StatusTone.WARNING, t("Global regions cannot load a cuboid selection.", "Глобальні регіони не можуть завантажувати кубоїдне виділення."));
        } else if (!plugin.canManageRegion(playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
            setStatus(StatusTone.ERROR, t("You do not have permission to load this region into selection.", "У вас немає дозволу завантажити цей регіон у виділення."));
        } else if (plugin.loadRegionSelection(playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.selectionLoaded, Map.of("name", region.getName()));
            plugin.playSuccessSound(playerRef);
            setStatus(StatusTone.SUCCESS, f("Selection loaded from %s.", "Виділення завантажено з %s.", region.getName()));
        }
    }

    private void setSpawn(Region region) {
        if (!plugin.canManageSpawn(playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
            setStatus(StatusTone.ERROR, t("You do not have permission to change this region's spawn point.", "У вас немає дозволу змінювати точку спавну цього регіону."));
        } else if (playerRef.getTransform() == null || playerRef.getTransform().getPosition() == null) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.teleportFailed);
            setStatus(StatusTone.ERROR, t("Your current position is unavailable, so spawn cannot be set.", "Ваша поточна позиція недоступна, тому точку спавну не можна встановити."));
        } else {
            BlockPos spawnPoint = new BlockPos(
                (int) Math.floor(playerRef.getTransform().getPosition().getX()),
                (int) Math.floor(playerRef.getTransform().getPosition().getY()),
                (int) Math.floor(playerRef.getTransform().getPosition().getZ())
            );
            region.setSpawnPoint(spawnPoint);
            plugin.saveRegion(region);
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionSpawnSet, Map.of(
                "name", region.getName(),
                "pos", spawnPoint.toString()
            ));
            plugin.playSuccessSound(playerRef);
            setStatus(StatusTone.SUCCESS, f("Spawn point saved at %s.", "Точку спавну збережено на %s.", spawnPoint));
        }
    }

    private void delete(Region region, Ref<EntityStore> entityRef, Store<EntityStore> store) {
        if (!plugin.canManageRegion(playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
            setStatus(StatusTone.ERROR, t("You do not have permission to delete this region.", "У вас немає дозволу видалити цей регіон."));
        } else if (plugin.hasChildRegions(region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionDeleteHasChildren, Map.of("name", region.getName()));
            setStatus(StatusTone.ERROR, t("Delete or move this region's child plots first.", "Спочатку видаліть або перемістіть дочірні ділянки цього регіону."));
        } else if (!deleteArmed) {
            deleteArmed = true;
            setStatus(StatusTone.WARNING, t("Dangerous action armed. Press Delete Region again to confirm.", "Небезпечну дію підготовлено. Натисніть Видалити регіон ще раз для підтвердження."));
        } else if (plugin.deleteRegion(region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionDeleted, Map.of("name", region.getName()));
            plugin.playDeleteSound(playerRef);
            plugin.openRegionBrowser(store, entityRef, playerRef);
        }
    }

    private void refresh(Ref<EntityStore> entityRef, Store<EntityStore> store) {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evt = new UIEventBuilder();
        render(entityRef, store, cmd, evt);
        sendUpdate(cmd, evt, true);
    }

    private void render(Ref<EntityStore> entityRef,
                        Store<EntityStore> store,
                        UICommandBuilder cmd,
                        UIEventBuilder evt) {
        cmd.append(UI_PAGE);
        bind(evt, "#BackButton", "Back");
        bind(evt, "#CloseIconButton", "Close");
        bind(evt, "#OverviewTabButton", "SwitchTab:Overview");
        bind(evt, "#AccessTabButton", "SwitchTab:Access");
        bind(evt, "#RulesTabButton", "SwitchTab:Rules");
        bind(evt, "#MapTabButton", "SwitchTab:Map");
        bind(evt, "#AccessMembersModeButton", "SwitchAccess:Members");
        bind(evt, "#AccessBlacklistModeButton", "SwitchAccess:Blacklist");
        bind(evt, "#TeleportButton", "Teleport");
        bind(evt, "#SelectButton", "Select");
        bind(evt, "#SetSpawnButton", "SetSpawn");
        bind(evt, "#DeleteButton", "Delete");
        bind(evt, "#OpenMembersButton", "OpenMembers");
        bind(evt, "#OpenBlacklistButton", "OpenBlacklist");
        bind(evt, "#OpenRulesButton", "OpenRules");
        bind(evt, "#OpenMapButton", "OpenMap");

        Region region = plugin.findRegionByName(worldName, regionName);
        if (region == null) {
            cmd.set("#PageTitle.Text", t("Region Missing", "Регіон відсутній"));
            cmd.set("#PageSubtitle.Text", f("Region: %s | World: %s", "Регіон: %s | Світ: %s", regionName, worldName));
            cmd.set("#SummaryOwner.Text", t("n/a", "н/д"));
            cmd.set("#SummaryPriority.Text", "0");
            cmd.set("#SummaryMembers.Text", "0");
            cmd.set("#SummaryParent.Text", t("Missing", "Відсутній"));
            cmd.set("#SummaryMap.Text", t("Missing", "Відсутній"));
            cmd.set("#NameValue.Text", regionName);
            cmd.set("#OwnerValue.Text", t("n/a", "н/д"));
            cmd.set("#WorldValue.Text", worldName);
            cmd.set("#BoundsValue.Text", t("missing", "відсутні"));
            cmd.set("#MembersValue.Text", "0");
            cmd.set("#PriorityValue.Text", "0");
            cmd.set("#HierarchyValue.Text", t("missing", "відсутня"));
            cmd.set("#SpawnValue.Text", t("n/a", "н/д"));
            cmd.set("#AccessSummaryValue.Text", t("Region missing", "Регіон відсутній"));
            cmd.set("#AccessSelectedName.Text", t("n/a", "н/д"));
            cmd.set("#AccessSelectedDetail.Text", t("Openers unavailable.", "Відкривачі недоступні."));
            cmd.set("#RulesSummaryValue.Text", t("Region missing", "Регіон відсутній"));
            cmd.set("#RulesCategoryValue.Text", t("n/a", "н/д"));
            cmd.set("#RulesHintValue.Text", t("Openers unavailable.", "Відкривачі недоступні."));
            cmd.set("#MapVisibleValue.Text", t("n/a", "н/д"));
            cmd.set("#MapColorValue.Text", t("n/a", "н/д"));
            cmd.set("#MapHintValue.Text", t("Openers unavailable.", "Відкривачі недоступні."));
            cmd.set("#MapPreviewColor.Background", "#404040");
            setActionAvailability(cmd, false, false, false, false);
            setPanelState(cmd);
            applyStatus(cmd);
            return;
        }

        boolean canTeleport = plugin.canTeleportToRegion(playerRef, region);
        boolean canManage = plugin.canManageRegion(playerRef, region);
        boolean canLoadSelection = canManage && !region.isGlobal();
        boolean canSetSpawn = plugin.canManageSpawn(playerRef, region);
        setActionAvailability(cmd, canTeleport, canLoadSelection, canSetSpawn, canManage);
        setPanelState(cmd);

        cmd.set("#PageTitle.Text", region.getName());
        cmd.set("#PageSubtitle.Text", f("Region: %s | World: %s", "Регіон: %s | Світ: %s", region.getName(), region.getWorldId()));
        cmd.set("#SummaryOwner.Text", region.getOwnerName());
        cmd.set("#SummaryPriority.Text", String.valueOf(region.getPriority()));
        cmd.set("#SummaryMembers.Text", String.valueOf(region.getMembers().size()));
        cmd.set("#SummaryParent.Text", region.getParentRegionId() == null || region.getParentRegionId().isBlank()
            ? t("Root", "Корінь")
            : plugin.getRegionNameById(region.getParentRegionId(), worldName));
        cmd.set("#SummaryMap.Text", summarizeMap(region));

        cmd.set("#NameValue.Text", region.getName());
        cmd.set("#OwnerValue.Text", region.getOwnerName());
        cmd.set("#WorldValue.Text", region.getWorldId());
        cmd.set("#BoundsValue.Text", formatBounds(region));
        cmd.set("#MembersValue.Text", String.valueOf(region.getMembers().size()));
        cmd.set("#PriorityValue.Text", String.valueOf(region.getPriority()));
        cmd.set("#HierarchyValue.Text", formatHierarchy(region));
        cmd.set("#SpawnValue.Text", region.getSpawnPoint() == null ? t("not set", "не задано") : region.getSpawnPoint().toString());
        cmd.set("#DeleteButton.Text", deleteArmed ? t("Підтвердити", "Підтвердити") : t("Видалити", "Видалити"));
        cmd.set("#DeleteHint.Text", !canManage
            ? t("Locked", "Заблоковано")
            : deleteArmed
                ? t("Press again to delete.", "Натисніть ще раз для видалення.")
                : t("Permanent action.", "Незворотна дія."));

        int blacklistCount = plugin.getEntryBlacklist(region).size();
        cmd.set("#AccessSummaryValue.Text", accessMode == AccessMode.BLACKLIST
            ? f("Blocked players: %d", "Заблоковані гравці: %d", blacklistCount)
            : f("Members: %d | Blocked: %d", "Учасники: %d | Заблоковано: %d", region.getMembers().size() + 1, blacklistCount));
        cmd.set("#AccessSelectedName.Text", accessMode == AccessMode.BLACKLIST
            ? t("Blacklist mode", "Режим чорного списку")
            : t("Members mode", "Режим учасників"));
        cmd.set("#AccessSelectedDetail.Text", accessMode == AccessMode.BLACKLIST
            ? t("Open the legacy blacklist editor while this section is being merged.", "Відкрийте legacy-редактор чорного списку, поки секція ще зливається.")
            : t("Open the legacy member editor while this section is being merged.", "Відкрийте legacy-редактор учасників, поки секція ще зливається."));

        cmd.set("#RulesSummaryValue.Text", f("Active flags: %d", "Активні прапори: %d", region.getFlags().size()));
        cmd.set("#RulesCategoryValue.Text", t("Compact merge in progress", "Компактне злиття триває"));
        cmd.set("#RulesHintValue.Text", t("Open the legacy rules editor from here until inline rows are merged.", "Відкрийте legacy-редактор правил звідси, доки inline-рядки ще зливаються."));

        String mapColor = HyGuardRegionMapStyle.resolveColorHex(region);
        cmd.set("#MapVisibleValue.Text", HyGuardRegionMapStyle.isVisible(region) ? t("Visible", "Видимий") : t("Hidden", "Прихований"));
        cmd.set("#MapColorValue.Text", mapColor);
        cmd.set("#MapHintValue.Text", t("Open the legacy map editor for color and visibility changes.", "Відкрийте legacy-редактор мапи для зміни кольору і видимості."));
        cmd.set("#MapPreviewColor.Background", mapColor.toLowerCase(Locale.ROOT));
        applyStatus(cmd);
    }

    private void setPanelState(UICommandBuilder cmd) {
        cmd.set("#OverviewTabSelected.Visible", activeTab == WorkspaceTab.OVERVIEW);
        cmd.set("#AccessTabSelected.Visible", activeTab == WorkspaceTab.ACCESS);
        cmd.set("#RulesTabSelected.Visible", activeTab == WorkspaceTab.RULES);
        cmd.set("#MapTabSelected.Visible", activeTab == WorkspaceTab.MAP);
        cmd.set("#OverviewPanel.Visible", activeTab == WorkspaceTab.OVERVIEW);
        cmd.set("#AccessPanel.Visible", activeTab == WorkspaceTab.ACCESS);
        cmd.set("#RulesPanel.Visible", activeTab == WorkspaceTab.RULES);
        cmd.set("#MapPanel.Visible", activeTab == WorkspaceTab.MAP);
        cmd.set("#AccessMembersModeSelected.Visible", accessMode == AccessMode.MEMBERS);
        cmd.set("#AccessBlacklistModeSelected.Visible", accessMode == AccessMode.BLACKLIST);
    }

    private void setActionAvailability(UICommandBuilder cmd,
                                       boolean canTeleport,
                                       boolean canLoadSelection,
                                       boolean canSetSpawn,
                                       boolean canDelete) {
        cmd.set("#TeleportButton.Visible", canTeleport);
        cmd.set("#TeleportButtonDisabled.Visible", !canTeleport);
        cmd.set("#SelectButton.Visible", canLoadSelection);
        cmd.set("#SelectButtonDisabled.Visible", !canLoadSelection);
        cmd.set("#SetSpawnButton.Visible", canSetSpawn);
        cmd.set("#SetSpawnButtonDisabled.Visible", !canSetSpawn);
        cmd.set("#DeleteButton.Visible", canDelete);
        cmd.set("#DeleteButtonDisabled.Visible", !canDelete);
    }

    private String summarizeMap(Region region) {
        return f("%s • %s", "%s • %s",
            HyGuardRegionMapStyle.isVisible(region) ? t("Visible", "Видимий") : t("Hidden", "Прихований"),
            HyGuardRegionMapStyle.resolveColorHex(region));
    }

    private String formatHierarchy(Region region) {
        if (region.getParentRegionId() == null || region.getParentRegionId().isBlank()) {
            return t("Root", "Корінь");
        }
        return f("Parent: %s", "Батько: %s", plugin.getRegionNameById(region.getParentRegionId(), worldName));
    }

    private String formatBounds(Region region) {
        if (region.isGlobal()) {
            return t("Global region covering the entire world.", "Глобальний регіон, що покриває весь світ.");
        }
        if (region.getMin() == null || region.getMax() == null) {
            return t("missing", "відсутні");
        }
        BlockPos min = region.getMin();
        BlockPos max = region.getMax();
        int width = max.getX() - min.getX() + 1;
        int height = max.getY() - min.getY() + 1;
        int depth = max.getZ() - min.getZ() + 1;
        return f(
            "X %d..%d | Y %d..%d | Z %d..%d | S %dx%dx%d",
            "X %d..%d | Y %d..%d | Z %d..%d | Р %dx%dx%d",
            min.getX(), max.getX(),
            min.getY(), max.getY(),
            min.getZ(), max.getZ(),
            width, height, depth
        );
    }

    private void applyStatus(UICommandBuilder cmd) {
        cmd.set("#StatusText.Text", statusMessage == null ? "" : statusMessage);
        cmd.set("#StatusInfo.Visible", statusTone == StatusTone.INFO);
        cmd.set("#StatusSuccess.Visible", statusTone == StatusTone.SUCCESS);
        cmd.set("#StatusWarning.Visible", statusTone == StatusTone.WARNING);
        cmd.set("#StatusError.Visible", statusTone == StatusTone.ERROR);
    }

    private void setStatus(StatusTone tone, String message) {
        statusTone = tone == null ? StatusTone.INFO : tone;
        statusMessage = message == null ? "" : message;
    }

    private void bind(UIEventBuilder evt, String selector, String action) {
        evt.addEventBinding(CustomUIEventBindingType.Activating, selector, EventData.of("Action", action), false);
    }

    private String t(String english, String ukrainian) {
        return UiText.choose(playerRef, english, ukrainian);
    }

    private String f(String english, String ukrainian, Object... args) {
        return UiText.format(playerRef, english, ukrainian, args);
    }
}