# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

InvSwitcher is a BentoBox addon for Minecraft (Spigot/Bukkit) that gives players separate inventories, ender chests, health, food, experience, advancements, game modes, and statistics per game-world. Nether and End dimensions are automatically grouped with their overworld.

## Build Commands

- **Build**: `mvn clean package`
- **Run tests**: `mvn test`
- **Run a single test class**: `mvn test -Dtest=StoreTest`
- **Run a single test method**: `mvn test -Dtest=StoreTest#testMethodName`

Requires Java 21. The build produces a shaded JAR in `target/`.

## CI

Jenkins at ci.codemc.io builds the `develop` and `master` branches. The CI agent runs JDK 25 (OpenJDK_25) targeting release 21. The `maven-compiler-plugin` has `forceJavacCompilerUse=true` set in the pom.xml to work around a JDK compiler hashing bug (`"this.hashes" is null`) that surfaces when compiling on JDK 25+. Do not remove this flag.

## Architecture

This is a BentoBox Addon (extends `Addon`, not a standalone Bukkit plugin). Key flow:

- **InvSwitcher** - Addon entry point. Loads config, resolves configured world names to `World` objects (including nether/end variants), creates the `Store`, and registers `PlayerListener`.
- **Store** - Core logic. Maintains an in-memory `Map<UUID, InventoryStorage>` cache backed by BentoBox's `Database`. On world change: stores current player state to the old world's slot, clears the player, then loads the new world's slot. Handles XP math manually (Bukkit's `getTotalExperience()` is unreliable). Tracks per-player `currentKey` to map saves/loads to the correct storage slot.
- **InventoryStorage** - `DataObject` (BentoBox DB entity) keyed by player UUID. All per-world data is stored as `Map<String, ...>` where the key is either the overworld name (e.g., `"oneblock_world"`) or an island-specific key (e.g., `"oneblock_world/islandId"`). Persisted via BentoBox's database abstraction (JSON, MySQL, etc. — **not** YAML, which is explicitly unsupported).
- **PlayerListener** - Listens to `PlayerChangedWorldEvent`, `PlayerJoinEvent`, `PlayerQuitEvent`, `IslandEnterEvent`, and `PlayerRespawnEvent` to trigger store/load operations.
- **Settings** - `ConfigObject` loaded from `config.yml`. Each switchable aspect (inventory, health, food, etc.) has a world-level boolean toggle and a per-island sub-toggle.

## Key Design Details

- World name normalization: nether (`_nether`) and end (`_the_end`) suffixes are stripped to map all three dimensions to the same overworld key.
- **Per-island inventory switching**: When `islandsActive` is enabled and a player owns multiple concurrent islands, data is keyed as `"worldName/islandId"` instead of just `"worldName"`. Each data type (inventory, health, etc.) has its own per-island sub-toggle.
- **Storage key transitions**: When a player goes from 1 island to multiple, their `currentKey` must be upgraded from a world-only key to an island-specific key. This is handled proactively in `PlayerListener.onIslandEnter` via `Store.upgradeWorldKeyToIsland()` before any save/load occurs.
- **Backward compatibility migration**: `Store.getInventory()` migrates world-only data to island-specific keys on first load if no island-specific data exists yet.
- Statistics saving runs asynchronously via `Bukkit.getScheduler()` except during shutdown (where scheduling is unavailable).
- Tests use JUnit 5 + MockBukkit + Mockito. The surefire plugin requires extensive `--add-opens` flags (already configured in pom.xml).

## Dependency Source Lookup

When you need to inspect source code for a dependency (e.g., BentoBox, addons):

1. **Check local Maven repo first**: `~/.m2/repository/` — sources jars are named `*-sources.jar`
2. **Check the workspace**: Look for sibling directories or Git submodules that may contain the dependency as a local project (e.g., `../bentoBox`, `../addon-*`)
3. **Check Maven local cache for already-extracted sources** before downloading anything
4. Only download a jar or fetch from the internet if the above steps yield nothing useful

Prefer reading `.java` source files directly from a local Git clone over decompiling or extracting a jar.

In general, the latest version of BentoBox should be targeted.

## Project Layout

Related projects are checked out as siblings under `~/git/`:

**Core:**
- `bentobox/` — core BentoBox framework

**Game modes:**
- `addon-acidisland/` — AcidIsland game mode
- `addon-bskyblock/` — BSkyBlock game mode
- `Boxed/` — Boxed game mode (expandable box area)
- `CaveBlock/` — CaveBlock game mode
- `OneBlock/` — AOneBlock game mode
- `SkyGrid/` — SkyGrid game mode
- `RaftMode/` — Raft survival game mode
- `StrangerRealms/` — StrangerRealms game mode
- `Brix/` — plot game mode
- `parkour/` — Parkour game mode
- `poseidon/` — Poseidon game mode
- `gg/` — gg game mode

**Addons:**
- `addon-level/` — island level calculation
- `addon-challenges/` — challenges system
- `addon-welcomewarpsigns/` — warp signs
- `addon-limits/` — block/entity limits
- `addon-invSwitcher/` / `invSwitcher/` — inventory switcher
- `addon-biomes/` / `Biomes/` — biomes management
- `Bank/` — island bank
- `Border/` — world border for islands
- `Chat/` — island chat
- `CheckMeOut/` — island submission/voting
- `ControlPanel/` — game mode control panel
- `Converter/` — ASkyBlock to BSkyBlock converter
- `DimensionalTrees/` — dimension-specific trees
- `discordwebhook/` — Discord integration
- `Downloads/` — BentoBox downloads site
- `DragonFights/` — per-island ender dragon fights
- `ExtraMobs/` — additional mob spawning rules
- `FarmersDance/` — twerking crop growth
- `GravityFlux/` — gravity addon
- `Greenhouses-addon/` — greenhouse biomes
- `IslandFly/` — island flight permission
- `IslandRankup/` — island rankup system
- `Likes/` — island likes/dislikes
- `Limits/` — block/entity limits
- `lost-sheep/` — lost sheep adventure
- `MagicCobblestoneGenerator/` — custom cobblestone generator
- `PortalStart/` — portal-based island start
- `pp/` — pp addon
- `Regionerator/` — region management
- `Residence/` — residence addon
- `TopBlock/` — top ten for OneBlock
- `TwerkingForTrees/` — twerking tree growth
- `Upgrades/` — island upgrades (Vault)
- `Visit/` — island visiting
- `weblink/` — web link addon
- `CrowdBound/` — CrowdBound addon

**Data packs:**
- `BoxedDataPack/` — advancement datapack for Boxed

**Documentation & tools:**
- `docs/` — main documentation site
- `docs-chinese/` — Chinese documentation
- `docs-french/` — French documentation
- `BentoBoxWorld.github.io/` — GitHub Pages site
- `website/` — website
- `translation-tool/` — translation tool

Check these for source before any network fetch.

## Key Dependencies (source locations)

- `world.bentobox:bentobox` → `~/git/bentobox/src/`
