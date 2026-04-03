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

    public RegionBrowserPage(PlayerRef playerRef, HyGuardPlugin plugin, String worldName) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.plugin = plugin;
        this.worldName = worldName;
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
            case "Close" -> close();
            default -> {
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
        cmd.set("#WorldValue.Text", worldName);

        List<Region> regions = plugin.getWorldRegions(worldName);
        cmd.set("#PageTitle.Text", regions.isEmpty()
            ? "No regions are defined in this world yet."
            : "Hover a region card for details, then open it to inspect or manage that region.");
        cmd.set("#CountValue.Text", String.valueOf(regions.size()));
        cmd.set("#HelpText.Text", "Use /hg create <name> after making a selection to add a new region.");

        bind(evt, "#CloseIconButton", "Close");

        if (regions.isEmpty()) {
            addRow(cmd, evt, 0, "No regions found", "Create one with /hg create <name>", "The browser will list regions here once the world has at least one saved region.", null);
            return;
        }

        int index = 0;
        for (Region region : regions) {
            String subtitle = "Owner: " + region.getOwnerName();
            String parentName = region.getParentRegionId() == null
                    ? "root"
                    : plugin.getRegionNameById(region.getParentRegionId(), worldName);
            String detail = region.isGlobal()
                ? "Global region | Priority: " + region.getPriority() + " | Members: " + region.getMembers().size()
                : "Priority: " + region.getPriority() + " | Members: " + region.getMembers().size()
                + " | Parent: " + parentName;
            addRow(cmd, evt, index++, region.getName(), subtitle, detail, "Region:" + region.getName());
        }
    }

    private void addRow(UICommandBuilder cmd,
                        UIEventBuilder evt,
                        int index,
                        String title,
                        String subtitle,
                        String detail,
                        String action) {
        cmd.append(GROUP_ROOT, UI_ROW);
        String rowId = GROUP_ROOT + "[" + index + "]";
        cmd.set(rowId + " #RowTitle.Text", title);
        cmd.set(rowId + " #RowHint.Text", action == null ? "Info" : "Open region");
        cmd.set(rowId + " #RowSubtitle.Text", subtitle == null ? "" : subtitle);
        cmd.set(rowId + " #RowDetail.Text", detail == null ? "" : detail);
        if (action != null) {
            evt.addEventBinding(CustomUIEventBindingType.Activating, rowId, EventData.of("Action", action), false);
        }
    }

    private void bind(UIEventBuilder evt, String selector, String action) {
        evt.addEventBinding(CustomUIEventBindingType.Activating, selector, EventData.of("Action", action), false);
    }
}