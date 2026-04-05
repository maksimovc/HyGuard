# HyGuard Release Guide

This document captures the release process and deployment expectations for HyGuard `1.0.9`.

## Release Identity

| Field | Value |
| --- | --- |
| Mod name | `HyGuard` |
| Version | `1.0.9` |
| Output jar | `build/libs/HyGuard-1.0.9.jar` |
| Supported server baseline | `2026.03.26-89796e57b` |
| Java baseline | `25` |
| License | `AGPL-3.0` |

## What 1.0.9 Includes

### Claim Types

- Player-owned cuboid regions created with `/hg create <name>`
- Server-owned cuboid regions created by admins with `/hg create <name> --server`
- Server-owned, world-scoped fallback regions created by admins with `/hg create __global__`

### Protection Coverage

- Block break, place, and interact protection
- PvP and player damage protection
- Fall damage protection
- Mob damage to players protection
- Item drop and pickup protection
- Entry and exit rules
- Entry role filtering through `ENTRY_PLAYERS`
- Invincibility through `INVINCIBLE`
- Region-enforced `GAME_MODE` and `FLY`

### Management Surface

- Wand selection workflow
- `/hg select`, `/hg redefine`, `/hg expand`, `/hg contract`, and `/hg shift`
- Region browser, detail, member manager, and flag editor UI pages
- Per-region spawn points and teleport
- Async saves, offline player directory cache, manual backups, and scheduled backups

### Runtime Packaging

- Runtime asset-pack bootstrap into the world save
- Separate data root and asset-pack root
- Wand item localization in `items.lang`
- Localization bundles for `en-US` and `uk-UA`

## Verified Build Output

Build command on Windows:

```bat
call gradlew.bat clean build
```

Expected output jars:

```text
build/libs/HyGuard-1.0.9.jar
build/libs/HyGuard-1.0.9-sources.jar
```

The release artifact is `build/libs/HyGuard-1.0.9.jar`.

## Pre-Release Checklist

1. Run `call gradlew.bat clean build`.
2. Confirm `build/libs/HyGuard-1.0.9.jar` exists.
3. Confirm `src/main/resources/manifest.json` and Gradle jar naming both still report `1.0.9`.
4. Review `README.md` and this file for drift against the current code.
5. Verify that `Assets.zip` is available so the build is not using an incomplete environment.

## Deployment Checklist

### Clean Deployment To A World Save

1. Stop the target Hytale server/world.
2. Open the target world's `mods/` directory.
3. Remove or archive any stale non-versioned HyGuard jar such as `HyGuard.jar` to avoid ambiguity.
4. Copy `build/libs/HyGuard-1.0.9.jar` into the `mods/` directory.
5. Start the world/server.
6. Let HyGuard initialize once so the runtime asset pack and data directories are prepared.

### Expected Runtime Layout After First Start

Inside the world save `mods/` folder you should see:

- `HyGuard-1.0.9.jar`
- `thenexusgates_HyGuard/`
- `thenexusgates_HyGuardData/`

Expected purpose of those folders:

- `thenexusgates_HyGuard/` contains the runtime asset pack (`Common/`, `Server/`, `manifest.json`)
- `thenexusgates_HyGuardData/` contains persistent mod data (`config.json`, `players/`, `regions/`, `backups/`)

## Release Validation

### Required Smoke Tests

1. Run `/hg help`, `/hg wand`, and `/hg gui`.
2. Create a player-owned cuboid with `/hg create testclaim`.
3. As admin, create a server-owned cuboid with `/hg create spawn --server`.
4. As admin, create a world fallback region with `/hg create __global__`.
5. If the server has multiple worlds, repeat `/hg create __global__` in another world and confirm each world keeps its own fallback region.
6. Change at least one flag on a cuboid region and on a global region.
7. Add and remove at least one member.
8. Verify teleport and region spawn behavior.
9. Reload once with `/hg reload`.
10. Flush pending writes once with `/hg save`.

### UI And Asset Checks

1. Confirm the wand item shows a localized name instead of the raw `items.hyguard_wand.name` key.
2. Confirm the wand icon is visible in inventory.
3. Confirm the wand has the intended in-hand item-style appearance.
4. Confirm HyGuard chat/help text is localized.

### Protection Checks

1. Verify block break/place/interact denial inside a protected region.
2. Verify entry denial messaging if `ENTRY` or `ENTRY_PLAYERS` requires it.
3. Verify `INVINCIBLE` cancels damage where configured.
4. Verify `GAME_MODE` and `FLY` state application in a configured region.

## Log Checks

After first start, inspect server logs for these classes of messages:

- HyGuard enabled message with region count and runtime roots
- Runtime asset-pack registration line
- No repeated JSON parse failures
- No missing item/localization asset warnings for the wand

Capture first-run logs for the release archive if you are preparing a public release package.

## Known Release Notes For 1.0.9

- `__global__` is per world, not a single cross-world region.
- Server-owned cuboid claims use `--server` and display `Server` as owner.
- Server-owned regions are not counted against a player's personal region limit.
- Global regions are server-owned and act as a fallback layer for the current world.

## GitHub Release Procedure

1. Push the final source state to the `main` branch.
2. Create tag `v1.0.9`.
3. Draft a GitHub release titled `HyGuard v1.0.9`.
4. Attach `build/libs/HyGuard-1.0.9.jar`.
5. Optionally attach `build/libs/HyGuard-1.0.9-sources.jar`.
6. Paste or adapt the changelog below.

Suggested changelog:

```markdown
## HyGuard v1.0.9

Small cleanup and fix release for HyGuard.

### Changed
- Fixed region flags and runtime checks.
- Cleaned up private hierarchy and fallback behavior.
- Improved selection updates.
- Optimized pickup, fire, and liquid handling.

### Requirements
- Hytale Server `2026.03.26-89796e57b` or newer
- Java 25
```

## CurseForge Upload

If you publish on CurseForge:

1. Set release type to `Release`.
2. Upload `build/libs/HyGuard-1.0.9.jar`.
3. Use version `1.0.9`.
4. Set license to `AGPL-3.0`.
5. Link source to `https://github.com/maksimovc/HyGuard`.

## Post-Release Follow-Up

1. Keep the first public server logs for regression comparison.
2. Keep one tested world save with working `thenexusgates_HyGuard` and `thenexusgates_HyGuardData` as a rollback reference.
3. If a user reports claim issues, first confirm whether the region is player-owned, server-owned, or world-global before debugging permissions or overlap behavior.