package com.wasteofplastic.invswitcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
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

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.DatabaseSetup.DatabaseType;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.util.Util;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/**
 * @author tastybento
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class StoreTest {

    @Mock
    private InvSwitcher addon;
    @Mock
    private Player player;
    @Mock
    private World world;
    @Mock
    private world.bentobox.bentobox.Settings bbSettings;
    @Mock
    private IslandsManager islandsManager;

    private Store s;

    private Settings sets;

    private UUID playerUUID;

    @Mock
    private Logger logger;

    private MockedStatic<BentoBox> mockedBentoBox;

    @BeforeEach
    public void setUp() throws Exception {
        ServerMock server = MockBukkit.mock();

        // BentoBox
        BentoBox plugin = mock(BentoBox.class);
        mockedBentoBox = Mockito.mockStatic(BentoBox.class);
        mockedBentoBox.when(BentoBox::getInstance).thenReturn(plugin);
        when(plugin.getSettings()).thenReturn(bbSettings);

        // Player mock
        playerUUID = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUUID);
        AttributeInstance attribute = mock(AttributeInstance.class);
        // Health
        when(attribute.getValue()).thenReturn(18D);
        when(player.getAttribute(any())).thenReturn(attribute);
        // Inventory
        PlayerInventory inv = mock(PlayerInventory.class);
        ItemStack[] contents = { new ItemStack(Material.ACACIA_BOAT, 1), null, new ItemStack(Material.BAKED_POTATO, 32), null, null, new ItemStack(Material.CAVE_SPIDER_SPAWN_EGG, 2) };
        when(inv.getContents()).thenReturn(contents);
        when(player.getInventory()).thenReturn(inv);
        when(player.getEnderChest()).thenReturn(inv);

        // World mock
        when(world.getName()).thenReturn("world_the_end_nether");
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);

        // World 2
        World fromWorld = mock(World.class);

        // Settings
        sets = new Settings();
        when(addon.getSettings()).thenReturn(sets);

        // Addon
        when(addon.getLogger()).thenReturn(logger);
        when(addon.getIslands()).thenReturn(islandsManager);

        try (MockedStatic<Util> utilities = Mockito.mockStatic(Util.class)) {
            utilities.when(() -> Util.getWorld(world)).thenReturn(world);
            utilities.when(() -> Util.getWorld(fromWorld)).thenReturn(fromWorld);
        }
        DatabaseType mockDbt = mock(DatabaseType.class);
        when(bbSettings.getDatabaseType()).thenReturn(mockDbt);

        // Disable island switching by default for existing tests
        sets.setIslandsActive(false);

        // Class under test
        s = new Store(addon);
    }

    @AfterEach
    public void tearDown() throws IOException {
        if (mockedBentoBox != null) {
            mockedBentoBox.close();
        }
        MockBukkit.unmock();
        //remove any database data
        File file = new File("database");
        Path pathToBeDeleted = file.toPath();
        if (file.exists()) {
            Files.walk(pathToBeDeleted)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
        }
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.Store#Store(com.wasteofplastic.invswitcher.InvSwitcher)}.
     */
    @Test
    public void testStore() {
        assertNotNull(s);
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.Store#isWorldStored(org.bukkit.entity.Player, org.bukkit.World)}.
     */
    @Test
    public void testIsWorldStored() {
        assertFalse(s.isWorldStored(player, world));
        // Disable statistics to avoid registry issues when Bukkit static mock overrides MockBukkit
        sets.setStatistics(false);
        // Mock the static method
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS)) {
            // Run the code under test
            s.storeInventory(player, world);
        }
        assertTrue(s.isWorldStored(player, world));
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.Store#getInventory(org.bukkit.entity.Player, org.bukkit.World)}.
     */
    @Test
    public void testGetInventory() {
        s.getInventory(player, world);
        verify(player).setFoodLevel(20);
        verify(player).setHealth(18);
        verify(player).getInventory();
        verify(player).setTotalExperience(0);
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.Store#removeFromCache(org.bukkit.entity.Player)}.
     */
    @Test
    public void testRemoveFromCache() {
        s.getInventory(player, world);
        assertNotNull(s.getCurrentKey(player));
        s.removeFromCache(player);
        assertNull(s.getCurrentKey(player));
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.Store#storeInventory(org.bukkit.entity.Player, org.bukkit.World)}.
     */
    @Test
    public void testStoreInventoryNothing() {
        // Do not actually save anything
        sets.setAdvancements(false);
        sets.setEnderChest(false);
        sets.setExperience(false);
        sets.setFood(false);
        sets.setGamemode(false);
        sets.setHealth(false);
        sets.setInventory(false);
        sets.setStatistics(false);
        // Mock the static method
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class)) {
            // Run the code under test
            s.storeInventory(player, world);

            // Verify that the static method was never called
            mockedBukkit.verify(() -> Bukkit.advancementIterator(), never());
        }
        verify(player, never()).getInventory();
        verify(player, never()).getEnderChest();
        verify(player, never()).getFoodLevel();
        verify(player, never()).getExp();
        verify(player, never()).getLevel();
        verify(player, never()).getHealth();
        verify(player, never()).getGameMode();
        verify(player, never()).getAdvancementProgress(any());

        // No Player clearing
        verify(player, never()).setExp(anyFloat());
        verify(player, never()).setLevel(anyInt());
        verify(player, never()).setTotalExperience(anyInt());
        verify(player, never()).setStatistic(any(), any(EntityType.class), anyInt());
        verify(player, never()).setStatistic(any(), any(Material.class), anyInt());
        verify(player, never()).setStatistic(any(), anyInt());

    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.Store#storeInventory(org.bukkit.entity.Player, org.bukkit.World)}.
     */
    @Test
    public void testStoreInventoryAll() {
        sets.setAdvancements(true);
        sets.setEnderChest(true);
        sets.setExperience(true);
        sets.setFood(true);
        sets.setGamemode(true);
        sets.setHealth(true);
        sets.setInventory(true);
        // Statistics disabled: MockedStatic<Bukkit> overrides MockBukkit's real registries
        // which breaks Material.isItem()/isBlock() calls in resetStats
        sets.setStatistics(false);
        // Mock the static method
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS)) {
            // Run the code under test
            s.storeInventory(player, world);

            // Verify that the static method was called
            mockedBukkit.verify(() -> Bukkit.advancementIterator(), times(2));
        }
        verify(player, times(2)).getInventory();
        verify(player, times(2)).getEnderChest();
        verify(player).getFoodLevel();
        verify(player).getExp();
        verify(player, times(2)).getLevel();
        verify(player).getHealth();
        verify(player).getGameMode();
        // Player clearing
        verify(player).setExp(0);
        verify(player).setLevel(0);
        verify(player).setTotalExperience(0);


    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.Store#saveOnShutdown()}.
     */
    @Test
    public void testSaveOnlinePlayers() {
        // Mock the static method
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class)) {
            // Run the code under test
            s.saveOnShutdown();

            // Verify that the static method was called
            mockedBukkit.verify(() -> Bukkit.getOnlinePlayers());
        }
    }

    // --- Per-island storage key tests ---

    @Test
    public void testGetStorageKeyIslandsDisabled() {
        sets.setIslandsActive(false);
        String key = s.getStorageKey(player, world);
        assertEquals("world", key); // nether suffix stripped
    }

    @Test
    public void testGetStorageKeySingleIsland() {
        sets.setIslandsActive(true);
        try (MockedStatic<Util> utilities = Mockito.mockStatic(Util.class)) {
            utilities.when(() -> Util.getWorld(world)).thenReturn(world);
            when(islandsManager.getNumberOfConcurrentIslands(playerUUID, world)).thenReturn(1);
            String key = s.getStorageKey(player, world);
            assertEquals("world", key); // just overworld name, no island suffix
        }
    }

    @Test
    public void testGetStorageKeyMultipleIslandsOnOwnIsland() {
        sets.setIslandsActive(true);
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(playerUUID);
        when(island.getUniqueId()).thenReturn("island-123");
        Location loc = mock(Location.class);
        when(player.getLocation()).thenReturn(loc);

        try (MockedStatic<Util> utilities = Mockito.mockStatic(Util.class)) {
            utilities.when(() -> Util.getWorld(world)).thenReturn(world);
            when(islandsManager.getNumberOfConcurrentIslands(playerUUID, world)).thenReturn(2);
            when(islandsManager.getIslandAt(loc)).thenReturn(Optional.of(island));

            String key = s.getStorageKey(player, world);
            assertEquals("world/island-123", key);
        }
    }

    @Test
    public void testGetStorageKeyMultipleIslandsOnOtherPlayerIsland() {
        sets.setIslandsActive(true);
        Island island = mock(Island.class);
        UUID otherPlayer = UUID.randomUUID();
        when(island.getOwner()).thenReturn(otherPlayer);
        Location loc = mock(Location.class);
        when(player.getLocation()).thenReturn(loc);

        try (MockedStatic<Util> utilities = Mockito.mockStatic(Util.class)) {
            utilities.when(() -> Util.getWorld(world)).thenReturn(world);
            when(islandsManager.getNumberOfConcurrentIslands(playerUUID, world)).thenReturn(2);
            when(islandsManager.getIslandAt(loc)).thenReturn(Optional.of(island));

            String key = s.getStorageKey(player, world);
            // Not on own island, falls back to overworld name
            assertEquals("world", key);
        }
    }

    @Test
    public void testGetStorageKeyWithSpecificIsland() {
        sets.setIslandsActive(true);
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(playerUUID);
        when(island.getUniqueId()).thenReturn("island-456");

        try (MockedStatic<Util> utilities = Mockito.mockStatic(Util.class)) {
            utilities.when(() -> Util.getWorld(world)).thenReturn(world);
            when(islandsManager.getNumberOfConcurrentIslands(playerUUID, world)).thenReturn(2);

            String key = s.getStorageKey(player, world, island);
            assertEquals("world/island-456", key);
        }
    }

    @Test
    public void testGetStorageKeyGenericNether() {
        sets.setIslandsActive(true);
        // Set up a nether world
        World netherWorld = mock(World.class);
        when(netherWorld.getName()).thenReturn("world_nether");
        when(netherWorld.getEnvironment()).thenReturn(Environment.NETHER);

        // Set up overworld for Util.getWorld
        World overworld = mock(World.class);
        when(overworld.getName()).thenReturn("world");

        // Set up BentoBox IWM - reconfigure the class-level mockedBentoBox
        BentoBox bbPlugin = mock(BentoBox.class);
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(bbPlugin.getIWM()).thenReturn(iwm);
        when(iwm.isIslandNether(netherWorld)).thenReturn(false); // generic nether
        mockedBentoBox.when(BentoBox::getInstance).thenReturn(bbPlugin);

        try (MockedStatic<Util> utilities = Mockito.mockStatic(Util.class)) {
            utilities.when(() -> Util.getWorld(netherWorld)).thenReturn(overworld);
            when(islandsManager.getNumberOfConcurrentIslands(playerUUID, overworld)).thenReturn(2);

            // No current key set yet, should fall back to overworld name
            String key = s.getStorageKey(player, netherWorld);
            assertEquals("world", key);
        }
    }

    @Test
    public void testGetCurrentKeyNullByDefault() {
        assertNull(s.getCurrentKey(player));
    }

    @Test
    public void testGetCurrentKeySetAfterGetInventory() {
        sets.setIslandsActive(false);
        s.getInventory(player, world);
        assertEquals("world", s.getCurrentKey(player));
    }

    @Test
    public void testRemoveFromCacheClearsCurrentKey() {
        sets.setIslandsActive(false);
        s.getInventory(player, world);
        assertNotNull(s.getCurrentKey(player));
        s.removeFromCache(player);
        assertNull(s.getCurrentKey(player));
    }

}
