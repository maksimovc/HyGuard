package dev.thenexusgates.hyguard.config;

import dev.thenexusgates.hyguard.core.region.RegionFlagValue;

public final class HyGuardConfig {

    public int schemaVersion = 1;
    public General general = new General();
    public Messages messages = new Messages();
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

        if (defaults == null) {
            defaults = new Defaults();
        }
        defaults.normalize();
        return this;
    }

    public static final class General {
        public String wandItemId = "HyGuard_Wand";
        public String adminPermission = "hyguard.admin";
        public String bypassPermission = "hyguard.bypass";
        public long autoBackupIntervalMinutes = 30L;
        public int maxBackups = 12;

        private void normalize() {
            if (wandItemId == null || wandItemId.isBlank()) {
                wandItemId = "HyGuard_Wand";
            }
            if (adminPermission == null || adminPermission.isBlank()) {
                adminPermission = "hyguard.admin";
            }
            if (bypassPermission == null || bypassPermission.isBlank()) {
                bypassPermission = "hyguard.bypass";
            }
            if (autoBackupIntervalMinutes < 0L) {
                autoBackupIntervalMinutes = 0L;
            }
            if (maxBackups < 1) {
                maxBackups = 1;
            }
        }
    }

    public static final class Messages {
        public String prefix = "[HyGuard] ";
        public String noPermission = "You do not have permission for that action.";
        public String wandGiven = "Wand added to your inventory.";
        public String selectionPointOneSet = "Selection point 1 set at {pos}.";
        public String selectionPointTwoSet = "Selection point 2 set at {pos}. Size: {size}.";
        public String selectionIncomplete = "Set both selection points first.";
        public String selectionLoaded = "Selection loaded from region {name}.";
        public String globalSelectionUnsupported = "Global regions do not use two-point bounds.";
        public String selectionUpdated = "Selection updated: min={min}, max={max}, size={size}.";
        public String selectionWorldMismatch = "Your selection must be in the same world as the region.";
        public String invalidSelectionEdit = "Usage: /hg expand|contract|shift <north|south|east|west|up|down> <amount>.";
        public String invalidSelectionAmount = "Selection amount must be an integer from 1 to 500 that keeps the selection valid.";
        public String regionCreated = "Region {name} created.";
        public String regionRedefined = "Region {name} redefined from your current selection.";
        public String regionDeleted = "Region {name} deleted.";
        public String regionDeleteConfirmRequired = "Run /hg delete {name} --confirm to delete this region.";
        public String regionNotFound = "Region not found.";
        public String regionAlreadyExists = "A region with that name already exists.";
        public String regionOverlapConflict = "That selection overlaps a region owned by another player.";
        public String regionListEmpty = "No regions found in this world.";
        public String regionList = "Regions in {world}: {regions}";
        public String protectionDenied = "This area is protected.";
        public String invalidRegionName = "Region names must be 3-32 characters and use only letters, numbers, underscores, or dashes.";
        public String invalidPriority = "Priority must be an integer from 0 to 100.";
        public String invalidFlag = "Unknown region flag.";
        public String invalidFlagValue = "Unknown flag value. Use ALLOW, DENY, INHERIT, ALLOW_MEMBERS, ALLOW_TRUSTED, or clear.";
        public String priorityUpdated = "Region {name} priority set to {priority}.";
        public String flagUpdated = "Region {name} flag {flag} set to {value}.";
        public String flagCleared = "Region {name} flag {flag} cleared.";
        public String flagsHeader = "Flags for {name} page {page}/{pages}:";
        public String flagListEntry = "{flag} = {value}";
        public String invalidPage = "Page must be a positive integer.";
        public String playerLookupFailed = "Player not found online or in saved player data.";
        public String invalidRole = "Unknown role. Use CO_OWNER, MANAGER, MEMBER, TRUSTED, or VISITOR.";
        public String cannotAssignOwner = "OWNER cannot be assigned with member commands.";
        public String cannotRemoveOwner = "The region owner cannot be removed.";
        public String memberAdded = "Added {player} to region {name} as {role}.";
        public String memberRemoved = "Removed {player} from region {name}.";
        public String memberRoleUpdated = "Updated {player} in region {name} to {role}.";
        public String memberNotFound = "That player is not a member of region {name}.";
        public String memberReplaceConfirm = "{player} is already a member with role {role}. Repeat the command with --confirm to replace it.";
        public String memberListEmpty = "Region {name} has no members.";
        public String memberListHeader = "Members of {name} page {page}/{pages}:";
        public String memberListEntry = "[{role}] {player}";
        public String tpDenied = "You do not have permission to teleport to that region.";
        public String teleportFailed = "Teleport failed.";
        public String regionTeleported = "Teleported to region {name}.";
        public String regionSpawnSet = "Spawn point for {name} set to {pos}.";
        public String backupStarted = "Backup started.";
        public String backupCompleted = "Backup completed: {path}";
        public String backupFailed = "Backup failed. Check server logs.";
        public String invalidDebugCommand = "Usage: /hg debug pos";
        public String debugPosNone = "No regions contain your current position.";
        public String debugPosHeader = "Regions at your position:";
        public String debugPosEntry = "{name} priority={priority} owner={owner} flags={flags}";
        public String saveCompleted = "Pending region saves flushed to disk.";
        public String reloadCompleted = "Configuration and regions reloaded from disk.";
        public String bypassEnabled = "Bypass enabled.";
        public String bypassDisabled = "Bypass disabled.";
        public String helpPageHeader = "Help page {page}/{pages}:";
        public String helpEntry = "{usage} - {description}";
        public String helpDetailHeader = "Help for /hg {command}:";
        public String helpDetailUsage = "Usage: {usage}";
        public String helpDetailDescription = "Description: {description}";
        public String helpDetailExample = "Example: {example}";
        public String helpDetailPermission = "Permission: {permission}";
        public String helpUnknownTopic = "Unknown help topic.";
        public String help = "Commands: /hg wand, /hg create <name>, /hg delete <name> --confirm, /hg info [name], /hg list, /hg select <name>, /hg redefine <name>, /hg expand <dir> <amount>, /hg contract <dir> <amount>, /hg shift <dir> <amount>, /hg priority <name> <value>, /hg flag <name> <flag> <value|clear> [text], /hg flags <name>, /hg member <subcommand>, /hg tp <name>, /hg setspawn <name>, /hg backup, /hg debug pos, /hg bypass, /hg save, /hg reload, /hg help [topic]";
        public String regionInfo = "Region {name}: owner={owner}, world={world}, min={min}, max={max}";

        private void normalize() {
            if (prefix == null) {
                prefix = "[HyGuard] ";
            }
            if (noPermission == null) {
                noPermission = "You do not have permission for that action.";
            }
            if (wandGiven == null) {
                wandGiven = "Wand added to your inventory.";
            }
            if (selectionPointOneSet == null) {
                selectionPointOneSet = "Selection point 1 set at {pos}.";
            }
            if (selectionPointTwoSet == null) {
                selectionPointTwoSet = "Selection point 2 set at {pos}. Size: {size}.";
            }
            if (selectionIncomplete == null) {
                selectionIncomplete = "Set both selection points first.";
            }
            if (selectionLoaded == null) {
                selectionLoaded = "Selection loaded from region {name}.";
            }
            if (globalSelectionUnsupported == null) {
                globalSelectionUnsupported = "Global regions do not use two-point bounds.";
            }
            if (selectionUpdated == null) {
                selectionUpdated = "Selection updated: min={min}, max={max}, size={size}.";
            }
            if (selectionWorldMismatch == null) {
                selectionWorldMismatch = "Your selection must be in the same world as the region.";
            }
            if (invalidSelectionEdit == null) {
                invalidSelectionEdit = "Usage: /hg expand|contract|shift <north|south|east|west|up|down> <amount>.";
            }
            if (invalidSelectionAmount == null) {
                invalidSelectionAmount = "Selection amount must be an integer from 1 to 500 that keeps the selection valid.";
            }
            if (regionCreated == null) {
                regionCreated = "Region {name} created.";
            }
            if (regionRedefined == null) {
                regionRedefined = "Region {name} redefined from your current selection.";
            }
            if (regionDeleted == null) {
                regionDeleted = "Region {name} deleted.";
            }
            if (regionDeleteConfirmRequired == null) {
                regionDeleteConfirmRequired = "Run /hg delete {name} --confirm to delete this region.";
            }
            if (regionNotFound == null) {
                regionNotFound = "Region not found.";
            }
            if (regionAlreadyExists == null) {
                regionAlreadyExists = "A region with that name already exists.";
            }
            if (regionOverlapConflict == null) {
                regionOverlapConflict = "That selection overlaps a region owned by another player.";
            }
            if (regionListEmpty == null) {
                regionListEmpty = "No regions found in this world.";
            }
            if (regionList == null) {
                regionList = "Regions in {world}: {regions}";
            }
            if (protectionDenied == null) {
                protectionDenied = "This area is protected.";
            }
            if (invalidRegionName == null) {
                invalidRegionName = "Region names must be 3-32 characters and use only letters, numbers, underscores, or dashes.";
            }
            if (invalidPriority == null) {
                invalidPriority = "Priority must be an integer from 0 to 100.";
            }
            if (invalidFlag == null) {
                invalidFlag = "Unknown region flag.";
            }
            if (invalidFlagValue == null) {
                invalidFlagValue = "Unknown flag value. Use ALLOW, DENY, INHERIT, ALLOW_MEMBERS, ALLOW_TRUSTED, or clear.";
            }
            if (priorityUpdated == null) {
                priorityUpdated = "Region {name} priority set to {priority}.";
            }
            if (flagUpdated == null) {
                flagUpdated = "Region {name} flag {flag} set to {value}.";
            }
            if (flagCleared == null) {
                flagCleared = "Region {name} flag {flag} cleared.";
            }
            if (flagsHeader == null) {
                flagsHeader = "Flags for {name} page {page}/{pages}:";
            }
            if (flagListEntry == null) {
                flagListEntry = "{flag} = {value}";
            }
            if (invalidPage == null) {
                invalidPage = "Page must be a positive integer.";
            }
            if (playerLookupFailed == null) {
                playerLookupFailed = "Player not found online or in saved player data.";
            }
            if (invalidRole == null) {
                invalidRole = "Unknown role. Use CO_OWNER, MANAGER, MEMBER, TRUSTED, or VISITOR.";
            }
            if (cannotAssignOwner == null) {
                cannotAssignOwner = "OWNER cannot be assigned with member commands.";
            }
            if (cannotRemoveOwner == null) {
                cannotRemoveOwner = "The region owner cannot be removed.";
            }
            if (memberAdded == null) {
                memberAdded = "Added {player} to region {name} as {role}.";
            }
            if (memberRemoved == null) {
                memberRemoved = "Removed {player} from region {name}.";
            }
            if (memberRoleUpdated == null) {
                memberRoleUpdated = "Updated {player} in region {name} to {role}.";
            }
            if (memberNotFound == null) {
                memberNotFound = "That player is not a member of region {name}.";
            }
            if (memberReplaceConfirm == null) {
                memberReplaceConfirm = "{player} is already a member with role {role}. Repeat the command with --confirm to replace it.";
            }
            if (memberListEmpty == null) {
                memberListEmpty = "Region {name} has no members.";
            }
            if (memberListHeader == null) {
                memberListHeader = "Members of {name} page {page}/{pages}:";
            }
            if (memberListEntry == null) {
                memberListEntry = "[{role}] {player}";
            }
            if (tpDenied == null) {
                tpDenied = "You do not have permission to teleport to that region.";
            }
            if (teleportFailed == null) {
                teleportFailed = "Teleport failed.";
            }
            if (regionTeleported == null) {
                regionTeleported = "Teleported to region {name}.";
            }
            if (regionSpawnSet == null) {
                regionSpawnSet = "Spawn point for {name} set to {pos}.";
            }
            if (backupStarted == null) {
                backupStarted = "Backup started.";
            }
            if (backupCompleted == null) {
                backupCompleted = "Backup completed: {path}";
            }
            if (backupFailed == null) {
                backupFailed = "Backup failed. Check server logs.";
            }
            if (invalidDebugCommand == null) {
                invalidDebugCommand = "Usage: /hg debug pos";
            }
            if (debugPosNone == null) {
                debugPosNone = "No regions contain your current position.";
            }
            if (debugPosHeader == null) {
                debugPosHeader = "Regions at your position:";
            }
            if (debugPosEntry == null) {
                debugPosEntry = "{name} priority={priority} owner={owner} flags={flags}";
            }
            if (saveCompleted == null) {
                saveCompleted = "Pending region saves flushed to disk.";
            }
            if (reloadCompleted == null) {
                reloadCompleted = "Configuration and regions reloaded from disk.";
            }
            if (bypassEnabled == null) {
                bypassEnabled = "Bypass enabled.";
            }
            if (bypassDisabled == null) {
                bypassDisabled = "Bypass disabled.";
            }
            if (helpPageHeader == null) {
                helpPageHeader = "Help page {page}/{pages}:";
            }
            if (helpEntry == null) {
                helpEntry = "{usage} - {description}";
            }
            if (helpDetailHeader == null) {
                helpDetailHeader = "Help for /hg {command}:";
            }
            if (helpDetailUsage == null) {
                helpDetailUsage = "Usage: {usage}";
            }
            if (helpDetailDescription == null) {
                helpDetailDescription = "Description: {description}";
            }
            if (helpDetailExample == null) {
                helpDetailExample = "Example: {example}";
            }
            if (helpDetailPermission == null) {
                helpDetailPermission = "Permission: {permission}";
            }
            if (helpUnknownTopic == null) {
                helpUnknownTopic = "Unknown help topic.";
            }
            if (help == null) {
                help = "Commands: /hg wand, /hg create <name>, /hg delete <name> --confirm, /hg info [name], /hg list, /hg select <name>, /hg redefine <name>, /hg expand <dir> <amount>, /hg contract <dir> <amount>, /hg shift <dir> <amount>, /hg priority <name> <value>, /hg flag <name> <flag> <value|clear> [text], /hg flags <name>, /hg member <subcommand>, /hg tp <name>, /hg setspawn <name>, /hg gui [name], /hg backup, /hg debug pos, /hg bypass, /hg save, /hg reload, /hg help [topic]";
            }
            if (regionInfo == null) {
                regionInfo = "Region {name}: owner={owner}, world={world}, min={min}, max={max}";
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