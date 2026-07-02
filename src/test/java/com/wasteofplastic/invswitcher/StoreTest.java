package com.wasteofplastic.invswitcher;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
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
import com.wasteofplastic.invswitcher.dataobjects.InventoryStorage;

import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.util.Util;
import org.mockbukkit.mockbukkit.MockBukkit;

/**
 * @author tastybento
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StoreTest {

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

    private Set<World> bentoboxWorlds;

    private UUID playerUUID;

    @Mock
    private Logger logger;

    private MockedStatic<BentoBox> mockedBentoBox;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();

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

        // Register world as a BentoBox world
        bentoboxWorlds = new HashSet<>();
        bentoboxWorlds.add(world);
        when(addon.getWorlds()).thenReturn(bentoboxWorlds);

        // Disable island switching by default for existing tests
        sets.setIslandsActive(false);

        // Class under test
        s = new Store(addon);
    }

    @AfterEach
    void tearDown() throws IOException {
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
    void testStore() {
        assertNotNull(s);
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.Store#isWorldStored(org.bukkit.entity.Player, org.bukkit.World)}.
     */
    @Test
    void testIsWorldStored() {
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
    void testGetInventory() {
        s.getInventory(player, world);
        verify(player).setFoodLevel(20);
        verify(player).setHealth(18);
        verify(player).getInventory();
        verify(player).setTotalExperience(0);
    }

    /**
     * Issue #56: a stored health of 0 is only ever captured when the player's state is saved
     * mid-death (e.g. per-island health enabled and the player died on another island). Loading
     * that value into a live player would kill them the instant they enter the world, causing an
     * endless respawn/death loop. Loading must restore full health instead of applying 0.
     */
    @Test
    void testGetInventoryNeverLoadsFatalHealth() {
        sets.setStatistics(false);
        sets.setAdvancements(false);

        // Simulate the player's state being captured mid-death with 0 health
        when(player.getHealth()).thenReturn(0D);
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS)) {
            s.storeInventory(player, world);
        }

        // Re-entering the world must restore full health (max = 18), never the fatal stored 0
        s.getInventory(player, world);
        verify(player).setHealth(18D);
        verify(player, never()).setHealth(0D);
    }

    /**
     * Test that advancement grants during {@link Store#getInventory} do not modify the player's
     * experience points. Some advancements reward XP when their criteria are awarded; the store
     * must save and restore XP around the advancement grant step.
     */
    @Test
    void testGetInventoryAdvancementsPreservesExperience() {
        sets.setAdvancements(true);
        sets.setExperience(true);
        sets.setStatistics(false);
        sets.setIslandsActive(false);

        // Mock an advancement with awarded criteria
        Advancement advancement = mock(Advancement.class);
        NamespacedKey advKey = NamespacedKey.minecraft("story_mine_stone");
        when(advancement.getKey()).thenReturn(advKey);

        AdvancementProgress progress = mock(AdvancementProgress.class);
        Set<String> criteria = new HashSet<>(Set.of("mine_stone"));
        when(progress.getAwardedCriteria()).thenReturn(criteria);
        when(player.getAdvancementProgress(advancement)).thenReturn(progress);

        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS)) {
            // Return a fresh iterator each time so both storeInventory and getInventory can iterate
            mockedBukkit.when(Bukkit::advancementIterator).thenAnswer(inv -> List.of(advancement).iterator());

            // Store inventory (saves advancement data and clears player including XP reset)
            s.storeInventory(player, world);

            // Load inventory — experience set, advancements granted, XP restored
            s.getInventory(player, world);
        }

        // setTotalExperience should be called exactly 3 times:
        // 1. clearPlayer during storeInventory (XP reset to 0)
        // 2. experience loading during getInventory (XP set to stored value 0)
        // 3. XP restoration inside setAdvancements after granting advancement criteria
        verify(player, times(3)).setTotalExperience(0);
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.Store#removeFromCache(org.bukkit.entity.Player)}.
     */
    @Test
    void testRemoveFromCache() {
        s.getInventory(player, world);
        assertNotNull(s.getCurrentKey(player));
        s.removeFromCache(player);
        assertNull(s.getCurrentKey(player));
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.Store#storeInventory(org.bukkit.entity.Player, org.bukkit.World)}.
     */
    @Test
    void testStoreInventoryNothing() {
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
    void testStoreInventoryAll() {
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
    void testSaveOnlinePlayers() {
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
    void testGetStorageKeyIslandsDisabled() {
        sets.setIslandsActive(false);
        String key = s.getStorageKey(player, world);
        assertEquals("world", key); // nether suffix stripped
    }

    @Test
    void testGetStorageKeySingleIsland() {
        sets.setIslandsActive(true);
        try (MockedStatic<Util> utilities = Mockito.mockStatic(Util.class)) {
            utilities.when(() -> Util.getWorld(world)).thenReturn(world);
            when(islandsManager.getNumberOfConcurrentIslands(playerUUID, world)).thenReturn(1);
            String key = s.getStorageKey(player, world);
            assertEquals("world", key); // just overworld name, no island suffix
        }
    }

    @Test
    void testGetStorageKeyMultipleIslandsOnOwnIsland() {
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
    void testGetStorageKeyMultipleIslandsOnOtherPlayerIsland() {
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
    void testGetStorageKeyWithSpecificIsland() {
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
    void testGetStorageKeyGenericNether() {
        sets.setIslandsActive(true);
        // Set up a nether world
        World netherWorld = mock(World.class);
        when(netherWorld.getName()).thenReturn("world_nether");
        when(netherWorld.getEnvironment()).thenReturn(Environment.NETHER);
        bentoboxWorlds.add(netherWorld);

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
    void testGetCurrentKeyNullByDefault() {
        assertNull(s.getCurrentKey(player));
    }

    @Test
    void testGetCurrentKeySetAfterGetInventory() {
        sets.setIslandsActive(false);
        s.getInventory(player, world);
        assertEquals("world", s.getCurrentKey(player));
    }

    @Test
    void testRemoveFromCacheClearsCurrentKey() {
        sets.setIslandsActive(false);
        s.getInventory(player, world);
        assertNotNull(s.getCurrentKey(player));
        s.removeFromCache(player);
        assertNull(s.getCurrentKey(player));
    }

    /**
     * Test upgradeWorldKeyToIsland clears world data and updates currentKey.
     */
    @Test
    void testUpgradeWorldKeyToIsland() {
        sets.setIslandsActive(true);
        sets.setStatistics(false);

        Island oldIsland = mock(Island.class);
        when(oldIsland.getOwner()).thenReturn(playerUUID);
        when(oldIsland.getUniqueId()).thenReturn("island-primary");

        try (MockedStatic<Util> utilities = Mockito.mockStatic(Util.class);
             MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS)) {
            utilities.when(() -> Util.getWorld(world)).thenReturn(world);
            when(islandsManager.getNumberOfConcurrentIslands(playerUUID, world)).thenReturn(1);

            // Simulate login and play — data saved under world-only key
            s.getInventory(player, world);
            assertEquals("world", s.getCurrentKey(player));
            s.storeInventory(player, world);
            assertTrue(s.isWorldStored(player, world));

            // Upgrade: transitions from world-only to island-specific key
            s.upgradeWorldKeyToIsland(player, world, oldIsland);

            // currentKey should now be island-specific
            assertEquals("world/island-primary", s.getCurrentKey(player));
            // World-only data should be cleared
            assertFalse(s.isWorldStored(player, world));
        }
    }

    /**
     * Full scenario: player has 1 island, creates 2nd, goes to new island, returns to original.
     * Simulates what onIslandEnter does: upgradeWorldKey, storeInventory, getInventory.
     */
    @Test
    void testFullScenarioSingleToMultipleIslands() {
        sets.setIslandsActive(true);
        sets.setStatistics(false);
        sets.setHealth(false);
        sets.setFood(false);
        sets.setExperience(false);
        sets.setGamemode(false);
        sets.setAdvancements(false);
        sets.setEnderChest(false);

        Island primaryIsland = mock(Island.class);
        when(primaryIsland.getOwner()).thenReturn(playerUUID);
        when(primaryIsland.getUniqueId()).thenReturn("island-primary");

        Island newIsland = mock(Island.class);
        when(newIsland.getOwner()).thenReturn(playerUUID);
        when(newIsland.getUniqueId()).thenReturn("island-new");

        Location loc = mock(Location.class);
        when(player.getLocation()).thenReturn(loc);

        try (MockedStatic<Util> utilities = Mockito.mockStatic(Util.class);
             MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS)) {
            utilities.when(() -> Util.getWorld(world)).thenReturn(world);
            when(islandsManager.getNumberOfConcurrentIslands(playerUUID, world)).thenReturn(1);

            // Step 1: Player has 1 island. Login and play.
            s.getInventory(player, world);
            s.storeInventory(player, world);
            assertTrue(s.isWorldStored(player, world));

            // Step 2: Player creates 2nd island and teleports to it.
            // onIslandEnter detects world-only key and upgrades BEFORE store/load.
            when(islandsManager.getNumberOfConcurrentIslands(playerUUID, world)).thenReturn(2);
            s.upgradeWorldKeyToIsland(player, world, primaryIsland);
            assertEquals("world/island-primary", s.getCurrentKey(player));

            // Now storeInventory saves to the island-specific key for the OLD island
            s.storeInventory(player, world);
            // Load new island — no world data to migrate, so player gets empty inventory
            s.getInventory(player, world, newIsland);
            assertEquals("world/island-new", s.getCurrentKey(player));

            // Step 3: Player teleports back to primary island.
            s.storeInventory(player, world);
            s.getInventory(player, world, primaryIsland);
            assertEquals("world/island-primary", s.getCurrentKey(player));

            // Verify inventory was loaded (setContents called for the primary island load)
            verify(player.getInventory(), atLeastOnce()).setContents(any(ItemStack[].class));
        }
    }

    // --- Non-BentoBox world tests ---

    @Test
    void testGetStorageKeyNonBentoBoxWorld() {
        World otherWorld = mock(World.class);
        when(otherWorld.getName()).thenReturn("em_adventurers_guild");
        // otherWorld is NOT in bentoboxWorlds
        String key = s.getStorageKey(player, otherWorld);
        assertEquals(Store.DEFAULT_WORLD_KEY, key);
    }

    @Test
    void testAllNonBentoBoxWorldsShareKey() {
        World world1 = mock(World.class);
        when(world1.getName()).thenReturn("world");
        World world2 = mock(World.class);
        when(world2.getName()).thenReturn("em_adventurers_guild");
        World world3 = mock(World.class);
        when(world3.getName()).thenReturn("em_diamond_arena");
        // None are in bentoboxWorlds
        assertEquals(Store.DEFAULT_WORLD_KEY, s.getStorageKey(player, world1));
        assertEquals(Store.DEFAULT_WORLD_KEY, s.getStorageKey(player, world2));
        assertEquals(Store.DEFAULT_WORLD_KEY, s.getStorageKey(player, world3));
    }

    @Test
    void testBentoBoxToNonBentoBoxRestoresInventory() {
        sets.setStatistics(false);
        sets.setAdvancements(false);

        World nonBBWorld = mock(World.class);
        when(nonBBWorld.getName()).thenReturn("em_adventurers_guild");

        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS)) {
            // Step 1: Enter BentoBox world from non-BB world — saves "outside" inventory
            s.storeInventory(player, nonBBWorld);
            s.getInventory(player, world);
            assertEquals("world", s.getCurrentKey(player));

            // Step 2: Leave BentoBox world to a DIFFERENT non-BB world
            s.storeInventory(player, world);
            s.getInventory(player, nonBBWorld);
            // Should load from DEFAULT_WORLD_KEY (where step 1 saved)
            assertEquals(Store.DEFAULT_WORLD_KEY, s.getCurrentKey(player));

            // Verify inventory was loaded (setContents called)
            verify(player.getInventory(), atLeastOnce()).setContents(any(ItemStack[].class));
        }
    }

    @Test
    void testMigrationFromOldWorldKey() {
        sets.setStatistics(false);
        sets.setAdvancements(false);

        // Simulate old data: inventory was saved under "overworld" (old behavior)
        World overworld = mock(World.class);
        when(overworld.getName()).thenReturn("overworld");
        // Temporarily add overworld to BentoBox worlds so storeAndSave uses "overworld" key
        bentoboxWorlds.add(overworld);

        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS)) {
            s.storeInventory(player, overworld);
            assertTrue(s.isWorldStored(player, overworld));
        }

        // Now remove overworld from BentoBox worlds (simulating the fix being applied)
        bentoboxWorlds.remove(overworld);

        // Loading for a non-BB world should migrate from the old "overworld" key
        World otherWorld = mock(World.class);
        when(otherWorld.getName()).thenReturn("em_adventurers_guild");
        s.getInventory(player, otherWorld);
        assertEquals(Store.DEFAULT_WORLD_KEY, s.getCurrentKey(player));

        // Verify inventory was loaded (migration found the old "overworld" data)
        verify(player.getInventory(), atLeastOnce()).setContents(any(ItemStack[].class));
    }

    // --- clearStoredXForWorld Tests ---

    /**
     * When inventory is enabled and the event fires for a world the player is not in,
     * the stored inventory for that world should be cleared.
     * After clearing, loading inventory for that world should give empty contents.
     */
    @Test
    void testClearStoredInventoryForWorld() {
        sets.setStatistics(false);
        sets.setAdvancements(false);
        Island island = mock(Island.class);
        // First save something in the store
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS)) {
            s.storeInventory(player, world);
            assertTrue(s.isWorldStored(player, world), "Inventory should be stored before clearing");

            // Clear stored inventory for the world
            s.clearStoredInventoryForWorld(player, world, island);

            // isWorldStored returns true even after clearing (entry exists but is empty list)
            assertTrue(s.isWorldStored(player, world));

            // Load inventory back - should set empty contents to player
            s.getInventory(player, world);
            // setContents should have been called with empty array
            verify(player.getInventory(), atLeastOnce()).setContents(any(ItemStack[].class));
        }
    }

    /**
     * When ender chest is enabled, clearStoredEnderChestForWorld should work without error.
     */
    @Test
    void testClearStoredEnderChestForWorld() {
        sets.setStatistics(false);
        sets.setAdvancements(false);
        Island island = mock(Island.class);
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS)) {
            s.storeInventory(player, world);
            assertDoesNotThrow(() -> s.clearStoredEnderChestForWorld(player, world, island));
        }
    }

    /**
     * When experience is enabled, clearStoredExpForWorld should zero out the stored exp.
     */
    @Test
    void testClearStoredExpForWorld() {
        sets.setStatistics(false);
        sets.setAdvancements(false);
        Island island = mock(Island.class);
        when(player.getTotalExperience()).thenReturn(500);
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS)) {
            s.storeInventory(player, world);
            assertDoesNotThrow(() -> s.clearStoredExpForWorld(player, world, island));
        }
    }

    /**
     * When money is enabled, clearStoredMoneyForWorld should zero the stored balance for the world.
     */
    @Test
    void testClearStoredMoneyForWorld() {
        sets.setStatistics(false);
        sets.setAdvancements(false);
        sets.setMoney(true);
        Island island = mock(Island.class);
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS)) {
            s.storeInventory(player, world);
            // Seed a balance for the world
            s.getStorageObject(playerUUID).setMoney("world", 500D);
            // Clear it
            s.clearStoredMoneyForWorld(player, world, island);
        }
        assertEquals(0D, s.getStorageObject(playerUUID).getMoney("world"), 0.0001);
    }

    /**
     * An offline player's economy write must be visible to an immediately following read, even
     * before the asynchronous save flushes. Regression test for give/set reporting a stale balance
     * because the follow-up read reloaded the player from the database before the save landed.
     * Verifies {@link Store#saveStorage} tracks the write and {@link Store#getStorageObject} reuses
     * the pending object.
     */
    @Test
    void testOfflineWriteVisibleToImmediateRead() {
        sets.setMoney(true);
        // Offline player: never cached, so saveStorage takes the pending-save path and getStorageObject
        // must return a value consistent with the write that just happened. Keep the post-save eviction
        // scheduled (a no-op on the mocked scheduler) rather than run inline, so the pending object is
        // still held when we read it back.
        BentoBox enabledPlugin = mock(BentoBox.class);
        when(enabledPlugin.isEnabled()).thenReturn(true);
        when(addon.getPlugin()).thenReturn(enabledPlugin);
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS)) {
            InventoryStorage obj = s.getStorageObject(playerUUID);
            obj.setMoney("world", 2000D);
            s.saveStorage(obj);
            // The follow-up read must reflect the write, not a stale/empty reload.
            assertEquals(2000D, s.getStorageObject(playerUUID).getMoney("world"), 0.0001);
        }
    }

    /**
     * clearStoredMoneyForWorld should be a no-op when money is disabled in settings.
     */
    @Test
    void testClearStoredMoneyForWorldMoneyDisabled() {
        sets.setStatistics(false);
        sets.setAdvancements(false);
        sets.setMoney(false);
        Island island = mock(Island.class);
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS)) {
            s.storeInventory(player, world);
            s.getStorageObject(playerUUID).setMoney("world", 500D);
            s.clearStoredMoneyForWorld(player, world, island);
        }
        // Balance untouched
        assertEquals(500D, s.getStorageObject(playerUUID).getMoney("world"), 0.0001);
    }

    /**
     * When health is enabled, clearStoredHealthForWorld should work without error.
     */
    @Test
    void testClearStoredHealthForWorld() {
        sets.setStatistics(false);
        sets.setAdvancements(false);
        Island island = mock(Island.class);
        when(player.getHealth()).thenReturn(10.0);
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS)) {
            s.storeInventory(player, world);
            assertDoesNotThrow(() -> s.clearStoredHealthForWorld(player, world, island));
        }
    }

    /**
     * When food is enabled, clearStoredFoodForWorld should work without error.
     */
    @Test
    void testClearStoredFoodForWorld() {
        sets.setStatistics(false);
        sets.setAdvancements(false);
        Island island = mock(Island.class);
        when(player.getFoodLevel()).thenReturn(8);
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS)) {
            s.storeInventory(player, world);
            assertDoesNotThrow(() -> s.clearStoredFoodForWorld(player, world, island));
        }
    }

    /**
     * clearStoredInventoryForWorld should be a no-op when inventory is disabled in settings.
     * No exceptions should be thrown.
     */
    @Test
    void testClearStoredInventoryForWorldInventoryDisabled() {
        sets.setInventory(false);
        sets.setStatistics(false);
        sets.setAdvancements(false);
        Island island = mock(Island.class);
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS)) {
            s.storeInventory(player, world);
            // With inventory disabled, nothing is stored in the inventory map
            assertFalse(s.isWorldStored(player, world));

            // Calling clear should be a no-op (no exception, no effect)
            s.clearStoredInventoryForWorld(player, world, island);
            assertFalse(s.isWorldStored(player, world));
        }
    }

    /**
     * getStorageKeyForEvent should return the world name when islands mode is inactive.
     */
    @Test
    void testGetStorageKeyForEventIslandsDisabled() {
        sets.setIslandsActive(false);
        Island island = mock(Island.class);
        String key = s.getStorageKeyForEvent(player, world, island);
        assertEquals("world", key);
    }

    /**
     * getStorageKeyForEvent should return world name when player has only 1 island.
     */
    @Test
    void testGetStorageKeyForEventSingleIsland() {
        sets.setIslandsActive(true);
        Island island = mock(Island.class);
        try (MockedStatic<Util> mockedUtil = mockStatic(Util.class)) {
            mockedUtil.when(() -> Util.getWorld(world)).thenReturn(world);
            when(islandsManager.getNumberOfConcurrentIslands(playerUUID, world)).thenReturn(1);
            String key = s.getStorageKeyForEvent(player, world, island);
            assertEquals("world", key);
        }
    }

    /**
     * getStorageKeyForEvent should return island-specific key when player owns the island
     * and has multiple islands.
     */
    @Test
    void testGetStorageKeyForEventMultipleIslandsOwner() {
        sets.setIslandsActive(true);
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(playerUUID);
        when(island.getUniqueId()).thenReturn("island-abc");
        try (MockedStatic<Util> mockedUtil = mockStatic(Util.class)) {
            mockedUtil.when(() -> Util.getWorld(world)).thenReturn(world);
            when(islandsManager.getNumberOfConcurrentIslands(playerUUID, world)).thenReturn(2);
            String key = s.getStorageKeyForEvent(player, world, island);
            assertEquals("world/island-abc", key);
        }
    }

    /**
     * getStorageKeyForEvent should return just the world name when player does not own the island
     * (e.g., kicked from a team). Uses the world-level key since the player is a member, not owner.
     */
    @Test
    void testGetStorageKeyForEventMultipleIslandsNotOwner() {
        sets.setIslandsActive(true);
        Island island = mock(Island.class);
        UUID otherOwner = UUID.randomUUID();
        when(island.getOwner()).thenReturn(otherOwner); // player is NOT the owner
        try (MockedStatic<Util> mockedUtil = mockStatic(Util.class)) {
            mockedUtil.when(() -> Util.getWorld(world)).thenReturn(world);
            when(islandsManager.getNumberOfConcurrentIslands(playerUUID, world)).thenReturn(2);
            String key = s.getStorageKeyForEvent(player, world, island);
            assertEquals("world", key); // Falls back to world name
        }
    }

}
