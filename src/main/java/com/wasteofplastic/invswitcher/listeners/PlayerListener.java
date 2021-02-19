package com.wasteofplastic.invswitcher.listeners;

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
        if (!Util.sameWorld(event.getPlayer().getWorld(), event.getFrom())) {
            addon.getStore().getInventory(event.getPlayer(), event.getPlayer().getWorld());
        }
    }

    /**
     * Saves inventory
     * @param event - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onWorldExit(final PlayerChangedWorldEvent event) {
        if (!Util.sameWorld(event.getPlayer().getWorld(), event.getFrom())) {
            addon.getStore().storeInventory(event.getPlayer(), event.getFrom());
        }
    }


    /**
     * Loads inventory
     * @param event - event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled=true)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        addon.getStore().getInventory(event.getPlayer(), event.getPlayer().getWorld());
    }

    /**
     * Saves inventory
     * @param event - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        addon.getStore().storeAndSave(event.getPlayer(), event.getPlayer().getWorld());
        addon.getStore().removeFromCache(event.getPlayer());
    }


}
