package com.wasteofplastic.invswitcher.listeners;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.wasteofplastic.invswitcher.InvSwitcher;
import com.wasteofplastic.invswitcher.Store;

import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Util.class)
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

    /**
     */
    @Before
    public void setUp() {
        // Util
        PowerMockito.mockStatic(Util.class, Mockito.RETURNS_MOCKS);
        when(Util.sameWorld(world, world)).thenReturn(true);
        // Player
        when(player.getWorld()).thenReturn(world);
        // Addon
        when(addon.getStore()).thenReturn(store);
        when(addon.getWorlds()).thenReturn(Set.of(world));
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
        pl.onWorldEnter(event);
        verify(store).storeInventory(any(), any());
        verify(store).getInventory(any(), any());
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
        verify(store).storeAndSave(player, world);
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
        verify(store, never()).storeAndSave(player, world);
        verify(store).removeFromCache(player);
    }

}
