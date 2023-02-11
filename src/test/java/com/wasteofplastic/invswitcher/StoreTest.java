package com.wasteofplastic.invswitcher;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.database.DatabaseSetup.DatabaseType;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, Util.class})
public class StoreTest {

    @Mock
    private InvSwitcher addon;
    @Mock
    private Player player;
    @Mock
    private World world;

    private Store s;
    private com.wasteofplastic.invswitcher.Settings sets;

    @Mock
    private Logger logger;

    @Before
    public void setUp() {
        // Bukkit
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);

        // BentoBox
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        Settings settings = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(settings);
        when(settings.getDatabaseType()).thenReturn(DatabaseType.YAML);

        // Player mock
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
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

        // World 2
        World fromWorld = mock(World.class);
        when(fromWorld.getName()).thenReturn("from_the_end_nether");

        // Settings
        sets = new com.wasteofplastic.invswitcher.Settings();
        when(addon.getSettings()).thenReturn(sets);
        when(addon.getWorlds()).thenReturn(Collections.singleton(world));

        // Addon
        when(addon.getLogger()).thenReturn(logger);

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(world)).thenReturn(world);
        when(Util.getWorld(fromWorld)).thenReturn(fromWorld);

        // Class under test
        s = new Store(addon);
    }

    @After
    public void clear() throws IOException{
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
        s.storeInventory(player, world);
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
        testIsWorldStored();
        s.removeFromCache(player);
        assertFalse(s.isWorldStored(player, world));
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
        s.storeInventory(player, world);
        verify(player, never()).getInventory();
        verify(player, never()).getEnderChest();
        verify(player, never()).getFoodLevel();
        verify(player, never()).getExp();
        verify(player, never()).getLevel();
        verify(player, never()).getHealth();
        verify(player, never()).getGameMode();
        verify(player, never()).getAdvancementProgress(any());
        PowerMockito.verifyStatic(Bukkit.class, never());
        Bukkit.advancementIterator();
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
        // Do not actually save anything
        sets.setAdvancements(true);
        sets.setEnderChest(true);
        sets.setExperience(true);
        sets.setFood(true);
        sets.setGamemode(true);
        sets.setHealth(true);
        sets.setInventory(true);
        sets.setStatistics(true);
        s.storeInventory(player, world);
        verify(player, times(2)).getInventory();
        verify(player, times(2)).getEnderChest();
        verify(player).getFoodLevel();
        verify(player).getExp();
        verify(player, times(2)).getLevel();
        verify(player).getHealth();
        verify(player).getGameMode();
        PowerMockito.verifyStatic(Bukkit.class, times(2));
        Bukkit.advancementIterator();
        // Player clearing
        verify(player).setExp(0);
        verify(player).setLevel(0);
        verify(player).setTotalExperience(0);
        verify(player, atLeastOnce()).setStatistic(any(), any(EntityType.class), anyInt());
        verify(player, atLeastOnce()).setStatistic(any(), any(Material.class), anyInt());
        verify(player, atLeastOnce()).setStatistic(any(), anyInt());


    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.Store#saveOnlinePlayers()}.
     */
    @Test
    public void testSaveOnlinePlayers() {
        s.saveOnlinePlayers();
        PowerMockito.verifyStatic(Bukkit.class);
        Bukkit.getOnlinePlayers();
    }

}
