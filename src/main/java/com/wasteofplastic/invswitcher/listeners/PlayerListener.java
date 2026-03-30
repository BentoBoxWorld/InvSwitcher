package com.wasteofplastic.invswitcher.listeners;

import java.util.Objects;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.entity.Player;

import com.wasteofplastic.invswitcher.InvSwitcher;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.events.island.IslandEnterEvent;
import world.bentobox.bentobox.database.objects.Island;
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
     * Handles inventory switching when a player enters an island they own.
     * Only triggers when per-island switching is enabled and the player owns multiple islands.
     * @param event - event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onIslandEnter(IslandEnterEvent event) {
        BentoBox.getInstance().logDebug("IslandEnterEvent triggered for player " + event.getPlayerUUID() + " on island " + event.getIsland().getUniqueId());
        if (!addon.getSettings().isIslands()) {
            return;
        }

        Player player = Bukkit.getPlayer(event.getPlayerUUID());
        if (player == null) {
            return;
        }

        World world = player.getWorld();
        if (!addon.getWorlds().contains(world)) {
            BentoBox.getInstance().logDebug("World " + world.getName() + " is not in the list of worlds to manage. Ignoring island enter event.");
            return;
        }

        Island island = event.getIsland();

        // Only switch if the player owns the island they're entering
        if (island.getOwner() == null || !island.getOwner().equals(player.getUniqueId())) {
            BentoBox.getInstance().logDebug("Player " + player.getName() + " does not own island " + island.getUniqueId() + ". No inventory switch.");
            return;
        }

        // Only switch if player owns multiple islands (otherwise key is the same)
        World overworld = Util.getWorld(world);
        int count = addon.getIslands().getNumberOfConcurrentIslands(
                player.getUniqueId(), Objects.requireNonNull(overworld));
        if (count <= 1) {
            BentoBox.getInstance().logDebug("Player " + player.getName() + " owns only one island in world " + overworld.getName() + ". No inventory switch.");
            return;
        }

        // Compute new key and compare to current key
        String newKey = addon.getStore().getStorageKey(player, world, island);
        String currentKeyValue = addon.getStore().getCurrentKey(player);
        if (newKey.equals(currentKeyValue)) {
            BentoBox.getInstance().logDebug("Player " + player.getName() + " is entering island " + island.getUniqueId() + " which has the same inventory key as their current location. No inventory switch.");
            return; // same island, no switch
        }

        // Switch: store old, load new
        BentoBox.getInstance().logDebug("Switching inventory for player " + player.getName() + " from key " + currentKeyValue + " to new key " + newKey);
        addon.getStore().storeInventory(player, world);
        addon.getStore().getInventory(player, world, island);
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
     * Handles inventory switching when a player respawns on a different island they own.
     * @param event - event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!addon.getSettings().isIslands()) {
            return;
        }

        Player player = event.getPlayer();
        World world = event.getRespawnLocation().getWorld();
        if (world == null || !addon.getWorlds().contains(world)) {
            return;
        }

        // Determine which island the respawn location is on
        Optional<Island> islandOpt = addon.getIslands().getIslandAt(event.getRespawnLocation());
        if (islandOpt.isEmpty()) {
            return;
        }

        Island island = islandOpt.get();
        String newKey = addon.getStore().getStorageKey(player, world, island);
        String currentKeyValue = addon.getStore().getCurrentKey(player);

        if (currentKeyValue != null && !newKey.equals(currentKeyValue)) {
            // Player died on one island, respawning on another they own.
            // Save the post-death state (empty inventory, etc.) to the old island key.
            addon.getStore().storeAndSave(player, world, false);
            // Load the respawn island's inventory
            addon.getStore().getInventory(player, world, island);
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
