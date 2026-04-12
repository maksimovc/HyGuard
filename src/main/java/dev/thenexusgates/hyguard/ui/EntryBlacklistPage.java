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

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public final class EntryBlacklistPage extends InteractiveCustomUIPage<EntryBlacklistPage.PageData> {

    private enum StatusTone {
        INFO,
        SUCCESS,
        WARNING,
        ERROR
    }

    private record CandidateEntry(String uuid, String name, boolean blocked) {
    }

    public static final class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (data, value) -> data.action = value, data -> data.action).add()
                .append(new KeyedCodec<>("@SearchInput", Codec.STRING), (data, value) -> data.searchInput = value, data -> data.searchInput).add()
                .build();

        String action;
        String searchInput;
    }

    private static final String UI_PAGE = "Pages/HyGuardEntryBlacklist.ui";
    private static final String UI_ROW = "Pages/HyGuardMemberRow.ui";
    private static final String GROUP_ROOT = "#PlayerRowsGroup";

    private final HyGuardPlugin plugin;
    private final String worldName;
    private final String regionName;

    private String searchInput = "";
    private String selectedPlayerUuid;
    private String statusMessage;
    private StatusTone statusTone = StatusTone.INFO;

    public EntryBlacklistPage(PlayerRef playerRef, HyGuardPlugin plugin, String worldName, String regionName) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.plugin = plugin;
        this.worldName = worldName;
        this.regionName = regionName;
        this.statusMessage = t(
            "Manage blocked players.",
            "Керуйте заблокованими гравцями."
        );
    }

    @Override
    public void build(Ref<EntityStore> entityRef,
                      UICommandBuilder cmd,
                      UIEventBuilder evt,
                      Store<EntityStore> store) {
        render(cmd, evt);
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
                plugin.openRegionWorkspace(store, entityRef, playerRef, worldName, regionName, RegionWorkspacePage.WorkspaceTab.ACCESS, RegionWorkspacePage.AccessMode.BLACKLIST);
                return;
            }
            case "Close" -> {
                close();
                return;
            }
            case "Add" -> addPlayer(region);
            case "Remove" -> removePlayer(region);
            default -> {
                if (!data.action.startsWith("SelectPlayer|")) {
                    return;
                }
                selectedPlayerUuid = data.action.substring("SelectPlayer|".length());
                CandidateEntry selected = getSelectedCandidate(region);
                if (selected != null) {
                    searchInput = selected.name();
                    setStatus(StatusTone.INFO, selected.blocked()
                            ? f("%s is blocked.", "%s заблокований.", selected.name())
                            : f("Selected: %s", "Вибрано: %s", selected.name()));
                }
            }
        }

        refresh();
    }

    private void addPlayer(Region region) {
        if (!plugin.canManageRegion(playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
            setStatus(StatusTone.ERROR, t("You do not have permission to change region flags here.", "У вас немає дозволу змінювати прапори регіону тут."));
            return;
        }

        HyGuardPlugin.PlayerIdentity identity = resolveSelectedIdentity(region);
        if (identity == null) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.playerLookupFailed);
            setStatus(StatusTone.ERROR, t("Player not found. Pick a known player or type a remembered or online username.", "Гравця не знайдено. Виберіть відомого гравця або введіть збережене чи онлайн-ім'я."));
            return;
        }

        LinkedHashMap<String, HyGuardPlugin.PlayerIdentity> blacklist = new LinkedHashMap<>();
        for (HyGuardPlugin.PlayerIdentity entry : plugin.getEntryBlacklist(region)) {
            blacklist.put(entry.uuid(), entry);
        }
        if (blacklist.containsKey(identity.uuid())) {
            selectedPlayerUuid = identity.uuid();
            setStatus(StatusTone.WARNING, f("%s is already on the entry blacklist.", "%s уже є в чорному списку входу.", identity.username()));
            return;
        }

        blacklist.put(identity.uuid(), identity);
        plugin.setEntryBlacklist(region, blacklist.values());
        plugin.saveRegion(region);
        plugin.playSuccessSound(playerRef);
        selectedPlayerUuid = identity.uuid();
        searchInput = identity.username();
        setStatus(StatusTone.SUCCESS, f("Blocked %s from entering %s.", "Гравцю %s заборонено входити в %s.", identity.username(), region.getName()));
    }

    private void removePlayer(Region region) {
        if (!plugin.canManageRegion(playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
            setStatus(StatusTone.ERROR, t("You do not have permission to change region flags here.", "У вас немає дозволу змінювати прапори регіону тут."));
            return;
        }

        HyGuardPlugin.PlayerIdentity identity = resolveSelectedIdentity(region);
        if (identity == null) {
            setStatus(StatusTone.WARNING, t("Select or type a player before trying to remove them.", "Виберіть або введіть гравця перед спробою видалити його."));
            return;
        }

        LinkedHashMap<String, HyGuardPlugin.PlayerIdentity> blacklist = new LinkedHashMap<>();
        for (HyGuardPlugin.PlayerIdentity entry : plugin.getEntryBlacklist(region)) {
            blacklist.put(entry.uuid(), entry);
        }
        if (blacklist.remove(identity.uuid()) == null) {
            setStatus(StatusTone.WARNING, f("%s is not currently blacklisted for entry.", "%s зараз не перебуває в чорному списку входу.", identity.username()));
            return;
        }

        plugin.setEntryBlacklist(region, blacklist.values());
        plugin.saveRegion(region);
        plugin.playSuccessSound(playerRef);
        setStatus(StatusTone.SUCCESS, f("Removed %s from the entry blacklist.", "%s видалено з чорного списку входу.", identity.username()));
    }

    private HyGuardPlugin.PlayerIdentity resolveSelectedIdentity(Region region) {
        CandidateEntry selected = getSelectedCandidate(region);
        if (selected != null) {
            return new HyGuardPlugin.PlayerIdentity(selected.uuid(), selected.name());
        }
        return plugin.resolvePlayerIdentity(searchInput == null ? null : searchInput.trim());
    }

    private void refresh() {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evt = new UIEventBuilder();
        render(cmd, evt);
        sendUpdate(cmd, evt, true);
    }

    private void render(UICommandBuilder cmd,
                        UIEventBuilder evt) {
        cmd.append(UI_PAGE);
        bindClick(evt, "#BackButton", "Back");
        bindClick(evt, "#CloseIconButton", "Close");
        bindClick(evt, "#AddButton", "Add");
        bindClick(evt, "#RemoveButton", "Remove");
        bindValue(evt, "#SearchInput", "@SearchInput");

        cmd.set("#PageTitle.Text", f("Entry Blacklist - %s", "Чорний список входу - %s", regionName));
        cmd.set("#Subtitle.Text", f("Region: %s | World: %s", "Регіон: %s | Світ: %s", regionName, worldName));
        cmd.set("#SearchInput.Value", searchInput == null ? "" : searchInput);
        applyStatus(cmd);

        Region region = plugin.findRegionByName(worldName, regionName);
        if (region == null) {
            cmd.set("#BlacklistSummary.Text", t("Region missing", "Регіон відсутній"));
            cmd.set("#SelectedName.Text", t("Region missing", "Регіон відсутній"));
            cmd.set("#SelectedState.Text", t("n/a", "н/д"));
            cmd.set("#SelectedHint.Text", t("This region no longer exists.", "Цей регіон більше не існує."));
            return;
        }

        List<HyGuardPlugin.PlayerIdentity> blacklist = plugin.getEntryBlacklist(region);
        cmd.set("#BlacklistSummary.Text", blacklist.isEmpty()
            ? t("No players are blocked from entering this region yet.", "Жодного гравця ще не заблоковано для входу в цей регіон.")
            : f("Blocked players: %d", "Заблоковані гравці: %d", blacklist.size()));

        renderCandidates(cmd, evt, region);

        CandidateEntry selected = getSelectedCandidate(region);
        cmd.set("#SelectedName.Text", selected == null ? t("No player selected", "Гравця не вибрано") : selected.name());
        cmd.set("#SelectedState.Text", selected == null
            ? t("Type a name or pick a row", "Введіть ім'я або виберіть рядок")
            : (selected.blocked() ? t("Blocked", "Заблоковано") : t("Not blocked", "Не заблоковано")));
        cmd.set("#SelectedHint.Text", selected == null
            ? t("Pick or type a player.", "Виберіть або введіть гравця.")
                : (selected.blocked()
                ? t("Remove to allow entry.", "Приберіть для дозволу входу.")
                : t("Add to block entry.", "Додайте для блокування входу.")));
    }

    private void renderCandidates(UICommandBuilder cmd,
                                  UIEventBuilder evt,
                                  Region region) {
        List<CandidateEntry> entries = buildCandidates(region);
        if (selectedPlayerUuid != null && entries.stream().noneMatch(entry -> entry.uuid().equals(selectedPlayerUuid))) {
            selectedPlayerUuid = null;
        }

        int index = 0;
        for (CandidateEntry entry : entries) {
            cmd.append(GROUP_ROOT, UI_ROW);
            String rowId = GROUP_ROOT + "[" + index++ + "]";
            boolean selected = entry.uuid().equals(selectedPlayerUuid);
            boolean isSelf = playerRef.getUuid() != null && entry.uuid().equals(playerRef.getUuid().toString());
                cmd.set(rowId + " #RoleBadge.Text", entry.blocked() ? t("BLOCKED", "ЗАБОРОНЕНО") : t("KNOWN", "ВІДОМИЙ"));
            cmd.set(rowId + " #MemberName.Text", entry.name());
            cmd.set(rowId + " #MemberMeta.Text", entry.blocked()
                    ? t("Entry denied in this region.", "Вхід заборонено в цьому регіоні.")
                    : t("Known player.", "Відомий гравець."));
            cmd.set(rowId + " #SelectedAccent.Visible", selected);
            cmd.set(rowId + " #OwnerChip.Visible", false);
            cmd.set(rowId + " #YouChip.Visible", isSelf);
            cmd.set(rowId + " #SelectedHint.Visible", selected);
            bindClick(evt, rowId, "SelectPlayer|" + entry.uuid());
        }
    }

    private List<CandidateEntry> buildCandidates(Region region) {
        LinkedHashMap<String, CandidateEntry> entries = new LinkedHashMap<>();
        for (HyGuardPlugin.PlayerIdentity identity : plugin.getKnownPlayerIdentities()) {
            entries.put(identity.uuid(), new CandidateEntry(identity.uuid(), identity.username(), false));
        }
        for (HyGuardPlugin.PlayerIdentity identity : plugin.getEntryBlacklist(region)) {
            entries.put(identity.uuid(), new CandidateEntry(identity.uuid(), identity.username(), true));
        }

        String query = normalizedSearch();
        return entries.values().stream()
                .filter(entry -> query.isBlank() || entry.name().toLowerCase(Locale.ROOT).contains(query))
                .sorted(Comparator
                        .comparing(CandidateEntry::blocked).reversed()
                        .thenComparing(CandidateEntry::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private CandidateEntry getSelectedCandidate(Region region) {
        if (selectedPlayerUuid == null || selectedPlayerUuid.isBlank()) {
            return null;
        }
        for (CandidateEntry entry : buildCandidates(region)) {
            if (entry.uuid().equals(selectedPlayerUuid)) {
                return entry;
            }
        }
        return null;
    }

    private String normalizedSearch() {
        return searchInput == null ? "" : searchInput.trim().toLowerCase(Locale.ROOT);
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