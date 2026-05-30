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

import world.bentobox.bentobox.api.events.island.IslandEnterEvent;
import world.bentobox.bentobox.api.events.player.PlayerBaseEvent;
import world.bentobox.bentobox.api.events.player.PlayerResetEnderChestEvent;
import world.bentobox.bentobox.api.events.player.PlayerResetExpEvent;
import world.bentobox.bentobox.api.events.player.PlayerResetHealthEvent;
import world.bentobox.bentobox.api.events.player.PlayerResetHungerEvent;
import world.bentobox.bentobox.api.events.player.PlayerResetInventoryEvent;
import world.bentobox.bentobox.api.events.player.PlayerResetMoneyEvent;
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
        if (!addon.getSettings().isIslandsActive()) {
            return;
        }

        Player player = Bukkit.getPlayer(event.getPlayerUUID());
        if (player == null) {
            return;
        }

        World world = player.getWorld();
        if (!addon.getWorlds().contains(world)) {
            return;
        }

        Island island = event.getIsland();

        // Only switch if the player owns the island they're entering
        if (island.getOwner() == null || !island.getOwner().equals(player.getUniqueId())) {
            return;
        }

        // Only switch if player owns multiple islands (otherwise key is the same)
        World overworld = Util.getWorld(world);
        int count = addon.getIslands().getNumberOfConcurrentIslands(
                player.getUniqueId(), Objects.requireNonNull(overworld));
        if (count <= 1) {
            return;
        }

        // Compute new key and compare to current key
        String newKey = addon.getStore().getStorageKey(player, world, island);
        String currentKeyValue = addon.getStore().getCurrentKey(player);
        if (newKey.equals(currentKeyValue)) {
            return; // same island, no switch
        }

        // If currentKey is a world-only key (no "/"), the player is transitioning from
        // single-island to multi-island mode. Upgrade the key so storeInventory saves to
        // the correct island-specific key instead of the world key.
        if (currentKeyValue != null && !currentKeyValue.contains("/")) {
            Island oldIsland = addon.getIslands().getIsland(overworld, player.getUniqueId());
            if (oldIsland != null && !oldIsland.getUniqueId().equals(island.getUniqueId())) {
                addon.getStore().upgradeWorldKeyToIsland(player, world, oldIsland);
            } else {
                // Primary island is the one being entered; find another owned island
                addon.getIslands().getIslands(overworld, player.getUniqueId()).stream()
                        .filter(i -> !i.getUniqueId().equals(island.getUniqueId()))
                        .findFirst()
                        .ifPresent(i -> addon.getStore().upgradeWorldKeyToIsland(player, world, i));
            }
        }

        // Switch: store old, load new
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
        if (!addon.getSettings().isIslandsActive()) {
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

    /**
     * Intercepts BentoBox's inventory reset when the player is not in the BentoBox world.
     * Cancels the direct clear and instead wipes the stored inventory data for that world.
     * @param event - event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerResetInventory(PlayerResetInventoryEvent event) {
        if (!shouldInterceptPlayerReset(event)) return;
        event.setCancelled(true);
        Player player = Bukkit.getPlayer(event.getPlayerUUID());
        if (player != null) {
            addon.getStore().clearStoredInventoryForWorld(player, event.getWorld(), event.getIsland());
        }
    }

    /**
     * Intercepts BentoBox's ender chest reset when the player is not in the BentoBox world.
     * Cancels the direct clear and instead wipes the stored ender chest data for that world.
     * @param event - event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerResetEnderChest(PlayerResetEnderChestEvent event) {
        if (!shouldInterceptPlayerReset(event)) return;
        event.setCancelled(true);
        Player player = Bukkit.getPlayer(event.getPlayerUUID());
        if (player != null) {
            addon.getStore().clearStoredEnderChestForWorld(player, event.getWorld(), event.getIsland());
        }
    }

    /**
     * Intercepts BentoBox's experience reset when the player is not in the BentoBox world.
     * Cancels the direct clear and instead zeroes the stored experience for that world.
     * @param event - event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerResetExp(PlayerResetExpEvent event) {
        if (!shouldInterceptPlayerReset(event)) return;
        event.setCancelled(true);
        Player player = Bukkit.getPlayer(event.getPlayerUUID());
        if (player != null) {
            addon.getStore().clearStoredExpForWorld(player, event.getWorld(), event.getIsland());
        }
    }

    /**
     * Intercepts BentoBox's health reset when the player is not in the BentoBox world.
     * Cancels the direct reset and instead removes the stored health for that world
     * (so the player receives max health when they next enter the world).
     * @param event - event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerResetHealth(PlayerResetHealthEvent event) {
        if (!shouldInterceptPlayerReset(event)) return;
        event.setCancelled(true);
        Player player = Bukkit.getPlayer(event.getPlayerUUID());
        if (player != null) {
            addon.getStore().clearStoredHealthForWorld(player, event.getWorld(), event.getIsland());
        }
    }

    /**
     * Intercepts BentoBox's hunger reset when the player is not in the BentoBox world.
     * Cancels the direct reset and instead sets stored food to full (20) for that world.
     * @param event - event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerResetHunger(PlayerResetHungerEvent event) {
        if (!shouldInterceptPlayerReset(event)) return;
        event.setCancelled(true);
        Player player = Bukkit.getPlayer(event.getPlayerUUID());
        if (player != null) {
            addon.getStore().clearStoredFoodForWorld(player, event.getWorld(), event.getIsland());
        }
    }

    /**
     * Intercepts BentoBox's money reset when the player is not in the BentoBox world. BentoBox's
     * default reset reads the player's <em>current</em> world balance and withdraws it from the
     * event world, which is wrong when the player is elsewhere. Instead, cancel it and zero the
     * stored balance for the event world directly. When the player is in the event world the
     * reset is left to BentoBox, which routes correctly through InvSwitcher's economy.
     * @param event - event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerResetMoney(PlayerResetMoneyEvent event) {
        if (!addon.getSettings().isMoney()) {
            return;
        }
        if (!shouldInterceptPlayerReset(event)) {
            return;
        }
        event.setCancelled(true);
        Player player = Bukkit.getPlayer(event.getPlayerUUID());
        if (player != null) {
            addon.getStore().clearStoredMoneyForWorld(player, event.getWorld(), event.getIsland());
        }
    }

    /**
     * Determines whether InvSwitcher should intercept a BentoBox player reset event.
     * Returns true if the event's world is managed by InvSwitcher, the player is online,
     * and the player is currently in a different world (not the event world).
     * @param event - the reset event
     * @return true if InvSwitcher should cancel the event and handle it itself
     */
    private boolean shouldInterceptPlayerReset(PlayerBaseEvent event) {
        World eventWorld = event.getWorld();
        if (!addon.getWorlds().contains(eventWorld)) {
            return false;
        }
        Player player = Bukkit.getPlayer(event.getPlayerUUID());
        if (player == null) {
            return false;
        }
        // Only intercept if the player is not currently in the event world
        return !Util.sameWorld(player.getWorld(), eventWorld);
    }

}
