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
import dev.thenexusgates.hyguard.core.region.RegionFlag;
import dev.thenexusgates.hyguard.core.region.RegionFlagValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class FlagEditorPage extends InteractiveCustomUIPage<FlagEditorPage.PageData> {

    private enum StatusTone {
        INFO,
        SUCCESS,
        WARNING,
        ERROR
    }

    private enum FlagCategory {
        BLOCKS("Blocks"),
        PLAYERS("Players"),
        MOBS("Mobs"),
        ENVIRONMENT("Environment"),
        ENTRY_EXIT("Entry / Exit"),
        SPECIAL("Special");

        private final String title;

        FlagCategory(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }
    }

    public static final class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (data, value) -> data.action = value, data -> data.action).add()
                .append(new KeyedCodec<>("@SearchInput", Codec.STRING), (data, value) -> data.searchInput = value, data -> data.searchInput).add()
                .append(new KeyedCodec<>("@GreetMessageInput", Codec.STRING), (data, value) -> data.greetMessageInput = value, data -> data.greetMessageInput).add()
                .append(new KeyedCodec<>("@FarewellMessageInput", Codec.STRING), (data, value) -> data.farewellMessageInput = value, data -> data.farewellMessageInput).add()
                .append(new KeyedCodec<>("@EntryDenyMessageInput", Codec.STRING), (data, value) -> data.entryDenyMessageInput = value, data -> data.entryDenyMessageInput).add()
                .append(new KeyedCodec<>("@ExitDenyMessageInput", Codec.STRING), (data, value) -> data.exitDenyMessageInput = value, data -> data.exitDenyMessageInput).add()
                .build();

        String action;
        String searchInput;
        String greetMessageInput;
        String farewellMessageInput;
        String entryDenyMessageInput;
        String exitDenyMessageInput;
    }

    private static final String UI_PAGE = "Pages/HyGuardFlagEditor.ui";
    private static final String UI_SECTION_ROW = "Pages/HyGuardFlagSectionRow.ui";
    private static final String UI_FLAG_ROW = "Pages/HyGuardFlagRow.ui";
    private static final String UI_TEXT_ROW = "Pages/HyGuardFlagTextRow.ui";
    private static final String UI_INFO_ROW = "Pages/HyGuardFlagInfoRow.ui";
    private static final String GROUP_ROOT = "#FlagRowsGroup";
    private static final Set<RegionFlag> TEXT_FLAGS = Set.of(
            RegionFlag.GREET_MESSAGE,
            RegionFlag.FAREWELL_MESSAGE,
            RegionFlag.ENTRY_DENY_MESSAGE,
            RegionFlag.EXIT_DENY_MESSAGE,
            RegionFlag.GAME_MODE
    );

    private final HyGuardPlugin plugin;
    private final String worldName;
    private final String regionName;

    private String greetMessageInput = "";
    private String farewellMessageInput = "";
    private String entryDenyMessageInput = "";
    private String exitDenyMessageInput = "";
    private String searchInput = "";
    private String gameModeInput = "adventure";
    private FlagCategory selectedCategory = FlagCategory.BLOCKS;
    private String statusMessage = "Choose a category on the left, then adjust rules on the right.";
    private StatusTone statusTone = StatusTone.INFO;

    public FlagEditorPage(PlayerRef playerRef, HyGuardPlugin plugin, String worldName, String regionName) {
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
        if (data == null) {
            return;
        }
        if (data.searchInput != null) {
            searchInput = data.searchInput;
        }
        if (data.greetMessageInput != null) {
            greetMessageInput = data.greetMessageInput;
        }
        if (data.farewellMessageInput != null) {
            farewellMessageInput = data.farewellMessageInput;
        }
        if (data.entryDenyMessageInput != null) {
            entryDenyMessageInput = data.entryDenyMessageInput;
        }
        if (data.exitDenyMessageInput != null) {
            exitDenyMessageInput = data.exitDenyMessageInput;
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

        switch (data.action) {
            case "Back" -> {
                plugin.openRegionDetail(store, entityRef, playerRef, worldName, regionName);
                return;
            }
            case "Close" -> {
                close();
                return;
            }
            case "Category:BLOCKS" -> selectCategory(FlagCategory.BLOCKS);
            case "Category:PLAYERS" -> selectCategory(FlagCategory.PLAYERS);
            case "Category:MOBS" -> selectCategory(FlagCategory.MOBS);
            case "Category:ENVIRONMENT" -> selectCategory(FlagCategory.ENVIRONMENT);
            case "Category:ENTRY_EXIT" -> selectCategory(FlagCategory.ENTRY_EXIT);
            case "Category:SPECIAL" -> selectCategory(FlagCategory.SPECIAL);
            case "GameMode:adventure" -> applyGameMode(region, "adventure");
            case "GameMode:creative" -> applyGameMode(region, "creative");
            default -> {
                if (data.action.startsWith("SetFlag|")) {
                    applyModeFlag(region, data.action);
                } else if (data.action.startsWith("SaveText|")) {
                    applyTextFlag(region, data.action);
                } else if (data.action.startsWith("ClearFlag|")) {
                    clearFlag(region, data.action);
                } else {
                    return;
                }
            }
        }

        refresh(entityRef, store);
    }

    private void selectCategory(FlagCategory category) {
        selectedCategory = category;
        setStatus(StatusTone.INFO, "Viewing " + category.title() + " flags.");
    }

    private void applyModeFlag(Region region, String action) {
        if (!plugin.canManageRegion(playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
            setStatus(StatusTone.ERROR, "You do not have permission to edit flags in this region.");
            return;
        }

        String[] parts = action.split("\\|", 3);
        if (parts.length != 3) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidFlagValue);
            setStatus(StatusTone.ERROR, "The flag action payload was malformed.");
            return;
        }

        RegionFlag flag = parseFlag(parts[1]);
        RegionFlagValue.Mode mode = parseMode(parts[2]);
        if (flag == null || mode == null) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidFlagValue);
            setStatus(StatusTone.ERROR, "This flag mode is not valid.");
            return;
        }

        region.putFlag(flag, new RegionFlagValue(mode));
        plugin.saveRegion(region);
        plugin.send(playerRef, plugin.getConfigSnapshot().messages.flagUpdated, Map.of(
                "name", region.getName(),
                "flag", flag.name(),
                "value", mode.name()
        ));
        plugin.playSuccessSound(playerRef);
        setStatus(StatusTone.SUCCESS, RegionUiText.displayFlag(flag) + " updated to " + mode.name().toLowerCase(Locale.ROOT) + ".");
    }

    private void applyTextFlag(Region region, String action) {
        if (!plugin.canManageRegion(playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
            setStatus(StatusTone.ERROR, "You do not have permission to edit flags in this region.");
            return;
        }

        String[] parts = action.split("\\|", 2);
        if (parts.length != 2) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidFlag);
            setStatus(StatusTone.ERROR, "The text flag payload was malformed.");
            return;
        }

        RegionFlag flag = parseFlag(parts[1]);
        if (flag == null || !TEXT_FLAGS.contains(flag)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidFlag);
            setStatus(StatusTone.ERROR, "This flag cannot be edited as text.");
            return;
        }

        String textValue = getTextInput(flag).trim();
        if (textValue.isBlank()) {
            region.removeFlag(flag);
            plugin.saveRegion(region);
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.flagCleared, Map.of(
                    "name", region.getName(),
                    "flag", flag.name()
            ));
                plugin.playSuccessSound(playerRef);
            setStatus(StatusTone.SUCCESS, RegionUiText.displayFlag(flag) + " was cleared.");
            return;
        }

        if (flag == RegionFlag.GAME_MODE && plugin.parseConfiguredGameMode(textValue) == null) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidFlagValue);
            setStatus(StatusTone.ERROR, "Game mode must be Adventure or Creative.");
            return;
        }

        setTextInput(flag, textValue);
        region.putFlag(flag, new RegionFlagValue(RegionFlagValue.Mode.ALLOW, textValue));
        plugin.saveRegion(region);
        plugin.send(playerRef, plugin.getConfigSnapshot().messages.flagUpdated, Map.of(
                "name", region.getName(),
                "flag", flag.name(),
                "value", textValue
        ));
        plugin.playSuccessSound(playerRef);
        setStatus(StatusTone.SUCCESS, RegionUiText.displayFlag(flag) + " saved.");
    }

    private void applyGameMode(Region region, String modeName) {
        if (!plugin.canManageRegion(playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
            setStatus(StatusTone.ERROR, "You do not have permission to edit flags in this region.");
            return;
        }
        if (plugin.parseConfiguredGameMode(modeName) == null) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidFlagValue);
            setStatus(StatusTone.ERROR, "Game mode must be Adventure or Creative.");
            return;
        }

        gameModeInput = modeName;
        region.putFlag(RegionFlag.GAME_MODE, new RegionFlagValue(RegionFlagValue.Mode.ALLOW, modeName));
        plugin.saveRegion(region);
        plugin.send(playerRef, plugin.getConfigSnapshot().messages.flagUpdated, Map.of(
                "name", region.getName(),
                "flag", RegionFlag.GAME_MODE.name(),
                "value", modeName
        ));
        plugin.playSuccessSound(playerRef);
        setStatus(StatusTone.SUCCESS, "Game mode lock updated to " + capitalize(modeName) + ".");
    }

    private void clearFlag(Region region, String action) {
        if (!plugin.canManageRegion(playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
            setStatus(StatusTone.ERROR, "You do not have permission to edit flags in this region.");
            return;
        }

        String[] parts = action.split("\\|", 2);
        if (parts.length != 2) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidFlag);
            setStatus(StatusTone.ERROR, "The clear action payload was malformed.");
            return;
        }

        RegionFlag flag = parseFlag(parts[1]);
        if (flag == null) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidFlag);
            setStatus(StatusTone.ERROR, "This flag does not exist.");
            return;
        }

        setTextInput(flag, "");
        region.removeFlag(flag);
        plugin.saveRegion(region);
        plugin.send(playerRef, plugin.getConfigSnapshot().messages.flagCleared, Map.of(
                "name", region.getName(),
                "flag", flag.name()
        ));
        plugin.playSuccessSound(playerRef);
        setStatus(StatusTone.SUCCESS, RegionUiText.displayFlag(flag) + " was cleared.");
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
        bindClick(evt, "#CategoryBlocksButton", "Category:BLOCKS");
        bindClick(evt, "#CategoryPlayersButton", "Category:PLAYERS");
        bindClick(evt, "#CategoryMobsButton", "Category:MOBS");
        bindClick(evt, "#CategoryEnvironmentButton", "Category:ENVIRONMENT");
        bindClick(evt, "#CategoryEntryExitButton", "Category:ENTRY_EXIT");
        bindClick(evt, "#CategorySpecialButton", "Category:SPECIAL");
        bindValue(evt, "#SearchInput", "@SearchInput");

        Region region = plugin.findRegionByName(worldName, regionName);
        cmd.set("#PageTitle.Text", region == null ? "Flag Editor" : "Flag Editor - " + region.getName());
        cmd.set("#Subtitle.Text", "Region: " + regionName + " | World: " + worldName + " | Search or switch categories to focus the rule set.");
        cmd.set("#BodyHint.Text", "Use Allow, Deny, or Inherit for rule flags. Message fields save explicit values.");
        cmd.set("#SearchInput.Value", searchInput == null ? "" : searchInput);
        cmd.set("#ActiveCategoryTitle.Text", selectedCategory.title());
        cmd.set("#ActiveCategoryHint.Text", normalizedSearch().isBlank()
                ? "Showing all " + selectedCategory.title() + " flags."
                : "Filtered by \"" + searchInput.trim() + "\" inside " + selectedCategory.title() + ".");
        applyCategoryState(cmd);
        applyStatus(cmd);

        if (region == null) {
            return;
        }

        syncTextInputs(region);

        int index = 0;
        List<RegionFlag> visibleFlags = filteredFlagsForSelectedCategory();
        if (visibleFlags.isEmpty()) {
            appendInfoRow(cmd, index, "No flags match this filter.", "Change the search text or pick a different category.");
            return;
        }

        index = appendSectionHeader(cmd, index, selectedCategory.title() + " Rules");
        for (RegionFlag flag : visibleFlags) {
            if (TEXT_FLAGS.contains(flag)) {
                appendTextRow(cmd, evt, index++, region, flag);
            } else {
                appendModeRow(cmd, evt, index++, region, flag);
            }
        }
    }

    private void appendInfoRow(UICommandBuilder cmd, int index, String title, String detail) {
        cmd.append(GROUP_ROOT, UI_INFO_ROW);
        String rowId = GROUP_ROOT + "[" + index + "]";
        cmd.set(rowId + " #InfoTitle.Text", title);
        cmd.set(rowId + " #InfoDetail.Text", detail);
    }

    private int appendSectionHeader(UICommandBuilder cmd, int index, String title) {
        cmd.append(GROUP_ROOT, UI_SECTION_ROW);
        String rowId = GROUP_ROOT + "[" + index + "]";
        cmd.set(rowId + " #SectionTitle.Text", title);
        return index + 1;
    }

    private void appendModeRow(UICommandBuilder cmd,
                               UIEventBuilder evt,
                               int index,
                               Region region,
                               RegionFlag flag) {
        cmd.append(GROUP_ROOT, UI_FLAG_ROW);
        String rowId = GROUP_ROOT + "[" + index + "]";
        RegionFlagValue value = region.getFlags().get(flag);
        RegionFlagValue.Mode currentMode = value == null ? RegionFlagValue.Mode.INHERIT : value.getMode();
        boolean editable = plugin.canManageRegion(playerRef, region);

        cmd.set(rowId + " #FlagName.Text", RegionUiText.displayFlag(flag));
        cmd.set(rowId + " #FlagDescription.Text", RegionUiText.flagDescription(flag));
        cmd.set(rowId + " #ModeHint.Text", RegionUiText.flagModeHint(flag, currentMode));
        cmd.set(rowId + " #AllowSelected.Visible", currentMode == RegionFlagValue.Mode.ALLOW);
        cmd.set(rowId + " #DenySelected.Visible", currentMode == RegionFlagValue.Mode.DENY);
        cmd.set(rowId + " #InheritSelected.Visible", currentMode == RegionFlagValue.Mode.INHERIT);
        cmd.set(rowId + " #ReadOnlyHint.Visible", !editable);

        if (editable) {
            bindClick(evt, rowId + " #AllowBtn", "SetFlag|" + flag.name() + "|ALLOW");
            bindClick(evt, rowId + " #DenyBtn", "SetFlag|" + flag.name() + "|DENY");
            bindClick(evt, rowId + " #InheritBtn", "SetFlag|" + flag.name() + "|INHERIT");
        }
    }

    private void appendTextRow(UICommandBuilder cmd,
                               UIEventBuilder evt,
                               int index,
                               Region region,
                               RegionFlag flag) {
        cmd.append(GROUP_ROOT, UI_TEXT_ROW);
        String rowId = GROUP_ROOT + "[" + index + "]";
        String inputValue = getTextInput(flag);
        boolean editable = plugin.canManageRegion(playerRef, region);
        boolean isGameMode = flag == RegionFlag.GAME_MODE;

        cmd.set(rowId + " #FlagName.Text", RegionUiText.displayFlag(flag));
        cmd.set(rowId + " #FlagDescription.Text", RegionUiText.textFlagHelper(flag));
        cmd.set(rowId + " #FlagTextInput.Value", inputValue);
        cmd.set(rowId + " #FlagTextInput.PlaceholderText", RegionUiText.textFlagPlaceholder(flag));
        cmd.set(rowId + " #InputShell.Visible", !isGameMode);
        cmd.set(rowId + " #PresetRow.Visible", isGameMode);
        cmd.set(rowId + " #SaveBtn.Visible", editable && !isGameMode);
        cmd.set(rowId + " #SaveBtnDisabled.Visible", !editable && !isGameMode);
        cmd.set(rowId + " #ClearBtn.Visible", editable);
        cmd.set(rowId + " #ClearBtnDisabled.Visible", !editable);
        cmd.set(rowId + " #AdventureSelected.Visible", isGameMode && "adventure".equalsIgnoreCase(inputValue));
        cmd.set(rowId + " #CreativeSelected.Visible", isGameMode && "creative".equalsIgnoreCase(inputValue));
        cmd.set(rowId + " #ReadOnlyHint.Visible", !editable);

        if (editable) {
            if (isGameMode) {
                bindClick(evt, rowId + " #AdventureButton", "GameMode:adventure");
                bindClick(evt, rowId + " #CreativeButton", "GameMode:creative");
            } else {
                bindClick(evt, rowId + " #SaveBtn", "SaveText|" + flag.name());
                bindValue(evt, rowId + " #FlagTextInput", keyFor(flag));
            }
            bindClick(evt, rowId + " #ClearBtn", "ClearFlag|" + flag.name());
        }

        RegionFlagValue value = region.getFlags().get(flag);
        boolean hasValue = value != null && value.getTextValue() != null && !value.getTextValue().isBlank();
        String currentValue = hasValue ? value.getTextValue() : "Not set";
        cmd.set(rowId + " #CurrentValue.Text", isGameMode ? "Current enforced mode: " + currentValue : "Current value: " + currentValue);
    }

    private void syncTextInputs(Region region) {
        for (RegionFlag flag : TEXT_FLAGS) {
            String current = getTextInput(flag);
            if (current != null && !current.isBlank()) {
                continue;
            }
            RegionFlagValue value = region.getFlags().get(flag);
            if (value == null || value.getTextValue() == null || value.getTextValue().isBlank()) {
                continue;
            }
            setTextInput(flag, value.getTextValue());
        }
    }

    private List<RegionFlag> filteredFlagsForSelectedCategory() {
        List<RegionFlag> results = new ArrayList<>();
        String query = normalizedSearch();
        for (RegionFlag flag : flagsFor(selectedCategory)) {
            if (query.isBlank() || matchesFilter(flag, query)) {
                results.add(flag);
            }
        }
        return results;
    }

    private boolean matchesFilter(RegionFlag flag, String query) {
        return RegionUiText.displayFlag(flag).toLowerCase(Locale.ROOT).contains(query)
                || RegionUiText.flagDescription(flag).toLowerCase(Locale.ROOT).contains(query)
                || flag.name().toLowerCase(Locale.ROOT).contains(query);
    }

    private String normalizedSearch() {
        return searchInput == null ? "" : searchInput.trim().toLowerCase(Locale.ROOT);
    }

    private static List<RegionFlag> flagsFor(FlagCategory category) {
        return switch (category) {
            case BLOCKS -> List.of(
                    RegionFlag.BLOCK_BREAK,
                    RegionFlag.BLOCK_PLACE,
                    RegionFlag.BLOCK_INTERACT
            );
            case PLAYERS -> List.of(
                    RegionFlag.PVP,
                    RegionFlag.PLAYER_DAMAGE,
                    RegionFlag.PLAYER_FALL_DAMAGE,
                    RegionFlag.KNOCKBACK,
                    RegionFlag.PLAYER_HUNGER,
                    RegionFlag.PLAYER_ITEM_DROP,
                    RegionFlag.PLAYER_ITEM_PICKUP,
                    RegionFlag.INTERACT_INVENTORY
            );
            case MOBS -> List.of(
                    RegionFlag.MOB_DAMAGE_PLAYERS,
                    RegionFlag.ENTITY_DAMAGE,
                    RegionFlag.MOB_SPAWN,
                    RegionFlag.MOB_SPAWN_HOSTILE,
                    RegionFlag.MOB_SPAWN_PASSIVE
            );
            case ENVIRONMENT -> List.of(
                    RegionFlag.FIRE_SPREAD,
                    RegionFlag.EXPLOSION,
                    RegionFlag.EXPLOSION_BLOCK_DAMAGE,
                    RegionFlag.LIQUID_FLOW
            );
            case ENTRY_EXIT -> List.of(
                    RegionFlag.ENTRY,
                    RegionFlag.EXIT,
                    RegionFlag.ENTRY_PLAYERS,
                    RegionFlag.GREET_MESSAGE,
                    RegionFlag.FAREWELL_MESSAGE,
                    RegionFlag.ENTRY_DENY_MESSAGE,
                    RegionFlag.EXIT_DENY_MESSAGE
            );
            case SPECIAL -> List.of(
                    RegionFlag.INVINCIBLE,
                    RegionFlag.GAME_MODE,
                    RegionFlag.WEATHER_LOCK,
                    RegionFlag.TIME_LOCK,
                    RegionFlag.FLY,
                    RegionFlag.SPAWN_LOCATION
            );
        };
    }

    private String getTextInput(RegionFlag flag) {
        return switch (flag) {
            case GREET_MESSAGE -> greetMessageInput;
            case FAREWELL_MESSAGE -> farewellMessageInput;
            case ENTRY_DENY_MESSAGE -> entryDenyMessageInput;
            case EXIT_DENY_MESSAGE -> exitDenyMessageInput;
            case GAME_MODE -> gameModeInput;
            default -> "";
        };
    }

    private void setTextInput(RegionFlag flag, String value) {
        String normalizedValue = value == null ? "" : value;
        switch (flag) {
            case GREET_MESSAGE -> greetMessageInput = normalizedValue;
            case FAREWELL_MESSAGE -> farewellMessageInput = normalizedValue;
            case ENTRY_DENY_MESSAGE -> entryDenyMessageInput = normalizedValue;
            case EXIT_DENY_MESSAGE -> exitDenyMessageInput = normalizedValue;
            case GAME_MODE -> gameModeInput = normalizedValue;
            default -> {
            }
        }
    }

    private String keyFor(RegionFlag flag) {
        return switch (flag) {
            case GREET_MESSAGE -> "@GreetMessageInput";
            case FAREWELL_MESSAGE -> "@FarewellMessageInput";
            case ENTRY_DENY_MESSAGE -> "@EntryDenyMessageInput";
            case EXIT_DENY_MESSAGE -> "@ExitDenyMessageInput";
            default -> "@Unused";
        };
    }

    private void applyCategoryState(UICommandBuilder cmd) {
        cmd.set("#CategoryBlocksSelected.Visible", selectedCategory == FlagCategory.BLOCKS);
        cmd.set("#CategoryPlayersSelected.Visible", selectedCategory == FlagCategory.PLAYERS);
        cmd.set("#CategoryMobsSelected.Visible", selectedCategory == FlagCategory.MOBS);
        cmd.set("#CategoryEnvironmentSelected.Visible", selectedCategory == FlagCategory.ENVIRONMENT);
        cmd.set("#CategoryEntryExitSelected.Visible", selectedCategory == FlagCategory.ENTRY_EXIT);
        cmd.set("#CategorySpecialSelected.Visible", selectedCategory == FlagCategory.SPECIAL);
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

    private RegionFlag parseFlag(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return RegionFlag.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException illegalArgumentException) {
            return null;
        }
    }

    private RegionFlagValue.Mode parseMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return RegionFlagValue.Mode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException illegalArgumentException) {
            return null;
        }
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private void bindClick(UIEventBuilder evt, String selector, String action) {
        evt.addEventBinding(CustomUIEventBindingType.Activating, selector, EventData.of("Action", action), false);
    }

    private void bindValue(UIEventBuilder evt, String selector, String key) {
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, selector, EventData.of(key, selector + ".Value"), false);
    }
}