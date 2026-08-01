/*
 * Copyright (c) 2017 - 2024 tastybento
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.wasteofplastic.invswitcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Registry;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.wasteofplastic.invswitcher.dataobjects.InventoryStorage;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

/**
 * Enables inventory switching between games. Handles food, experience and spawn points.
 * @author tastybento
 *
 */
public class Store {
    private static final CharSequence THE_END = "_the_end";
    private static final CharSequence NETHER = "_nether";
    public static final String DEFAULT_WORLD_KEY = "default";
    private final Database<InventoryStorage> database;
    private final Map<UUID, InventoryStorage> cache;
    private final Map<UUID, String> currentKey;
    private final InvSwitcher addon;
    /**
     * Offline storage objects whose asynchronous save has not yet flushed to the database, kept so
     * that a follow-up read or write reuses the same in-memory copy instead of reloading a stale one.
     * Without this, two rapid sequential economy transactions on an offline player would each load
     * an independent copy from the database before the first save landed, losing the first update.
     * Entries are dropped once all their in-flight saves complete (see {@link #pendingSaveCount}), so
     * a later login still reloads fresh data. Economy operations run on the main thread, so this map
     * stays small and self-clearing.
     */
    private final Map<UUID, InventoryStorage> pendingSaves = new ConcurrentHashMap<>();
    /** Number of in-flight saves per offline player, so a pending object is only evicted once the
     *  last of its saves has flushed. Mutated only on the main thread (see {@link #saveStorage}). */
    private final Map<UUID, Integer> pendingSaveCount = new ConcurrentHashMap<>();

    public Store(InvSwitcher addon) {
        this.addon = addon;
        database = new Database<>(addon, InventoryStorage.class);
        cache = new HashMap<>();
        currentKey = new HashMap<>();
    }

    /**
     * Compute the storage key for a player based on their current location.
     * Returns "worldName/islandId" if per-island mode is active and the player
     * owns multiple concurrent islands, otherwise returns just "worldName".
     * @param player - player
     * @param world - world
     * @return storage key
     */
    public String getStorageKey(Player player, World world) {
        return getStorageKey(player, world, player.getLocation(), null);
    }

    /**
     * Compute the storage key for a player targeting a specific island.
     * @param player - player
     * @param world - world
     * @param island - target island (may be null)
     * @return storage key
     */
    public String getStorageKey(Player player, World world, Island island) {
        return getStorageKey(player, world, player.getLocation(), island);
    }

    /**
     * Compute the storage key for a player at a specific location, optionally targeting a known island.
     * @param player - player
     * @param world - world
     * @param location - location to check
     * @param island - target island, or null to detect from location
     * @return storage key
     */
    String getStorageKey(Player player, World world, Location location, Island island) {
        if (!addon.getWorlds().contains(world)) {
            return DEFAULT_WORLD_KEY;
        }
        String overworldName = getOverworldName(world);

        if (!addon.getSettings().isIslandsActive()) {
            return overworldName;
        }

        // Check if player owns multiple concurrent islands in this world
        World overworld = Util.getWorld(world);
        int count = addon.getIslands().getNumberOfConcurrentIslands(player.getUniqueId(),
                Objects.requireNonNull(overworld));
        if (count <= 1) {
            return overworldName;
        }

        // If a specific island was provided, use it
        if (island != null && island.getOwner() != null
                && island.getOwner().equals(player.getUniqueId())) {
            return overworldName + "/" + island.getUniqueId();
        }

        // If in generic nether/end (not island nether/end), preserve current key
        if (world.getEnvironment() != World.Environment.NORMAL) {
            boolean isIslandDimension = (world.getEnvironment() == World.Environment.NETHER)
                ? BentoBox.getInstance().getIWM().isIslandNether(world)
                : BentoBox.getInstance().getIWM().isIslandEnd(world);
            if (!isIslandDimension) {
                String current = currentKey.get(player.getUniqueId());
                return (current != null) ? current : overworldName;
            }
        }

        // Detect island from location
        Optional<Island> islandOpt = addon.getIslands().getIslandAt(location);
        if (islandOpt.isPresent()) {
            Island loc = islandOpt.get();
            if (loc.getOwner() != null && loc.getOwner().equals(player.getUniqueId())) {
                return overworldName + "/" + loc.getUniqueId();
            }
        }

        // Fallback: use current key if available, else world name
        String current = currentKey.get(player.getUniqueId());
        return (current != null) ? current : overworldName;
    }

    /**
     * Get the overworld name from any world by stripping nether/end suffixes.
     * @param world - world
     * @return overworld name
     */
    private String getOverworldName(World world) {
        if (!addon.getWorlds().contains(world)) {
            return DEFAULT_WORLD_KEY;
        }
        return (world.getName().replace(THE_END, "")).replace(NETHER, "");
    }

    private String findOldNonBentoBoxKey(InventoryStorage store) {
        if (store.getInventory() == null) {
            return null;
        }
        Set<String> bentoboxOverworlds = addon.getWorlds().stream()
            .map(w -> (w.getName().replace(THE_END, "")).replace(NETHER, ""))
            .collect(Collectors.toSet());
        return store.getInventory().keySet().stream()
            .filter(k -> !k.contains("/"))
            .filter(k -> !bentoboxOverworlds.contains(k))
            .filter(k -> !DEFAULT_WORLD_KEY.equals(k))
            .findFirst()
            .orElse(null);
    }

    /**
     * Get the current storage key for a player.
     * @param player - player
     * @return the current storage key, or null if not set
     */
    public String getCurrentKey(Player player) {
        return currentKey.get(player.getUniqueId());
    }

    /**
     * Check if there is a world storage for the player for this world or not
     * @param player - player
     * @param world - world
     * @return true if there is a world stored for this player, otherwise false
     */
    public boolean isWorldStored(Player player, World world) {
        // Get the store
        InventoryStorage store = getInv(player);
        String key = getStorageKey(player, world);
        return store.isInventory(key);
    }

    /**
     * Gets items for world. Changes the inventory of player immediately.
     * @param player - player
     * @param world - world
     */
    public void getInventory(Player player, World world) {
        getInventory(player, world, null);
    }

    /**
     * Gets items for world and island. Changes the inventory of player immediately.
     * @param player - player
     * @param world - world
     * @param island - target island, or null to detect from location
     */
    public void getInventory(Player player, World world, Island island) {
        // Get the store
        InventoryStorage store = getInv(player);

        String islandKey = (island != null) ? getStorageKey(player, world, island) : getStorageKey(player, world);
        String worldKey = getOverworldName(world);

        // Always track the island-level key so future saves and island detection work correctly
        currentKey.put(player.getUniqueId(), islandKey);

        // Migration: non-BentoBox worlds previously stored data under individual world names.
        // Now they share DEFAULT_WORLD_KEY. Find and migrate old data on first access.
        String islandLoadKey = islandKey;
        if (DEFAULT_WORLD_KEY.equals(islandKey) && !store.isInventory(islandKey)) {
            String oldKey = findOldNonBentoBoxKey(store);
            if (oldKey != null) {
                islandLoadKey = oldKey;
                store.clearWorldData(oldKey);
            }
        }

        // Backward compat: if island-specific key has no data, migrate from world-only key.
        // This only happens once — the world-only data is cleared after migration so that
        // other islands don't also inherit a duplicate copy.
        if (islandKey.contains("/") && !store.isInventory(islandKey) && store.isInventory(worldKey)) {
            islandLoadKey = worldKey;
            // Clear the world-only data so it can't be claimed by another island
            store.clearWorldData(worldKey);
        }

        // Each option uses the island key or the world key based on its island sub-setting
        Settings settings = addon.getSettings();
        if (settings.isInventory()) {
            String k = settings.isIslandsInventory() ? islandLoadKey : worldKey;
            player.getInventory().setContents(store.getInventory(k).toArray(new ItemStack[0]));
        }
        if (settings.isHealth()) {
            String k = settings.isIslandsHealth() ? islandLoadKey : worldKey;
            setHeath(store, player, k);
        }
        if (settings.isFood()) {
            String k = settings.isIslandsFood() ? islandLoadKey : worldKey;
            setFood(store, player, k);
        }
        if (settings.isExperience()) {
            String k = settings.isIslandsExperience() ? islandLoadKey : worldKey;
            setTotalExperience(player, store.getExp().getOrDefault(k, 0));
        }
        if (settings.isGamemode()) {
            String k = settings.isIslandsGamemode() ? islandLoadKey : worldKey;
            player.setGameMode(store.getGameMode(k));
        }
        if (settings.isAdvancements()) {
            String k = settings.isIslandsAdvancements() ? islandLoadKey : worldKey;
            setAdvancements(store, player, k);
        }
        if (settings.isEnderChest()) {
            String k = settings.isIslandsEnderChest() ? islandLoadKey : worldKey;
            player.getEnderChest().setContents(store.getEnderChest(k).toArray(new ItemStack[0]));
        }
        if (settings.isStatistics()) {
            String k = settings.isIslandsStatistics() ? islandLoadKey : worldKey;
            getStats(store, player, k);
        }
    }

    private void setHeath(InventoryStorage store, Player player, String overworldName) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = (attr != null) ? attr.getValue() : 20D;

        // Health
        double health = store.getHealth().getOrDefault(overworldName, maxHealth);

        if (health > maxHealth) {
            health = maxHealth;
        }
        // Never load a fatal health value. A stored value of 0 (or less) only arises when the
        // player's state was captured mid-death (e.g. when they died on another island). Applying
        // it to a live player would kill them again the moment they load the world, causing an
        // endless respawn/death loop (issue #56). Restore full health instead, matching vanilla
        // respawn behaviour.
        if (health <= 0D) {
            health = maxHealth;
        }
        player.setHealth(health);

    }

    private void setFood(InventoryStorage store, Player player, String overworldName) {
        // Food
        int food = store.getFood().getOrDefault(overworldName, 20);
        if (food > 20) {
            food = 20;
        } else if (food < 0) {
            food = 0;
        }
        player.setFoodLevel(food);

    }

    private void setAdvancements(InventoryStorage store, Player player, String overworldName) {
        // Advancements
        Map<String, List<String>> advancements = store.getAdvancements(overworldName);
        if (advancements.isEmpty()) {
            return;
        }
        // Save current experience before granting advancements, because some advancements
        // reward XP when their criteria are awarded, which would incorrectly increase the
        // player's experience points.
        int savedExp = getTotalExperience(player);
        advancements.forEach((k, v) -> {
            Iterator<Advancement> it = Bukkit.advancementIterator();
            while (it.hasNext()) {
                Advancement a = it.next();
                if (a.getKey().toString().equals(k)) {
                    // Award
                    v.forEach(player.getAdvancementProgress(a)::awardCriteria);
                }
            }
        });
        // Restore experience to prevent advancement rewards from modifying it
        setTotalExperience(player, savedExp);
    }

    public void removeFromCache(Player player) {
        cache.remove(player.getUniqueId());
        currentKey.remove(player.getUniqueId());
    }

    /**
     * Upgrade a world-only currentKey to an island-specific key when a player transitions
     * from single-island to multi-island mode. Clears stale world-only data from the store
     * (storeAndSave will re-save world-level types like health/food to the world key).
     * @param player - player
     * @param world - world
     * @param oldIsland - the island the player was on (their original island)
     */
    public void upgradeWorldKeyToIsland(Player player, World world, Island oldIsland) {
        InventoryStorage store = getInv(player);
        String worldKey = getOverworldName(world);
        // Clear stale world-only data; storeAndSave will re-save world-level types
        store.clearWorldData(worldKey);
        // Update currentKey so subsequent storeAndSave saves per-island data to the correct key
        currentKey.put(player.getUniqueId(), worldKey + "/" + oldIsland.getUniqueId());
    }

    /**
     * Get the inventory storage object for player from the database or make a new one
     * @param player - player
     * @return inventory storage object
     */
    private InventoryStorage getInv(Player player) {
        if (cache.containsKey(player.getUniqueId())) {
            return cache.get(player.getUniqueId());
        }
        if (database.objectExists(player.getUniqueId().toString())) {
            InventoryStorage store = database.loadObject(player.getUniqueId().toString());
            if (store != null) {
                cache.put(player.getUniqueId(), store);
                return store;
            }
        }
        InventoryStorage store = new InventoryStorage();
        store.setUniqueId(player.getUniqueId().toString());
        cache.put(player.getUniqueId(), store);
        return store;
    }

    /**
     * Stores the player's inventory and other items
     * @param player - player
     * @param world - the world that is associated with these items/elements
     */
    public void storeInventory(Player player, World world) {
        storeAndSave(player, world, false);
        clearPlayer(player);
        // Done!
    }

    /**
     * Store and save the player to the database
     * @param player - player
     * @param world - world to save
     * @param shutdown - true if this is a shutdown save
     */
    public void storeAndSave(Player player, World world, boolean shutdown) {
        // Get the player's store
        InventoryStorage store = getInv(player);
        // Use the current tracked key if available (ensures we save to the correct island slot),
        // otherwise compute from location
        String islandKey = currentKey.getOrDefault(player.getUniqueId(), getStorageKey(player, world));
        String worldKey = getOverworldName(world);
        // Persist the key so economy transactions for this player can be routed to the
        // world they were last in, even after they log out.
        store.setLastKey(islandKey);
        // Each option saves to the island key or the world key based on its island sub-setting
        Settings settings = addon.getSettings();
        if (settings.isInventory()) {
            String k = settings.isIslandsInventory() ? islandKey : worldKey;
            List<ItemStack> contents = Arrays.asList(player.getInventory().getContents());
            store.setInventory(k, contents);
        }
        if (settings.isHealth()) {
            String k = settings.isIslandsHealth() ? islandKey : worldKey;
            store.setHealth(k, player.getHealth());
        }
        if (settings.isFood()) {
            String k = settings.isIslandsFood() ? islandKey : worldKey;
            store.setFood(k, player.getFoodLevel());
        }
        if (settings.isExperience()) {
            String k = settings.isIslandsExperience() ? islandKey : worldKey;
            store.setExp(k, getTotalExperience(player));
        }
        if (settings.isGamemode()) {
            String k = settings.isIslandsGamemode() ? islandKey : worldKey;
            store.setGameMode(k, player.getGameMode());
        }
        if (settings.isAdvancements()) {
            String k = settings.isIslandsAdvancements() ? islandKey : worldKey;
            store.clearAdvancement(k);
            Iterator<Advancement> it = Bukkit.advancementIterator();
            while (it.hasNext()) {
                Advancement a = it.next();
                AdvancementProgress p = player.getAdvancementProgress(a);
                if (!p.getAwardedCriteria().isEmpty()) {
                    store.setAdvancement(k, a.getKey().toString(), new ArrayList<>(p.getAwardedCriteria()));
                }
            }
        }
        if (settings.isEnderChest()) {
            String k = settings.isIslandsEnderChest() ? islandKey : worldKey;
            List<ItemStack> contents = Arrays.asList(player.getEnderChest().getContents());
            store.setEnderChest(k, contents);
        }
        if (settings.isStatistics()) {
            String k = settings.isIslandsStatistics() ? islandKey : worldKey;
            // On shutdown saveStats() gathers synchronously and returns an already-completed
            // future, so thenAccept runs on this thread and persist() writes before we return.
            saveStats(store, player, k, shutdown).thenAccept(s -> persist(s, shutdown));
            return;
        }
        persist(store, shutdown);
    }

    /**
     * Writes the store to the database, synchronously when the server is shutting down.
     * <p>
     * Saves are normally asynchronous, but a shutdown save must not be. BentoBox closes its
     * database immediately after addons are disabled, and players are only kicked afterwards, so an
     * asynchronous write issued from {@link #saveOnShutdown()} loses the race and is silently
     * dropped — and the {@code PlayerQuitEvent} that would otherwise save them fires after the
     * database is already closed.
     * <p>
     * The effect was that everything a player did since their last world change went unsaved when
     * the server stopped. Because {@code PlayerListener.onPlayerJoin} re-applies the stored
     * inventory on login, the stale snapshot then overwrote the player's real inventory and they
     * were rolled back to their last world change.
     * @param store - the store to write
     * @param shutdown - true if this is a shutdown save, which must be synchronous
     */
    private void persist(InventoryStorage store, boolean shutdown) {
        if (shutdown) {
            database.saveObject(store);
        } else {
            database.saveObjectAsync(store);
        }
    }

    private CompletableFuture<InventoryStorage> saveStats(InventoryStorage store, Player player, String worldName,
            boolean shutdown) {
        CompletableFuture<InventoryStorage> result = new CompletableFuture<>();
        store.clearStats(worldName);

        // Statistics
        if (shutdown) {
            saveStatistics(result, store, player, worldName);
        } else {
            // Cannot schedule tasks on shutdown
            Bukkit.getScheduler().runTaskAsynchronously(addon.getPlugin(),
                    () -> saveStatistics(result, store, player, worldName));
        }
        return result;

    }

    private void saveStatistics(CompletableFuture<InventoryStorage> result, InventoryStorage store, Player player,
            String worldName) {
        Registry.STATISTIC.forEach(s -> {
            Map<Material, Integer> map;
            Map<EntityType, Integer> entMap;
            switch (s.getType()) {
            case BLOCK -> {
                map = Registry.MATERIAL.stream().filter(Material::isBlock).filter(m -> player.getStatistic(s, m) > 0)
                        .collect(Collectors.toMap(k -> k, v -> player.getStatistic(s, v)));
                if (!map.isEmpty()) {
                    store.getBlockStats(worldName).put(s, map);
                }
            }
            case ITEM -> {
                map = Registry.MATERIAL.stream().filter(Material::isItem).filter(m -> player.getStatistic(s, m) > 0)
                        .collect(Collectors.toMap(k -> k, v -> player.getStatistic(s, v)));
                if (!map.isEmpty()) {
                    store.getItemStats(worldName).put(s, map);
                }
            }
            case ENTITY -> {
                entMap = Registry.ENTITY_TYPE.stream().filter(EntityType::isAlive)
                        .filter(m -> player.getStatistic(s, m) > 0)
                        .collect(Collectors.toMap(k -> k, v -> player.getStatistic(s, v)));
                if (!entMap.isEmpty()) {
                    store.getEntityStats(worldName).put(s, entMap);
                }
            }
            case UNTYPED -> {
                int sc = player.getStatistic(s);
                if (sc > 0) {
                    store.getUntypedStats(worldName).put(s, sc);
                }
            }
            }
        });
        result.complete(store);
    }

    /**
     * Get all the stats for this world and apply them to the player
     * @param store - store
     * @param player - player
     * @param worldName - world name
     */
    private void getStats(InventoryStorage store, Player player, String worldName) {
        // Statistics
        Arrays.stream(Statistic.values()).forEach(s -> getStat(s, store, player, worldName));

    }

    private void getStat(Statistic s, InventoryStorage store, Player player, String worldName) {
        switch(s.getType()) {
        case BLOCK -> store.getBlockStats(worldName).getOrDefault(s, Collections.emptyMap()).forEach((k,v) -> player.setStatistic(s, k, v));
        case ITEM -> store.getItemStats(worldName).getOrDefault(s, Collections.emptyMap()).forEach((k,v) -> player.setStatistic(s, k, v));
        case ENTITY -> store.getEntityStats(worldName).getOrDefault(s, Collections.emptyMap()).forEach((k,v) -> player.setStatistic(s, k, v));
        case UNTYPED -> Optional.ofNullable(store.getUntypedStats(worldName).get(s))
                .ifPresent(v -> player.setStatistic(s, v));
        }
    }

    private void clearPlayer(Player player) {
        if (this.addon.getSettings().isInventory())
        {
            // Clear the player's inventory
            player.getInventory().clear();
        }

        if (this.addon.getSettings().isExperience())
        {
            // Reset experience
            setTotalExperience(player, 0);
        }

        if (this.addon.getSettings().isAdvancements())
        {
            // Reset advancements
            resetAdv(player);
        }

        if (this.addon.getSettings().isEnderChest())
        {
            // Reset enderchest
            player.getEnderChest().clear();
        }

        if (this.addon.getSettings().isStatistics())
        {
            // Reset Statistics
            Arrays.stream(Statistic.values()).forEach(s ->
            resetStats(player, s));
        }
    }

    private void resetAdv(Player player) {
        Iterator<Advancement> it = Bukkit.advancementIterator();
        while (it.hasNext())
        {
            Advancement a = it.next();
            AdvancementProgress p = player.getAdvancementProgress(a);
            p.getAwardedCriteria().forEach(p::revokeCriteria);
        }
    }

    @SuppressWarnings("deprecation")
    private void resetStats(Player player, Statistic s) {
        switch (s.getType()) {
        case BLOCK -> Arrays.stream(Material.values()).filter(Material::isBlock).filter(m -> !m.isLegacy())
                .forEach(m -> player.setStatistic(s, m, 0));
        case ITEM -> Arrays.stream(Material.values()).filter(Material::isItem).filter(m -> !m.isLegacy())
                .forEach(m -> player.setStatistic(s, m, 0));
        case ENTITY ->
            Arrays.stream(EntityType.values()).filter(EntityType::isAlive).forEach(en -> player.setStatistic(s, en, 0));
        case UNTYPED -> player.setStatistic(s, 0);
        }
    }

    //new Exp Math from 1.8
    private  static  int getExpAtLevel(final int level)
    {
        if (level <= 15)
        {
            return (2*level) + 7;
        }
        if (level <= 30)
        {
            return (5 * level) -38;
        }
        return (9*level)-158;

    }

    private static int getExpAtLevel(final Player player)
    {
        return getExpAtLevel(player.getLevel());
    }

    //This method is required because the bukkit player.getTotalExperience() method, shows exp that has been 'spent'.
    //Without this people would be able to use exp and then still sell it.
    private static int getTotalExperience(final Player player)
    {
        int exp = Math.round(getExpAtLevel(player) * player.getExp());
        int currentLevel = player.getLevel();

        while (currentLevel > 0)
        {
            currentLevel--;
            exp += getExpAtLevel(currentLevel);
        }
        if (exp < 0)
        {
            exp = Integer.MAX_VALUE;
        }
        return exp;
    }

    // These next methods are taken from Essentials code

    //This method is used to update both the recorded total experience and displayed total experience.
    //We reset both types to prevent issues.
    private static void setTotalExperience(final Player player, final int exp)
    {
        if (exp < 0)
        {
            throw new IllegalArgumentException("Experience is negative!");
        }
        player.setExp(0);
        player.setLevel(0);
        player.setTotalExperience(0);

        //This following code is technically redundant now, as bukkit now calculates levels more or less correctly
        //At larger numbers however... player.getExp(3000), only seems to give 2999, putting the below calculations off.
        int amount = exp;
        while (amount > 0)
        {
            final int expToLevel = getExpAtLevel(player);
            amount -= expToLevel;
            if (amount >= 0)
            {
                // give until next level
                player.giveExp(expToLevel);
            }
            else
            {
                // give the rest
                amount += expToLevel;
                player.giveExp(amount);
                amount = 0;
            }
        }
    }

    /**
     * Save all online players
     */
    public void saveOnShutdown() {
        Bukkit.getOnlinePlayers().forEach(p -> this.storeAndSave(p, p.getWorld(), true));
    }

    /**
     * Compute the storage key for a player and island event world, without using
     * the player's current location. Used when the player is not in the target world.
     * @param player - player
     * @param world  - the BentoBox event world
     * @param island - the island involved in the event (may be null)
     * @return storage key for this world/island combination
     */
    String getStorageKeyForEvent(Player player, World world, Island island) {
        String overworldName = getOverworldName(world);
        if (!addon.getSettings().isIslandsActive()) {
            return overworldName;
        }
        World overworld = Util.getWorld(world);
        if (overworld == null) {
            return overworldName;
        }
        int count = addon.getIslands().getNumberOfConcurrentIslands(player.getUniqueId(), overworld);
        if (count <= 1) {
            return overworldName;
        }
        // Only use island-specific key if the player owns the island
        if (island != null && island.getOwner() != null && island.getOwner().equals(player.getUniqueId())) {
            return overworldName + "/" + island.getUniqueId();
        }
        return overworldName;
    }

    /**
     * Clears the stored inventory for a BentoBox world when the player is not currently in
     * that world. Called when BentoBox fires a {@code PlayerResetInventoryEvent} while the
     * player is in a non-BentoBox world so the player's current inventory is not affected.
     * @param player - online player
     * @param world  - the BentoBox world whose stored inventory should be cleared
     * @param island - the island involved in the reset (may be null)
     */
    public void clearStoredInventoryForWorld(Player player, World world, Island island) {
        InventoryStorage store = getInv(player);
        String key = getStorageKeyForEvent(player, world, island);
        String worldKey = getOverworldName(world);
        Settings settings = addon.getSettings();
        if (settings.isInventory()) {
            String k = settings.isIslandsInventory() ? key : worldKey;
            store.setInventory(k, Collections.emptyList());
        }
        database.saveObjectAsync(store);
    }

    /**
     * Clears the stored ender chest for a BentoBox world when the player is not currently in
     * that world. Called when BentoBox fires a {@code PlayerResetEnderChestEvent} while the
     * player is in a non-BentoBox world.
     * @param player - online player
     * @param world  - the BentoBox world whose stored ender chest should be cleared
     * @param island - the island involved in the reset (may be null)
     */
    public void clearStoredEnderChestForWorld(Player player, World world, Island island) {
        InventoryStorage store = getInv(player);
        String key = getStorageKeyForEvent(player, world, island);
        String worldKey = getOverworldName(world);
        Settings settings = addon.getSettings();
        if (settings.isEnderChest()) {
            String k = settings.isIslandsEnderChest() ? key : worldKey;
            store.setEnderChest(k, Collections.emptyList());
        }
        database.saveObjectAsync(store);
    }

    /**
     * Zeroes the stored experience for a BentoBox world when the player is not currently in
     * that world. Called when BentoBox fires a {@code PlayerResetExpEvent} while the
     * player is in a non-BentoBox world.
     * @param player - online player
     * @param world  - the BentoBox world whose stored experience should be zeroed
     * @param island - the island involved in the reset (may be null)
     */
    public void clearStoredExpForWorld(Player player, World world, Island island) {
        InventoryStorage store = getInv(player);
        String key = getStorageKeyForEvent(player, world, island);
        String worldKey = getOverworldName(world);
        Settings settings = addon.getSettings();
        if (settings.isExperience()) {
            String k = settings.isIslandsExperience() ? key : worldKey;
            store.setExp(k, 0);
        }
        database.saveObjectAsync(store);
    }

    /**
     * Removes the stored health for a BentoBox world when the player is not currently in
     * that world. Called when BentoBox fires a {@code PlayerResetHealthEvent} while the
     * player is in a non-BentoBox world. Removing the entry means the player will receive
     * maximum health the next time they enter the world.
     * @param player - online player
     * @param world  - the BentoBox world whose stored health should be removed
     * @param island - the island involved in the reset (may be null)
     */
    public void clearStoredHealthForWorld(Player player, World world, Island island) {
        InventoryStorage store = getInv(player);
        String key = getStorageKeyForEvent(player, world, island);
        String worldKey = getOverworldName(world);
        Settings settings = addon.getSettings();
        if (settings.isHealth()) {
            String k = settings.isIslandsHealth() ? key : worldKey;
            store.getHealth().remove(k);
        }
        database.saveObjectAsync(store);
    }

    /**
     * Resets the stored food level to full (20) for a BentoBox world when the player is not
     * currently in that world. Called when BentoBox fires a {@code PlayerResetHungerEvent}
     * while the player is in a non-BentoBox world.
     * @param player - online player
     * @param world  - the BentoBox world whose stored food level should be reset
     * @param island - the island involved in the reset (may be null)
     */
    public void clearStoredFoodForWorld(Player player, World world, Island island) {
        InventoryStorage store = getInv(player);
        String key = getStorageKeyForEvent(player, world, island);
        String worldKey = getOverworldName(world);
        Settings settings = addon.getSettings();
        if (settings.isFood()) {
            String k = settings.isIslandsFood() ? key : worldKey;
            store.setFood(k, 20);
        }
        database.saveObjectAsync(store);
    }

    /**
     * Zeroes the stored money balance for a BentoBox world when the player is not currently in
     * that world. Called when BentoBox fires a {@code PlayerResetMoneyEvent} (e.g. on island
     * reset) while the player is in a non-BentoBox world, so the correct world's balance is
     * cleared rather than BentoBox withdrawing from the wrong world.
     * @param player - online player
     * @param world  - the BentoBox world whose stored balance should be zeroed
     * @param island - the island involved in the reset (may be null)
     */
    public void clearStoredMoneyForWorld(Player player, World world, Island island) {
        InventoryStorage store = getInv(player);
        String key = getStorageKeyForEvent(player, world, island);
        String worldKey = getOverworldName(world);
        Settings settings = addon.getSettings();
        if (settings.isMoney()) {
            String k = settings.isIslandsMoney() ? key : worldKey;
            store.setMoney(k, 0D);
        }
        database.saveObjectAsync(store);
    }

    // ------ ECONOMY SUPPORT ------

    /**
     * Get the {@link InventoryStorage} for any player UUID, online or offline. For online
     * players this returns the cached session object so economy changes stay consistent
     * with the rest of their data. For offline players a transient copy is loaded from the
     * database and is deliberately <b>not</b> cached, so a later login reloads fresh data.
     * @param uuid - player UUID
     * @return the player's inventory storage (never null)
     */
    public InventoryStorage getStorageObject(UUID uuid) {
        if (cache.containsKey(uuid)) {
            return cache.get(uuid);
        }
        // An offline write may still be in flight (not yet flushed to the database). Reuse that same
        // object so this read - and any follow-up write built on it - sees the pending change rather
        // than a stale reload. This is what keeps rapid sequential offline transactions consistent.
        InventoryStorage pending = pendingSaves.get(uuid);
        if (pending != null) {
            return pending;
        }
        if (database.objectExists(uuid.toString())) {
            InventoryStorage store = database.loadObject(uuid.toString());
            if (store != null) {
                return store;
            }
        }
        InventoryStorage store = new InventoryStorage();
        store.setUniqueId(uuid.toString());
        return store;
    }

    /**
     * Persist a storage object asynchronously.
     * <p>
     * For an online (cached) player the cache is the source of truth, so a plain async save cannot
     * be read back stale. For an offline player the object is transient and not cached, so the save
     * is tracked in {@link #pendingSaves} until it flushes - otherwise a follow-up read would reload
     * a stale copy from the database before the async write landed, losing the update.
     * @param store - storage to save
     */
    public void saveStorage(InventoryStorage store) {
        UUID uuid = UUID.fromString(store.getUniqueId());
        if (cache.containsKey(uuid)) {
            database.saveObjectAsync(store);
            return;
        }
        pendingSaves.put(uuid, store);
        pendingSaveCount.merge(uuid, 1, Integer::sum);
        database.saveObjectAsync(store).whenComplete((r, ex) -> onOfflineSaveComplete(uuid));
    }

    /**
     * Drop a pending offline save once it has flushed, but only when it was the last save in flight
     * for that player, so a still-pending later write keeps its object available. Runs the eviction
     * on the main thread to stay consistent with {@link #saveStorage}; during shutdown the scheduler
     * is unavailable, so it evicts inline.
     * @param uuid - the player whose save completed
     */
    private void onOfflineSaveComplete(UUID uuid) {
        Runnable evict = () -> {
            if (pendingSaveCount.merge(uuid, -1, Integer::sum) <= 0) {
                pendingSaveCount.remove(uuid);
                pendingSaves.remove(uuid);
            }
        };
        if (addon.getPlugin().isEnabled()) {
            Bukkit.getScheduler().runTask(addon.getPlugin(), evict);
        } else {
            evict.run();
        }
    }

    /**
     * Resolve the money storage key for a player in a specific world.
     * @param player - player (online or offline)
     * @param world  - world to resolve the key for
     * @return the money key, or {@code null} if the world is not managed by InvSwitcher
     */
    public String getMoneyKey(OfflinePlayer player, World world) {
        if (world == null || !addon.getWorlds().contains(world)) {
            return null;
        }
        String overworldName = getOverworldName(world);
        Settings settings = addon.getSettings();
        if (!settings.isIslandsActive() || !settings.isIslandsMoney()) {
            return overworldName;
        }
        // Best-effort per-island routing
        Player online = player.getPlayer();
        if (online != null && world.equals(online.getWorld())) {
            return getStorageKey(online, world);
        }
        // Offline or in another world: fall back to the player's last island key for this overworld
        String last = getStorageObject(player.getUniqueId()).getLastKey();
        if (last != null && last.startsWith(overworldName + "/")) {
            return last;
        }
        return overworldName;
    }

    /**
     * Resolve the money storage key for a player's current (online) or last-known (offline)
     * world, used when a caller does not specify a world.
     * @param player - player (online or offline)
     * @return the money key, or {@code null} if it cannot be resolved to a managed world
     */
    public String getCurrentMoneyKey(OfflinePlayer player) {
        Player online = player.getPlayer();
        if (online != null) {
            return getMoneyKey(player, online.getWorld());
        }
        String last = getStorageObject(player.getUniqueId()).getLastKey();
        if (last == null || DEFAULT_WORLD_KEY.equals(last)) {
            return null;
        }
        return last;
    }

}
