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

import java.util.Locale;

public final class RegionMapSettingsPage extends InteractiveCustomUIPage<RegionMapSettingsPage.PageData> {

    private enum StatusTone {
        INFO,
        SUCCESS,
        WARNING,
        ERROR
    }

    public static final class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (data, value) -> data.action = value, data -> data.action).add()
                .append(new KeyedCodec<>("@HexInput", Codec.STRING), (data, value) -> data.hexInput = value, data -> data.hexInput).add()
                .build();

        String action;
        String hexInput;
    }

    private static final String UI_PAGE = "Pages/HyGuardRegionMapSettings.ui";
    private static final String LEGACY_OPACITY_KEY = "mapOverlay.opacity";

    private final HyGuardPlugin plugin;
    private final String worldName;
    private final String regionName;

    private boolean inputsInitialized;
    private boolean desiredVisibility = HyGuardRegionMapStyle.DEFAULT_ENABLED;
    private String selectedColorHex = toHex(HyGuardRegionMapStyle.defaultColor());
    private String hexInput = toHex(HyGuardRegionMapStyle.defaultColor()).substring(1);
    private String statusMessage;
    private StatusTone statusTone = StatusTone.INFO;

    public RegionMapSettingsPage(PlayerRef playerRef, HyGuardPlugin plugin, String worldName, String regionName) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.plugin = plugin;
        this.worldName = worldName;
        this.regionName = regionName;
        this.statusMessage = t(
            "Map settings.",
            "Налаштування мапи."
        );
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
        if (data == null) {
            return;
        }
        if (data.hexInput != null) {
            hexInput = sanitizeHexInput(data.hexInput);
        }
        if (data.action == null) {
            return;
        }

        Region region = plugin.findRegionByName(worldName, regionName);
        if (region == null) {
            close();
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.regionNotFound);
            return;
        }

        seedInputs(region);

        switch (data.action) {
            case "Back" -> {
                plugin.openRegionWorkspace(store, entityRef, playerRef, worldName, regionName, RegionWorkspacePage.WorkspaceTab.MAP);
                return;
            }
            case "Close" -> {
                close();
                return;
            }
            case "Enable" -> applyVisibility(region, true);
            case "Disable" -> applyVisibility(region, false);
            case "ApplyCustomColor" -> applyCustomColor(region);
            case "Reset" -> resetStyle(region);
            default -> {
                return;
            }
        }

        refresh(entityRef, store);
    }

    private void applyVisibility(Region region, boolean visible) {
        if (!plugin.canManageRegion(playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
            setStatus(StatusTone.ERROR, t(
                    "You do not have permission to change map settings for this region.",
                    "У вас немає дозволу змінювати налаштування мапи для цього регіону."
            ));
            return;
        }

        region.putMetadata(HyGuardRegionMapStyle.ENABLED_KEY, String.valueOf(visible));
        plugin.saveRegion(region);
        forceWorldMapRefresh(region);
        desiredVisibility = visible;
        plugin.playSuccessSound(playerRef);
        setStatus(StatusTone.SUCCESS, visible
                ? t("This region is now visible on the world map.", "Тепер цей регіон видно на мапі світу.")
                : t("This region is now hidden from the world map.", "Тепер цей регіон приховано з мапи світу."));
    }

    private void applyColor(Region region, String requestedHex) {
        if (!plugin.canManageRegion(playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
            setStatus(StatusTone.ERROR, t(
                    "You do not have permission to change map settings for this region.",
                    "У вас немає дозволу змінювати налаштування мапи для цього регіону."
            ));
            return;
        }

        String normalizedColor = HyGuardRegionMapStyle.normalizeColor(requestedHex);
        if (normalizedColor == null) {
            setStatus(StatusTone.ERROR, t(
                    "Use a 6-character hex color such as 068F4B.",
                    "Використайте 6-символьний hex колір, наприклад 068F4B."
            ));
            return;
        }

        region.putMetadata(HyGuardRegionMapStyle.COLOR_KEY, normalizedColor);
        plugin.saveRegion(region);
        forceWorldMapRefresh(region);
        selectedColorHex = normalizedColor;
        hexInput = normalizedColor.substring(1);
        plugin.playSuccessSound(playerRef);
        setStatus(StatusTone.SUCCESS, t(
                "Map color updated.",
                "Колір мапи оновлено."
        ));
    }

    private void applyCustomColor(Region region) {
        if (!plugin.canManageRegion(playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
            setStatus(StatusTone.ERROR, t(
                    "You do not have permission to change map settings for this region.",
                    "У вас немає дозволу змінювати налаштування мапи для цього регіону."
            ));
            return;
        }

        String normalizedColor = HyGuardRegionMapStyle.normalizeColor(hexInput);
        if (normalizedColor == null) {
            setStatus(StatusTone.ERROR, t(
                    "Use a 6-character hex color such as 068F4B.",
                    "Використайте 6-символьний hex колір, наприклад 068F4B."
            ));
            return;
        }

        applyColor(region, normalizedColor);
    }

    private void resetStyle(Region region) {
        if (!plugin.canManageRegion(playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
            setStatus(StatusTone.ERROR, t(
                    "You do not have permission to change map settings for this region.",
                    "У вас немає дозволу змінювати налаштування мапи для цього регіону."
            ));
            return;
        }

        region.removeMetadata(HyGuardRegionMapStyle.ENABLED_KEY);
        region.removeMetadata(HyGuardRegionMapStyle.COLOR_KEY);
        region.removeMetadata(HyGuardRegionMapStyle.SHOW_LABEL_KEY);
        region.removeMetadata(LEGACY_OPACITY_KEY);
        plugin.saveRegion(region);
        forceWorldMapRefresh(region);
        inputsInitialized = false;
        seedInputs(region);
        plugin.playSuccessSound(playerRef);
        setStatus(StatusTone.SUCCESS, t(
                "Map settings reset to the HyGuard defaults.",
                "Налаштування мапи скинуто до стандартних значень HyGuard."
        ));
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
        bindClick(evt, "#BackButton", "Back");
        bindClick(evt, "#CloseIconButton", "Close");
        bindClick(evt, "#EnableButton", "Enable");
        bindClick(evt, "#DisableButton", "Disable");
        bindClick(evt, "#ApplyCustomColorButton", "ApplyCustomColor");
        bindClick(evt, "#ResetButton", "Reset");
        bindValue(evt, "#HexInput", "@HexInput");

        Region region = plugin.findRegionByName(worldName, regionName);
        cmd.set("#PageTitle.Text", region == null
                ? t("Map Settings", "Налаштування мапи")
                : f("Map Settings - %s", "Налаштування мапи - %s", region.getName()));
        cmd.set("#PageSubtitle.Text", f(
                "Region: %s | World: %s",
                "Регіон: %s | Світ: %s",
                regionName,
                worldName
        ));

        if (region == null) {
            cmd.set("#EnableSelected.Visible", false);
            cmd.set("#DisableSelected.Visible", false);
            cmd.set("#PreviewColor.Background", toHex(HyGuardRegionMapStyle.defaultColor()).toLowerCase(Locale.ROOT));
            cmd.set("#CurrentPreviewHex.Text", t("n/a", "н/д"));
            cmd.set("#HexInput.Value", hexInput == null ? "" : hexInput);
            applyEditState(cmd, false, false);
            applyStatus(cmd);
            return;
        }

        seedInputs(region);
        boolean canEdit = plugin.canManageRegion(playerRef, region);
        cmd.set("#EnableSelected.Visible", desiredVisibility);
        cmd.set("#DisableSelected.Visible", !desiredVisibility);
        cmd.set("#PreviewColor.Background", selectedColorHex.toLowerCase(Locale.ROOT));
        cmd.set("#CurrentPreviewHex.Text", selectedColorHex);
        cmd.set("#HexInput.Value", hexInput == null ? "" : hexInput);
        applyEditState(cmd, canEdit, true);
        applyStatus(cmd);
    }

    private void applyEditState(UICommandBuilder cmd, boolean canEdit, boolean regionPresent) {
        cmd.set("#ReadOnlyHint.Visible", !canEdit && regionPresent);
    }

    private void applyStatus(UICommandBuilder cmd) {
        cmd.set("#StatusText.Text", statusMessage == null ? "" : statusMessage);
        cmd.set("#StatusInfo.Visible", statusTone == StatusTone.INFO);
        cmd.set("#StatusSuccess.Visible", statusTone == StatusTone.SUCCESS);
        cmd.set("#StatusWarning.Visible", statusTone == StatusTone.WARNING);
        cmd.set("#StatusError.Visible", statusTone == StatusTone.ERROR);
    }

    private void seedInputs(Region region) {
        if (inputsInitialized || region == null) {
            return;
        }

        desiredVisibility = HyGuardRegionMapStyle.isVisible(region);
        selectedColorHex = HyGuardRegionMapStyle.resolveColorHex(region);
        hexInput = selectedColorHex.substring(1);
        inputsInitialized = true;
    }

    private void setStatus(StatusTone tone, String message) {
        statusTone = tone == null ? StatusTone.INFO : tone;
        statusMessage = message == null ? "" : message;
    }

    private void bindClick(UIEventBuilder evt, String selector, String action) {
        evt.addEventBinding(CustomUIEventBindingType.Activating, selector, EventData.of("Action", action), false);
    }

    private void bindValue(UIEventBuilder evt, String selector, String key) {
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, selector, EventData.of(key, selector + ".Value"), false);
    }

    private void forceWorldMapRefresh(Region region) {
        if (region == null || plugin.getMapOverlayManager() == null) {
            return;
        }
        plugin.getMapOverlayManager().invalidateWorld(region.getWorldId());
    }

    private String t(String english, String ukrainian) {
        return UiText.choose(playerRef, english, ukrainian);
    }

    private String f(String english, String ukrainian, Object... args) {
        return UiText.format(playerRef, english, ukrainian, args);
    }

    private static String toHex(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    private static String sanitizeHexInput(String raw) {
        if (raw == null) {
            return "";
        }

        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        return normalized.length() <= 6 ? normalized : normalized.substring(0, 6);
    }
}