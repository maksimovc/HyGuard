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

import java.util.List;

public final class RegionBrowserPage extends InteractiveCustomUIPage<RegionBrowserPage.PageData> {

    public static final class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (data, value) -> data.action = value, data -> data.action)
                .add()
                .build();

        String action;
    }

    private static final String UI_PAGE = "Pages/HyGuardRegionBrowser.ui";
    private static final String UI_ROW = "Pages/HyGuardRegionRow.ui";
    private static final String GROUP_ROOT = "#RegionGroup";

    private final HyGuardPlugin plugin;
    private final String worldName;
    private final String parentRegionName;

    public RegionBrowserPage(PlayerRef playerRef, HyGuardPlugin plugin, String worldName) {
        this(playerRef, plugin, worldName, null);
    }

    public RegionBrowserPage(PlayerRef playerRef, HyGuardPlugin plugin, String worldName, String parentRegionName) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.plugin = plugin;
        this.worldName = worldName;
        this.parentRegionName = parentRegionName;
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

        switch (data.action) {
            case "Back" -> {
                if (store == null || store.getExternalData() == null || store.getExternalData().getWorld() == null) {
                    close();
                    return;
                }
                plugin.openRegionBrowser(store, entityRef, playerRef, store.getExternalData().getWorld());
            }
            case "Close" -> close();
            default -> {
                if (data.action.startsWith("Children:")) {
                    plugin.openChildRegionBrowser(store, entityRef, playerRef, worldName, data.action.substring("Children:".length()));
                    return;
                }
                if (!data.action.startsWith("Region:")) {
                    return;
                }
                plugin.openRegionDetail(store, entityRef, playerRef, worldName, data.action.substring("Region:".length()));
            }
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
        boolean childBrowserMode = parentRegionName != null && !parentRegionName.isBlank();
        cmd.set("#WorldValue.Text", worldName);
        cmd.set("#BackButton.Visible", childBrowserMode);

        List<Region> allRegions = plugin.getWorldRegions(worldName);
        List<Region> regions = childBrowserMode ? resolveChildRegions() : plugin.getDisplayRootRegions(worldName);
        cmd.set("#PageTitle.Text", describePageTitle(childBrowserMode, allRegions.isEmpty(), regions));
        cmd.set("#CountValue.Text", String.valueOf(childBrowserMode ? regions.size() : allRegions.size()));
        cmd.set("#HelpText.Text", childBrowserMode
                ? "Child plots are shown here separately so the main browser stays focused on parent regions."
                : "Use /hg create <name> after making a selection to add a new region.");

        bind(evt, "#BackButton", "Back");
        bind(evt, "#CloseIconButton", "Close");

        if (regions.isEmpty()) {
            addRow(cmd, evt, 0,
                    childBrowserMode ? "No child regions found" : "No regions found",
                    childBrowserMode ? "This region does not have internal child plots." : "Create one with /hg create <name>",
                    childBrowserMode
                            ? "When this parent region gains internal claims, they will appear in this separate list."
                            : "The browser will list regions here once the world has at least one saved region.",
                    null,
                    null,
                    null);
            return;
        }

        int index = 0;
        for (Region region : regions) {
            addRegionRow(cmd, evt, index++, region, childBrowserMode);
        }
    }

    private List<Region> resolveChildRegions() {
        Region parentRegion = plugin.findRegionByName(worldName, parentRegionName);
        return parentRegion == null ? List.of() : plugin.getDisplayChildRegions(parentRegion);
    }

    private String describePageTitle(boolean childBrowserMode, boolean noWorldRegions, List<Region> regions) {
        if (childBrowserMode) {
            return regions.isEmpty()
                    ? "No internal regions exist for " + parentRegionName + "."
                    : "Internal regions of " + parentRegionName + ".";
        }
        return noWorldRegions
                ? "No regions are defined in this world yet."
                : "Browse parent regions. Open child plots only when you need to manage them separately.";
    }

    private void addRegionRow(UICommandBuilder cmd,
                              UIEventBuilder evt,
                              int index,
                              Region region,
                              boolean childBrowserMode) {
        String title = formatRegionTitle(region, childBrowserMode);
        String subtitle = "Owner: " + region.getOwnerName();
        String detail = formatRegionDetail(region);
        List<Region> childRegions = plugin.getDisplayChildRegions(region);
        String childAction = childRegions.isEmpty() ? null : "Children:" + region.getName();
        String childLabel = childRegions.isEmpty() ? null : "Child plots (" + childRegions.size() + ")";
        addRow(cmd, evt, index, title, subtitle, detail, "Region:" + region.getName(), childAction, childLabel);
    }

    private String formatRegionTitle(Region region, boolean childBrowserMode) {
        if (!childBrowserMode) {
            return region.getName();
        }
        return region.getName();
    }

    private String formatRegionDetail(Region region) {
        StringBuilder detail = new StringBuilder();
        if (region.isGlobal()) {
            detail.append("Global region");
        } else if (region.getParentRegionId() == null || region.getParentRegionId().isBlank()) {
            detail.append("Root region");
        } else {
            detail.append("Child of ")
                    .append(plugin.getRegionNameById(region.getParentRegionId(), worldName));
        }

        detail.append(" | Priority: ")
                .append(region.getPriority())
                .append(" | Members: ")
                .append(region.getMembers().size());

        int childCount = plugin.getDisplayChildRegions(region).size();
        if (childCount > 0) {
            detail.append(" | Children: ").append(childCount);
        }
        return detail.toString();
    }

    private void addRow(UICommandBuilder cmd,
                        UIEventBuilder evt,
                        int index,
                        String title,
                        String subtitle,
                        String detail,
                        String action,
                        String childAction,
                        String childLabel) {
        cmd.append(GROUP_ROOT, UI_ROW);
        String rowId = GROUP_ROOT + "[" + index + "]";
        cmd.set(rowId + " #RowTitle.Text", title);
        cmd.set(rowId + " #RowHint.Text", action == null ? "Info" : "Open region");
        cmd.set(rowId + " #RowSubtitle.Text", subtitle == null ? "" : subtitle);
        cmd.set(rowId + " #RowDetail.Text", detail == null ? "" : detail);
        cmd.set(rowId + " #ChildrenButton.Visible", childAction != null);
        cmd.set(rowId + " #ChildrenButtonText.Text", childLabel == null ? "" : childLabel);
        if (action != null) {
            evt.addEventBinding(CustomUIEventBindingType.Activating, rowId + " #OpenRegionButton", EventData.of("Action", action), false);
        }
        if (childAction != null) {
            evt.addEventBinding(CustomUIEventBindingType.Activating, rowId + " #ChildrenButton", EventData.of("Action", childAction), false);
        }
    }

    private void bind(UIEventBuilder evt, String selector, String action) {
        evt.addEventBinding(CustomUIEventBindingType.Activating, selector, EventData.of("Action", action), false);
    }
}