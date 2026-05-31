package com.wasteofplastic.invswitcher.dataobjects;

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

    /**
     * The unique identifier for this inventory storage.
     */
    @Expose
    private String uniqueId;

    /**
     * Map of world name to inventory contents.
     */
    @Expose
    private Map<String, List<ItemStack>> inventory = new HashMap<>();

    /**
     * Map of world name to player health.
     */
    @Expose
    private Map<String, Double> health = new HashMap<>();

    /**
     * Map of world name to player food level.
     */
    @Expose
    private Map<String, Integer> food = new HashMap<>();

    /**
     * Map of world name to player experience.
     */
    @Expose
    private Map<String, Integer> exp = new HashMap<>();

    /**
     * Map of world/island key to the player's money balance for that world.
     */
    @Expose
    private Map<String, Double> money = new HashMap<>();

    /**
     * The last storage key the player was tracked under. Persisted so that economy
     * transactions for an offline player can be routed to the world they were last in.
     */
    @Expose
    private String lastKey;

    /**
     * Whether this player's pre-existing balance has already been imported from the
     * previous economy provider. Prevents the one-time import from running twice.
     */
    @Expose
    private boolean imported;

    /**
     * Map of world name to player location.
     */
    @Expose
    private Map<String, Location> location = new HashMap<>();

    /**
     * Map of world name to player game mode.
     */
    @Expose
    private Map<String, GameMode> gameMode = new HashMap<>();

    /**
     * Map of world name to advancements (keyed by advancement key and criteria).
     */
    @Expose
    private Map<String, Map<String, List<String>>> advancements = new HashMap<>();

    /**
     * Map of world name to Ender Chest inventory contents.
     */
    @Expose
    private Map<String, List<ItemStack>> enderChest = new HashMap<>();

    /**
     * Map of world name to untyped statistics.
     */
    @Expose
    private Map<String, Map<Statistic, Integer>> untypedStats = new HashMap<>();

    /**
     * Map of world name to block statistics.
     */
    @Expose
    private Map<String, Map<Statistic, Map<Material, Integer>>> blockStats = new HashMap<>();

    /**
     * Map of world name to item statistics.
     */
    @Expose
    private Map<String, Map<Statistic, Map<Material, Integer>>> itemStats = new HashMap<>();

    /**
     * Map of world name to entity statistics.
     */
    @Expose
    private Map<String, Map<Statistic, Map<EntityType, Integer>>> entityStats = new HashMap<>();

    /**
     * Gets the unique identifier for this inventory storage.
     * @return the uniqueId
     */
    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * Sets the unique identifier for this inventory storage.
     * @param uniqueId the uniqueId to set
     */
    @Override
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    /**
     * Gets the inventory map.
     * @return the inventory
     */
    public Map<String, List<ItemStack>> getInventory() {
        return inventory;
    }

    /**
     * Gets the health map.
     * @return the health
     */
    public Map<String, Double> getHealth() {
        return health;
    }

    /**
     * Gets the food map.
     * @return the food
     */
    public Map<String, Integer> getFood() {
        return food;
    }

    /**
     * Gets the experience map.
     * @return the exp
     */
    public Map<String, Integer> getExp() {
        return exp;
    }

    /**
     * Gets the location map.
     * @return the location
     */
    public Map<String, Location> getLocation() {
        return location;
    }

    /**
     * Sets the inventory map.
     * @param inventory the inventory to set
     */
    public void setInventory(Map<String, List<ItemStack>> inventory) {
        this.inventory = inventory;
    }

    /**
     * Sets the inventory for a specific world.
     * @param worldname the world name
     * @param inventory the inventory to set
     */
    public void setInventory(String worldname, List<ItemStack> inventory) {
        this.inventory.put(worldname, inventory);
    }

    /**
     * Sets the health map.
     * @param health the health to set
     */
    public void setHealth(Map<String, Double> health) {
        this.health = health;
    }

    /**
     * Sets the food map.
     * @param food the food to set
     */
    public void setFood(Map<String, Integer> food) {
        this.food = food;
    }

    /**
     * Sets the experience map.
     * @param exp the exp to set
     */
    public void setExp(Map<String, Integer> exp) {
        this.exp = exp;
    }

    /**
     * Sets the location map.
     * @param location the location to set
     */
    public void setLocation(Map<String, Location> location) {
        this.location = location;
    }

    /**
     * Sets the health for a specific world.
     * @param overworldName the world name
     * @param health2 the health value to set
     */
    public void setHealth(String overworldName, double health2) {
        this.health.put(overworldName, health2);
    }

    /**
     * Sets the food level for a specific world.
     * @param overworldName the world name
     * @param foodLevel the food level to set
     */
    public void setFood(String overworldName, int foodLevel) {
        this.food.put(overworldName, foodLevel);
    }

    /**
     * Sets the experience for a specific world.
     * @param overworldName the world name
     * @param totalExperience the experience value to set
     */
    public void setExp(String overworldName, int totalExperience) {
        this.exp.put(overworldName, totalExperience);
    }

    /**
     * Gets the money map.
     * @return the money map keyed by world/island key
     */
    public Map<String, Double> getMoney() {
        return money;
    }

    /**
     * Sets the money map.
     * @param money the money map to set
     */
    public void setMoney(Map<String, Double> money) {
        this.money = money;
    }

    /**
     * Gets the money balance for a specific key.
     * @param key the world/island key
     * @return the balance, or null if no balance has been stored for this key
     */
    public Double getMoney(String key) {
        return money == null ? null : money.get(key);
    }

    /**
     * Checks whether a balance has been stored for a specific key.
     * @param key the world/island key
     * @return true if a balance exists for this key
     */
    public boolean hasMoney(String key) {
        return money != null && money.containsKey(key);
    }

    /**
     * Sets the money balance for a specific key.
     * @param key the world/island key
     * @param balance the balance to set
     */
    public void setMoney(String key, double balance) {
        if (this.money == null) {
            this.money = new HashMap<>();
        }
        this.money.put(key, balance);
    }

    /**
     * Gets the last storage key the player was tracked under.
     * @return the last key, or null if never set
     */
    public String getLastKey() {
        return lastKey;
    }

    /**
     * Sets the last storage key the player was tracked under.
     * @param lastKey the last key to set
     */
    public void setLastKey(String lastKey) {
        this.lastKey = lastKey;
    }

    /**
     * @return whether this player's balance has already been imported
     */
    public boolean isImported() {
        return imported;
    }

    /**
     * @param imported whether this player's balance has been imported
     */
    public void setImported(boolean imported) {
        this.imported = imported;
    }

    /**
     * Sets the location for a specific world.
     * @param worldName the world name
     * @param location2 the location to set
     */
    public void setLocation(String worldName, Location location2) {
        this.location.put(worldName, location2);
    }

    /**
     * Gets the inventory for a specific world.
     * @param overworldName the world name
     * @return the inventory list
     */
    public List<ItemStack> getInventory(String overworldName) {
        return inventory == null ? new ArrayList<>() : inventory.getOrDefault(overworldName, new ArrayList<>());
    }

    /**
     * Checks if an inventory exists for a specific world.
     * @param overworldName the world name
     * @return true if inventory exists, false otherwise
     */
    public boolean isInventory(String overworldName) {
        return inventory != null && inventory.containsKey(overworldName);
    }

    /**
     * Sets the game mode for a specific world.
     * @param worldName the world name
     * @param gameMode the game mode to set
     */
    public void setGameMode(String worldName, GameMode gameMode) {
        this.gameMode.put(worldName, gameMode);
    }

    /**
     * Gets the game mode for a specific world.
     * @param worldName the world name
     * @return the game mode, or SURVIVAL if not set
     */
    public GameMode getGameMode(String worldName) {
        return this.gameMode.getOrDefault(worldName, GameMode.SURVIVAL);
    }

    /**
     * Sets an advancement for a specific world.
     * @param worldName the world name
     * @param key the advancement key
     * @param criteria the advancement criteria
     */
    public void setAdvancement(String worldName, String key, List<String> criteria) {
        this.advancements.computeIfAbsent(worldName, k -> new HashMap<>()).put(key, criteria);
    }

    /**
     * Clears advancements for a specific world.
     * @param worldName the world name
     */
    public void clearAdvancement(String worldName) {
        this.advancements.remove(worldName);
    }

    /**
     * Gets the advancements for a specific world.
     * @param worldName the world name
     * @return the advancements map
     */
    public Map<String, List<String>> getAdvancements(String worldName) {
        return advancements.getOrDefault(worldName, Collections.emptyMap());
    }

    /**
     * Gets the Ender Chest inventory for a specific world.
     * @param overworldName the world name
     * @return the Ender Chest inventory list
     */
    public List<ItemStack> getEnderChest(String overworldName) {
        return enderChest == null ? new ArrayList<>() : enderChest.getOrDefault(overworldName, new ArrayList<>());
    }

    /**
     * Sets the Ender Chest inventory for a specific world.
     * @param worldname the world name
     * @param inventory the inventory to set
     */
    public void setEnderChest(String worldname, List<ItemStack> inventory) {
        this.enderChest.put(worldname, inventory);
    }

    /**
     * Gets the Ender Chest inventory map.
     * @return the enderChest map
     */
    public Map<String, List<ItemStack>> getEnderChest() {
        return enderChest;
    }

    /**
     * Sets the Ender Chest inventory map.
     * @param enderChest the enderChest map to set
     */
    public void setEnderChest(Map<String, List<ItemStack>> enderChest) {
        this.enderChest = enderChest;
    }

    /**
     * Clears all statistics for a player for a specific world.
     * @param worldName the world name
     */
    public void clearStats(String worldName) {
        this.blockStats.remove(worldName);
        this.itemStats.remove(worldName);
        this.untypedStats.remove(worldName);
        this.entityStats.remove(worldName);
    }

    /**
     * Gets the untyped statistics for a specific world.
     * @param worldName the world name
     * @return the untypedStats map
     */
    public Map<Statistic, Integer> getUntypedStats(String worldName) {
        return untypedStats.computeIfAbsent(worldName, k -> new EnumMap<>(Statistic.class));
    }

    /**
     * Sets the untyped statistics for a specific world.
     * @param worldName the world name
     * @param untypedStats the untypedStats map to set
     */
    public void setUntypedStats(String worldName, Map<Statistic, Integer> untypedStats) {
        this.untypedStats.put(worldName, untypedStats);
    }

    /**
     * Gets the block statistics for a specific world.
     * @param worldName the world name
     * @return the blockStats map
     */
    public Map<Statistic, Map<Material, Integer>> getBlockStats(String worldName) {
        return blockStats.computeIfAbsent(worldName, k -> new EnumMap<>(Statistic.class));
    }

    /**
     * Sets the block statistics for a specific world.
     * @param worldName the world name
     * @param blockStats the blockStats map to set
     */
    public void setBlockStats(String worldName, Map<Statistic, Map<Material, Integer>> blockStats) {
        this.blockStats.put(worldName, blockStats);
    }

    /**
     * Gets the item statistics for a specific world.
     * @param worldName the world name
     * @return the itemStats map
     */
    public Map<Statistic, Map<Material, Integer>> getItemStats(String worldName) {
        return itemStats.computeIfAbsent(worldName, k -> new EnumMap<>(Statistic.class));
    }

    /**
     * Sets the item statistics for a specific world.
     * @param worldName the world name
     * @param itemStats the itemStats map to set
     */
    public void setItemStats(String worldName, Map<Statistic, Map<Material, Integer>> itemStats) {
        this.itemStats.put(worldName, itemStats);
    }

    /**
     * Gets the entity statistics for a specific world.
     * @param worldName the world name
     * @return the entityStats map
     */
    public Map<Statistic, Map<EntityType, Integer>> getEntityStats(String worldName) {
        return entityStats.computeIfAbsent(worldName, k -> new EnumMap<>(Statistic.class));
    }

    /**
     * Sets the entity statistics for a specific world.
     * @param worldName the world name
     * @param entityStats the entityStats map to set
     */
    public void setEntityStats(String worldName, Map<Statistic, Map<EntityType, Integer>> entityStats) {
        this.entityStats.put(worldName, entityStats);
    }

    /**
     * Clears all data for a specific world key. Used during migration from world-only
     * keys to island-specific keys to prevent data duplication.
     * @param worldName the world name key to clear
     */
    public void clearWorldData(String worldName) {
        this.inventory.remove(worldName);
        this.health.remove(worldName);
        this.food.remove(worldName);
        this.exp.remove(worldName);
        this.location.remove(worldName);
        this.gameMode.remove(worldName);
        this.advancements.remove(worldName);
        this.enderChest.remove(worldName);
        if (this.money != null) {
            this.money.remove(worldName);
        }
        clearStats(worldName);
    }

}