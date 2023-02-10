package com.wasteofplastic.invswitcher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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

    @Before
    public void setUp() {
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

        com.wasteofplastic.invswitcher.Settings sets = new com.wasteofplastic.invswitcher.Settings();
        when(addon.getSettings()).thenReturn(sets);
        when(addon.getWorlds()).thenReturn(Collections.singleton(world));

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(world)).thenReturn(world);
        when(Util.getWorld(fromWorld)).thenReturn(fromWorld);
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
        new Store(addon);
        verify(addon).getLogger();
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.Store#getInventory(org.bukkit.entity.Player, org.bukkit.World)}.
     */
    @Test
    public void testGetInventory() {
        Store s = new Store(addon);
        s.getInventory(player, world);
        verify(player).setFoodLevel(20);
        verify(player).setHealth(18);
        verify(player).getInventory();
        verify(player).setTotalExperience(0);
    }

    /*
      Test method for {@link Store#storeInventory(Player, World)}.
     */
    /*
     * TODO: Works in Eclipse, fails in MVN...
    @Test
    public void testStoreInventoryPlayerWorldLocation() {
        new Store(addon).storeInventory(player, world);
    }
     */
}
