package com.wasteofplastic.invswitcher.dataObjects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import com.google.gson.annotations.Expose;

import world.bentobox.bentobox.database.objects.DataObject;
import world.bentobox.bentobox.database.objects.Table;

@Table(name = "InventoryStorage")
public class InventoryStorage implements DataObject {

    @Expose
    private String uniqueId;
    @Expose
    private Map<String, List<ItemStack>> inventory = new HashMap<>();
    @Expose
    private Map<String, Double> health = new HashMap<>();
    @Expose
    private Map<String, Integer> food = new HashMap<>();
    @Expose
    private Map<String, Integer> exp = new HashMap<>();
    @Expose
    private Map<String, Location> location = new HashMap<>();
    @Expose
    private Map<String, GameMode> gameMode = new HashMap<>();
    @Expose
    private Map<String, Map<String, List<String>>> advancements = new HashMap<>();
    @Expose
    private Map<String, List<ItemStack>> enderChest = new HashMap<>();
    @Expose
    private Map<String, Map<Statistic, Integer>> untypedStats = new HashMap<>();
    @Expose
    private Map<String, Map<Statistic, Map<Material, Integer>>> blockStats = new HashMap<>();
    @Expose
    private Map<String, Map<Statistic, Map<Material, Integer>>> itemStats = new HashMap<>();
    @Expose
    private Map<String, Map<Statistic, Map<EntityType, Integer>>> entityStats = new HashMap<>();

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;

    }

    /**
     * @return the inventory
     */
    public Map<String, List<ItemStack>> getInventory() {
        return inventory;
    }

    /**
     * @return the health
     */
    public Map<String, Double> getHealth() {
        return health;
    }

    /**
     * @return the food
     */
    public Map<String, Integer> getFood() {
        return food;
    }

    /**
     * @return the exp
     */
    public Map<String, Integer> getExp() {
        return exp;
    }

    /**
     * @return the location
     */
    public Map<String, Location> getLocation() {
        return location;
    }


    /**
     * @param inventory the inventory to set
     */
    public void setInventory(Map<String, List<ItemStack>> inventory) {
        this.inventory = inventory;
    }

    /**
     *
     * @param worldname the world name
     * @param inventory the inventory to set
     */
    public void setInventory(String worldname, List<ItemStack> inventory) {
        this.inventory.put(worldname, inventory);

    }

    /**
     * @param health the health to set
     */
    public void setHealth(Map<String, Double> health) {
        this.health = health;
    }

    /**
     * @param food the food to set
     */
    public void setFood(Map<String, Integer> food) {
        this.food = food;
    }

    /**
     * @param exp the exp to set
     */
    public void setExp(Map<String, Integer> exp) {
        this.exp = exp;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(Map<String, Location> location) {
        this.location = location;
    }

    public void setHealth(String overworldName, double health2) {
        this.health.put(overworldName, health2);

    }

    public void setFood(String overworldName, int foodLevel) {
        this.food.put(overworldName, foodLevel);

    }

    public void setExp(String overworldName, int totalExperience) {
        this.exp.put(overworldName, totalExperience);

    }

    public void setLocation(String worldName, Location location2) {
        this.location.put(worldName, location2);

    }

    public List<ItemStack> getInventory(String overworldName) {
        return inventory == null ? new ArrayList<>() : inventory.getOrDefault(overworldName, new ArrayList<>());
    }

    /**
     * Check if an inventory for this world exists or not
     * @param overworldName - over world name
     * @return true if there is an inventory for this world, false if not.
     */
    public boolean isInventory(String overworldName) {
        return inventory != null && inventory.containsKey(overworldName);
    }

    public void getLocation(String worldName) {
        if (location != null) {
            location.get(worldName);
        }
    }

    public void setGameMode(String worldName, GameMode gameMode) {
        this.gameMode.put(worldName, gameMode);
    }

    public GameMode getGameMode(String worldName) {
        return this.gameMode.getOrDefault(worldName, GameMode.SURVIVAL);
    }

    public void setAdvancement(String worldName, String key, List<String> criteria) {
        this.advancements.computeIfAbsent(worldName, k -> new HashMap<>()).put(key, criteria);
    }

    /**
     * Clears advancements for world
     * @param worldName - world name
     */
    public void clearAdvancement(String worldName) {
        this.advancements.remove(worldName);
    }

    /**
     * @return the advancements
     */
    public Map<String, List<String>> getAdvancements(String worldName) {
        return advancements.getOrDefault(worldName, Collections.emptyMap());
    }

    /**
     * Get the EnderChest inventory
     * @param overworldName - world name
     * @return inventory
     */
    public List<ItemStack> getEnderChest(String overworldName) {
        return enderChest == null ? new ArrayList<>() : enderChest.getOrDefault(overworldName, new ArrayList<>());
    }

    /**
     *
     * @param worldname the world name
     * @param inventory the inventory to set
     */
    public void setEnderChest(String worldname, List<ItemStack> inventory) {
        this.enderChest.put(worldname, inventory);

    }

    /**
     * @return the enderChest
     */
    public Map<String, List<ItemStack>> getEnderChest() {
        return enderChest;
    }

    /**
     * @param enderChest the enderChest to set
     */
    public void setEnderChest(Map<String, List<ItemStack>> enderChest) {
        this.enderChest = enderChest;
    }

    /**
     * Clear the stats for player for world name
     * @param worldName World name
     */
    public void clearStats(String worldName) {
        this.blockStats.remove(worldName);
        this.itemStats.remove(worldName);
        this.untypedStats.remove(worldName);
        this.entityStats.remove(worldName);
    }

    /**
     * Get Untyped stats
     * @param worldName World name
     * @return the untypedStats
     */
    public Map<Statistic, Integer> getUntypedStats(String worldName) {
        return untypedStats.computeIfAbsent(worldName, k -> new EnumMap<>(Statistic.class));
    }

    /**
     * @param worldName World name
     * @param untypedStats the untypedStats to set
     */
    public void setUntypedStats(String worldName, Map<Statistic, Integer> untypedStats) {
        this.untypedStats.put(worldName, untypedStats);
    }

    /**
     * @param worldName World name
     * @return the blockStats
     */
    public Map<Statistic, Map<Material, Integer>> getBlockStats(String worldName) {
        return blockStats.computeIfAbsent(worldName, k -> new EnumMap<>(Statistic.class));
    }

    /**
     * @param worldName World name
     * @param blockStats the blockStats to set
     */
    public void setBlockStats(String worldName, Map<Statistic, Map<Material, Integer>> blockStats) {
        this.blockStats.put(worldName, blockStats);
    }

    /**
     * @param worldName World name
     * @return the itemStats
     */
    public Map<Statistic, Map<Material, Integer>> getItemStats(String worldName) {
        return itemStats.computeIfAbsent(worldName, k -> new EnumMap<>(Statistic.class));
    }

    /**
     * @param worldName World name
     * @param itemStats the itemStats to set
     */
    public void setItemStats(String worldName, Map<Statistic, Map<Material, Integer>> itemStats) {
        this.itemStats.put(worldName, itemStats);
    }

    /**
     * @param worldName World name
     * @return the entityStats
     */
    public Map<Statistic, Map<EntityType, Integer>> getEntityStats(String worldName) {
        return entityStats.computeIfAbsent(worldName, k -> new EnumMap<>(Statistic.class));
    }

    /**
     * @param worldName World name
     * @param entityStats the entityStats to set
     */
    public void setEntityStats(String worldName, Map<Statistic, Map<EntityType, Integer>> entityStats) {
        this.entityStats.put(worldName, entityStats);
    }


}