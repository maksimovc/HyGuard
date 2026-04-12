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
        BLOCKS("Blocks", "Блоки"),
        PLAYERS("Players", "Гравці"),
        MOBS("Mobs", "Моби"),
        ENVIRONMENT("Environment", "Середовище"),
        ENTRY_EXIT("Entry / Exit", "Вхід / Вихід"),
        SPECIAL("Special", "Особливе");

        private final String englishTitle;
        private final String ukrainianTitle;

        FlagCategory(String englishTitle, String ukrainianTitle) {
            this.englishTitle = englishTitle;
            this.ukrainianTitle = ukrainianTitle;
        }

        public String title(PlayerRef playerRef) {
            return UiText.choose(playerRef, englishTitle, ukrainianTitle);
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
                .append(new KeyedCodec<>("@WeatherLockInput", Codec.STRING), (data, value) -> data.weatherLockInput = value, data -> data.weatherLockInput).add()
                .append(new KeyedCodec<>("@TimeLockInput", Codec.STRING), (data, value) -> data.timeLockInput = value, data -> data.timeLockInput).add()
                .append(new KeyedCodec<>("@CommandBlacklistInput", Codec.STRING), (data, value) -> data.commandBlacklistInput = value, data -> data.commandBlacklistInput).add()
                .build();

        String action;
        String searchInput;
        String greetMessageInput;
        String farewellMessageInput;
        String entryDenyMessageInput;
        String exitDenyMessageInput;
        String weatherLockInput;
        String timeLockInput;
        String commandBlacklistInput;
    }

    private static final String UI_PAGE = "Pages/HyGuardFlagEditor.ui";
    private static final String UI_FLAG_ROW = "Pages/HyGuardFlagRow.ui";
    private static final String UI_TEXT_ROW = "Pages/HyGuardFlagTextRow.ui";
    private static final String UI_ACTION_ROW = "Pages/HyGuardFlagActionRow.ui";
    private static final String UI_INFO_ROW = "Pages/HyGuardFlagInfoRow.ui";
    private static final String GROUP_ROOT = "#FlagRowsGroup";
    private static final Set<RegionFlag> TEXT_FLAGS = Set.of(
            RegionFlag.GREET_MESSAGE,
            RegionFlag.FAREWELL_MESSAGE,
            RegionFlag.ENTRY_DENY_MESSAGE,
            RegionFlag.EXIT_DENY_MESSAGE,
            RegionFlag.GAME_MODE,
            RegionFlag.WEATHER_LOCK,
            RegionFlag.TIME_LOCK,
            RegionFlag.COMMAND_BLACKLIST
    );

    private final HyGuardPlugin plugin;
    private final String worldName;
    private final String regionName;

    private String greetMessageInput = "";
    private String farewellMessageInput = "";
    private String entryDenyMessageInput = "";
    private String exitDenyMessageInput = "";
    private String weatherLockInput = "";
    private String timeLockInput = "";
    private String commandBlacklistInput = "";
    private String searchInput = "";
    private String gameModeInput = "";
    private FlagCategory selectedCategory = FlagCategory.BLOCKS;
    private String statusMessage;
    private StatusTone statusTone = StatusTone.INFO;

    public FlagEditorPage(PlayerRef playerRef, HyGuardPlugin plugin, String worldName, String regionName) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.plugin = plugin;
        this.worldName = worldName;
        this.regionName = regionName;
        this.statusMessage = t(
            "Edit region rules.",
            "Змінюйте правила регіону."
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
        if (data.weatherLockInput != null) {
            weatherLockInput = data.weatherLockInput;
        }
        if (data.timeLockInput != null) {
            timeLockInput = data.timeLockInput;
        }
        if (data.commandBlacklistInput != null) {
            commandBlacklistInput = data.commandBlacklistInput;
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
                plugin.openRegionWorkspace(store, entityRef, playerRef, worldName, regionName, RegionWorkspacePage.WorkspaceTab.RULES);
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
            case "ManageEntryBlacklist" -> {
                plugin.openEntryBlacklistManager(store, entityRef, playerRef, worldName, regionName);
                return;
            }
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
        setStatus(StatusTone.INFO, f("Category: %s", "Категорія: %s", category.title(playerRef)));
    }

    private void applyModeFlag(Region region, String action) {
        if (!plugin.canManageRegion(playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
            setStatus(StatusTone.ERROR, t("You do not have permission to edit flags in this region.", "У вас немає дозволу редагувати прапори в цьому регіоні."));
            return;
        }

        String[] parts = action.split("\\|", 3);
        if (parts.length != 3) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidFlagValue);
            setStatus(StatusTone.ERROR, t("The flag action payload was malformed.", "Пакет дії прапора має неправильний формат."));
            return;
        }

        RegionFlag flag = parseFlag(parts[1]);
        RegionFlagValue.Mode mode = parseMode(parts[2]);
        if (flag == null || mode == null) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidFlagValue);
            setStatus(StatusTone.ERROR, t("This flag mode is not valid.", "Цей режим прапора недійсний."));
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
        setStatus(StatusTone.SUCCESS, f("%s updated to %s.", "%s оновлено до %s.", RegionUiText.displayFlag(playerRef, flag), RegionUiText.displayMode(playerRef, mode)));
    }

    private void applyTextFlag(Region region, String action) {
        if (!plugin.canManageRegion(playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
            setStatus(StatusTone.ERROR, t("You do not have permission to edit flags in this region.", "У вас немає дозволу редагувати прапори в цьому регіоні."));
            return;
        }

        String[] parts = action.split("\\|", 2);
        if (parts.length != 2) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidFlag);
            setStatus(StatusTone.ERROR, t("The text flag payload was malformed.", "Пакет текстового прапора має неправильний формат."));
            return;
        }

        RegionFlag flag = parseFlag(parts[1]);
        if (flag == null || !TEXT_FLAGS.contains(flag)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidFlag);
            setStatus(StatusTone.ERROR, t("This flag cannot be edited as text.", "Цей прапор не можна редагувати як текст."));
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
                setStatus(StatusTone.SUCCESS, f("%s was cleared.", "%s очищено.", RegionUiText.displayFlag(playerRef, flag)));
            return;
        }

        if (flag == RegionFlag.GAME_MODE && plugin.parseConfiguredGameMode(textValue) == null) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidFlagValue);
                setStatus(StatusTone.ERROR, t("Game mode must be Adventure or Creative.", "Режим гри має бути Adventure або Creative."));
            return;
        }
        if (flag == RegionFlag.WEATHER_LOCK && plugin.parseConfiguredWeatherLock(textValue) == null) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidFlagValue);
                setStatus(StatusTone.ERROR, t("Weather lock must be a weather index or one of: clear, rain, storm.", "Фіксація погоди має бути індексом погоди або одним із значень: clear, rain, storm."));
            return;
        }
        if (flag == RegionFlag.TIME_LOCK && plugin.parseConfiguredTimeLock(textValue) == null) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidFlagValue);
                setStatus(StatusTone.ERROR, t("Time lock must be written as HH or HH:MM.", "Фіксацію часу треба вказувати у форматі HH або HH:MM."));
            return;
        }
        if (flag == RegionFlag.COMMAND_BLACKLIST) {
            textValue = plugin.sanitizeCommandBlacklist(textValue);
            if (textValue.isBlank()) {
                plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidFlagValue);
                    setStatus(StatusTone.ERROR, t("Command blacklist must contain at least one command pattern.", "Чорний список команд має містити хоча б один шаблон команди."));
                return;
            }
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
        setStatus(StatusTone.SUCCESS, f("%s saved.", "%s збережено.", RegionUiText.displayFlag(playerRef, flag)));
    }

    private void applyGameMode(Region region, String modeName) {
        if (!plugin.canManageRegion(playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
            setStatus(StatusTone.ERROR, t("You do not have permission to edit flags in this region.", "У вас немає дозволу редагувати прапори в цьому регіоні."));
            return;
        }
        if (plugin.parseConfiguredGameMode(modeName) == null) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidFlagValue);
            setStatus(StatusTone.ERROR, t("Game mode must be Adventure or Creative.", "Режим гри має бути Adventure або Creative."));
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
        setStatus(StatusTone.SUCCESS, f("Game mode lock updated to %s.", "Фіксацію режиму гри оновлено до %s.", RegionUiText.displayConfiguredGameMode(playerRef, modeName)));
    }

    private void clearFlag(Region region, String action) {
        if (!plugin.canManageRegion(playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
            setStatus(StatusTone.ERROR, t("You do not have permission to edit flags in this region.", "У вас немає дозволу редагувати прапори в цьому регіоні."));
            return;
        }

        String[] parts = action.split("\\|", 2);
        if (parts.length != 2) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidFlag);
            setStatus(StatusTone.ERROR, t("The clear action payload was malformed.", "Пакет очищення має неправильний формат."));
            return;
        }

        RegionFlag flag = parseFlag(parts[1]);
        if (flag == null) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.invalidFlag);
            setStatus(StatusTone.ERROR, t("This flag does not exist.", "Цей прапор не існує."));
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
        setStatus(StatusTone.SUCCESS, f("%s was cleared.", "%s очищено.", RegionUiText.displayFlag(playerRef, flag)));
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
        cmd.set("#PageTitle.Text", region == null ? t("Flag Editor", "Редактор прапорів") : f("Flag Editor - %s", "Редактор прапорів - %s", region.getName()));
        cmd.set("#Subtitle.Text", f("Region: %s | World: %s", "Регіон: %s | Світ: %s", regionName, worldName));
        cmd.set("#SearchInput.Value", searchInput == null ? "" : searchInput);
        applyCategoryState(cmd);
        applyStatus(cmd);

        if (region == null) {
            return;
        }

        syncTextInputs(region);

        int index = 0;
        List<RegionFlag> visibleFlags = filteredFlagsForSelectedCategory();
        if (visibleFlags.isEmpty()) {
            appendInfoRow(cmd, index, t("No matches", "Нічого не знайдено"), t("Change the filter.", "Змініть фільтр."));
            return;
        }

        for (RegionFlag flag : visibleFlags) {
            if (flag == RegionFlag.ENTRY_BLACKLIST) {
                appendEntryBlacklistRow(cmd, evt, index++, region);
            } else if (TEXT_FLAGS.contains(flag)) {
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

        cmd.set(rowId + " #FlagName.Text", RegionUiText.displayFlag(playerRef, flag));
        cmd.set(rowId + " #FlagDescription.Text", RegionUiText.flagDescription(playerRef, flag));
        cmd.set(rowId + " #ModeHint.Text", RegionUiText.flagModeHint(playerRef, flag, currentMode));
        cmd.set(rowId + " #ReadOnlyHint.Text", t("Read only: you can inspect this flag, but you cannot change it.", "Лише перегляд: ви можете оглянути цей прапор, але не можете його змінити."));
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

        cmd.set(rowId + " #FlagName.Text", RegionUiText.displayFlag(playerRef, flag));
        cmd.set(rowId + " #FlagDescription.Text", RegionUiText.textFlagHelper(playerRef, flag));
        cmd.set(rowId + " #FlagTextInput.Value", inputValue);
        cmd.set(rowId + " #FlagTextInput.PlaceholderText", RegionUiText.textFlagPlaceholder(playerRef, flag));
        cmd.set(rowId + " #ReadOnlyHint.Text", t("Read only: you can inspect this flag, but you cannot change it.", "Лише перегляд: ви можете оглянути цей прапор, але не можете його змінити."));
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
        String currentValue = hasValue ? value.getTextValue() : t("Not set", "Не задано");
        if (isGameMode && hasValue) {
            currentValue = RegionUiText.displayConfiguredGameMode(playerRef, currentValue);
        }
        cmd.set(rowId + " #CurrentValue.Text", isGameMode
                ? f("Current enforced mode: %s", "Поточний примусовий режим: %s", currentValue)
                : f("Current value: %s", "Поточне значення: %s", currentValue));
    }

    private void appendEntryBlacklistRow(UICommandBuilder cmd,
                                         UIEventBuilder evt,
                                         int index,
                                         Region region) {
        cmd.append(GROUP_ROOT, UI_ACTION_ROW);
        String rowId = GROUP_ROOT + "[" + index + "]";
        boolean editable = plugin.canManageRegion(playerRef, region);
        List<HyGuardPlugin.PlayerIdentity> blacklist = plugin.getEntryBlacklist(region);
        String preview = blacklist.stream()
                .limit(3)
                .map(HyGuardPlugin.PlayerIdentity::username)
                .reduce((left, right) -> left + ", " + right)
            .orElse(t("No players are currently blocked from entering this region.", "Наразі жодному гравцю не заборонено входити в цей регіон."));
        if (blacklist.size() > 3) {
            preview += f(" +%d more", " +ще %d", blacklist.size() - 3);
        }

        cmd.set(rowId + " #FlagName.Text", RegionUiText.displayFlag(playerRef, RegionFlag.ENTRY_BLACKLIST));
        cmd.set(rowId + " #FlagDescription.Text", RegionUiText.flagDescription(playerRef, RegionFlag.ENTRY_BLACKLIST));
        cmd.set(rowId + " #SummaryText.Text", preview);
        cmd.set(rowId + " #CurrentValue.Text", blacklist.isEmpty()
            ? t("Current value: empty blacklist", "Поточне значення: чорний список порожній")
            : f("Current value: %d blocked player(s)", "Поточне значення: %d заблокованих гравців", blacklist.size()));
        cmd.set(rowId + " #ActionButton.Visible", editable);
        cmd.set(rowId + " #ActionButtonDisabled.Visible", !editable);
        cmd.set(rowId + " #ClearBtn.Visible", editable && !blacklist.isEmpty());
        cmd.set(rowId + " #ClearBtnDisabled.Visible", !editable);
        cmd.set(rowId + " #ReadOnlyHint.Visible", !editable);

        if (editable) {
            bindClick(evt, rowId + " #ActionButton", "ManageEntryBlacklist");
            bindClick(evt, rowId + " #ClearBtn", "ClearFlag|ENTRY_BLACKLIST");
        }
    }

    private void syncTextInputs(Region region) {
        for (RegionFlag flag : TEXT_FLAGS) {
            if (flag == RegionFlag.GAME_MODE) {
                RegionFlagValue value = region.getFlags().get(flag);
                setTextInput(flag, value == null || value.getTextValue() == null ? "" : value.getTextValue());
                continue;
            }
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
                || RegionUiText.displayFlag(playerRef, flag).toLowerCase(Locale.ROOT).contains(query)
                || RegionUiText.flagDescription(flag).toLowerCase(Locale.ROOT).contains(query)
                || RegionUiText.flagDescription(playerRef, flag).toLowerCase(Locale.ROOT).contains(query)
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
                    RegionFlag.PLAYER_ITEM_PICKUP
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
                    RegionFlag.LIQUID_FLOW
            );
            case ENTRY_EXIT -> List.of(
                    RegionFlag.ENTRY,
                    RegionFlag.EXIT,
                    RegionFlag.ENTRY_BLACKLIST,
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
                        RegionFlag.COMMAND_BLACKLIST,
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
            case WEATHER_LOCK -> weatherLockInput;
            case TIME_LOCK -> timeLockInput;
            case COMMAND_BLACKLIST -> commandBlacklistInput;
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
            case WEATHER_LOCK -> weatherLockInput = normalizedValue;
            case TIME_LOCK -> timeLockInput = normalizedValue;
            case COMMAND_BLACKLIST -> commandBlacklistInput = normalizedValue;
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
            case WEATHER_LOCK -> "@WeatherLockInput";
            case TIME_LOCK -> "@TimeLockInput";
            case COMMAND_BLACKLIST -> "@CommandBlacklistInput";
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

    private void bindClick(UIEventBuilder evt, String selector, String action) {
        evt.addEventBinding(CustomUIEventBindingType.Activating, selector, EventData.of("Action", action), false);
    }

    private void bindValue(UIEventBuilder evt, String selector, String key) {
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, selector, EventData.of(key, selector + ".Value"), false);
    }

    private String t(String english, String ukrainian) {
        return UiText.choose(playerRef, english, ukrainian);
    }

    private String f(String english, String ukrainian, Object... args) {
        return UiText.format(playerRef, english, ukrainian, args);
    }
}