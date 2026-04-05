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

    public RegionBrowserPage(PlayerRef playerRef, HyGuardPlugin plugin) {
        this(playerRef, plugin, null, null);
    }

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
                if (store == null) {
                    close();
                    return;
                }
                plugin.openRegionBrowser(store, entityRef, playerRef);
            }
            case "Close" -> close();
            default -> {
                if (data.action.startsWith("Children:")) {
                    Region region = plugin.findRegionById(data.action.substring("Children:".length()));
                    if (region != null) {
                        plugin.openChildRegionBrowser(store, entityRef, playerRef, region.getWorldId(), region.getName());
                    }
                    return;
                }
                if (!data.action.startsWith("Region:")) {
                    return;
                }
                Region region = plugin.findRegionById(data.action.substring("Region:".length()));
                if (region != null) {
                    plugin.openRegionDetail(store, entityRef, playerRef, region.getWorldId(), region.getName());
                }
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
        boolean allWorldsMode = !childBrowserMode && (worldName == null || worldName.isBlank());
        List<Region> allRegions = childBrowserMode
            ? plugin.getWorldRegions(worldName)
            : (allWorldsMode ? plugin.getAllRegions() : plugin.getWorldRegions(worldName));
        List<Region> regions = childBrowserMode
            ? resolveChildRegions()
            : (allWorldsMode ? plugin.getAllDisplayRootRegions() : plugin.getDisplayRootRegions(worldName));

        cmd.set("#WorldValue.Text", describeWorldLabel(childBrowserMode, allWorldsMode));
        cmd.set("#BackButton.Visible", childBrowserMode);

        cmd.set("#PageTitle.Text", describePageTitle(childBrowserMode, allWorldsMode, allRegions.isEmpty(), regions));
        cmd.set("#CountValue.Text", String.valueOf(childBrowserMode ? regions.size() : allRegions.size()));
        cmd.set("#HelpText.Text", describeHelpText(childBrowserMode, allWorldsMode));

        bind(evt, "#BackButton", "Back");
        bind(evt, "#CloseIconButton", "Close");

        if (regions.isEmpty()) {
            addRow(cmd, evt, 0,
                childBrowserMode
                    ? t("No child regions found", "Внутрішніх регіонів не знайдено")
                    : t("No regions found", "Регіонів не знайдено"),
                childBrowserMode
                    ? t("This region does not have internal child plots.", "У цього регіону ще немає внутрішніх дочірніх ділянок.")
                    : t("Create one with /hg create <name>", "Створіть регіон через /hg create <name>"),
                childBrowserMode
                    ? t("When this parent region gains internal claims, they will appear in this separate list.", "Коли в цього батьківського регіону з'являться внутрішні ділянки, вони з'являться в цьому окремому списку.")
                    : t("The browser will list regions here once the world has at least one saved region.", "Браузер покаже регіони тут, щойно у світі з'явиться хоча б один збережений регіон."),
                    null,
                    null,
                    null);
            return;
        }

        int index = 0;
        for (Region region : regions) {
            addRegionRow(cmd, evt, index++, region, childBrowserMode, allWorldsMode);
        }
    }

    private String describeWorldLabel(boolean childBrowserMode, boolean allWorldsMode) {
        if (childBrowserMode) {
            return worldName;
        }
        if (allWorldsMode) {
            return f("All worlds (%d)", "Усі світи (%d)", plugin.getKnownWorldIds().size());
        }
        return worldName;
    }

    private List<Region> resolveChildRegions() {
        Region parentRegion = plugin.findRegionByName(worldName, parentRegionName);
        return parentRegion == null ? List.of() : plugin.getDisplayChildRegions(parentRegion);
    }

    private String describePageTitle(boolean childBrowserMode,
                                     boolean allWorldsMode,
                                     boolean noWorldRegions,
                                     List<Region> regions) {
        if (childBrowserMode) {
            return regions.isEmpty()
                    ? f("No internal regions exist for %s.", "Для %s внутрішніх регіонів не існує.", parentRegionName)
                    : f("Internal regions of %s.", "Внутрішні регіони %s.", parentRegionName);
        }
        if (allWorldsMode) {
            return noWorldRegions
                    ? t("No regions are defined in any loaded world yet.", "У жодному завантаженому світі ще немає визначених регіонів.")
                    : t("Browse parent regions across every loaded world.", "Переглядайте батьківські регіони в усіх завантажених світах.");
        }
        return noWorldRegions
                ? t("No regions are defined in this world yet.", "У цьому світі ще немає визначених регіонів.")
                : t("Browse parent regions. Open child plots only when you need to manage them separately.", "Переглядайте батьківські регіони. Відкривайте дочірні ділянки лише тоді, коли треба керувати ними окремо.");
    }

    private String describeHelpText(boolean childBrowserMode, boolean allWorldsMode) {
        if (childBrowserMode) {
            return t("Child plots are shown here separately so the main browser stays focused on parent regions.", "Дочірні ділянки показуються тут окремо, щоб головний браузер залишався зосередженим на батьківських регіонах.");
        }
        if (allWorldsMode) {
            return t("This browser now merges saved regions from every world. Open any card to manage it in its own world context.", "Цей браузер тепер об'єднує збережені регіони з усіх світів. Відкрийте будь-яку картку, щоб керувати нею в її власному контексті світу.");
        }
        return t("Use /hg create <name> after making a selection to add a new region.", "Використайте /hg create <name> після виділення, щоб додати новий регіон.");
    }

    private void addRegionRow(UICommandBuilder cmd,
                              UIEventBuilder evt,
                              int index,
                              Region region,
                              boolean childBrowserMode,
                              boolean allWorldsMode) {
        String title = formatRegionTitle(region, childBrowserMode);
        String subtitle = allWorldsMode
            ? f("World: %s | Owner: %s", "Світ: %s | Власник: %s", region.getWorldId(), region.getOwnerName())
            : f("Owner: %s", "Власник: %s", region.getOwnerName());
        String detail = formatRegionDetail(region, allWorldsMode);
        List<Region> childRegions = plugin.getDisplayChildRegions(region);
        String childAction = childRegions.isEmpty() ? null : "Children:" + region.getId();
        String childLabel = childRegions.isEmpty() ? null : f("Child plots (%d)", "Дочірні ділянки (%d)", childRegions.size());
        addRow(cmd, evt, index, title, subtitle, detail, "Region:" + region.getId(), childAction, childLabel);
    }

    private String formatRegionTitle(Region region, boolean childBrowserMode) {
        if (!childBrowserMode) {
            return region.getName();
        }
        return region.getName();
    }

    private String formatRegionDetail(Region region, boolean allWorldsMode) {
        StringBuilder detail = new StringBuilder();
        if (region.isGlobal()) {
            detail.append(t("Global region", "Глобальний регіон"));
        } else if (region.getParentRegionId() == null || region.getParentRegionId().isBlank()) {
            detail.append(t("Root region", "Кореневий регіон"));
        } else {
            detail.append(t("Child of ", "Дочірній для "))
                    .append(plugin.getRegionNameById(region.getParentRegionId(), worldName));
        }

        if (allWorldsMode) {
            detail.append(t(" | World: ", " | Світ: ")).append(region.getWorldId());
        }

        detail.append(t(" | Priority: ", " | Пріоритет: "))
                .append(region.getPriority())
                .append(t(" | Members: ", " | Учасники: "))
                .append(region.getMembers().size());

        int childCount = plugin.getDisplayChildRegions(region).size();
        if (childCount > 0) {
            detail.append(t(" | Children: ", " | Дочірні: ")).append(childCount);
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
        cmd.set(rowId + " #RowHint.Text", action == null ? t("Info", "Інфо") : t("Open region", "Відкрити регіон"));
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

    private String t(String english, String ukrainian) {
        return UiText.choose(playerRef, english, ukrainian);
    }

    private String f(String english, String ukrainian, Object... args) {
        return UiText.format(playerRef, english, ukrainian, args);
    }
}