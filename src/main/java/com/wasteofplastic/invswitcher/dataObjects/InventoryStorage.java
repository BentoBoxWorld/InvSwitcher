package com.wasteofplastic.invswitcher.dataObjects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.GameMode;
import org.bukkit.Location;
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
     * @return the advancements
     */
    public Map<String, List<String>> getAdvancements(String worldName) {
        return advancements.getOrDefault(worldName, Collections.emptyMap());
    }


}