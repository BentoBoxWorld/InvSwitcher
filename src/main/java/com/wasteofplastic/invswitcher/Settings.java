package com.wasteofplastic.invswitcher;

import java.util.HashSet;
import java.util.Set;

import world.bentobox.bentobox.api.configuration.ConfigComment;
import world.bentobox.bentobox.api.configuration.ConfigEntry;
import world.bentobox.bentobox.api.configuration.ConfigObject;
import world.bentobox.bentobox.api.configuration.StoreAt;

@StoreAt(filename = "config.yml", path = "addons/InvSwitcher")
public class Settings implements ConfigObject {

    @ConfigComment("InvSwitcher Config")
    @ConfigComment("Worlds to operate. Nether and End worlds are automatically included.")
    @ConfigEntry(path = "worlds")
    private Set<String> worlds = new HashSet<>();

    @ConfigComment("")
    @ConfigComment("Per-world settings. Gamemode means Survivial, Creative, etc.")
    @ConfigEntry(path = "options.inventory")
    private boolean inventory = true;
    @ConfigEntry(path = "options.health")
    private boolean health = true;
    @ConfigEntry(path = "options.food")
    private boolean food = true;
    @ConfigEntry(path = "options.advancements")
    private boolean advancements = true;
    @ConfigEntry(path = "options.gamemode")
    private boolean gamemode = true;
    @ConfigEntry(path = "options.experience")
    private boolean experience = true;
    @ConfigEntry(path = "options.ender-chest")
    private boolean enderChest = true;
    @ConfigEntry(path = "options.statistics")
    private boolean statistics = true;

    @ConfigComment("Switch inventories based on island. Only applies if players own more than one island.")
    @ConfigComment("Each sub-option controls whether that aspect is switched per-island.")
    @ConfigComment("The world-level option must also be true for the island option to have any effect.")
    @ConfigEntry(path = "options.islands.active")
    private boolean islandsActive = true;
    @ConfigEntry(path = "options.islands.inventory")
    private boolean islandsInventory = true;
    @ConfigEntry(path = "options.islands.health")
    private boolean islandsHealth = false;
    @ConfigEntry(path = "options.islands.food")
    private boolean islandsFood = false;
    @ConfigEntry(path = "options.islands.advancements")
    private boolean islandsAdvancements = false;
    @ConfigEntry(path = "options.islands.gamemode")
    private boolean islandsGamemode = false;
    @ConfigEntry(path = "options.islands.experience")
    private boolean islandsExperience = false;
    @ConfigEntry(path = "options.islands.ender-chest")
    private boolean islandsEnderChest = true;
    @ConfigEntry(path = "options.islands.statistics")
    private boolean islandsStatistics = false;

    /**
     * @return the worlds
     */
    public Set<String> getWorlds() {
        return worlds;
    }
    /**
     * @param worlds the worlds to set
     */
    public void setWorlds(Set<String> worlds) {
        this.worlds = worlds;
    }
    /**
     * @return the inventory
     */
    public boolean isInventory() {
        return inventory;
    }
    /**
     * @param inventory the inventory to set
     */
    public void setInventory(boolean inventory) {
        this.inventory = inventory;
    }
    /**
     * @return the health
     */
    public boolean isHealth() {
        return health;
    }
    /**
     * @param health the health to set
     */
    public void setHealth(boolean health) {
        this.health = health;
    }
    /**
     * @return the food
     */
    public boolean isFood() {
        return food;
    }
    /**
     * @param food the food to set
     */
    public void setFood(boolean food) {
        this.food = food;
    }
    /**
     * @return the advancements
     */
    public boolean isAdvancements() {
        return advancements;
    }
    /**
     * @param advancements the advancements to set
     */
    public void setAdvancements(boolean advancements) {
        this.advancements = advancements;
    }
    /**
     * @return the gamemode
     */
    public boolean isGamemode() {
        return gamemode;
    }
    /**
     * @param gamemode the gamemode to set
     */
    public void setGamemode(boolean gamemode) {
        this.gamemode = gamemode;
    }
    /**
     * @return the experience
     */
    public boolean isExperience() {
        return experience;
    }
    /**
     * @param experience the experience to set
     */
    public void setExperience(boolean experience) {
        this.experience = experience;
    }
    /**
     * @return the enderChest
     */
    public boolean isEnderChest() {
        return enderChest;
    }
    /**
     * @param enderChest the enderChest to set
     */
    public void setEnderChest(boolean enderChest) {
        this.enderChest = enderChest;
    }
    /**
     * @return the statistics
     */
    public boolean isStatistics() {
        return statistics;
    }
    /**
     * @param statistics the statistics to set
     */
    public void setStatistics(boolean statistics) {
        this.statistics = statistics;
    }
    /**
     * @return whether per-island switching is active
     */
    public boolean isIslandsActive() {
        return islandsActive;
    }
    /**
     * @param islandsActive whether to enable per-island switching
     */
    public void setIslandsActive(boolean islandsActive) {
        this.islandsActive = islandsActive;
    }
    public boolean isIslandsInventory() {
        return islandsInventory;
    }
    public void setIslandsInventory(boolean islandsInventory) {
        this.islandsInventory = islandsInventory;
    }
    public boolean isIslandsHealth() {
        return islandsHealth;
    }
    public void setIslandsHealth(boolean islandsHealth) {
        this.islandsHealth = islandsHealth;
    }
    public boolean isIslandsFood() {
        return islandsFood;
    }
    public void setIslandsFood(boolean islandsFood) {
        this.islandsFood = islandsFood;
    }
    public boolean isIslandsAdvancements() {
        return islandsAdvancements;
    }
    public void setIslandsAdvancements(boolean islandsAdvancements) {
        this.islandsAdvancements = islandsAdvancements;
    }
    public boolean isIslandsGamemode() {
        return islandsGamemode;
    }
    public void setIslandsGamemode(boolean islandsGamemode) {
        this.islandsGamemode = islandsGamemode;
    }
    public boolean isIslandsExperience() {
        return islandsExperience;
    }
    public void setIslandsExperience(boolean islandsExperience) {
        this.islandsExperience = islandsExperience;
    }
    public boolean isIslandsEnderChest() {
        return islandsEnderChest;
    }
    public void setIslandsEnderChest(boolean islandsEnderChest) {
        this.islandsEnderChest = islandsEnderChest;
    }
    public boolean isIslandsStatistics() {
        return islandsStatistics;
    }
    public void setIslandsStatistics(boolean islandsStatistics) {
        this.islandsStatistics = islandsStatistics;
    }

}
