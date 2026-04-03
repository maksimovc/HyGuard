# HyGuard Research Notes

## Implementation Status

Date: 2026-04-03

Implemented on verified public APIs:

- Region storage with cached lookup, async JSON saves, corrupt-file skip logging, and atomic-move fallback.
- Cuboid selections through block break and block use wand interception.
- Selection visualization through `ParticleUtil.spawnParticleEffect(...)` using verified public particle-system asset ids.
- Region browser, detail, member manager, and flag editor pages through `InteractiveCustomUIPage`.
- Disconnect cleanup through `PlayerDisconnectEvent`, including selection, bypass, move-state, and visual subscription cleanup.
- Runtime protection for block break, block place, block interact, player damage, PVP, fall damage, mob damage to players, item drop, item pickup, entry, and exit.
- Player state enforcement for `GAME_MODE` and `FLY`.
- Manual and scheduled backups with retention pruning.

## Verified Public API Surfaces Used

- `BreakBlockEvent`
- `PlaceBlockEvent`
- `UseBlockEvent.Pre`
- `ChangeGameModeEvent`
- damage-related ECS hooks used by `HyGuardEntityDamageSystem`
- item drop and pickup ECS hooks used by `HyGuardItemSystem`
- `PlayerDisconnectEvent`
- `InteractiveCustomUIPage<T>`
- `ParticleUtil.spawnParticleEffect(...)`
- `MovementManager` and `MovementSettings`
- `Player.setGameMode(...)`

## Final NO_API Summary

These flags remain blocked by missing confirmed stable public interception hooks in the current audited API surface:

- `MOB_SPAWN`
- `MOB_SPAWN_HOSTILE`
- `MOB_SPAWN_PASSIVE`
- `MOB_DAMAGE_BLOCKS`
- `MOB_GRIEF`
- `FIRE_SPREAD`
- `LIQUID_FLOW`
- `BLOCK_DECAY`
- `BLOCK_SPREAD`
- `BLOCK_FADE`
- `BLOCK_FORM`
- `TNT`
- `EXPLOSION`
- `EXPLOSION_BLOCK_DAMAGE`

Other flags are still stored and editable but remain unimplemented by this mod because no finished runtime behavior was added in this pass:

- `BLOCK_TRAMPLE`
- `PLAYER_HUNGER`
- `INTERACT_INVENTORY`
- `ANIMAL_DAMAGE`
- `LIGHTNING`
- `ENTRY_PLAYERS`
- `INVINCIBLE`
- `WEATHER_LOCK`
- `TIME_LOCK`
- `SPAWN_LOCATION`

## Particle Notes

- Public particle emission is available through `ParticleUtil`.
- Verified particle system ids in workspace assets include `Block_Hit_Fail`, `Block_Hit_Stone`, and `Block_Hit_Metal`.
- HyGuard now uses distinct particle systems for valid selections, conflict selections, and region borders.
- Exact public RGB tint control was not confirmed during this audit, so the visual layer uses separate asset ids instead of true color parameters.

## Explosion And Natural Update Notes

- Internal explosion helpers exist, but no confirmed public cancellable explosion event was found.
- Natural block updates appear to be driven by internal ticker/procedure systems rather than stable plugin-facing events.
- Because of that, explosion and natural-world protection flags remain intentionally marked `NO_API`.

## Spawn Notes

- Internal spawn pipeline classes exist in the server runtime.
- No confirmed general public mob-spawn interception event was found for natural world spawning.
- `LoadedNPCEvent` and interaction-driven spawning are not a substitute for a general cancellable spawn hook.

## Disconnect Lifecycle Notes

- `PlayerDisconnectEvent(PlayerRef)` remains the correct cleanup anchor because it still carries a usable `PlayerRef`.
- HyGuard now uses a dedicated cleanup handler instead of an inline anonymous listener.

## Workspace Conventions Confirmed

- Entrypoint remains a `JavaPlugin` under `dev.thenexusgates.hyguard`.
- Commands are registered in `setup()` through `getCommandRegistry().registerCommand(...)`.
- ECS systems are registered through `getEntityStoreRegistry().registerSystem(...)`.
- Custom `.ui` pages live under `src/main/resources/Common/UI/Custom/Pages/`.
- Windows Gradle invocation should use `call gradlew.bat ...` from `cmd.exe`.
- `com/hypixel/hytale/server/core/prefab/selection/SelectionProvider.class`
- `com/hypixel/hytale/server/core/prefab/selection/standard/BlockSelection.class`
- `com/hypixel/hytale/protocol/packets/buildertools/BuilderToolSelectionUpdate.class`
- `com/hypixel/hytale/protocol/packets/buildertools/BuilderToolSelectionTransform.class`
- `com/hypixel/hytale/protocol/packets/interface_/EditorSelection.class`
- `com/hypixel/hytale/builtin/buildertools/snapshot/SelectionSnapshot.class`
- `com/hypixel/hytale/builtin/buildertools/snapshot/BlockSelectionSnapshot.class`

Community docs also confirm that Creative Mode has a Selection Tool for transforming and extruding selections:

- `Hytale-Modding-Tutorials/content/docs/en/established-information/gameplay/creative.mdx`

#### Selection conclusion

The built-in system clearly exists, but this audit did not find a stable, documented plugin-facing API showing how server plugins are supposed to create, attach, or persist those editor selections for arbitrary gameplay items.

Decision:

- Do not depend on internal buildertool packets or prefab-selection internals for the first implementation.
- Reuse the concept of cuboid block selection, but implement HyGuard's own `SelectionSession` and visualizer.
- Treat built-in selection classes as research references only unless a stable plugin hook becomes available later.

### Position, vectors, and block access

- `com.hypixel.hytale.protocol.BlockPosition` is present and stores `x`, `y`, `z`.
- ECS block events commonly expose `com.hypixel.hytale.math.vector.Vector3i`.
- `BlockPlaceUtils.javap.txt` confirms `PlaceBlockEvent` contains target block and rotation, and is cancellable before final placement.
- `WorldChunk` implements `BlockAccessor`, confirming server-side chunk/world abstractions are available.
- `BoxBlockIterator` exists and may be useful for iterating cuboids efficiently.

Observed in:

- `Hytale-API/Server/BlockPosition.javap.txt`
- `Hytale-API/Server/BlockPlaceUtils.javap.txt`
- `Hytale-API/Server/WorldChunk.javap.txt`
- `Hytale-API/Server/npc_probe/BoxBlockIterator.javap.txt`

### Events and protection hooks

Relevant event classes found in `hytale-server-classes.txt` and tutorials:

- `PlayerMouseButtonEvent`
- `PlayerInteractEvent`
- `BreakBlockEvent`
- `PlaceBlockEvent`
- `UseBlockEvent.Pre`
- `UseBlockEvent.Post`
- `DropItemEvent`
- `Damage`
- `PlayerDisconnectEvent`
- `AddPlayerToWorldEvent`

Existing mod patterns show:

- Block protection is practical through `EntityEventSystem<EntityStore, BreakBlockEvent>`.
- Placement protection is practical through `EntityEventSystem<EntityStore, PlaceBlockEvent>`.
- Block use and container gating is practical through `EntityEventSystem<EntityStore, UseBlockEvent.Pre>`.
- Damage rules are practical through `DamageEventSystem`.

Observed in:

- `MyMods/NexusVendingMachines/src/main/java/dev/thenexusgates/nexusvendingmachines/NvmBreakBlockSystem.java`
- `MyMods/NexusVendingMachines/src/main/java/dev/thenexusgates/nexusvendingmachines/NvmPlaceBlockSystem.java`
- `MyMods/NexusVendingMachines/src/main/java/dev/thenexusgates/nexusvendingmachines/NvmUseBlockSystem.java`
- `MyMods/SimpleClaimsSpawnProtection/src/main/java/dev/thenexusgates/scsp/ScspClaimDamageProtectionSystem.java`
- `Hytale-Modding-Tutorials/content/docs/en/server/events.mdx`
- `Hytale-Modding-Tutorials/content/docs/en/guides/plugin/creating-events.mdx`

### Commands

Relevant command classes found in `hytale-server-classes.txt`:

- `AbstractCommand`
- `CommandContext`
- `AbstractPlayerCommand`
- `AbstractCommandCollection`
- `CommandRegistry`

Decision:

- Use `AbstractPlayerCommand` for primary `/hg` command handling so region operations run with direct world/player access.
- Keep subcommand dispatch in Java code rather than spreading one file per command during the first working iteration.

### Permissions

`PermissionsModule` exists and is already used by local mods.

Observed in:

- `Hytale-API/Server/hytale-server-classes.txt`
- `MyMods/NexusTP/src/main/java/dev/thenexusgates/nexustp/command/NexusTpCommand.java`

Audit result:

- A permission-node system is available.
- This audit did not find a built-in WorldGuard-style region role model.
- HyGuard therefore needs its own region member/role model, while bypass/admin checks can integrate with permission nodes.

### UI and screens

Relevant UI classes found in `hytale-server-classes.txt`:

- `CustomUIPage`
- `BasicCustomUIPage`
- `InteractiveCustomUIPage`
- `PageManager`

Docs confirm that interactive server-driven UI is built with `.ui` assets plus `UICommandBuilder` updates.

Observed in:

- `Hytale-Modding-Tutorials/content/docs/en/guides/plugin/ui.mdx`
- `Hytale-Modding-Tutorials/content/docs/en/official-documentation/custom-ui/index.mdx`

Decision:

- GUI is feasible, but it should come after the protection core and repository are stable.
- HyGuard should use page-based UI, not HUD-based UI, for region management.

### Items and wand implementation options

Community docs confirm custom items are defined through assets in `Server/Item/Items/...` and can bind custom interactions.

Observed in:

- `Hytale-Modding-Tutorials/content/docs/en/guides/plugin/item-interaction.mdx`
- `MyMods/NexusVendingMachines/src/main/resources/Server/Item/Items/NexusVendingMachines/Player_Vending_Machine.json`

Additional usable pattern:

- `PlayerInteractEvent` listeners can inspect held-item interactions at global priority.

Observed in:

- `MyMods/SimpleClaimsItemFramesProtection/src/main/java/dev/thenexusgates/scip/ItemFramesProtectionListener.java`

Decision:

- Implement the wand as a custom asset-backed item when assets are ready.
- For the first working pass, the wand logic can also identify a configured item id from held item data during interaction events.

### Data storage and serialization

The tutorials describe component-based persistence, but the user explicitly requested file-backed storage instead of player NBT/component persistence for guard state.

Decision:

- Region data and player selection sessions should be stored in plugin-owned JSON files under `data/hyguard/...`.
- Do not store region ownership or selection state in player NBT/components.
- Use atomic file replacement and background saves.

## Practical Design Decisions For HyGuard

### Package and project identity

Adapted to workspace conventions:

- Base package: `dev.thenexusgates.hyguard`
- Plugin class: `dev.thenexusgates.hyguard.HyGuardPlugin`
- Mod folder: `MyMods/HyGuard`

### Minimum safe technical approach

1. Create a standalone plugin project under `MyMods/HyGuard` using the template build style.
2. Implement data model and JSON repository first.
3. Implement chunk-indexed in-memory cache as the read path for all protection queries.
4. Implement selection sessions independent from internal buildertool APIs.
5. Register ECS systems for `BreakBlockEvent`, `PlaceBlockEvent`, and `UseBlockEvent.Pre` as the first protection surface.
6. Add GUI only after core region persistence and permission checks are stable.

### Selection strategy

Recommended first implementation:

- Session per player in memory, keyed by player UUID.
- Point 1 and Point 2 stored as simple integer block positions.
- Bind wand interactions through player interaction or mouse/button events.
- Visualize with a custom border renderer later.

### Protection strategy

Recommended first implementation:

- Main lookup input: world name plus `Vector3i` block position.
- Resolve candidate regions through chunk index.
- Evaluate player role, bypass node, and region flags.
- Cancel ECS block/place/use events on deny.

### Storage strategy

Recommended first implementation:

- `ConcurrentHashMap`-backed cache.
- Single async save executor to serialize disk writes.
- Temp file write + atomic move.
- Validation on load; skip corrupted region files instead of failing plugin startup.

## Missing Or Unverified Areas

The following capabilities are not yet proven by this audit and should be treated cautiously:

- Direct public API for rendering the same creative selection gizmo used by internal builder tools
- A public region/claim API already built into Hytale
- A documented public particle line or wireframe API specifically for custom 3D region outlines
- A documented built-in role system for region membership beyond generic permission nodes

## Final Phase 1 Conclusion

HyGuard is feasible in this workspace without relying on unstable internal buildertool machinery.

The most reliable implementation path is:

- workspace-native `JavaPlugin`
- JSON-backed region repository

## Phase 3 API Research

This section captures the verified API surface needed for the final implementation phase. Only methods confirmed through `javap`, local working mods, or tutorial docs are listed here.

### A1. Player lookup by username

Requested class `com.hypixel.hytale.server.core.Server` does not exist in this runtime. The relevant runtime classes are:

- `com.hypixel.hytale.server.core.universe.Universe`
- `com.hypixel.hytale.server.core.NameMatching`

Verified methods from `javap -p com.hypixel.hytale.server.core.universe.Universe`:

- `public static com.hypixel.hytale.server.core.universe.Universe get()`
- `public java.util.List<com.hypixel.hytale.server.core.universe.PlayerRef> getPlayers()`
- `public com.hypixel.hytale.server.core.universe.PlayerRef getPlayer(java.util.UUID)`
- `public com.hypixel.hytale.server.core.universe.PlayerRef getPlayer(java.lang.String, com.hypixel.hytale.server.core.NameMatching)`
- `public com.hypixel.hytale.server.core.universe.PlayerRef getPlayerByUsername(java.lang.String, com.hypixel.hytale.server.core.NameMatching)`
- `public int getPlayerCount()`
- `public com.hypixel.hytale.server.core.universe.playerdata.PlayerStorage getPlayerStorage()`

Verified enum values from `com.hypixel.hytale.server.core.NameMatching`:

- `EXACT`
- `EXACT_IGNORE_CASE`
- `STARTS_WITH`
- `STARTS_WITH_IGNORE_CASE`
- `DEFAULT`

Verified `PlayerRef` methods relevant to lookup and follow-up handling:

- `public java.util.UUID getUuid()`
- `public java.lang.String getUsername()`
- `public com.hypixel.hytale.math.vector.Transform getTransform()`
- `public com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> getReference()`

Conclusion:

- Online player lookup should use `Universe.get().getPlayerByUsername(name, NameMatching.EXACT_IGNORE_CASE)` first.
- Partial-match fallback can use `Universe.get().getPlayerByUsername(name, NameMatching.STARTS_WITH_IGNORE_CASE)` if desired.
- There was no direct local mod example of this lookup path, but the runtime API is present and explicit.

### A2. Teleport and position API

Verified class:

- `com.hypixel.hytale.server.core.modules.entity.component.TransformComponent`

Verified methods from `javap`:

- `public com.hypixel.hytale.math.vector.Vector3d getPosition()`
- `public void setPosition(com.hypixel.hytale.math.vector.Vector3d)`
- `public void teleportPosition(com.hypixel.hytale.math.vector.Vector3d)`
- `public com.hypixel.hytale.math.vector.Vector3f getRotation()`
- `public void setRotation(com.hypixel.hytale.math.vector.Vector3f)`
- `public void teleportRotation(com.hypixel.hytale.math.vector.Vector3f)`
- `public com.hypixel.hytale.math.vector.Transform getTransform()`

Verified access pattern from local mods:

- `Store<EntityStore>.getComponent(entityRef, TransformComponent.getComponentType())`

Observed in:

- `MyMods/SimpleClaimsSpawnProtection/src/main/java/dev/thenexusgates/scsp/ScspCommand.java`
- `MyMods/SimpleClaimsMoreFlags/src/main/java/dev/thenexusgates/scmf/ScmfCommand.java`

Conclusion:

- Safe teleport implementation path is to resolve `Ref<EntityStore>` from `PlayerRef.getReference()`, fetch `TransformComponent`, then call `teleportPosition(new Vector3d(...))`.
- `setPosition(...)` also exists, but `teleportPosition(...)` is the better semantic match for `/hg tp`.
- `NexusTP` is not a same-world teleport example; it uses `PlayerRef.referToServer(host, port)` for inter-server transfer.

### A3. Particle / visual API

No public server manager class named either of the following was found in the current runtime:

- `com.hypixel.hytale.server.core.world.particle.ParticleManager`
- `com.hypixel.hytale.server.core.world.effect.EffectManager`

Relevant classes that do exist in the runtime:

- `com/hypixel/hytale/server/core/asset/type/model/config/ModelParticle.class`
- `com/hypixel/hytale/server/core/asset/type/particle/config/WorldParticle.class`
- `com/hypixel/hytale/protocol/packets/entities/SpawnModelParticles.class`

Verified packet constructor from `javap`:

- `public com.hypixel.hytale.protocol.packets.entities.SpawnModelParticles(int, com.hypixel.hytale.protocol.ModelParticle[])`

Tutorial docs confirm particle concepts only at the interaction-asset layer:

- `InteractionEffects` includes `ModelParticles` and `WorldParticles`
- `DamageEffects` also includes `ModelParticles` and `WorldParticles`

Observed in:

- `Hytale-Modding-Tutorials/content/docs/en/server/interaction-reference.mdx`

Conclusion:

- The runtime exposes particle-related asset and packet classes, but this audit did not find a stable public plugin API for rendering arbitrary world-space particles to one player.
- Selection and border visualizers should therefore be implemented as stubs or adapter classes first, with comments noting that a safe server-side emission hook was not verified in API `0.2.14`.

### A4. Scheduler / periodic tasks

Verified runtime scheduler:

- `com.hypixel.hytale.server.core.HytaleServer.SCHEDULED_EXECUTOR`

Verified from `javap -p com.hypixel.hytale.server.core.HytaleServer`:

- `public static final java.util.concurrent.ScheduledExecutorService SCHEDULED_EXECUTOR;`

Working local examples:

- `HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(...)`
- `HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(...)`
- `Executors.newSingleThreadScheduledExecutor(...)` stored per-plugin and exposed through `getScheduler()`

Observed in:

- `MyMods/ServerAdminPanel/src/main/java/dev/thenexusgates/serveradminpanel/ServerAdminSnapshotService.java`
- `MyMods/NexusStop/src/main/java/dev/thenexusgates/nexusstop/NexusStopPlugin.java`
- `MyMods/BlockMorph/src/main/java/dev/thenexusgates/blockmorph/BlockMorphGameManager.java`
- `MyMods/NexusTP/src/main/java/dev/thenexusgates/nexustp/NexusTpPlugin.java`

Conclusion:

- `HytaleServer.SCHEDULED_EXECUTOR` is a proven shared scheduler for periodic tasks.
- A plugin-owned `ScheduledExecutorService` is also an accepted workspace pattern when task isolation is useful.
- No `TickEvent` or `ServerTickEvent` usage was found in local mods; scheduled executors are the verified path.

### A5. Player disconnect event

Verified event class:

- `com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent`

Verified usage pattern from local mods:

- `getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, event -> { ... })`
- `event.getPlayerRef()` returns the disconnecting player reference

Observed in:

- `MyMods/HyGuard/src/main/java/dev/thenexusgates/hyguard/HyGuardPlugin.java`
- `MyMods/PlayerAvatarMarker/src/main/java/dev/thenexusgates/playeravatarmarker/PlayerAvatarMarkerPlugin.java`
- `MyMods/BlockMorph/src/main/java/dev/thenexusgates/blockmorph/BlockMorphPlugin.java`

Conclusion:

- `PlayerDisconnectEvent` is the correct cleanup hook for `SelectionService`, movement-region caches, and visual task cleanup.

### A6. GUI / screen API

Verified base classes and helpers:

- `com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage<T>`
- `com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime`
- `com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType`
- `com.hypixel.hytale.server.core.ui.builder.UICommandBuilder`
- `com.hypixel.hytale.server.core.ui.builder.UIEventBuilder`
- `com.hypixel.hytale.server.core.ui.builder.EventData`
- `com.hypixel.hytale.codec.builder.BuilderCodec`
- `com.hypixel.hytale.codec.KeyedCodec`

Verified constructor pattern:

- `super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC)`

Verified open pattern:

- `player.getPageManager().openCustomPage(entityRef, store, page)`

Verified page lifecycle methods:

- `build(Ref<EntityStore>, UICommandBuilder, UIEventBuilder, Store<EntityStore>)`
- `handleDataEvent(Ref<EntityStore>, Store<EntityStore>, PageData)`
- `close()`

Observed in:

- `MyMods/NexusTP/src/main/java/dev/thenexusgates/nexustp/ui/PortalMenuPage.java`
- `MyMods/NexusTP/src/main/java/dev/thenexusgates/nexustp/command/NexusTpCommand.java`
- `MyMods/ServerAdminPanel/src/main/java/dev/thenexusgates/serveradminpanel/ui/ServerAdminDashboardPage.java`
- `MyMods/ServerAdminPanel/src/main/java/dev/thenexusgates/serveradminpanel/ServerAdminPanelPlugin.java`
- `Hytale-Modding-Tutorials/content/docs/en/guides/plugin/ui.mdx`

Conclusion:

- GUI pages for HyGuard should extend `InteractiveCustomUIPage<T>` and be opened through `Player.getPageManager().openCustomPage(...)`.
- Button/input callbacks should use `UIEventBuilder.addEventBinding(...)` with a codec-backed page data object.
- chunk-indexed cache
- ECS protection systems
- custom selection sessions
- page-based GUI

This is the implementation path that should be used for Phase 2 onward.