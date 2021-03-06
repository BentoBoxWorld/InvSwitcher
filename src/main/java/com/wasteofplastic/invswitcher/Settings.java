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
    @ConfigEntry(path = "options.location")
    private boolean location = true;
    @ConfigEntry(path = "options.ender-chest")
    private boolean enderChest = true;
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
     * @return the location
     */
    public boolean isLocation() {
        return location;
    }
    /**
     * @param location the location to set
     */
    public void setLocation(boolean location) {
        this.location = location;
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


}
