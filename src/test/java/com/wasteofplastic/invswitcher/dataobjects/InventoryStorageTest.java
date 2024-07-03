package com.wasteofplastic.invswitcher.dataobjects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author tastybentos
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class InventoryStorageTest {


    private InventoryStorage is;

    /**
     */
    @Before
    public void setUp() {
        is = new InventoryStorage();
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#getUniqueId()}.
     */
    @Test
    public void testGetUniqueId() {
        assertNull(is.getUniqueId());
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#setUniqueId(java.lang.String)}.
     */
    @Test
    public void testSetUniqueId() {
        String uuid = UUID.randomUUID().toString();
        is.setUniqueId(uuid);
        assertEquals(uuid, is.getUniqueId());
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#getInventory()}.
     */
    @Test
    public void testGetInventory() {
        assertTrue(is.getInventory().isEmpty());
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#getHealth()}.
     */
    @Test
    public void testGetHealth() {
        assertTrue(is.getHealth().isEmpty());
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#getFood()}.
     */
    @Test
    public void testGetFood() {
        assertTrue(is.getFood().isEmpty());
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#getExp()}.
     */
    @Test
    public void testGetExp() {
        assertTrue(is.getExp().isEmpty());
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#getLocation()}.
     */
    @Test
    public void testGetLocation() {
        assertTrue(is.getLocation().isEmpty());
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#setInventory(java.util.Map)}.
     */
    @Test
    public void testSetInventoryMapOfStringListOfItemStack() {
        ItemStack item = new ItemStack(Material.ACACIA_BOAT);
        is.setInventory(Map.of("test", List.of(item)));
        Map<String, List<ItemStack>> map = is.getInventory();
        assertEquals(item, map.get("test").get(0));
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#setInventory(java.lang.String, java.util.List)}.
     */
    @Test
    public void testSetInventoryStringListOfItemStack() {
        ItemStack item = new ItemStack(Material.ACACIA_BOAT);
        is.setInventory("worldName", List.of(item));
        Map<String, List<ItemStack>> map = is.getInventory();
        assertEquals(item, map.get("worldName").get(0));
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#setHealth(java.util.Map)}.
     */
    @Test
    public void testSetHealthMapOfStringDouble() {
        Map<String, Double> map = Map.of("test", 234D);
        is.setHealth(map);
        assertEquals(234D, is.getHealth().get("test"), 0D);
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#setFood(java.util.Map)}.
     */
    @Test
    public void testSetFoodMapOfStringInteger() {
        Map<String, Integer> map = Map.of("test", 234);
        is.setFood(map);
        assertEquals(234, is.getFood().get("test").intValue());
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#setExp(java.util.Map)}.
     */
    @Test
    public void testSetExpMapOfStringInteger() {
        Map<String, Integer> map = Map.of("test", 234);
        is.setExp(map);
        assertEquals(234, is.getExp().get("test").intValue());
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#setLocation(java.util.Map)}.
     */
    @Test
    public void testSetLocationMapOfStringLocation() {
        Location loc = mock(Location.class);
        Map<String, Location> map = Map.of("test", loc);
        is.setLocation(map);
        assertEquals(loc, is.getLocation().get("test"));
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#setHealth(java.lang.String, double)}.
     */
    @Test
    public void testSetHealthStringDouble() {
        is.setHealth("test", 10D);
        assertEquals(10D, is.getHealth().get("test"), 0D);
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#setFood(java.lang.String, int)}.
     */
    @Test
    public void testSetFoodStringInt() {
        is.setFood("test", 234);
        assertEquals(234, is.getFood().get("test").intValue());
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#setExp(java.lang.String, int)}.
     */
    @Test
    public void testSetExpStringInt() {
        is.setExp("test", 234);
        assertEquals(234, is.getExp().get("test").intValue());
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#setLocation(java.lang.String, org.bukkit.Location)}.
     */
    @Test
    public void testSetLocationStringLocation() {
        Location loc = mock(Location.class);
        is.setLocation("test", loc);
        assertEquals(loc, is.getLocation().get("test"));
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#getInventory(java.lang.String)}.
     */
    @Test
    public void testGetInventoryString() {
        assertTrue(is.getInventory("test").isEmpty());
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#isInventory(java.lang.String)}.
     */
    @Test
    public void testIsInventory() {
        assertFalse(is.isInventory("test"));
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#setGameMode(java.lang.String, org.bukkit.GameMode)}.
     */
    @Test
    public void testSetGameMode() {
        is.setGameMode("test", GameMode.ADVENTURE);
        assertEquals(GameMode.ADVENTURE, is.getGameMode("test"));
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#getGameMode(java.lang.String)}.
     */
    @Test
    public void testGetGameMode() {
        is.setGameMode("test2", GameMode.ADVENTURE);
        assertEquals(GameMode.SURVIVAL, is.getGameMode("test"));
        assertEquals(GameMode.ADVENTURE, is.getGameMode("test2"));
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#setAdvancement(java.lang.String, java.lang.String, java.util.List)}.
     */
    @Test
    public void testSetAdvancement() {
        is.setAdvancement("word", "key", List.of("criteria", "cirt"));
        assertTrue(is.getAdvancements("test").isEmpty());
        Map<String, List<String>> r = is.getAdvancements("word");
        assertEquals("cirt", r.get("key").get(1));
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#clearAdvancement(java.lang.String)}.
     */
    @Test
    public void testClearAdvancement() {
        is.setAdvancement("word", "key", List.of("criteria", "cirt"));
        assertFalse(is.getAdvancements("word").isEmpty());
        is.clearAdvancement("wowoow");
        assertFalse(is.getAdvancements("word").isEmpty());
        is.clearAdvancement("word");
        assertTrue(is.getAdvancements("word").isEmpty());
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#getAdvancements(java.lang.String)}.
     */
    @Test
    public void testGetAdvancements() {
        assertTrue(is.getAdvancements("test").isEmpty());
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#getEnderChest(java.lang.String)}.
     */
    @Test
    public void testGetEnderChestString() {
        assertTrue(is.getEnderChest("test").isEmpty());
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#setEnderChest(java.lang.String, java.util.List)}.
     */
    @Test
    public void testSetEnderChestStringListOfItemStack() {
        ItemStack item = new ItemStack(Material.ACACIA_BOAT);
        is.setEnderChest("worldName", List.of(item));
        Map<String, List<ItemStack>> map = is.getEnderChest();
        assertEquals(item, map.get("worldName").get(0));
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#getEnderChest()}.
     */
    @Test
    public void testGetEnderChest() {
        assertTrue(is.getEnderChest().isEmpty());
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#setEnderChest(java.util.Map)}.
     */
    @Test
    public void testSetEnderChestMapOfStringListOfItemStack() {
        ItemStack item = new ItemStack(Material.ACACIA_BOAT);
        Map<String, List<ItemStack>> map = Map.of("worldName", List.of(item));
        is.setEnderChest(map);
        Map<String, List<ItemStack>> map2 = is.getEnderChest();
        assertEquals(item, map2.get("worldName").get(0));
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#clearStats(java.lang.String)}.
     */
    @Test
    public void testClearStats() {
        is.setBlockStats("test", Map.of(Statistic.MINE_BLOCK, Map.of(Material.STONE, 3000)));
        is.setEntityStats("test", Map.of(Statistic.ENTITY_KILLED_BY, Map.of(EntityType.BEE, 4000)));
        is.setItemStats("test", Map.of(Statistic.USE_ITEM, Map.of(Material.DIAMOND_AXE, 5000)));
        is.setUntypedStats("test", Map.of(Statistic.ARMOR_CLEANED, 30));
        is.clearStats("test2");
        assertFalse(is.getBlockStats("test").isEmpty());
        assertFalse(is.getEntityStats("test").isEmpty());
        assertFalse(is.getItemStats("test").isEmpty());
        assertFalse(is.getUntypedStats("test").isEmpty());
        is.clearStats("test");
        assertTrue(is.getBlockStats("test").isEmpty());
        assertTrue(is.getEntityStats("test").isEmpty());
        assertTrue(is.getItemStats("test").isEmpty());
        assertTrue(is.getUntypedStats("test").isEmpty());
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#getUntypedStats(java.lang.String)}.
     */
    @Test
    public void testGetUntypedStats() {
        assertTrue(is.getUntypedStats("test").isEmpty());
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#setUntypedStats(java.lang.String, java.util.Map)}.
     */
    @Test
    public void testSetUntypedStats() {
        assertTrue(is.getUntypedStats("test").isEmpty());
        is.setUntypedStats("test", Map.of(Statistic.ARMOR_CLEANED, 30));
        assertEquals(30, is.getUntypedStats("test").get(Statistic.ARMOR_CLEANED).intValue());
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#getBlockStats(java.lang.String)}.
     */
    @Test
    public void testGetBlockStats() {
        assertTrue(is.getBlockStats("test").isEmpty());
        is.setBlockStats("test", Map.of(Statistic.MINE_BLOCK, Map.of(Material.STONE, 3000)));
        assertEquals(3000, is.getBlockStats("test").get(Statistic.MINE_BLOCK).get(Material.STONE).intValue());

    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#getItemStats(java.lang.String)}.
     */
    @Test
    public void testGetItemStats() {
        assertTrue(is.getItemStats("test").isEmpty());
        is.setItemStats("test", Map.of(Statistic.MINE_BLOCK, Map.of(Material.STONE, 3000)));
        assertEquals(3000, is.getItemStats("test").get(Statistic.MINE_BLOCK).get(Material.STONE).intValue());
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.dataobjects.InventoryStorage#getEntityStats(java.lang.String)}.
     */
    @Test
    public void testGetEntityStats() {
        assertTrue(is.getEntityStats("test").isEmpty());
        is.setEntityStats("test", Map.of(Statistic.MINE_BLOCK, Map.of(EntityType.BLAZE, 3000)));
        assertEquals(3000, is.getEntityStats("test").get(Statistic.MINE_BLOCK).get(EntityType.BLAZE).intValue());

    }

}
