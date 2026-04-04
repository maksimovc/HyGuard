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
import dev.thenexusgates.hyguard.util.BlockPos;

import java.util.Locale;
import java.util.Map;

public final class RegionDetailPage extends InteractiveCustomUIPage<RegionDetailPage.PageData> {

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

    private static final String UI_PAGE = "Pages/HyGuardRegionDetail.ui";

    private final HyGuardPlugin plugin;
    private final String worldName;
    private final String regionName;
    private boolean deleteArmed;
    private String statusMessage = "Use navigation, management, and danger actions from their separate cards below.";
    private StatusTone statusTone = StatusTone.INFO;

    public RegionDetailPage(PlayerRef playerRef, HyGuardPlugin plugin, String worldName, String regionName) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.plugin = plugin;
        this.worldName = worldName;
        this.regionName = regionName;
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
            case "Teleport" -> {
                if (!plugin.canTeleportToRegion(playerRef, region)) {
                    plugin.send(playerRef, plugin.getConfigSnapshot().messages.tpDenied);
                    setStatus(StatusTone.ERROR, "You cannot teleport to this region.");
                } else if (!plugin.teleportToRegion(store, entityRef, playerRef, region)) {
                    plugin.send(playerRef, plugin.getConfigSnapshot().messages.teleportFailed);
                    setStatus(StatusTone.ERROR, "Teleport failed. The target location may be invalid.");
                } else {
                    plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionTeleported, Map.of("name", region.getName()));
                    plugin.playSuccessSound(playerRef);
                    setStatus(StatusTone.SUCCESS, "Teleported to " + region.getName() + ".");
                }
            }
            case "Select" -> {
                if (region.isGlobal()) {
                    plugin.send(playerRef, plugin.getConfigSnapshot().messages.globalSelectionUnsupported);
                    setStatus(StatusTone.WARNING, "Global regions cannot load a cuboid selection.");
                } else if (!plugin.canManageRegion(playerRef, region)) {
                    plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
                    setStatus(StatusTone.ERROR, "You do not have permission to load this region into selection.");
                } else if (plugin.loadRegionSelection(playerRef, region)) {
                    plugin.send(playerRef, plugin.getConfigSnapshot().messages.selectionLoaded, Map.of("name", region.getName()));
                    plugin.playSuccessSound(playerRef);
                    setStatus(StatusTone.SUCCESS, "Selection loaded from " + region.getName() + ".");
                }
            }
            case "SetSpawn" -> {
                if (!plugin.canManageSpawn(playerRef, region)) {
                    plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
                    setStatus(StatusTone.ERROR, "You do not have permission to change this region's spawn point.");
                } else if (playerRef.getTransform() == null || playerRef.getTransform().getPosition() == null) {
                    plugin.send(playerRef, plugin.getConfigSnapshot().messages.teleportFailed);
                    setStatus(StatusTone.ERROR, "Your current position is unavailable, so spawn cannot be set.");
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
                    setStatus(StatusTone.SUCCESS, "Spawn point saved at " + spawnPoint + ".");
                }
            }
            case "Members" -> {
                plugin.openMemberManager(store, entityRef, playerRef, worldName, regionName);
                return;
            }
            case "Flags" -> {
                plugin.openFlagEditor(store, entityRef, playerRef, worldName, regionName);
                return;
            }
            case "Delete" -> {
                if (!plugin.canManageRegion(playerRef, region)) {
                    plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
                    setStatus(StatusTone.ERROR, "You do not have permission to delete this region.");
                } else if (plugin.hasChildRegions(region)) {
                    plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionDeleteHasChildren, Map.of("name", region.getName()));
                    setStatus(StatusTone.ERROR, "Delete or move this region's child plots first.");
                } else if (!deleteArmed) {
                    deleteArmed = true;
                    setStatus(StatusTone.WARNING, "Dangerous action armed. Press Delete Region again to confirm.");
                } else if (plugin.deleteRegion(region)) {
                    plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionDeleted, Map.of("name", region.getName()));
                    plugin.playDeleteSound(playerRef);
                    plugin.openRegionBrowser(store, entityRef, playerRef);
                    return;
                }
            }
            default -> {
                return;
            }
        }

        refresh(entityRef, store);
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
        bind(evt, "#TeleportButton", "Teleport");
        bind(evt, "#SelectButton", "Select");
        bind(evt, "#SetSpawnButton", "SetSpawn");
        bind(evt, "#MembersButton", "Members");
        bind(evt, "#FlagsButton", "Flags");
        bind(evt, "#DeleteButton", "Delete");

        Region region = plugin.findRegionByName(worldName, regionName);
        if (region == null) {
            cmd.set("#PageTitle.Text", "Region Missing");
            cmd.set("#PageSubtitle.Text", "Region: " + regionName + " | World: " + worldName);
            cmd.set("#SummaryOwner.Text", "n/a");
            cmd.set("#SummaryPriority.Text", "0");
            cmd.set("#SummaryMembers.Text", "0");
            cmd.set("#SummaryParent.Text", "Missing");
            cmd.set("#SummarySpawn.Text", "n/a");
            cmd.set("#NameValue.Text", regionName);
            cmd.set("#OwnerValue.Text", "n/a");
            cmd.set("#WorldValue.Text", worldName);
            cmd.set("#BoundsValue.Text", "missing");
            cmd.set("#MembersValue.Text", "0");
            cmd.set("#PriorityValue.Text", "0");
            cmd.set("#HierarchyValue.Text", "missing");
            cmd.set("#SpawnValue.Text", "n/a");
            cmd.set("#DeleteHint.Text", "Region is missing.");
            setActionAvailability(cmd, false, false, false, false);
            applyStatus(cmd);
            return;
        }

        boolean canTeleport = plugin.canTeleportToRegion(playerRef, region);
        boolean canManage = plugin.canManageRegion(playerRef, region);
        boolean canLoadSelection = canManage && !region.isGlobal();
        boolean canSetSpawn = plugin.canManageSpawn(playerRef, region);
        setActionAvailability(cmd, canTeleport, canLoadSelection, canSetSpawn, canManage);

        cmd.set("#PageTitle.Text", region.getName());
        cmd.set("#PageSubtitle.Text", "Region: " + region.getName() + " | World: " + region.getWorldId());
        cmd.set("#SummaryOwner.Text", region.getOwnerName());
        cmd.set("#SummaryPriority.Text", String.valueOf(region.getPriority()));
        cmd.set("#SummaryMembers.Text", String.valueOf(region.getMembers().size()));
        cmd.set("#SummaryParent.Text", region.getParentRegionId() == null || region.getParentRegionId().isBlank()
                ? "Root"
                : plugin.getRegionNameById(region.getParentRegionId(), worldName));
        cmd.set("#SummarySpawn.Text", region.getSpawnPoint() == null ? "Not set" : "Set");
        cmd.set("#NameValue.Text", region.getName());
        cmd.set("#OwnerValue.Text", region.getOwnerName());
        cmd.set("#WorldValue.Text", region.getWorldId());
        cmd.set("#BoundsValue.Text", formatBounds(region));
        cmd.set("#MembersValue.Text", String.valueOf(region.getMembers().size()));
        cmd.set("#PriorityValue.Text", String.valueOf(region.getPriority()));
        cmd.set("#HierarchyValue.Text", formatHierarchy(region));
        cmd.set("#SpawnValue.Text", region.getSpawnPoint() == null ? "not set" : region.getSpawnPoint().toString());
        cmd.set("#DeleteButton.Text", deleteArmed ? "Confirm Delete" : "Delete Region");
        cmd.set("#DeleteHint.Text", !canManage
                ? "You may inspect this region, but deletion requires region-management permission."
                : deleteArmed
                    ? "Press Delete Region again to permanently remove this region."
                    : "Delete is isolated below because it permanently removes the region.");
        applyStatus(cmd);
    }

    private String formatHierarchy(Region region) {
        if (region.getParentRegionId() == null || region.getParentRegionId().isBlank()) {
            return "Root region";
        }
        return "Child of " + plugin.getRegionNameById(region.getParentRegionId(), worldName);
    }

    private String formatBounds(Region region) {
        if (region.isGlobal()) {
            return "Global region covering the entire world.";
        }
        if (region.getMin() == null || region.getMax() == null) {
            return "missing";
        }
        BlockPos min = region.getMin();
        BlockPos max = region.getMax();
        int width = max.getX() - min.getX() + 1;
        int height = max.getY() - min.getY() + 1;
        int depth = max.getZ() - min.getZ() + 1;
        return String.format(Locale.ROOT,
                "Min (%d, %d, %d) | Max (%d, %d, %d) | Size %dx%dx%d",
                min.getX(), min.getY(), min.getZ(),
                max.getX(), max.getY(), max.getZ(),
                width, height, depth);
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
}