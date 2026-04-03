# HyGuard

HyGuard is a Hytale server protection mod centered on cuboid regions, a world-wide `__global__` fallback region, JSON-backed persistence, offline member lookup, scheduled backups, and in-game management through commands and `InteractiveCustomUIPage` pages.

## What Is Implemented

- Cuboid selection with a wand, `/hg select`, `/hg redefine`, `/hg expand`, `/hg contract`, and `/hg shift`
- Region creation, deletion with confirmation, priority, teleport, and per-region spawn points
- World-scoped `__global__` regions for fallback rules in a world
- Member and role management with offline player lookup cache
- GUI pages for region browser, region detail, member management, and flag editing
- JSON persistence with temp-file writes and atomic move fallback logging
- Manual and scheduled backups with retention pruning
- Runtime protection for block break, block place, block interact, player damage, fall damage, PVP, mob damage to players, item drop, item pickup, entry, and exit
- Enter/exit messages and movement polling through `PlayerMoveSystem`
- Region-applied player state for `GAME_MODE` and `FLY`
- Public particle-based selection visuals using `ParticleUtil` with distinct valid/conflict/region outline particle systems

## Commands

| Command | Purpose |
| --- | --- |
| `/hg wand` | Give the region wand item. |
| `/hg create <name>` | Create a cuboid region from the current selection. |
| `/hg create __global__` | Create the world-wide fallback region. Admin-only. |
| `/hg delete <name> --confirm` | Delete a region. |
| `/hg info [name]` | Show region info for a named region or the region under the player. |
| `/hg list` | List regions in the current world. |
| `/hg select <name>` | Load a region's bounds into the current selection. |
| `/hg redefine <name>` | Replace a region's bounds with the current selection. |
| `/hg expand <dir> <amount>` | Expand the current selection. |
| `/hg contract <dir> <amount>` | Contract the current selection. |
| `/hg shift <dir> <amount>` | Move the current selection without resizing it. |
| `/hg priority <name> <0-100>` | Set a region priority. |
| `/hg flag <name> <flag> <value\|clear> [text]` | Set or clear an explicit region flag value. |
| `/hg flags <name> [page]` | List all region flags and effective values. |
| `/hg member add <region> <player> [role] [--confirm]` | Add a member to a region. |
| `/hg member remove <region> <player>` | Remove a member from a region. |
| `/hg member role <region> <player> <role>` | Change an existing member role. |
| `/hg member list <region> [page]` | List region members. |
| `/hg tp <name>` | Teleport to a region's spawn or center. |
| `/hg setspawn <name>` | Save the caller's current position as the region spawn. |
| `/hg gui [name]` | Open the region browser or a specific region detail page. |
| `/hg backup` | Run a manual backup. Admin-only. |
| `/hg debug pos` | Print regions covering the caller's current block. Admin-only. |
| `/hg bypass` | Toggle protection bypass. |
| `/hg save` | Flush pending async region saves. Admin-only. |
| `/hg reload` | Reload config and regions from disk. Admin-only. |
| `/hg help [topic\|page]` | Show paged help or command details. |

## GUI Pages

- Region Browser: lists regions in the current world and opens per-region details.
- Region Detail: shows owner, bounds, hierarchy, members, priority, spawn, teleport/select/set-spawn actions, and a delete-confirm flow.
- Member Manager: supports add member, explicit change-role, and remove member actions.
- Flag Editor: shows every `RegionFlag` value and allows explicit apply/clear edits.

## Region Roles

| Role | Meaning |
| --- | --- |
| `OWNER` | Region owner. Not assignable through member commands. |
| `CO_OWNER` | Full co-management of members and region settings. |
| `MANAGER` | Can manage most region settings but not ownership. |
| `MEMBER` | Regular allowed member role. |
| `TRUSTED` | Trusted access tier used by specific flags such as `ALLOW_TRUSTED`. |
| `VISITOR` | Explicitly tracked non-member role for restricted access cases. |

## RegionFlag Table

| Flag | Status | Notes |
| --- | --- | --- |
| `BLOCK_BREAK` | Implemented | Enforced in `HyGuardBreakBlockSystem`. |
| `BLOCK_PLACE` | Implemented | Enforced in `HyGuardPlaceBlockSystem`. |
| `BLOCK_INTERACT` | Implemented | Enforced in `HyGuardUseBlockSystem`. |
| `BLOCK_TRAMPLE` | Stored only | No dedicated runtime hook yet. |
| `BLOCK_DECAY` | `NO_API` | No confirmed public natural block-decay event hook. |
| `BLOCK_SPREAD` | `NO_API` | No confirmed public natural block-spread event hook. |
| `BLOCK_FADE` | `NO_API` | No confirmed public block-fade event hook. |
| `BLOCK_FORM` | `NO_API` | No confirmed public block-form event hook. |
| `PVP` | Implemented | Enforced through player damage handling. |
| `PLAYER_DAMAGE` | Implemented | Enforced through player damage handling. |
| `PLAYER_FALL_DAMAGE` | Implemented | Enforced through damage cause checks. |
| `PLAYER_HUNGER` | Stored only | No implemented public hunger handler in this mod. |
| `PLAYER_ITEM_DROP` | Implemented | Enforced through item drop ECS event. |
| `PLAYER_ITEM_PICKUP` | Implemented | Enforced through item pickup ECS event. |
| `INTERACT_INVENTORY` | Stored only | No dedicated runtime inventory-interaction hook yet. |
| `MOB_DAMAGE_PLAYERS` | Implemented | Enforced through damage source checks. |
| `MOB_SPAWN` | `NO_API` | No confirmed general public mob-spawn interception event. |
| `MOB_SPAWN_HOSTILE` | `NO_API` | Same limitation as `MOB_SPAWN`. |
| `MOB_SPAWN_PASSIVE` | `NO_API` | Same limitation as `MOB_SPAWN`. |
| `MOB_DAMAGE_BLOCKS` | `NO_API` | No public mob grief/block damage interception hook confirmed. |
| `MOB_GRIEF` | `NO_API` | No public mob grief interception hook confirmed. |
| `ANIMAL_DAMAGE` | Stored only | No dedicated runtime animal-damage branch yet. |
| `FIRE_SPREAD` | `NO_API` | No confirmed public fire spread event hook. |
| `TNT` | `NO_API` | No confirmed public TNT/explosion interception event hook. |
| `EXPLOSION` | `NO_API` | No confirmed public explosion interception event hook. |
| `EXPLOSION_BLOCK_DAMAGE` | `NO_API` | No confirmed public explosion block-damage event hook. |
| `LIQUID_FLOW` | `NO_API` | No confirmed public fluid-flow event hook. |
| `LIGHTNING` | Stored only | No dedicated runtime lightning interception handler yet. |
| `ENTRY` | Implemented | Enforced by `PlayerMoveSystem`. |
| `EXIT` | Implemented | Enforced by `PlayerMoveSystem`. |
| `ENTRY_DENY_MESSAGE` | Implemented | Used by entry deny renderer. |
| `EXIT_DENY_MESSAGE` | Implemented | Used by exit deny renderer. |
| `GREET_MESSAGE` | Implemented | Used on successful region entry. |
| `FAREWELL_MESSAGE` | Implemented | Used on successful region exit. |
| `ENTRY_PLAYERS` | Stored only | No dedicated player-filter rule implemented yet. |
| `INVINCIBLE` | Stored only | No implemented player-invulnerability state hook yet. |
| `GAME_MODE` | Implemented | Applied through player game mode state updates. |
| `WEATHER_LOCK` | Stored only | No world weather state handler implemented yet. |
| `TIME_LOCK` | Stored only | No world time state handler implemented yet. |
| `FLY` | Implemented | Applied through `MovementManager` settings. |
| `SPAWN_LOCATION` | Stored only | Region spawn uses `region.spawnPoint`; no dedicated flag runtime yet. |

## Permissions

- `hyguard.admin`: admin-only commands such as global region creation, backups, reload, and save.
- `hyguard.bypass`: bypass toggle access.

## Storage And Backups

- Regions are saved under the plugin data directory in per-world JSON files.
- Saves run asynchronously on a single-thread executor.
- Writes use a `.tmp` file and attempt `ATOMIC_MOVE`; if the filesystem does not support it, the repository logs a warning and falls back to a replace move.
- Corrupt JSON files are skipped with warning logs instead of aborting startup.
- Backups copy the `regions/` directory into timestamped folders under `backups/`.
- Automatic backups can be disabled with `general.autoBackupIntervalMinutes = 0`.
- Retention is controlled by `general.maxBackups`.

## Build

ScaffoldIt expects the Hytale assets archive at `hytale.home_path/latest/Assets.zip`.

In this workspace the required path is:

- `d:/MyHytaleMod/Hytale-API/latest/Assets.zip`

This must exist before running Gradle tasks. On Windows, use `call gradlew.bat ...` from `cmd.exe`.

Typical commands:

```bat
call gradlew.bat compileJava
call gradlew.bat build
```

## Known Limits

- Public Hytale API coverage still does not expose stable hooks for natural block updates, explosions, or general mob spawning, so those flags remain `NO_API`.
- Exact per-particle tint control was not confirmed on the public particle emission API. HyGuard uses distinct public particle system assets for valid selection, conflict selection, and region outlines instead of true RGB tinting.
- Global regions intentionally do not support two-point `select` or `redefine` bounds editing.