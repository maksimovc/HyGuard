package dev.thenexusgates.hyguard.ui;

import dev.thenexusgates.hyguard.core.region.RegionFlag;
import dev.thenexusgates.hyguard.core.region.RegionFlagValue;
import dev.thenexusgates.hyguard.core.region.RegionRole;

import java.util.Locale;

final class RegionUiText {

    private RegionUiText() {
    }

    static String displayRole(RegionRole role) {
        if (role == null) {
            return "Unknown";
        }
        return switch (role) {
            case OWNER -> "Owner";
            case CO_OWNER -> "Co-owner";
            case MANAGER -> "Manager";
            case MEMBER -> "Member";
            case TRUSTED -> "Trusted";
            case VISITOR -> "Visitor";
        };
    }

    static String roleDescription(RegionRole role) {
        if (role == null) {
            return "Role information is unavailable.";
        }
        return switch (role) {
            case OWNER -> "Full ownership. Cannot be removed or demoted from this screen.";
            case CO_OWNER -> "Can help manage region settings and trusted players.";
            case MANAGER -> "Can manage members, flags, and maintenance actions.";
            case MEMBER -> "Standard region member with normal access.";
            case TRUSTED -> "Limited trusted access with fewer editing powers.";
            case VISITOR -> "Basic access role with minimal permissions.";
        };
    }

    static String displayFlag(RegionFlag flag) {
        if (flag == null) {
            return "Unknown flag";
        }
        return switch (flag) {
            case BLOCK_BREAK -> "Block break";
            case BLOCK_PLACE -> "Block place";
            case BLOCK_INTERACT -> "Block interact";
            case BLOCK_TRAMPLE -> "Crop trampling";
            case BLOCK_DECAY -> "Block decay";
            case BLOCK_SPREAD -> "Block spread";
            case BLOCK_FADE -> "Block fade";
            case BLOCK_FORM -> "Block form";
            case PVP -> "PvP";
            case PLAYER_DAMAGE -> "Player damage";
            case PLAYER_FALL_DAMAGE -> "Fall damage";
            case PLAYER_HUNGER -> "Hunger";
            case PLAYER_ITEM_DROP -> "Item drop";
            case PLAYER_ITEM_PICKUP -> "Item pickup";
            case INTERACT_INVENTORY -> "Inventory interaction";
            case MOB_DAMAGE_PLAYERS -> "Mob damage players";
            case MOB_SPAWN -> "Mob spawn";
            case MOB_SPAWN_HOSTILE -> "Hostile mob spawn";
            case MOB_SPAWN_PASSIVE -> "Passive mob spawn";
            case MOB_DAMAGE_BLOCKS -> "Mob block damage";
            case MOB_GRIEF -> "Mob griefing";
            case ANIMAL_DAMAGE -> "Animal damage";
            case ENTITY_DAMAGE -> "Entity damage";
            case KNOCKBACK -> "Knockback";
            case FIRE_SPREAD -> "Fire spread";
            case TNT -> "TNT";
            case EXPLOSION -> "Explosions";
            case EXPLOSION_BLOCK_DAMAGE -> "Explosion block damage";
            case LIQUID_FLOW -> "Liquid flow";
            case LIGHTNING -> "Lightning";
            case ENTRY -> "Region entry";
            case EXIT -> "Region exit";
            case ENTRY_DENY_MESSAGE -> "Entry denied message";
            case EXIT_DENY_MESSAGE -> "Exit denied message";
            case GREET_MESSAGE -> "Greeting message";
            case FAREWELL_MESSAGE -> "Farewell message";
            case ENTRY_PLAYERS -> "Player entry";
            case INVINCIBLE -> "Invincibility";
            case GAME_MODE -> "Game mode lock";
            case WEATHER_LOCK -> "Weather lock";
            case TIME_LOCK -> "Time lock";
            case FLY -> "Flight";
            case SPAWN_LOCATION -> "Spawn location";
        };
    }

    static String flagDescription(RegionFlag flag) {
        if (flag == null) {
            return "No description available.";
        }
        return switch (flag) {
            case BLOCK_BREAK -> "Controls whether players may break blocks.";
            case BLOCK_PLACE -> "Controls whether players may place blocks.";
            case BLOCK_INTERACT -> "Controls interaction with doors, chests, levers, and similar blocks.";
            case BLOCK_TRAMPLE -> "Controls whether crops may be trampled.";
            case BLOCK_DECAY -> "Controls natural block decay such as leaves disappearing.";
            case BLOCK_SPREAD -> "Controls blocks that spread into neighboring space.";
            case BLOCK_FADE -> "Controls blocks that disappear or melt over time.";
            case BLOCK_FORM -> "Controls environmental block creation such as ice or snow.";
            case PVP -> "Controls whether players may damage each other.";
            case PLAYER_DAMAGE -> "Controls general player damage from other sources.";
            case PLAYER_FALL_DAMAGE -> "Controls whether fall damage applies.";
            case PLAYER_HUNGER -> "Controls hunger depletion inside the region.";
            case PLAYER_ITEM_DROP -> "Controls whether players can drop items.";
            case PLAYER_ITEM_PICKUP -> "Controls whether players can pick up items.";
            case INTERACT_INVENTORY -> "Controls opening or using inventories and storage blocks.";
            case MOB_DAMAGE_PLAYERS -> "Controls mob attacks against players.";
            case MOB_SPAWN -> "Controls all mob spawning.";
            case MOB_SPAWN_HOSTILE -> "Controls hostile mob spawning.";
            case MOB_SPAWN_PASSIVE -> "Controls passive mob spawning.";
            case MOB_DAMAGE_BLOCKS -> "Controls whether mobs can damage blocks.";
            case MOB_GRIEF -> "Controls mob grief behavior such as terrain damage.";
            case ANIMAL_DAMAGE -> "Controls damage against animals and passive creatures.";
            case ENTITY_DAMAGE -> "Controls damage against all non-player living entities, including NPCs, hostile mobs, and passive animals.";
            case KNOCKBACK -> "Controls whether damage may apply knockback inside the region.";
            case FIRE_SPREAD -> "Controls whether fire spreads between blocks.";
            case TNT -> "Controls TNT ignition or usage.";
            case EXPLOSION -> "Controls whether explosions are allowed.";
            case EXPLOSION_BLOCK_DAMAGE -> "Controls whether explosions damage blocks.";
            case LIQUID_FLOW -> "Controls water or lava movement.";
            case LIGHTNING -> "Controls lightning effects inside the region.";
            case ENTRY -> "Controls whether entities may enter the region.";
            case EXIT -> "Controls whether entities may leave the region.";
            case ENTRY_DENY_MESSAGE -> "Custom message shown when entry is denied.";
            case EXIT_DENY_MESSAGE -> "Custom message shown when exit is denied.";
            case GREET_MESSAGE -> "Message shown when a player enters the region.";
            case FAREWELL_MESSAGE -> "Message shown when a player leaves the region.";
            case ENTRY_PLAYERS -> "Controls whether players may enter the region.";
            case INVINCIBLE -> "Prevents players inside the region from taking damage.";
            case GAME_MODE -> "Forces players into a configured game mode while inside.";
            case WEATHER_LOCK -> "Overrides weather behavior within the region.";
            case TIME_LOCK -> "Overrides time behavior within the region.";
            case FLY -> "Controls flight permission.";
            case SPAWN_LOCATION -> "Overrides the location used when spawning in this region.";
        };
    }

    static String flagModeHint(RegionFlag flag, RegionFlagValue.Mode mode) {
        String effect = switch (flag) {
            case BLOCK_BREAK -> "players may break blocks";
            case BLOCK_PLACE -> "players may place blocks";
            case BLOCK_INTERACT -> "players may interact with blocks";
            case BLOCK_TRAMPLE -> "crop trampling is permitted";
            case BLOCK_DECAY -> "natural decay runs normally";
            case BLOCK_SPREAD -> "spreading blocks may expand";
            case BLOCK_FADE -> "fading blocks may disappear";
            case BLOCK_FORM -> "new environmental blocks may form";
            case PVP -> "players may fight each other";
            case PLAYER_DAMAGE -> "general player damage is allowed";
            case PLAYER_FALL_DAMAGE -> "fall damage applies";
            case PLAYER_HUNGER -> "hunger drains normally";
            case PLAYER_ITEM_DROP -> "players may drop items";
            case PLAYER_ITEM_PICKUP -> "players may pick up items";
            case INTERACT_INVENTORY -> "inventories can be opened and used";
            case MOB_DAMAGE_PLAYERS -> "mobs may damage players";
            case MOB_SPAWN -> "all mobs may spawn";
            case MOB_SPAWN_HOSTILE -> "hostile mobs may spawn";
            case MOB_SPAWN_PASSIVE -> "passive mobs may spawn";
            case MOB_DAMAGE_BLOCKS -> "mobs may damage blocks";
            case MOB_GRIEF -> "mobs may grief the world";
            case ANIMAL_DAMAGE -> "animals may be damaged";
            case ENTITY_DAMAGE -> "non-player living entities may be damaged";
            case KNOCKBACK -> "damage may apply knockback";
            case FIRE_SPREAD -> "fire may spread";
            case TNT -> "TNT use is permitted";
            case EXPLOSION -> "explosions are permitted";
            case EXPLOSION_BLOCK_DAMAGE -> "explosions may damage blocks";
            case LIQUID_FLOW -> "liquids may flow";
            case LIGHTNING -> "lightning effects are enabled";
            case ENTRY -> "entry is permitted";
            case EXIT -> "exit is permitted";
            case ENTRY_PLAYERS -> "players may enter";
            case INVINCIBLE -> "players become invincible";
            case WEATHER_LOCK -> "the region weather override applies";
            case TIME_LOCK -> "the region time override applies";
            case FLY -> "flight is permitted";
            case SPAWN_LOCATION -> "the region spawn override applies";
            default -> flagDescription(flag).toLowerCase(Locale.ROOT);
        };
        return switch (mode) {
            case ALLOW -> "Allowed: " + capitalize(effect) + ".";
            case DENY -> "Denied: " + capitalize(effect) + ".";
            case INHERIT -> "Inherited: Uses the parent or global rule for this setting.";
            case ALLOW_MEMBERS -> "Members only: Members and higher roles are allowed while visitors are restricted.";
            case ALLOW_TRUSTED -> "Trusted only: Trusted players and higher roles are allowed while visitors are restricted.";
        };
    }

    static String textFlagPlaceholder(RegionFlag flag) {
        if (flag == null) {
            return "";
        }
        return switch (flag) {
            case GREET_MESSAGE -> "Welcome to the region.";
            case FAREWELL_MESSAGE -> "See you next time.";
            case ENTRY_DENY_MESSAGE -> "You cannot enter this region.";
            case EXIT_DENY_MESSAGE -> "You cannot leave this region right now.";
            case GAME_MODE -> "Adventure or Creative";
            default -> "";
        };
    }

    static String textFlagHelper(RegionFlag flag) {
        if (flag == null) {
            return "";
        }
        return switch (flag) {
            case GREET_MESSAGE -> "Shown to players when they cross into the region.";
            case FAREWELL_MESSAGE -> "Shown to players when they leave the region.";
            case ENTRY_DENY_MESSAGE -> "Explains why entry is blocked.";
            case EXIT_DENY_MESSAGE -> "Explains why exit is blocked.";
            case GAME_MODE -> "Pick a valid enforced mode instead of typing a raw enum value.";
            default -> "";
        };
    }

    private static String capitalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}