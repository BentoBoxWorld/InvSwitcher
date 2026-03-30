package com.wasteofplastic.invswitcher.listeners;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.wasteofplastic.invswitcher.InvSwitcher;
import com.wasteofplastic.invswitcher.Settings;
import com.wasteofplastic.invswitcher.Store;

import world.bentobox.bentobox.api.events.island.IslandEnterEvent;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class PlayerListenerTest {

    @Mock
    private InvSwitcher addon;
    private PlayerListener pl;
    @Mock
    private Store store;
    @Mock
    private Player player;
    @Mock
    private World world;
    @Mock
    private World notWorld;
    @Mock
    private Settings settings;
    @Mock
    private IslandsManager islandsManager;

    private UUID playerUUID;

    /**
     */
    @Before
    public void setUp() {
        playerUUID = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUUID);
        // Util
        // Mock the static method
        try (MockedStatic<Util> mockedBukkit = mockStatic(Util.class, Mockito.RETURNS_MOCKS)) {
            when(Util.sameWorld(any(), any())).thenReturn(true);
        }
        when(world.getName()).thenReturn("world");
        // Player
        when(player.getWorld()).thenReturn(world);
        // Addon
        when(addon.getStore()).thenReturn(store);
        when(addon.getWorlds()).thenReturn(Set.of(world));
        when(addon.getSettings()).thenReturn(settings);
        when(addon.getIslands()).thenReturn(islandsManager);
        pl = new PlayerListener(addon);
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.listeners.PlayerListener#PlayerListener(com.wasteofplastic.invswitcher.InvSwitcher)}.
     */
    @Test
    public void testPlayerListener() {
        assertNotNull(pl);
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.listeners.PlayerListener#onWorldEnter(org.bukkit.event.player.PlayerChangedWorldEvent)}.
     */
    @Test
    public void testOnWorldEnterSameWorld() {
        PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(player, world);
        pl.onWorldEnter(event);
        verify(store, never()).storeInventory(any(), any());
        verify(store, never()).getInventory(any(), any());
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.listeners.PlayerListener#onWorldEnter(org.bukkit.event.player.PlayerChangedWorldEvent)}.
     */
    @Test
    public void testOnWorldEnterDifferentWorld() {
        PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(player, notWorld);
        // Mock the static method
        try (MockedStatic<Util> mockedBukkit = mockStatic(Util.class, Mockito.RETURNS_MOCKS)) {
            when(Util.sameWorld(world, world)).thenReturn(true);
            pl.onWorldEnter(event);
            verify(store).storeInventory(any(), any());
            verify(store).getInventory(any(), any());
        }
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.listeners.PlayerListener#onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent)}.
     */
    @Test
    public void testOnPlayerJoin() {
        PlayerJoinEvent event = new PlayerJoinEvent(player, "");
        pl.onPlayerJoin(event);
        // No storage yet
        verify(store, never()).getInventory(any(), any());
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.listeners.PlayerListener#onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent)}.
     */
    @Test
    public void testOnPlayerJoinNonHandledWorld() {
        when(player.getWorld()).thenReturn(notWorld);
        PlayerJoinEvent event = new PlayerJoinEvent(player, "");
        pl.onPlayerJoin(event);
        // No storage yet
        verify(store, never()).getInventory(any(), any());
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.listeners.PlayerListener#onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent)}.
     */
    @Test
    public void testOnPlayerJoinWithStorage() {
        testOnWorldEnterDifferentWorld();
        when(player.getWorld()).thenReturn(notWorld);
        PlayerJoinEvent event = new PlayerJoinEvent(player, "");
        pl.onPlayerJoin(event);
        // No storage yet
        verify(store).getInventory(any(), any());
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.listeners.PlayerListener#onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent)}.
     */
    @Test
    public void testOnPlayerQuit() {
        PlayerQuitEvent event = new PlayerQuitEvent(player, "");
        pl.onPlayerQuit(event);
        verify(store).storeAndSave(player, world, false);
        verify(store).removeFromCache(player);
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.listeners.PlayerListener#onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent)}.
     */
    @Test
    public void testOnPlayerQuitNotCoveredWorld() {
        when(player.getWorld()).thenReturn(notWorld);
        PlayerQuitEvent event = new PlayerQuitEvent(player, "");
        pl.onPlayerQuit(event);
        verify(store, never()).storeAndSave(player, world, false);
        verify(store).removeFromCache(player);
    }

    // --- Island Enter Event Tests ---

    @Test
    public void testOnIslandEnterDisabled() {
        when(settings.isIslands()).thenReturn(false);
        Island island = mock(Island.class);
        IslandEnterEvent event = new IslandEnterEvent(island, playerUUID, false, null, island, null);
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class)) {
            mockedBukkit.when(() -> Bukkit.getPlayer(playerUUID)).thenReturn(player);
            pl.onIslandEnter(event);
        }
        verify(store, never()).storeInventory(any(), any());
    }

    @Test
    public void testOnIslandEnterNotOwner() {
        when(settings.isIslands()).thenReturn(true);
        Island island = mock(Island.class);
        UUID otherOwner = UUID.randomUUID();
        when(island.getOwner()).thenReturn(otherOwner);
        IslandEnterEvent event = new IslandEnterEvent(island, playerUUID, false, null, island, null);
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class)) {
            mockedBukkit.when(() -> Bukkit.getPlayer(playerUUID)).thenReturn(player);
            pl.onIslandEnter(event);
        }
        verify(store, never()).storeInventory(any(), any());
    }

    @Test
    public void testOnIslandEnterSingleIsland() {
        when(settings.isIslands()).thenReturn(true);
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(playerUUID);

        IslandEnterEvent event = new IslandEnterEvent(island, playerUUID, false, null, island, null);
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class);
             MockedStatic<Util> utilities = mockStatic(Util.class)) {
            mockedBukkit.when(() -> Bukkit.getPlayer(playerUUID)).thenReturn(player);
            utilities.when(() -> Util.getWorld(world)).thenReturn(world);
            when(islandsManager.getNumberOfConcurrentIslands(playerUUID, world)).thenReturn(1);
            pl.onIslandEnter(event);
        }
        verify(store, never()).storeInventory(any(), any());
    }

    @Test
    public void testOnIslandEnterMultipleIslandsSameKey() {
        when(settings.isIslands()).thenReturn(true);
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(playerUUID);
        when(island.getUniqueId()).thenReturn("island-1");

        // Current key already matches
        when(store.getStorageKey(player, world, island)).thenReturn("world/island-1");
        when(store.getCurrentKey(player)).thenReturn("world/island-1");

        IslandEnterEvent event = new IslandEnterEvent(island, playerUUID, false, null, island, null);
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class);
             MockedStatic<Util> utilities = mockStatic(Util.class)) {
            mockedBukkit.when(() -> Bukkit.getPlayer(playerUUID)).thenReturn(player);
            utilities.when(() -> Util.getWorld(world)).thenReturn(world);
            when(islandsManager.getNumberOfConcurrentIslands(playerUUID, world)).thenReturn(2);
            pl.onIslandEnter(event);
        }
        // Same key, no switch
        verify(store, never()).storeInventory(any(), any());
    }

    @Test
    public void testOnIslandEnterMultipleIslandsDifferentKey() {
        when(settings.isIslands()).thenReturn(true);
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(playerUUID);
        when(island.getUniqueId()).thenReturn("island-2");

        when(store.getStorageKey(player, world, island)).thenReturn("world/island-2");
        when(store.getCurrentKey(player)).thenReturn("world/island-1");

        IslandEnterEvent event = new IslandEnterEvent(island, playerUUID, false, null, island, null);
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class);
             MockedStatic<Util> utilities = mockStatic(Util.class)) {
            mockedBukkit.when(() -> Bukkit.getPlayer(playerUUID)).thenReturn(player);
            utilities.when(() -> Util.getWorld(world)).thenReturn(world);
            when(islandsManager.getNumberOfConcurrentIslands(playerUUID, world)).thenReturn(2);
            pl.onIslandEnter(event);
        }
        // Different key, switch should happen
        verify(store).storeInventory(player, world);
        verify(store).getInventory(player, world, island);
    }

    // --- Respawn Event Tests ---

    @Test
    public void testOnPlayerRespawnDisabled() {
        when(settings.isIslands()).thenReturn(false);
        Location respawnLoc = mock(Location.class);
        when(respawnLoc.getWorld()).thenReturn(world);
        PlayerRespawnEvent event = new PlayerRespawnEvent(player, respawnLoc, false);
        pl.onPlayerRespawn(event);
        verify(store, never()).storeAndSave(any(), any(), any(boolean.class));
    }

    @Test
    public void testOnPlayerRespawnSameIsland() {
        when(settings.isIslands()).thenReturn(true);
        Location respawnLoc = mock(Location.class);
        when(respawnLoc.getWorld()).thenReturn(world);

        Island island = mock(Island.class);
        when(islandsManager.getIslandAt(respawnLoc)).thenReturn(Optional.of(island));
        when(store.getStorageKey(player, world, island)).thenReturn("world/island-1");
        when(store.getCurrentKey(player)).thenReturn("world/island-1");

        PlayerRespawnEvent event = new PlayerRespawnEvent(player, respawnLoc, false);
        pl.onPlayerRespawn(event);
        // Same island, no switch
        verify(store, never()).storeAndSave(any(), any(), any(boolean.class));
    }

    @Test
    public void testOnPlayerRespawnDifferentIsland() {
        when(settings.isIslands()).thenReturn(true);
        Location respawnLoc = mock(Location.class);
        when(respawnLoc.getWorld()).thenReturn(world);

        Island island = mock(Island.class);
        when(islandsManager.getIslandAt(respawnLoc)).thenReturn(Optional.of(island));
        when(store.getStorageKey(player, world, island)).thenReturn("world/island-2");
        when(store.getCurrentKey(player)).thenReturn("world/island-1");

        PlayerRespawnEvent event = new PlayerRespawnEvent(player, respawnLoc, false);
        pl.onPlayerRespawn(event);
        // Different island, switch should happen
        verify(store).storeAndSave(player, world, false);
        verify(store).getInventory(player, world, island);
    }

    @Test
    public void testOnPlayerRespawnNoIsland() {
        when(settings.isIslands()).thenReturn(true);
        Location respawnLoc = mock(Location.class);
        when(respawnLoc.getWorld()).thenReturn(world);
        when(islandsManager.getIslandAt(respawnLoc)).thenReturn(Optional.empty());

        PlayerRespawnEvent event = new PlayerRespawnEvent(player, respawnLoc, false);
        pl.onPlayerRespawn(event);
        // No island at respawn, no switch
        verify(store, never()).storeAndSave(any(), any(), any(boolean.class));
    }

}
