package com.wasteofplastic.invswitcher.listeners;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.wasteofplastic.invswitcher.InvSwitcher;
import com.wasteofplastic.invswitcher.Settings;
import com.wasteofplastic.invswitcher.Store;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.events.island.IslandEnterEvent;
import world.bentobox.bentobox.api.events.player.PlayerResetEnderChestEvent;
import world.bentobox.bentobox.api.events.player.PlayerResetExpEvent;
import world.bentobox.bentobox.api.events.player.PlayerResetHealthEvent;
import world.bentobox.bentobox.api.events.player.PlayerResetHungerEvent;
import world.bentobox.bentobox.api.events.player.PlayerResetInventoryEvent;
import world.bentobox.bentobox.api.events.player.PlayerResetMoneyEvent;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlayerListenerTest {

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
    @Mock
    private Island island;

    private UUID playerUUID;
    private MockedStatic<BentoBox> mockedBentoBox;

    /**
     */
    @BeforeEach
    void setUp() {
        // BentoBox static mock (needed for logDebug calls in PlayerListener)
        BentoBox bbPlugin = mock(BentoBox.class);
        mockedBentoBox = Mockito.mockStatic(BentoBox.class);
        mockedBentoBox.when(BentoBox::getInstance).thenReturn(bbPlugin);
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

    @AfterEach
    void tearDown() {
        if (mockedBentoBox != null) {
            mockedBentoBox.close();
        }
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.listeners.PlayerListener#PlayerListener(com.wasteofplastic.invswitcher.InvSwitcher)}.
     */
    @Test
    void testPlayerListener() {
        assertNotNull(pl);
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.listeners.PlayerListener#onWorldEnter(org.bukkit.event.player.PlayerChangedWorldEvent)}.
     */
    @Test
    void testOnWorldEnterSameWorld() {
        PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(player, world);
        pl.onWorldEnter(event);
        verify(store, never()).storeInventory(any(), any());
        verify(store, never()).getInventory(any(), any());
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.listeners.PlayerListener#onWorldEnter(org.bukkit.event.player.PlayerChangedWorldEvent)}.
     */
    @Test
    void testOnWorldEnterDifferentWorld() {
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
    void testOnPlayerJoin() {
        PlayerJoinEvent event = new PlayerJoinEvent(player, "");
        pl.onPlayerJoin(event);
        // No storage yet
        verify(store, never()).getInventory(any(), any());
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.listeners.PlayerListener#onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent)}.
     */
    @Test
    void testOnPlayerJoinNonHandledWorld() {
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
    void testOnPlayerJoinWithStorage() {
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
    void testOnPlayerQuit() {
        PlayerQuitEvent event = new PlayerQuitEvent(player, "");
        pl.onPlayerQuit(event);
        verify(store).storeAndSave(player, world, false);
        verify(store).removeFromCache(player);
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.listeners.PlayerListener#onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent)}.
     */
    @Test
    void testOnPlayerQuitNotCoveredWorld() {
        when(player.getWorld()).thenReturn(notWorld);
        PlayerQuitEvent event = new PlayerQuitEvent(player, "");
        pl.onPlayerQuit(event);
        verify(store, never()).storeAndSave(player, world, false);
        verify(store).removeFromCache(player);
    }

    // --- Island Enter Event Tests ---

    @Test
    void testOnIslandEnterDisabled() {
        when(settings.isIslandsActive()).thenReturn(false);
        IslandEnterEvent event = new IslandEnterEvent(island, playerUUID, false, null, island, null);
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class)) {
            mockedBukkit.when(() -> Bukkit.getPlayer(playerUUID)).thenReturn(player);
            pl.onIslandEnter(event);
        }
        verify(store, never()).storeInventory(any(), any());
    }

    @Test
    void testOnIslandEnterNotOwner() {
        when(settings.isIslandsActive()).thenReturn(true);
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
    void testOnIslandEnterSingleIsland() {
        when(settings.isIslandsActive()).thenReturn(true);
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
    void testOnIslandEnterMultipleIslandsSameKey() {
        when(settings.isIslandsActive()).thenReturn(true);
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
    void testOnIslandEnterMultipleIslandsDifferentKey() {
        when(settings.isIslandsActive()).thenReturn(true);
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
    void testOnPlayerRespawnDisabled() {
        when(settings.isIslandsActive()).thenReturn(false);
        Location respawnLoc = mock(Location.class);
        when(respawnLoc.getWorld()).thenReturn(world);
        PlayerRespawnEvent event = mock(PlayerRespawnEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getRespawnLocation()).thenReturn(respawnLoc);
        pl.onPlayerRespawn(event);
        verify(store, never()).storeAndSave(any(), any(), any(boolean.class));
    }

    @Test
    void testOnPlayerRespawnSameIsland() {
        when(settings.isIslandsActive()).thenReturn(true);
        Location respawnLoc = mock(Location.class);
        when(respawnLoc.getWorld()).thenReturn(world);

        when(islandsManager.getIslandAt(respawnLoc)).thenReturn(Optional.of(island));
        when(store.getStorageKey(player, world, island)).thenReturn("world/island-1");
        when(store.getCurrentKey(player)).thenReturn("world/island-1");

        PlayerRespawnEvent event = mock(PlayerRespawnEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getRespawnLocation()).thenReturn(respawnLoc);
        pl.onPlayerRespawn(event);
        // Same island, no switch
        verify(store, never()).storeAndSave(any(), any(), any(boolean.class));
    }

    @Test
    void testOnPlayerRespawnDifferentIsland() {
        when(settings.isIslandsActive()).thenReturn(true);
        Location respawnLoc = mock(Location.class);
        when(respawnLoc.getWorld()).thenReturn(world);

        when(islandsManager.getIslandAt(respawnLoc)).thenReturn(Optional.of(island));
        when(store.getStorageKey(player, world, island)).thenReturn("world/island-2");
        when(store.getCurrentKey(player)).thenReturn("world/island-1");

        PlayerRespawnEvent event = mock(PlayerRespawnEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getRespawnLocation()).thenReturn(respawnLoc);
        pl.onPlayerRespawn(event);
        // Different island, switch should happen
        verify(store).storeAndSave(player, world, false);
        verify(store).getInventory(player, world, island);
    }

    @Test
    void testOnPlayerRespawnNoIsland() {
        when(settings.isIslandsActive()).thenReturn(true);
        Location respawnLoc = mock(Location.class);
        when(respawnLoc.getWorld()).thenReturn(world);
        when(islandsManager.getIslandAt(respawnLoc)).thenReturn(Optional.empty());

        PlayerRespawnEvent event = mock(PlayerRespawnEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getRespawnLocation()).thenReturn(respawnLoc);
        pl.onPlayerRespawn(event);
        // No island at respawn, no switch
        verify(store, never()).storeAndSave(any(), any(), any(boolean.class));
    }

    // --- Player Reset Event Tests ---

    /**
     * When the event world is not managed by InvSwitcher, the event should not be intercepted
     * and the store clear methods should not be called.
     */
    @Test
    void testOnPlayerResetInventoryWorldNotManaged() {
        // notWorld is not in the addon's worlds set
        when(addon.getWorlds()).thenReturn(Set.of(world)); // only 'world' is managed
        PlayerResetInventoryEvent event = new PlayerResetInventoryEvent(notWorld, island, playerUUID);
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class)) {
            mockedBukkit.when(() -> Bukkit.getPlayer(playerUUID)).thenReturn(player);
            pl.onPlayerResetInventory(event);
        }
        assertFalse(event.isCancelled(), "Event should not be cancelled when world is not managed");
        verify(store, never()).clearStoredInventoryForWorld(any(), any(), any());
    }

    /**
     * When the player is offline, the event should not be intercepted.
     */
    @Test
    void testOnPlayerResetInventoryPlayerOffline() {
        PlayerResetInventoryEvent event = new PlayerResetInventoryEvent(world, island, playerUUID);
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class)) {
            mockedBukkit.when(() -> Bukkit.getPlayer(playerUUID)).thenReturn(null); // offline
            pl.onPlayerResetInventory(event);
        }
        assertFalse(event.isCancelled(), "Event should not be cancelled when player is offline");
        verify(store, never()).clearStoredInventoryForWorld(any(), any(), any());
    }

    /**
     * When the player is currently in the event world, BentoBox should handle the reset directly.
     */
    @Test
    void testOnPlayerResetInventoryPlayerInEventWorld() {
        // player.getWorld() returns 'world', event world is also 'world'
        when(player.getWorld()).thenReturn(world);
        PlayerResetInventoryEvent event = new PlayerResetInventoryEvent(world, island, playerUUID);
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class);
             MockedStatic<Util> mockedUtil = mockStatic(Util.class)) {
            mockedBukkit.when(() -> Bukkit.getPlayer(playerUUID)).thenReturn(player);
            mockedUtil.when(() -> Util.sameWorld(world, world)).thenReturn(true);
            pl.onPlayerResetInventory(event);
        }
        assertFalse(event.isCancelled(), "Event should not be cancelled when player is in event world");
        verify(store, never()).clearStoredInventoryForWorld(any(), any(), any());
    }

    /**
     * When the player is in a non-BentoBox world, the inventory reset event should be cancelled
     * and the stored inventory for the BentoBox world should be cleared.
     */
    @Test
    void testOnPlayerResetInventoryPlayerInDifferentWorld() {
        when(settings.isInventory()).thenReturn(true);
        // player is in notWorld, event fires for world
        when(player.getWorld()).thenReturn(notWorld);
        PlayerResetInventoryEvent event = new PlayerResetInventoryEvent(world, island, playerUUID);
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class);
             MockedStatic<Util> mockedUtil = mockStatic(Util.class)) {
            mockedBukkit.when(() -> Bukkit.getPlayer(playerUUID)).thenReturn(player);
            mockedUtil.when(() -> Util.sameWorld(notWorld, world)).thenReturn(false);
            pl.onPlayerResetInventory(event);
        }
        assertTrue(event.isCancelled(), "Event should be cancelled when player is in a different world");
        verify(store).clearStoredInventoryForWorld(player, world, island);
    }

    /**
     * When inventory switching is disabled, the reset must NOT be intercepted - it is left to
     * BentoBox - otherwise the reset would be cancelled and never performed.
     */
    @Test
    void testOnPlayerResetInventoryNotInterceptedWhenSwitchingDisabled() {
        when(settings.isInventory()).thenReturn(false);
        when(player.getWorld()).thenReturn(notWorld);
        PlayerResetInventoryEvent event = new PlayerResetInventoryEvent(world, island, playerUUID);
        pl.onPlayerResetInventory(event);
        assertFalse(event.isCancelled(), "Event should not be cancelled when inventory switching is disabled");
        verify(store, never()).clearStoredInventoryForWorld(any(), any(), any());
    }

    /**
     * Ender chest reset should be intercepted when the player is in a different world.
     */
    @Test
    void testOnPlayerResetEnderChestPlayerInDifferentWorld() {
        when(settings.isEnderChest()).thenReturn(true);
        when(player.getWorld()).thenReturn(notWorld);
        PlayerResetEnderChestEvent event = new PlayerResetEnderChestEvent(world, island, playerUUID);
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class);
             MockedStatic<Util> mockedUtil = mockStatic(Util.class)) {
            mockedBukkit.when(() -> Bukkit.getPlayer(playerUUID)).thenReturn(player);
            mockedUtil.when(() -> Util.sameWorld(notWorld, world)).thenReturn(false);
            pl.onPlayerResetEnderChest(event);
        }
        assertTrue(event.isCancelled(), "Event should be cancelled when player is in a different world");
        verify(store).clearStoredEnderChestForWorld(player, world, island);
    }

    /**
     * Ender chest reset should not be intercepted when the player is in the event world.
     */
    @Test
    void testOnPlayerResetEnderChestPlayerInEventWorld() {
        when(player.getWorld()).thenReturn(world);
        PlayerResetEnderChestEvent event = new PlayerResetEnderChestEvent(world, island, playerUUID);
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class);
             MockedStatic<Util> mockedUtil = mockStatic(Util.class)) {
            mockedBukkit.when(() -> Bukkit.getPlayer(playerUUID)).thenReturn(player);
            mockedUtil.when(() -> Util.sameWorld(world, world)).thenReturn(true);
            pl.onPlayerResetEnderChest(event);
        }
        assertFalse(event.isCancelled());
        verify(store, never()).clearStoredEnderChestForWorld(any(), any(), any());
    }

    /**
     * Experience reset should be intercepted when the player is in a different world.
     */
    @Test
    void testOnPlayerResetExpPlayerInDifferentWorld() {
        when(settings.isExperience()).thenReturn(true);
        when(player.getWorld()).thenReturn(notWorld);
        PlayerResetExpEvent event = new PlayerResetExpEvent(world, island, playerUUID);
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class);
             MockedStatic<Util> mockedUtil = mockStatic(Util.class)) {
            mockedBukkit.when(() -> Bukkit.getPlayer(playerUUID)).thenReturn(player);
            mockedUtil.when(() -> Util.sameWorld(notWorld, world)).thenReturn(false);
            pl.onPlayerResetExp(event);
        }
        assertTrue(event.isCancelled(), "Event should be cancelled when player is in a different world");
        verify(store).clearStoredExpForWorld(player, world, island);
    }

    /**
     * Health reset should be intercepted when the player is in a different world.
     */
    @Test
    void testOnPlayerResetHealthPlayerInDifferentWorld() {
        when(settings.isHealth()).thenReturn(true);
        when(player.getWorld()).thenReturn(notWorld);
        PlayerResetHealthEvent event = new PlayerResetHealthEvent(world, island, playerUUID);
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class);
             MockedStatic<Util> mockedUtil = mockStatic(Util.class)) {
            mockedBukkit.when(() -> Bukkit.getPlayer(playerUUID)).thenReturn(player);
            mockedUtil.when(() -> Util.sameWorld(notWorld, world)).thenReturn(false);
            pl.onPlayerResetHealth(event);
        }
        assertTrue(event.isCancelled(), "Event should be cancelled when player is in a different world");
        verify(store).clearStoredHealthForWorld(player, world, island);
    }

    /**
     * Hunger reset should be intercepted when the player is in a different world.
     */
    @Test
    void testOnPlayerResetHungerPlayerInDifferentWorld() {
        when(settings.isFood()).thenReturn(true);
        when(player.getWorld()).thenReturn(notWorld);
        PlayerResetHungerEvent event = new PlayerResetHungerEvent(world, island, playerUUID);
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class);
             MockedStatic<Util> mockedUtil = mockStatic(Util.class)) {
            mockedBukkit.when(() -> Bukkit.getPlayer(playerUUID)).thenReturn(player);
            mockedUtil.when(() -> Util.sameWorld(notWorld, world)).thenReturn(false);
            pl.onPlayerResetHunger(event);
        }
        assertTrue(event.isCancelled(), "Event should be cancelled when player is in a different world");
        verify(store).clearStoredFoodForWorld(player, world, island);
    }

    /**
     * Money reset should be intercepted when the player is in a different world.
     */
    @Test
    void testOnPlayerResetMoneyPlayerInDifferentWorld() {
        when(settings.isMoney()).thenReturn(true);
        when(player.getWorld()).thenReturn(notWorld);
        PlayerResetMoneyEvent event = new PlayerResetMoneyEvent(world, island, playerUUID);
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class);
             MockedStatic<Util> mockedUtil = mockStatic(Util.class)) {
            mockedBukkit.when(() -> Bukkit.getPlayer(playerUUID)).thenReturn(player);
            mockedUtil.when(() -> Util.sameWorld(notWorld, world)).thenReturn(false);
            pl.onPlayerResetMoney(event);
        }
        assertTrue(event.isCancelled(), "Event should be cancelled when player is in a different world");
        verify(store).clearStoredMoneyForWorld(player, world, island);
    }

    /**
     * Money reset should be left to BentoBox (not intercepted) when the player is in the event world,
     * because BentoBox's reset routes correctly through InvSwitcher's economy.
     */
    @Test
    void testOnPlayerResetMoneyPlayerInEventWorld() {
        when(settings.isMoney()).thenReturn(true);
        when(player.getWorld()).thenReturn(world);
        PlayerResetMoneyEvent event = new PlayerResetMoneyEvent(world, island, playerUUID);
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class);
             MockedStatic<Util> mockedUtil = mockStatic(Util.class)) {
            mockedBukkit.when(() -> Bukkit.getPlayer(playerUUID)).thenReturn(player);
            mockedUtil.when(() -> Util.sameWorld(world, world)).thenReturn(true);
            pl.onPlayerResetMoney(event);
        }
        assertFalse(event.isCancelled());
        verify(store, never()).clearStoredMoneyForWorld(any(), any(), any());
    }

    /**
     * Money reset should be ignored entirely when InvSwitcher money is disabled.
     */
    @Test
    void testOnPlayerResetMoneyMoneyDisabled() {
        when(settings.isMoney()).thenReturn(false);
        when(player.getWorld()).thenReturn(notWorld);
        PlayerResetMoneyEvent event = new PlayerResetMoneyEvent(world, island, playerUUID);
        pl.onPlayerResetMoney(event);
        assertFalse(event.isCancelled());
        verify(store, never()).clearStoredMoneyForWorld(any(), any(), any());
    }

}
