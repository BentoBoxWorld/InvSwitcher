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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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
    private final Database<InventoryStorage> database;
    private final Map<UUID, InventoryStorage> cache;
    private final Map<UUID, String> currentKey;
    private final InvSwitcher addon;

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
        String overworldName = getOverworldName(world);

        if (!addon.getSettings().isIslands()) {
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
        return (world.getName().replace(THE_END, "")).replace(NETHER, "");
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

        String key = (island != null) ? getStorageKey(player, world, island) : getStorageKey(player, world);

        // Always track the resolved key (including island suffix) so future saves go to the right slot
        currentKey.put(player.getUniqueId(), key);

        // Backward compat: if island-specific key has no data, migrate from world-only key.
        // This only happens once — the world-only data is cleared after migration so that
        // other islands don't also inherit a duplicate copy.
        String loadKey = key;
        if (key.contains("/") && !store.isInventory(key)) {
            String overworldName = getOverworldName(world);
            if (store.isInventory(overworldName)) {
                loadKey = overworldName;
                // Clear the world-only data so it can't be claimed by another island
                store.clearWorldData(overworldName);
            }
        }

        // Inventory
        if (addon.getSettings().isInventory()) {
            player.getInventory().setContents(store.getInventory(loadKey).toArray(new ItemStack[0]));
        }
        if (addon.getSettings().isHealth()) {
            setHeath(store, player, loadKey);
        }
        if (addon.getSettings().isFood()) {
            setFood(store, player, loadKey);
        }
        if (addon.getSettings().isExperience()) {
            // Experience
            setTotalExperience(player, store.getExp().getOrDefault(loadKey, 0));
        }
        if (addon.getSettings().isGamemode()) {
            // Game modes
            player.setGameMode(store.getGameMode(loadKey));
        }
        if (addon.getSettings().isAdvancements()) {
            setAdvancements(store, player, loadKey);
        }
        if (addon.getSettings().isEnderChest()) {
            player.getEnderChest().setContents(store.getEnderChest(loadKey).toArray(new ItemStack[0]));
        }
        if (addon.getSettings().isStatistics()) {
            getStats(store, player, loadKey);
        }
    }

    private void setHeath(InventoryStorage store, Player player, String overworldName) {
        // Health
        double health = store.getHealth().getOrDefault(overworldName,
                player.getAttribute(Attribute.MAX_HEALTH).getValue());

        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null && health > attr.getValue()) {
            health = attr.getValue();
        }
        if (health < 0D) {
            health = 0D;
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
        store.getAdvancements(overworldName).forEach((k, v) -> {
            Iterator<Advancement> it = Bukkit.advancementIterator();
            while (it.hasNext()) {
                Advancement a = it.next();
                if (a.getKey().toString().equals(k)) {
                    // Award
                    v.forEach(player.getAdvancementProgress(a)::awardCriteria);
                }
            }
        });

    }

    public void removeFromCache(Player player) {
        cache.remove(player.getUniqueId());
        currentKey.remove(player.getUniqueId());
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
        String key = currentKey.getOrDefault(player.getUniqueId(), getStorageKey(player, world));
        if (addon.getSettings().isInventory()) {
            // Copy the player's items to the store
            List<ItemStack> contents = Arrays.asList(player.getInventory().getContents());
            store.setInventory(key, contents);
        }
        if (addon.getSettings().isHealth()) {
            store.setHealth(key, player.getHealth());
        }
        if (addon.getSettings().isFood()) {
            store.setFood(key, player.getFoodLevel());
        }
        if (addon.getSettings().isExperience()) {
            store.setExp(key, getTotalExperience(player));
        }
        if (addon.getSettings().isGamemode()) {
            store.setGameMode(key, player.getGameMode());
        }
        if (addon.getSettings().isAdvancements()) {
            // Advancements
            store.clearAdvancement(key);
            Iterator<Advancement> it = Bukkit.advancementIterator();
            while (it.hasNext()) {
                Advancement a = it.next();
                AdvancementProgress p = player.getAdvancementProgress(a);
                if (!p.getAwardedCriteria().isEmpty()) {
                    store.setAdvancement(key, a.getKey().toString(), new ArrayList<>(p.getAwardedCriteria()));
                }
            }
        }
        if (addon.getSettings().isEnderChest()) {
            // Copy the player's ender chest items to the store
            List<ItemStack> contents = Arrays.asList(player.getEnderChest().getContents());
            store.setEnderChest(key, contents);
        }
        if (addon.getSettings().isStatistics()) {
            saveStats(store, player, key, shutdown).thenAccept(database::saveObjectAsync);
            return;
        }
        database.saveObjectAsync(store);
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
        case UNTYPED -> {
            if (store.getUntypedStats(worldName).containsKey(s)) {
                player.setStatistic(s, store.getUntypedStats(worldName).get(s));
            }
        }
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
}
