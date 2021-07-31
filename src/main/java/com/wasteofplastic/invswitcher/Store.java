/*
 * Copyright (c) 2017 - 2021 tastybento
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.wasteofplastic.invswitcher.dataObjects.InventoryStorage;

import world.bentobox.bentobox.database.Database;
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
    private final InvSwitcher addon;

    public Store(InvSwitcher addon) {
        this.addon = addon;
        database = new Database<>(addon, InventoryStorage.class);
        cache = new HashMap<>();
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
        String overworldName = (world.getName().replace(THE_END, "")).replace(NETHER, "");
        return store.isInventory(overworldName);
    }

    /**
     * Gets items for world. Changes the inventory of player immediately.
     * @param player - player
     * @param world - world
     */
    public void getInventory(Player player, World world) {
        // Get the store
        InventoryStorage store = getInv(player);

        // Do not differentiate between world environments.
        String overworldName = Util.getWorld(world).getName();

        // Inventory
        if (addon.getSettings().isInventory()) {
            player.getInventory().setContents(store.getInventory(overworldName).toArray(new ItemStack[0]));
        }
        if (addon.getSettings().isHealth()) {
            setHeath(store, player, overworldName);
        }
        if (addon.getSettings().isFood()) {
            setFood(store, player, overworldName);
        }
        if (addon.getSettings().isExperience()) {
            // Experience
            setTotalExperience(player, store.getExp().getOrDefault(overworldName, 0));
        }
        if (addon.getSettings().isGamemode()) {
            // Game modes
            player.setGameMode(store.getGameMode(overworldName));
        }
        if (addon.getSettings().isAdvancements()) {
            setAdvancements(store, player, overworldName);
        }
        if (addon.getSettings().isEnderChest()) {
            player.getEnderChest().setContents(store.getEnderChest(overworldName).toArray(new ItemStack[0]));
        }
        if (addon.getSettings().isStatistics()) {
            getStats(store, player, overworldName);
        }
    }

    private void setHeath(InventoryStorage store, Player player, String overworldName) {
        // Health
        double health = store.getHealth().getOrDefault(overworldName, player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());

        if (health > player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()) {
            health = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
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
        storeAndSave(player, world);
        clearPlayer(player);
        // Done!
    }

    /**
     * Store and save the player to the database
     * @param player - player
     * @param world - world to save
     */
    public void storeAndSave(Player player, World world) {
        // Get the player's store
        InventoryStorage store = getInv(player);
        // Do not differentiate between world environments
        String worldName = world.getName();
        String overworldName = (world.getName().replace(THE_END, "")).replace(NETHER, "");
        if (addon.getSettings().isInventory()) {
            // Copy the player's items to the store
            List<ItemStack> contents = Arrays.asList(player.getInventory().getContents());
            store.setInventory(overworldName, contents);
        }
        if (addon.getSettings().isHealth()) {
            store.setHealth(overworldName, player.getHealth());
        }
        if (addon.getSettings().isFood()) {
            store.setFood(overworldName, player.getFoodLevel());
        }
        if (addon.getSettings().isExperience()) {
            store.setExp(overworldName, getTotalExperience(player));
        }
        if (addon.getSettings().isGamemode()) {
            store.setGameMode(overworldName, player.getGameMode());
        }
        if (addon.getSettings().isAdvancements()) {
            // Advancements
            store.clearAdvancement(worldName);
            Iterator<Advancement> it = Bukkit.advancementIterator();
            while (it.hasNext()) {
                Advancement a = it.next();
                AdvancementProgress p = player.getAdvancementProgress(a);
                if (!p.getAwardedCriteria().isEmpty()) {
                    store.setAdvancement(worldName, a.getKey().toString(), new ArrayList<>(p.getAwardedCriteria()));
                }
            }
        }
        if (addon.getSettings().isEnderChest()) {
            // Copy the player's ender chest items to the store
            List<ItemStack> contents = Arrays.asList(player.getEnderChest().getContents());
            store.setEnderChest(overworldName, contents);
        }
        if (addon.getSettings().isStatistics()) {
            saveStats(store, player, overworldName);
        }
        database.saveObjectAsync(store);
    }

    @SuppressWarnings("deprecation")
    private void saveStats(InventoryStorage store, Player player, String worldName) {
        store.clearStats(worldName);
        // Statistics
        Arrays.stream(Statistic.values()).forEach(s -> {
            Map<Material, Integer> map;
            Map<EntityType, Integer> entMap;
            switch(s.getType()) {
            case BLOCK:
                map = Arrays.stream(Material.values()).filter(Material::isBlock)
                .filter(m -> !m.isLegacy())
                .filter(m -> player.getStatistic(s, m) > 0)
                .collect(Collectors.toMap(k -> k, v -> player.getStatistic(s, v)));
                if (!map.isEmpty()) {
                    store.getBlockStats(worldName).put(s, map);
                }
                break;
            case ITEM:
                map = Arrays.stream(Material.values()).filter(Material::isItem)
                .filter(m -> !m.isLegacy())
                .filter(m -> player.getStatistic(s, m) > 0)
                .collect(Collectors.toMap(k -> k, v -> player.getStatistic(s, v)));
                if (!map.isEmpty()) {
                    store.getItemStats(worldName).put(s, map);
                }
                break;
            case ENTITY:
                entMap = Arrays.stream(EntityType.values()).filter(EntityType::isAlive)
                .filter(m -> player.getStatistic(s, m) > 0)
                .collect(Collectors.toMap(k -> k, v -> player.getStatistic(s, v)));
                if (!entMap.isEmpty()) {
                    store.getEntityStats(worldName).put(s, entMap);
                }
                break;
            case UNTYPED:
                int sc = player.getStatistic(s);
                if (sc > 0) {
                    store.getUntypedStats(worldName).put(s, sc);
                }
                break;
            default:
                break;

            }
        });

    }

    /**
     * Get all the stats for this world and apply them to the player
     * @param store - store
     * @param player - player
     * @param worldName - world name
     */
    private void getStats(InventoryStorage store, Player player, String worldName) {
        // Statistics
        Arrays.stream(Statistic.values()).forEach(s -> {
            switch(s.getType()) {
            case BLOCK:
                if (store.getBlockStats(worldName).containsKey(s)) {
                    for (Entry<Material, Integer> en : store.getBlockStats(worldName).get(s).entrySet()) {
                        player.setStatistic(s, en.getKey(), en.getValue());
                    }
                }
                break;
            case ITEM:
                if (store.getItemStats(worldName).containsKey(s)) {
                    for (Entry<Material, Integer> en : store.getItemStats(worldName).get(s).entrySet()) {
                        player.setStatistic(s, en.getKey(), en.getValue());
                    }
                }
                break;
            case ENTITY:
                if (store.getEntityStats(worldName).containsKey(s)) {
                    for (Entry<EntityType, Integer> en : store.getEntityStats(worldName).get(s).entrySet()) {
                        player.setStatistic(s, en.getKey(), en.getValue());
                    }
                }
                break;
            case UNTYPED:
                if (store.getUntypedStats(worldName).containsKey(s)) {
                    player.setStatistic(s, store.getUntypedStats(worldName).get(s));
                }
                break;
            default:
                break;

            }
        });

    }

    @SuppressWarnings("deprecation")
    private void clearPlayer(Player player) {
        // Clear the player's inventory
        player.getInventory().clear();
        setTotalExperience(player, 0);
        Iterator<Advancement> it = Bukkit.advancementIterator();
        while (it.hasNext()) {
            Advancement a = it.next();
            AdvancementProgress p = player.getAdvancementProgress(a);
            p.getAwardedCriteria().forEach(p::revokeCriteria);
        }
        player.getEnderChest().clear();
        // Statistics
        Arrays.stream(Statistic.values()).forEach(s -> {
            switch(s.getType()) {
            case BLOCK:
                for (Material m: Material.values()) {
                    if (m.isBlock() && !m.isLegacy()) {
                        player.setStatistic(s, m, 0);
                    }
                }
                break;
            case ITEM:
                for (Material m: Material.values()) {
                    if (m.isItem() && !m.isLegacy()) {
                        player.setStatistic(s, m, 0);
                    }
                }
                break;
            case ENTITY:
                for (EntityType en: EntityType.values()) {
                    if (en.isAlive()) {
                        player.setStatistic(s, en, 0);
                    }
                }
                break;
            case UNTYPED:
                player.setStatistic(s, 0);
                break;
            default:
                break;

            }

        });
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
    public void saveOnlinePlayers() {
        Bukkit.getOnlinePlayers().forEach(p -> this.storeAndSave(p, p.getWorld()));
    }
}
