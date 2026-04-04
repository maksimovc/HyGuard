package dev.thenexusgates.hyguard.config;

import dev.thenexusgates.hyguard.core.region.RegionFlagValue;

import java.util.ArrayList;
import java.util.List;

public final class HyGuardConfig {

    public int schemaVersion = 2;
    public General general = new General();
    public transient Messages messages = new Messages();
    public Chat chat = new Chat();
    public Limits limits = new Limits();
    public Sounds sounds = new Sounds();
    public Defaults defaults = new Defaults();

    public HyGuardConfig normalize() {
        if (general == null) {
            general = new General();
        }
        general.normalize();

        if (messages == null) {
            messages = new Messages();
        }
        messages.normalize();

        if (chat == null) {
            chat = new Chat();
        }
        chat.normalize();

        if (limits == null) {
            limits = new Limits();
        }
        limits.normalize();

        if (sounds == null) {
            sounds = new Sounds();
        }
        sounds.normalize();

        if (defaults == null) {
            defaults = new Defaults();
        }
        defaults.normalize();
        return this;
    }

    public static final class General {
        public String wandItemId = "HyGuard_Wand";
        public String usePermission = "hyguard.use";
        public String adminPermission = "hyguard.admin";
        public String bypassPermission = "hyguard.bypass";
        public String wandPermission = "hyguard.use";
        public String infoPermission = "hyguard.use";
        public String listPermission = "hyguard.use";
        public String selectPermission = "hyguard.use";
        public String createPermission = "hyguard.use";
        public String redefinePermission = "hyguard.use";
        public String deletePermission = "hyguard.use";
        public String selectionEditPermission = "hyguard.use";
        public String priorityPermission = "hyguard.use";
        public String flagsViewPermission = "hyguard.use";
        public String flagEditPermission = "hyguard.use";
        public String memberPermission = "hyguard.use";
        public String teleportPermission = "hyguard.use";
        public String setSpawnPermission = "hyguard.use";
        public String guiPermission = "hyguard.use";
        public String backupPermission = "hyguard.admin";
        public String debugPermission = "hyguard.admin";
        public String savePermission = "hyguard.admin";
        public String reloadPermission = "hyguard.admin";
        public String bypassTogglePermission = "hyguard.bypass";
        public long autoBackupIntervalMinutes = 30L;
        public int maxBackups = 12;

        private void normalize() {
            if (wandItemId == null || wandItemId.isBlank()) {
                wandItemId = "HyGuard_Wand";
            }
            if (usePermission == null || usePermission.isBlank()) {
                usePermission = "hyguard.use";
            }
            if (adminPermission == null || adminPermission.isBlank()) {
                adminPermission = "hyguard.admin";
            }
            if (bypassPermission == null || bypassPermission.isBlank()) {
                bypassPermission = "hyguard.bypass";
            }
            if (wandPermission == null || wandPermission.isBlank()) {
                wandPermission = usePermission;
            }
            if (infoPermission == null || infoPermission.isBlank()) {
                infoPermission = usePermission;
            }
            if (listPermission == null || listPermission.isBlank()) {
                listPermission = usePermission;
            }
            if (selectPermission == null || selectPermission.isBlank()) {
                selectPermission = usePermission;
            }
            if (createPermission == null || createPermission.isBlank()) {
                createPermission = usePermission;
            }
            if (redefinePermission == null || redefinePermission.isBlank()) {
                redefinePermission = usePermission;
            }
            if (deletePermission == null || deletePermission.isBlank()) {
                deletePermission = usePermission;
            }
            if (selectionEditPermission == null || selectionEditPermission.isBlank()) {
                selectionEditPermission = usePermission;
            }
            if (priorityPermission == null || priorityPermission.isBlank()) {
                priorityPermission = usePermission;
            }
            if (flagsViewPermission == null || flagsViewPermission.isBlank()) {
                flagsViewPermission = usePermission;
            }
            if (flagEditPermission == null || flagEditPermission.isBlank()) {
                flagEditPermission = usePermission;
            }
            if (memberPermission == null || memberPermission.isBlank()) {
                memberPermission = usePermission;
            }
            if (teleportPermission == null || teleportPermission.isBlank()) {
                teleportPermission = usePermission;
            }
            if (setSpawnPermission == null || setSpawnPermission.isBlank()) {
                setSpawnPermission = usePermission;
            }
            if (guiPermission == null || guiPermission.isBlank()) {
                guiPermission = usePermission;
            }
            if (backupPermission == null || backupPermission.isBlank()) {
                backupPermission = adminPermission;
            }
            if (debugPermission == null || debugPermission.isBlank()) {
                debugPermission = adminPermission;
            }
            if (savePermission == null || savePermission.isBlank()) {
                savePermission = adminPermission;
            }
            if (reloadPermission == null || reloadPermission.isBlank()) {
                reloadPermission = adminPermission;
            }
            if (bypassTogglePermission == null || bypassTogglePermission.isBlank()) {
                bypassTogglePermission = bypassPermission;
            }
            if (autoBackupIntervalMinutes < 0L) {
                autoBackupIntervalMinutes = 0L;
            }
            if (maxBackups < 1) {
                maxBackups = 1;
            }
        }
    }

    public static final class Chat {
        public String prefix = "\u00a76[\u00a7eHyGuard\u00a76] \u00a7r";

        private void normalize() {
            if (prefix == null) {
                prefix = "\u00a76[\u00a7eHyGuard\u00a76] \u00a7r";
            }
        }
    }

    public static final class Limits {
        public int regionNameMinLength = 3;
        public int regionNameMaxLength = 32;
        public String regionNamePattern = "^[a-zA-Z0-9_-]+$";
        public int maxPriority = 100;
        public int maxSelectionEditAmount = 500;
        public int maxRegionsPerPlayer = 64;
        public int maxChildRegionsPerParent = 3;
        public int maxMembersPerRegion = 64;

        private void normalize() {
            if (regionNameMinLength < 1) {
                regionNameMinLength = 1;
            }
            if (regionNameMaxLength < regionNameMinLength) {
                regionNameMaxLength = regionNameMinLength;
            }
            if (regionNamePattern == null || regionNamePattern.isBlank()) {
                regionNamePattern = "^[a-zA-Z0-9_-]+$";
            }
            if (maxPriority < 0) {
                maxPriority = 0;
            }
            if (maxSelectionEditAmount < 1) {
                maxSelectionEditAmount = 1;
            }
            if (maxRegionsPerPlayer < 1) {
                maxRegionsPerPlayer = 1;
            }
            if (maxChildRegionsPerParent < 0) {
                maxChildRegionsPerParent = 0;
            }
            if (maxMembersPerRegion < 1) {
                maxMembersPerRegion = 1;
            }
        }
    }

    public static final class Sounds {
        public boolean enabled = true;
        public List<String> selectionPointOne = new ArrayList<>(List.of("SFX_Attn_Quiet", "SFX_Chest_Wooden_Open"));
        public List<String> selectionPointTwo = new ArrayList<>(List.of("SFX_Attn_Moderate", "SFX_Capture_Crate_Capture_Succeed"));
        public List<String> success = new ArrayList<>(List.of("SFX_Capture_Crate_Capture_Succeed", "SFX_Attn_Moderate"));
        public List<String> delete = new ArrayList<>(List.of("SFX_Cactus_Large_Hit", "SFX_Attn_Quiet"));
        public List<String> memberAdded = new ArrayList<>(List.of("SFX_Attn_Moderate", "SFX_Capture_Crate_Capture_Succeed"));
        public List<String> memberRemoved = new ArrayList<>(List.of("SFX_Cactus_Large_Hit", "SFX_Attn_Quiet"));

        private void normalize() {
            selectionPointOne = normalizeList(selectionPointOne, List.of("SFX_Attn_Quiet", "SFX_Chest_Wooden_Open"));
            selectionPointTwo = normalizeList(selectionPointTwo, List.of("SFX_Attn_Moderate", "SFX_Capture_Crate_Capture_Succeed"));
            success = normalizeList(success, List.of("SFX_Capture_Crate_Capture_Succeed", "SFX_Attn_Moderate"));
            delete = normalizeList(delete, List.of("SFX_Cactus_Large_Hit", "SFX_Attn_Quiet"));
            memberAdded = normalizeList(memberAdded, List.of("SFX_Attn_Moderate", "SFX_Capture_Crate_Capture_Succeed"));
            memberRemoved = normalizeList(memberRemoved, List.of("SFX_Cactus_Large_Hit", "SFX_Attn_Quiet"));
        }

        private static List<String> normalizeList(List<String> value, List<String> defaults) {
            List<String> resolved = new ArrayList<>();
            if (value != null) {
                for (String entry : value) {
                    if (entry != null && !entry.isBlank()) {
                        resolved.add(entry);
                    }
                }
            }
            if (resolved.isEmpty()) {
                resolved.addAll(defaults);
            }
            return resolved;
        }
    }

    public static final class Messages {
        public String noPermission = "hyguard.message.no_permission";
        public String wandGiven = "hyguard.message.wand_given";
        public String selectionPointOneSet = "hyguard.message.selection_point_one_set";
        public String selectionPointTwoSet = "hyguard.message.selection_point_two_set";
        public String selectionCleared = "hyguard.message.selection_cleared";
        public String selectionIncomplete = "hyguard.message.selection_incomplete";
        public String selectionLoaded = "hyguard.message.selection_loaded";
        public String globalSelectionUnsupported = "hyguard.message.global_selection_unsupported";
        public String selectionUpdated = "hyguard.message.selection_updated";
        public String selectionWorldMismatch = "hyguard.message.selection_world_mismatch";
        public String invalidSelectionEdit = "hyguard.message.invalid_selection_edit";
        public String invalidSelectionAmount = "hyguard.message.invalid_selection_amount";
        public String regionCreated = "hyguard.message.region_created";
        public String regionRedefined = "hyguard.message.region_redefined";
        public String regionDeleted = "hyguard.message.region_deleted";
        public String regionDeleteConfirmRequired = "hyguard.message.region_delete_confirm_required";
        public String regionNotFound = "hyguard.message.region_not_found";
        public String regionAlreadyExists = "hyguard.message.region_already_exists";
        public String regionOverlapConflict = "hyguard.message.region_overlap_conflict";
        public String regionHierarchyConflict = "hyguard.message.region_hierarchy_conflict";
        public String regionLimitReached = "hyguard.message.region_limit_reached";
        public String regionChildLimitReached = "hyguard.message.region_child_limit_reached";
        public String regionDeleteHasChildren = "hyguard.message.region_delete_has_children";
        public String regionListEmpty = "hyguard.message.region_list_empty";
        public String regionList = "hyguard.message.region_list";
        public String protectionDenied = "hyguard.message.protection_denied";
        public String invalidRegionName = "hyguard.message.invalid_region_name";
        public String invalidPriority = "hyguard.message.invalid_priority";
        public String invalidFlag = "hyguard.message.invalid_flag";
        public String invalidFlagValue = "hyguard.message.invalid_flag_value";
        public String priorityUpdated = "hyguard.message.priority_updated";
        public String flagUpdated = "hyguard.message.flag_updated";
        public String flagCleared = "hyguard.message.flag_cleared";
        public String flagsHeader = "hyguard.message.flags_header";
        public String flagListEntry = "hyguard.message.flag_list_entry";
        public String invalidPage = "hyguard.message.invalid_page";
        public String playerLookupFailed = "hyguard.message.player_lookup_failed";
        public String invalidRole = "hyguard.message.invalid_role";
        public String cannotAssignOwner = "hyguard.message.cannot_assign_owner";
        public String cannotRemoveOwner = "hyguard.message.cannot_remove_owner";
        public String memberAdded = "hyguard.message.member_added";
        public String memberRemoved = "hyguard.message.member_removed";
        public String memberRoleUpdated = "hyguard.message.member_role_updated";
        public String memberNotFound = "hyguard.message.member_not_found";
        public String memberReplaceConfirm = "hyguard.message.member_replace_confirm";
        public String memberLimitReached = "hyguard.message.member_limit_reached";
        public String memberListEmpty = "hyguard.message.member_list_empty";
        public String memberListHeader = "hyguard.message.member_list_header";
        public String memberListEntry = "hyguard.message.member_list_entry";
        public String tpDenied = "hyguard.message.tp_denied";
        public String teleportFailed = "hyguard.message.teleport_failed";
        public String regionTeleported = "hyguard.message.region_teleported";
        public String regionSpawnSet = "hyguard.message.region_spawn_set";
        public String backupStarted = "hyguard.message.backup_started";
        public String backupCompleted = "hyguard.message.backup_completed";
        public String backupFailed = "hyguard.message.backup_failed";
        public String invalidDebugCommand = "hyguard.message.invalid_debug_command";
        public String debugPosNone = "hyguard.message.debug_pos_none";
        public String debugPosHeader = "hyguard.message.debug_pos_header";
        public String debugPosEntry = "hyguard.message.debug_pos_entry";
        public String saveCompleted = "hyguard.message.save_completed";
        public String reloadCompleted = "hyguard.message.reload_completed";
        public String bypassEnabled = "hyguard.message.bypass_enabled";
        public String bypassDisabled = "hyguard.message.bypass_disabled";
        public String helpPageHeader = "hyguard.message.help_page_header";
        public String helpEntry = "hyguard.message.help_entry";
        public String helpDetailHeader = "hyguard.message.help_detail_header";
        public String helpDetailUsage = "hyguard.message.help_detail_usage";
        public String helpDetailDescription = "hyguard.message.help_detail_description";
        public String helpDetailExample = "hyguard.message.help_detail_example";
        public String helpDetailPermission = "hyguard.message.help_detail_permission";
        public String helpUnknownTopic = "hyguard.message.help_unknown_topic";
        public String help = "hyguard.message.help";
        public String regionInfo = "hyguard.message.region_info";

        private void normalize() {
            if (noPermission == null) {
                noPermission = "hyguard.message.no_permission";
            }
            if (wandGiven == null) {
                wandGiven = "hyguard.message.wand_given";
            }
            if (selectionPointOneSet == null) {
                selectionPointOneSet = "hyguard.message.selection_point_one_set";
            }
            if (selectionPointTwoSet == null) {
                selectionPointTwoSet = "hyguard.message.selection_point_two_set";
            }
            if (selectionCleared == null) {
                selectionCleared = "hyguard.message.selection_cleared";
            }
            if (selectionIncomplete == null) {
                selectionIncomplete = "hyguard.message.selection_incomplete";
            }
            if (selectionLoaded == null) {
                selectionLoaded = "hyguard.message.selection_loaded";
            }
            if (globalSelectionUnsupported == null) {
                globalSelectionUnsupported = "hyguard.message.global_selection_unsupported";
            }
            if (selectionUpdated == null) {
                selectionUpdated = "hyguard.message.selection_updated";
            }
            if (selectionWorldMismatch == null) {
                selectionWorldMismatch = "hyguard.message.selection_world_mismatch";
            }
            if (invalidSelectionEdit == null) {
                invalidSelectionEdit = "hyguard.message.invalid_selection_edit";
            }
            if (invalidSelectionAmount == null) {
                invalidSelectionAmount = "hyguard.message.invalid_selection_amount";
            }
            if (regionCreated == null) {
                regionCreated = "hyguard.message.region_created";
            }
            if (regionRedefined == null) {
                regionRedefined = "hyguard.message.region_redefined";
            }
            if (regionDeleted == null) {
                regionDeleted = "hyguard.message.region_deleted";
            }
            if (regionDeleteConfirmRequired == null) {
                regionDeleteConfirmRequired = "hyguard.message.region_delete_confirm_required";
            }
            if (regionNotFound == null) {
                regionNotFound = "hyguard.message.region_not_found";
            }
            if (regionAlreadyExists == null) {
                regionAlreadyExists = "hyguard.message.region_already_exists";
            }
            if (regionOverlapConflict == null) {
                regionOverlapConflict = "hyguard.message.region_overlap_conflict";
            }
            if (regionHierarchyConflict == null) {
                regionHierarchyConflict = "hyguard.message.region_hierarchy_conflict";
            }
            if (regionLimitReached == null) {
                regionLimitReached = "hyguard.message.region_limit_reached";
            }
            if (regionChildLimitReached == null) {
                regionChildLimitReached = "hyguard.message.region_child_limit_reached";
            }
            if (regionDeleteHasChildren == null) {
                regionDeleteHasChildren = "hyguard.message.region_delete_has_children";
            }
            if (regionListEmpty == null) {
                regionListEmpty = "hyguard.message.region_list_empty";
            }
            if (regionList == null) {
                regionList = "hyguard.message.region_list";
            }
            if (protectionDenied == null) {
                protectionDenied = "hyguard.message.protection_denied";
            }
            if (invalidRegionName == null) {
                invalidRegionName = "hyguard.message.invalid_region_name";
            }
            if (invalidPriority == null) {
                invalidPriority = "hyguard.message.invalid_priority";
            }
            if (invalidFlag == null) {
                invalidFlag = "hyguard.message.invalid_flag";
            }
            if (invalidFlagValue == null) {
                invalidFlagValue = "hyguard.message.invalid_flag_value";
            }
            if (priorityUpdated == null) {
                priorityUpdated = "hyguard.message.priority_updated";
            }
            if (flagUpdated == null) {
                flagUpdated = "hyguard.message.flag_updated";
            }
            if (flagCleared == null) {
                flagCleared = "hyguard.message.flag_cleared";
            }
            if (flagsHeader == null) {
                flagsHeader = "hyguard.message.flags_header";
            }
            if (flagListEntry == null) {
                flagListEntry = "hyguard.message.flag_list_entry";
            }
            if (invalidPage == null) {
                invalidPage = "hyguard.message.invalid_page";
            }
            if (playerLookupFailed == null) {
                playerLookupFailed = "hyguard.message.player_lookup_failed";
            }
            if (invalidRole == null) {
                invalidRole = "hyguard.message.invalid_role";
            }
            if (cannotAssignOwner == null) {
                cannotAssignOwner = "hyguard.message.cannot_assign_owner";
            }
            if (cannotRemoveOwner == null) {
                cannotRemoveOwner = "hyguard.message.cannot_remove_owner";
            }
            if (memberAdded == null) {
                memberAdded = "hyguard.message.member_added";
            }
            if (memberRemoved == null) {
                memberRemoved = "hyguard.message.member_removed";
            }
            if (memberRoleUpdated == null) {
                memberRoleUpdated = "hyguard.message.member_role_updated";
            }
            if (memberNotFound == null) {
                memberNotFound = "hyguard.message.member_not_found";
            }
            if (memberReplaceConfirm == null) {
                memberReplaceConfirm = "hyguard.message.member_replace_confirm";
            }
            if (memberLimitReached == null) {
                memberLimitReached = "hyguard.message.member_limit_reached";
            }
            if (memberListEmpty == null) {
                memberListEmpty = "hyguard.message.member_list_empty";
            }
            if (memberListHeader == null) {
                memberListHeader = "hyguard.message.member_list_header";
            }
            if (memberListEntry == null) {
                memberListEntry = "hyguard.message.member_list_entry";
            }
            if (tpDenied == null) {
                tpDenied = "hyguard.message.tp_denied";
            }
            if (teleportFailed == null) {
                teleportFailed = "hyguard.message.teleport_failed";
            }
            if (regionTeleported == null) {
                regionTeleported = "hyguard.message.region_teleported";
            }
            if (regionSpawnSet == null) {
                regionSpawnSet = "hyguard.message.region_spawn_set";
            }
            if (backupStarted == null) {
                backupStarted = "hyguard.message.backup_started";
            }
            if (backupCompleted == null) {
                backupCompleted = "hyguard.message.backup_completed";
            }
            if (backupFailed == null) {
                backupFailed = "hyguard.message.backup_failed";
            }
            if (invalidDebugCommand == null) {
                invalidDebugCommand = "hyguard.message.invalid_debug_command";
            }
            if (debugPosNone == null) {
                debugPosNone = "hyguard.message.debug_pos_none";
            }
            if (debugPosHeader == null) {
                debugPosHeader = "hyguard.message.debug_pos_header";
            }
            if (debugPosEntry == null) {
                debugPosEntry = "hyguard.message.debug_pos_entry";
            }
            if (saveCompleted == null) {
                saveCompleted = "hyguard.message.save_completed";
            }
            if (reloadCompleted == null) {
                reloadCompleted = "hyguard.message.reload_completed";
            }
            if (bypassEnabled == null) {
                bypassEnabled = "hyguard.message.bypass_enabled";
            }
            if (bypassDisabled == null) {
                bypassDisabled = "hyguard.message.bypass_disabled";
            }
            if (helpPageHeader == null) {
                helpPageHeader = "hyguard.message.help_page_header";
            }
            if (helpEntry == null) {
                helpEntry = "hyguard.message.help_entry";
            }
            if (helpDetailHeader == null) {
                helpDetailHeader = "hyguard.message.help_detail_header";
            }
            if (helpDetailUsage == null) {
                helpDetailUsage = "hyguard.message.help_detail_usage";
            }
            if (helpDetailDescription == null) {
                helpDetailDescription = "hyguard.message.help_detail_description";
            }
            if (helpDetailExample == null) {
                helpDetailExample = "hyguard.message.help_detail_example";
            }
            if (helpDetailPermission == null) {
                helpDetailPermission = "hyguard.message.help_detail_permission";
            }
            if (helpUnknownTopic == null) {
                helpUnknownTopic = "hyguard.message.help_unknown_topic";
            }
            if (help == null) {
                help = "hyguard.message.help";
            }
            if (regionInfo == null) {
                regionInfo = "hyguard.message.region_info";
            }
        }
    }

    public static final class Defaults {
        public String blockBreak = RegionFlagValue.Mode.DENY.name();
        public String blockPlace = RegionFlagValue.Mode.DENY.name();
        public String blockInteract = RegionFlagValue.Mode.DENY.name();

        private void normalize() {
            if (blockBreak == null || blockBreak.isBlank()) {
                blockBreak = RegionFlagValue.Mode.DENY.name();
            }
            if (blockPlace == null || blockPlace.isBlank()) {
                blockPlace = RegionFlagValue.Mode.DENY.name();
            }
            if (blockInteract == null || blockInteract.isBlank()) {
                blockInteract = RegionFlagValue.Mode.DENY.name();
            }
        }
    }
}