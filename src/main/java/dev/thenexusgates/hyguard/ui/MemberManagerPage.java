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
import dev.thenexusgates.hyguard.core.region.RegionMember;
import dev.thenexusgates.hyguard.core.region.RegionRole;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class MemberManagerPage extends InteractiveCustomUIPage<MemberManagerPage.PageData> {

    private enum StatusTone {
        INFO,
        SUCCESS,
        WARNING,
        ERROR
    }

    private record MemberEntry(String uuid, String name, RegionRole role, boolean owner) {
    }

    public static final class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (data, value) -> data.action = value, data -> data.action).add()
                .append(new KeyedCodec<>("@AddMemberInput", Codec.STRING), (data, value) -> data.addMemberInput = value, data -> data.addMemberInput).add()
                .build();

        String action;
        String addMemberInput;
    }

    private static final String UI_PAGE = "Pages/HyGuardMemberManager.ui";
    private static final String UI_ROW = "Pages/HyGuardMemberRow.ui";
    private static final String GROUP_ROOT = "#MemberRowsGroup";

    private final HyGuardPlugin plugin;
    private final String worldName;
    private final String regionName;

    private String addMemberInput = "";
    private RegionRole selectedAddRole = RegionRole.MEMBER;
    private String selectedMemberUuid;
    private boolean removeArmed;
    private String statusMessage = "Select a member to manage their role, or add a new player on the right.";
    private StatusTone statusTone = StatusTone.INFO;

    public MemberManagerPage(PlayerRef playerRef, HyGuardPlugin plugin, String worldName, String regionName) {
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
        if (data.addMemberInput != null) {
            addMemberInput = data.addMemberInput;
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
            case "PickRole:CO_OWNER" -> pickRole(RegionRole.CO_OWNER);
            case "PickRole:MANAGER" -> pickRole(RegionRole.MANAGER);
            case "PickRole:MEMBER" -> pickRole(RegionRole.MEMBER);
            case "PickRole:TRUSTED" -> pickRole(RegionRole.TRUSTED);
            case "PickRole:VISITOR" -> pickRole(RegionRole.VISITOR);
            case "AddOrUpdate" -> applyAdd(region);
            case "Promote" -> promoteSelected(region);
            case "Demote" -> demoteSelected(region);
            case "Remove" -> removeSelected(region);
            default -> {
                if (!data.action.startsWith("SelectMember|")) {
                    return;
                }
                selectedMemberUuid = data.action.substring("SelectMember|".length());
                removeArmed = false;
                MemberEntry selected = getSelectedEntry(region);
                if (selected != null) {
                    setStatus(StatusTone.INFO, selected.owner()
                            ? selected.name() + " is the owner and cannot be demoted or removed here."
                            : "Selected " + selected.name() + ". Use the action panel to promote, demote, or remove them.");
                }
            }
        }

        refresh(entityRef, store);
    }

    private void pickRole(RegionRole role) {
        selectedAddRole = role;
        setStatus(StatusTone.INFO, "New or updated members will be assigned as " + RegionUiText.displayRole(role) + ".");
    }

    private void applyAdd(Region region) {
        if (!plugin.canManageMembership(playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
            setStatus(StatusTone.ERROR, "You do not have permission to manage membership in this region.");
            return;
        }

        HyGuardPlugin.PlayerIdentity target = plugin.resolvePlayerIdentity(addMemberInput == null ? null : addMemberInput.trim());
        if (target == null) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.playerLookupFailed);
            setStatus(StatusTone.ERROR, "Player not found. Use an exact username or a remembered player name.");
            return;
        }
        if (target.uuid().equals(region.getOwnerUuid())) {
            selectedMemberUuid = target.uuid();
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.cannotAssignOwner);
            setStatus(StatusTone.WARNING, "The owner already has the highest role and cannot be reassigned here.");
            return;
        }

        RegionMember existing = region.getMember(target.uuid());
        RegionMember updated = new RegionMember(target.uuid(), target.username(), selectedAddRole);
        region.addMember(updated);
        plugin.saveRegion(region);
        selectedMemberUuid = target.uuid();
        removeArmed = false;
        plugin.send(playerRef,
                existing == null ? plugin.getConfigSnapshot().messages.memberAdded : plugin.getConfigSnapshot().messages.memberRoleUpdated,
                Map.of(
                        "player", updated.getName(),
                        "name", region.getName(),
                        "role", updated.getRole().name()
                ));
        setStatus(StatusTone.SUCCESS,
                (existing == null ? "Added " : "Updated ") + updated.getName() + " as " + RegionUiText.displayRole(updated.getRole()) + ".");
    }

    private void promoteSelected(Region region) {
        if (!plugin.canManageMembership(playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
            setStatus(StatusTone.ERROR, "You do not have permission to promote members in this region.");
            return;
        }

        MemberEntry selected = getSelectedEntry(region);
        if (selected == null) {
            setStatus(StatusTone.WARNING, "Select a member row first.");
            return;
        }
        if (selected.owner()) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.cannotAssignOwner);
            setStatus(StatusTone.WARNING, "The owner cannot be promoted beyond owner.");
            return;
        }

        RegionRole nextRole = promoteRole(selected.role());
        if (nextRole == null) {
            setStatus(StatusTone.WARNING, RegionUiText.displayRole(selected.role()) + " is already at the highest assignable tier.");
            return;
        }

        region.addMember(new RegionMember(selected.uuid(), selected.name(), nextRole));
        plugin.saveRegion(region);
        removeArmed = false;
        plugin.send(playerRef, plugin.getConfigSnapshot().messages.memberRoleUpdated, Map.of(
                "player", selected.name(),
                "name", region.getName(),
                "role", nextRole.name()
        ));
        setStatus(StatusTone.SUCCESS, selected.name() + " is now " + RegionUiText.displayRole(nextRole) + ".");
    }

    private void demoteSelected(Region region) {
        if (!plugin.canManageMembership(playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
            setStatus(StatusTone.ERROR, "You do not have permission to demote members in this region.");
            return;
        }

        MemberEntry selected = getSelectedEntry(region);
        if (selected == null) {
            setStatus(StatusTone.WARNING, "Select a member row first.");
            return;
        }
        if (selected.owner()) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.cannotAssignOwner);
            setStatus(StatusTone.WARNING, "The owner cannot be demoted.");
            return;
        }

        RegionRole nextRole = demoteRole(selected.role());
        if (nextRole == null) {
            setStatus(StatusTone.WARNING, RegionUiText.displayRole(selected.role()) + " is already the lowest tier.");
            return;
        }

        region.addMember(new RegionMember(selected.uuid(), selected.name(), nextRole));
        plugin.saveRegion(region);
        removeArmed = false;
        plugin.send(playerRef, plugin.getConfigSnapshot().messages.memberRoleUpdated, Map.of(
                "player", selected.name(),
                "name", region.getName(),
                "role", nextRole.name()
        ));
        setStatus(StatusTone.SUCCESS, selected.name() + " is now " + RegionUiText.displayRole(nextRole) + ".");
    }

    private void removeSelected(Region region) {
        if (!plugin.canManageMembership(playerRef, region)) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.noPermission);
            setStatus(StatusTone.ERROR, "You do not have permission to remove members from this region.");
            return;
        }

        MemberEntry selected = getSelectedEntry(region);
        if (selected == null) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.memberNotFound, Map.of("name", region.getName()));
            setStatus(StatusTone.WARNING, "Select a member before trying to remove them.");
            return;
        }
        if (selected.owner()) {
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.cannotRemoveOwner);
            setStatus(StatusTone.WARNING, "The owner cannot be removed from the region.");
            return;
        }
        if (!removeArmed) {
            removeArmed = true;
            setStatus(StatusTone.WARNING, "Press Remove Member again to confirm removing " + selected.name() + ".");
            return;
        }

        if (region.removeMember(selected.uuid())) {
            plugin.saveRegion(region);
            plugin.send(playerRef, plugin.getConfigSnapshot().messages.memberRemoved, Map.of(
                    "player", selected.name(),
                    "name", region.getName()
            ));
            selectedMemberUuid = null;
            removeArmed = false;
            setStatus(StatusTone.SUCCESS, "Removed " + selected.name() + " from the region.");
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

        bindClick(evt, "#BackButton", "Back");
        bindClick(evt, "#CloseIconButton", "Close");
        bindClick(evt, "#AddMemberButton", "AddOrUpdate");
        bindClick(evt, "#PromoteButton", "Promote");
        bindClick(evt, "#DemoteButton", "Demote");
        bindClick(evt, "#RemoveButton", "Remove");
        bindClick(evt, "#RoleCoOwnerButton", "PickRole:CO_OWNER");
        bindClick(evt, "#RoleManagerButton", "PickRole:MANAGER");
        bindClick(evt, "#RoleMemberButton", "PickRole:MEMBER");
        bindClick(evt, "#RoleTrustedButton", "PickRole:TRUSTED");
        bindClick(evt, "#RoleVisitorButton", "PickRole:VISITOR");
        bindValue(evt, "#AddMemberInput", "@AddMemberInput");

        cmd.set("#PageTitle.Text", "Member Manager - " + regionName);
        cmd.set("#Subtitle.Text", "Region: " + regionName + " | World: " + worldName + " | Add members with the role picker instead of typing raw enum names.");
        cmd.set("#AddMemberInput.Value", addMemberInput == null ? "" : addMemberInput);
        cmd.set("#AddHint.Text", RegionUiText.roleDescription(selectedAddRole));
        applyStatus(cmd);
        setRolePickerState(cmd);

        Region region = plugin.findRegionByName(worldName, regionName);
        if (region == null) {
            cmd.set("#OwnerValue.Text", "Owner: n/a");
            cmd.set("#SelectedName.Text", "Region missing");
            cmd.set("#SelectedRole.Text", "n/a");
            cmd.set("#SelectionHint.Text", "This region no longer exists.");
            cmd.set("#AddMemberButton.Visible", false);
            cmd.set("#AddMemberButtonDisabled.Visible", true);
            setActionAvailability(cmd, false, false, false);
            cmd.set("#RemoveHint.Text", "");
            return;
        }

        cmd.set("#OwnerValue.Text", "Owner: " + region.getOwnerName());
        renderMembers(cmd, evt, region);

        MemberEntry selected = getSelectedEntry(region);
        boolean hasPermission = plugin.canManageMembership(playerRef, region);
        cmd.set("#AddMemberButton.Visible", hasPermission);
        cmd.set("#AddMemberButtonDisabled.Visible", !hasPermission);
        boolean canPromote = hasPermission && selected != null && !selected.owner() && promoteRole(selected.role()) != null;
        boolean canDemote = hasPermission && selected != null && !selected.owner() && demoteRole(selected.role()) != null;
        boolean canRemove = hasPermission && selected != null && !selected.owner();
        setActionAvailability(cmd, canPromote, canDemote, canRemove);

        cmd.set("#SelectedName.Text", selected == null ? "No member selected" : selected.name());
        cmd.set("#SelectedRole.Text", selected == null ? "Choose a row from the left" : RegionUiText.displayRole(selected.role()));
        cmd.set("#SelectionHint.Text", selected == null
                ? "Select a member row to promote, demote, or remove that player."
                : selected.owner()
                    ? "Owner can be viewed but not removed or demoted from this menu."
                    : RegionUiText.roleDescription(selected.role()));
        cmd.set("#RemoveButtonLabel.Text", removeArmed && canRemove ? "Confirm Remove" : "Remove Member");
        cmd.set("#RemoveHint.Text", !hasPermission
                ? "You can inspect members here, but you lack permission to change them."
                : removeArmed && canRemove
                    ? "Dangerous action armed. Press Remove Member again to confirm."
                    : selected != null && selected.owner()
                        ? "Owner is locked and cannot be removed."
                        : selected == null
                            ? "Select a member to unlock management actions."
                            : "Promote and Demote move the selected member by one role step.");
    }

    private void renderMembers(UICommandBuilder cmd,
                               UIEventBuilder evt,
                               Region region) {
        List<MemberEntry> entries = buildMemberEntries(region);
        if (selectedMemberUuid != null && entries.stream().noneMatch(entry -> entry.uuid().equals(selectedMemberUuid))) {
            selectedMemberUuid = null;
            removeArmed = false;
        }

        int index = 0;
        for (MemberEntry entry : entries) {
            cmd.append(GROUP_ROOT, UI_ROW);
            String rowId = GROUP_ROOT + "[" + index++ + "]";
            boolean selected = entry.uuid().equals(selectedMemberUuid);
            boolean isSelf = playerRef.getUuid() != null && entry.uuid().equals(playerRef.getUuid().toString());
            cmd.set(rowId + " #RoleBadge.Text", RegionUiText.displayRole(entry.role()));
            cmd.set(rowId + " #MemberName.Text", entry.name());
            cmd.set(rowId + " #MemberMeta.Text", entry.owner()
                    ? (isSelf ? "Owner • You" : "Owner")
                    : (isSelf ? RegionUiText.roleDescription(entry.role()) + " • You" : RegionUiText.roleDescription(entry.role())));
            cmd.set(rowId + " #SelectedAccent.Visible", selected);
            cmd.set(rowId + " #OwnerChip.Visible", entry.owner());
            cmd.set(rowId + " #YouChip.Visible", isSelf);
            cmd.set(rowId + " #SelectedHint.Visible", selected);
            bindClick(evt, rowId, "SelectMember|" + entry.uuid());
        }
    }

    private List<MemberEntry> buildMemberEntries(Region region) {
        List<MemberEntry> entries = new ArrayList<>();
        entries.add(new MemberEntry(region.getOwnerUuid(), region.getOwnerName(), RegionRole.OWNER, true));

        List<RegionMember> members = new ArrayList<>(region.getMembers().values());
        members.removeIf(member -> member.getUuid() == null || member.getUuid().equals(region.getOwnerUuid()));
        members.sort(Comparator
                .comparing((RegionMember member) -> member.getRole().ordinal())
                .thenComparing(RegionMember::getName, String.CASE_INSENSITIVE_ORDER));
        for (RegionMember member : members) {
            entries.add(new MemberEntry(member.getUuid(), member.getName(), member.getRole(), false));
        }
        return entries;
    }

    private MemberEntry getSelectedEntry(Region region) {
        if (selectedMemberUuid == null || selectedMemberUuid.isBlank()) {
            return null;
        }
        if (selectedMemberUuid.equals(region.getOwnerUuid())) {
            return new MemberEntry(region.getOwnerUuid(), region.getOwnerName(), RegionRole.OWNER, true);
        }
        RegionMember member = region.getMember(selectedMemberUuid);
        if (member == null) {
            return null;
        }
        return new MemberEntry(member.getUuid(), member.getName(), member.getRole(), false);
    }

    private RegionRole promoteRole(RegionRole role) {
        return switch (role) {
            case OWNER, CO_OWNER -> null;
            case MANAGER -> RegionRole.CO_OWNER;
            case MEMBER -> RegionRole.MANAGER;
            case TRUSTED -> RegionRole.MEMBER;
            case VISITOR -> RegionRole.TRUSTED;
        };
    }

    private RegionRole demoteRole(RegionRole role) {
        return switch (role) {
            case OWNER -> null;
            case CO_OWNER -> RegionRole.MANAGER;
            case MANAGER -> RegionRole.MEMBER;
            case MEMBER -> RegionRole.TRUSTED;
            case TRUSTED -> RegionRole.VISITOR;
            case VISITOR -> null;
        };
    }

    private void setActionAvailability(UICommandBuilder cmd,
                                       boolean canPromote,
                                       boolean canDemote,
                                       boolean canRemove) {
        cmd.set("#PromoteButton.Visible", canPromote);
        cmd.set("#PromoteButtonDisabled.Visible", !canPromote);
        cmd.set("#DemoteButton.Visible", canDemote);
        cmd.set("#DemoteButtonDisabled.Visible", !canDemote);
        cmd.set("#RemoveButton.Visible", canRemove);
        cmd.set("#RemoveButtonDisabled.Visible", !canRemove);
    }

    private void setRolePickerState(UICommandBuilder cmd) {
        setRoleState(cmd, "#RoleCoOwnerSelected", selectedAddRole == RegionRole.CO_OWNER);
        setRoleState(cmd, "#RoleManagerSelected", selectedAddRole == RegionRole.MANAGER);
        setRoleState(cmd, "#RoleMemberSelected", selectedAddRole == RegionRole.MEMBER);
        setRoleState(cmd, "#RoleTrustedSelected", selectedAddRole == RegionRole.TRUSTED);
        setRoleState(cmd, "#RoleVisitorSelected", selectedAddRole == RegionRole.VISITOR);
        cmd.set("#SelectedRolePreview.Text", "Selected role: " + RegionUiText.displayRole(selectedAddRole));
    }

    private void setRoleState(UICommandBuilder cmd, String selector, boolean visible) {
        cmd.set(selector + ".Visible", visible);
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
}