/*
 * Copyright (c) 2017 tastybento
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
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.wasteofplastic.invswitcher.dataObjects.InventoryStorage;

import world.bentobox.bentobox.database.Database;

/**
 * Enables inventory switching between games. Handles food, experience and spawn points.
 * @author tastybento
 *
 */
public class Store {
    private final Database<InventoryStorage> database;
    private final Map<UUID, InventoryStorage> cache;

    public Store(InvSwitcher addon) {
        database = new Database<>(addon, InventoryStorage.class);
        cache = new HashMap<>();
    }

    /**
     * Gets items for world. Changes the inventory of player immediately.
     * @param player - player
     * @param world - world
     */
    public void getInventory(Player player, World world) {
        // Get inventory
        InventoryStorage store = getInv(player);

        // Do not differentiate between world environments. Only the location is different
        String worldName = world.getName();
        String overworldName = (world.getName().replace("_the_end", "")).replace("_nether", "");

        player.getInventory().setContents(store.getInventory(overworldName).toArray(new ItemStack[0]));


        double health = store.getHealth().getOrDefault(overworldName, player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());

        if (health > player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()) {
            health = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        }
        if (health < 0D) {
            health = 0D;
        }
        player.setHealth(health);


        int food = store.getFood().getOrDefault(overworldName, 20);
        if (food > 20) {
            food = 20;
        } else if (food < 0) {
            food = 0;
        }
        player.setFoodLevel(food);

        setTotalExperience(player, store.getExp().getOrDefault(overworldName, 0));

        player.setGameMode(store.getGameMode(overworldName));

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
        // Get Spawn Point
        store.getLocation(worldName);
    }

    public void removeFromCache(Player player) {
        cache.remove(player.getUniqueId());
    }

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
        // Get the player's inventory
        InventoryStorage store = getInv(player);
        // Do not differentiate between world environments
        String worldName = world.getName();
        String overworldName = (world.getName().replace("_the_end", "")).replace("_nether", "");
        // Copy the player's items to the store
        List<ItemStack> contents = Arrays.asList(player.getInventory().getContents());
        store.setInventory(overworldName, contents);
        store.setHealth(overworldName, player.getHealth());
        store.setFood(overworldName, player.getFoodLevel());
        store.setExp(overworldName, getTotalExperience(player));
        store.setLocation(worldName, player.getLocation());
        store.setGameMode(overworldName, player.getGameMode());
        database.saveObjectAsync(store);
        // Clear the player's inventory
        player.getInventory().clear();
        setTotalExperience(player, 0);
        // Advancements
        Iterator<Advancement> it = Bukkit.advancementIterator();
        while (it.hasNext()) {
            Advancement a = it.next();
            AdvancementProgress p = player.getAdvancementProgress(a);
            if (!p.getAwardedCriteria().isEmpty()) {
                store.setAdvancement(worldName, a.getKey().toString(), new ArrayList<>(p.getAwardedCriteria()));
            }
            p.getAwardedCriteria().forEach(p::revokeCriteria);
        }
        // Done!
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
}
