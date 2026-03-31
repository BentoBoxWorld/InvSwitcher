# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

InvSwitcher is a BentoBox addon for Minecraft (Spigot/Bukkit) that gives players separate inventories, ender chests, health, food, experience, advancements, game modes, and statistics per game-world. Nether and End dimensions are automatically grouped with their overworld.

## Build Commands

- **Build**: `mvn clean package`
- **Run tests**: `mvn test -Dmaven.compiler.forceJavacCompilerUse=true`
- **Run a single test class**: `mvn test -Dmaven.compiler.forceJavacCompilerUse=true -Dtest=StoreTest`
- **Run a single test method**: `mvn test -Dmaven.compiler.forceJavacCompilerUse=true -Dtest=StoreTest#testMethodName`

Requires Java 21. The build produces a shaded JAR in `target/`. The `-Dmaven.compiler.forceJavacCompilerUse=true` flag is needed to work around a compiler hashing bug with the current JDK.

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
