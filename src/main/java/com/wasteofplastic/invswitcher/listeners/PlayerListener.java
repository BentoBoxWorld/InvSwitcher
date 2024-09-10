package com.wasteofplastic.invswitcher.listeners;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.wasteofplastic.invswitcher.InvSwitcher;

import world.bentobox.bentobox.util.Util;

/**
 * Handles all teleportation events, e.g., player teleporting into world
 *
 * @author tastybento
 *
 */
public class PlayerListener implements Listener {

    private final InvSwitcher addon;

    /**
     * @param addon - Add-on
     */
    public PlayerListener(InvSwitcher addon) {
        this.addon = addon;
    }

    /**
     * Loads inventory
     * @param event - event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled=true)
    public void onWorldEnter(final PlayerChangedWorldEvent event) {
        /*
         * 0. From same world (e.g., nether/end) to same world.
         * 1. From non-game world to non-game world
         * 2. From non-game world to game world
         * 3. From game world to non-game world
         * 4. From game world to another game world
         *
         */
        World from = event.getFrom();
        World to = event.getPlayer().getWorld();
        if (Util.sameWorld(to, from) || (!addon.getWorlds().contains(from) && !addon.getWorlds().contains(to))) {
            return;
        }
        addon.getStore().storeInventory(event.getPlayer(), from);
        addon.getStore().getInventory(event.getPlayer(), to);
    }


    /**
     * Loads inventory
     * @param event - event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled=true)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        if (addon.getWorlds().contains(event.getPlayer().getWorld()) && addon.getStore().isWorldStored(event.getPlayer(), event.getPlayer().getWorld())) {
            addon.getStore().getInventory(event.getPlayer(), event.getPlayer().getWorld());
        }
    }

    /**
     * Saves inventory
     * @param event - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        if (addon.getWorlds().contains(event.getPlayer().getWorld())) {
            addon.getStore().storeAndSave(event.getPlayer(), event.getPlayer().getWorld(), false);
        }
        addon.getStore().removeFromCache(event.getPlayer());
    }


}
