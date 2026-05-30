package com.wasteofplastic.invswitcher.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
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
import org.mockbukkit.mockbukkit.MockBukkit;

import com.wasteofplastic.invswitcher.InvSwitcher;
import com.wasteofplastic.invswitcher.Settings;
import com.wasteofplastic.invswitcher.Store;
import com.wasteofplastic.invswitcher.dataobjects.InventoryStorage;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.DatabaseSetup.DatabaseType;
import world.bentobox.bentobox.managers.IslandsManager;

/**
 * Tests for {@link InvEconomy} per-world routing, delegation, starting balance and import.
 * @author tastybento
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class InvEconomyTest {

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
    @Mock
    private Logger logger;
    @Mock
    private Economy delegate;

    private Store store;
    private Settings sets;
    private Set<World> bentoboxWorlds;
    private UUID playerUUID;
    private InvEconomy economy;
    private MockedStatic<BentoBox> mockedBentoBox;

    @BeforeEach
    public void setUp() {
        MockBukkit.mock();

        BentoBox plugin = mock(BentoBox.class);
        mockedBentoBox = Mockito.mockStatic(BentoBox.class);
        mockedBentoBox.when(BentoBox::getInstance).thenReturn(plugin);
        when(plugin.getSettings()).thenReturn(bbSettings);
        DatabaseType mockDbt = mock(DatabaseType.class);
        when(bbSettings.getDatabaseType()).thenReturn(mockDbt);

        playerUUID = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUUID);
        // Treat the player as online and in the managed world by default
        when(player.getPlayer()).thenReturn(player);
        when(player.getWorld()).thenReturn(world);

        when(world.getName()).thenReturn("world");

        sets = new Settings();
        sets.setIslandsActive(false);
        sets.setImportExistingBalances(false);
        when(addon.getSettings()).thenReturn(sets);
        when(addon.getLogger()).thenReturn(logger);
        when(addon.getIslands()).thenReturn(islandsManager);

        bentoboxWorlds = new HashSet<>();
        bentoboxWorlds.add(world);
        when(addon.getWorlds()).thenReturn(bentoboxWorlds);

        store = new Store(addon);
        when(addon.getStore()).thenReturn(store);

        economy = new InvEconomy(addon, delegate);
    }

    @AfterEach
    public void tearDown() throws IOException {
        if (mockedBentoBox != null) {
            mockedBentoBox.close();
        }
        MockBukkit.unmock();
        File file = new File("database");
        if (file.exists()) {
            Files.walk(file.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    @Test
    public void testEnabledAndName() {
        assertTrue(economy.isEnabled());
        assertEquals("InvSwitcher", economy.getName());
    }

    @Test
    public void testFormatSingularAndPlural() {
        assertEquals("1.00 Dollar", economy.format(1.0));
        assertEquals("2.50 Dollars", economy.format(2.5));
    }

    @Test
    public void testDepositManagedWorld() {
        EconomyResponse r = economy.depositPlayer(player, 100.0);
        assertTrue(r.transactionSuccess());
        assertEquals(100.0, economy.getBalance(player), 0.0001);
        // Stored under the overworld key
        assertEquals(100.0, store.getStorageObject(playerUUID).getMoney("world"), 0.0001);
        // The delegate was never touched for a managed world
        verify(delegate, never()).depositPlayer(any(Player.class), anyDouble());
    }

    @Test
    public void testWithdrawSuccessAndInsufficient() {
        economy.depositPlayer(player, 100.0);
        EconomyResponse ok = economy.withdrawPlayer(player, 30.0);
        assertTrue(ok.transactionSuccess());
        assertEquals(70.0, economy.getBalance(player), 0.0001);

        EconomyResponse fail = economy.withdrawPlayer(player, 1000.0);
        assertFalse(fail.transactionSuccess());
        assertEquals(70.0, economy.getBalance(player), 0.0001);
    }

    @Test
    public void testStartingBalance() {
        sets.setStartingBalance(50.0);
        assertEquals(50.0, economy.getBalance(player), 0.0001);
    }

    @Test
    public void testImportOnce() {
        sets.setImportExistingBalances(true);
        when(delegate.getBalance(player)).thenReturn(500.0);

        // First touch imports the delegate balance
        assertEquals(500.0, economy.getBalance(player), 0.0001);
        assertTrue(store.getStorageObject(playerUUID).isImported());

        // Even if the delegate balance changes, our stored balance is now authoritative
        when(delegate.getBalance(player)).thenReturn(999.0);
        assertEquals(500.0, economy.getBalance(player), 0.0001);
    }

    @Test
    public void testDelegateUnmanagedWorld() {
        World lobby = mock(World.class);
        when(lobby.getName()).thenReturn("lobby");
        when(player.getWorld()).thenReturn(lobby); // not a managed world
        when(delegate.depositPlayer(player, 100.0))
                .thenReturn(new EconomyResponse(100, 100, ResponseType.SUCCESS, null));

        EconomyResponse r = economy.depositPlayer(player, 100.0);
        assertTrue(r.transactionSuccess());
        verify(delegate).depositPlayer(player, 100.0);
        // Nothing stored in our own data
        assertFalse(store.getStorageObject(playerUUID).hasMoney(Store.DEFAULT_WORLD_KEY));
    }

    @Test
    public void testSetBalance() {
        economy.depositPlayer(player, 100.0);
        EconomyResponse r = economy.setBalance(player, 42.0);
        assertTrue(r.transactionSuccess());
        assertEquals(42.0, economy.getBalance(player), 0.0001);
    }

    @Test
    public void testWorldAwareManagedRouting() {
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class, RETURNS_MOCKS)) {
            mockedBukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);
            EconomyResponse r = economy.depositPlayer(player, "world", 250.0);
            assertTrue(r.transactionSuccess());
        }
        assertEquals(250.0, store.getStorageObject(playerUUID).getMoney("world"), 0.0001);
        verify(delegate, never()).depositPlayer(any(Player.class), eq("world"), anyDouble());
    }

    @Test
    public void testWorldAwareUnmanagedDelegates() {
        when(delegate.depositPlayer(player, "lobby", 10.0))
                .thenReturn(new EconomyResponse(10, 10, ResponseType.SUCCESS, null));
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class, RETURNS_MOCKS)) {
            // Bukkit.getWorld for an unknown world returns null -> unmanaged -> delegate
            mockedBukkit.when(() -> Bukkit.getWorld("lobby")).thenReturn(null);
            economy.depositPlayer(player, "lobby", 10.0);
        }
        verify(delegate).depositPlayer(player, "lobby", 10.0);
    }

    @Test
    public void testOfflineRoutingUsesLastKey() {
        // Simulate the player having been online: this caches the storage object and persists lastKey
        sets.setStatistics(false);
        sets.setAdvancements(false);
        sets.setInventory(false);
        sets.setEnderChest(false);
        sets.setExperience(false);
        sets.setFood(false);
        sets.setHealth(false);
        sets.setGamemode(false);
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class, RETURNS_MOCKS)) {
            store.getInventory(player, world);
            store.storeInventory(player, world); // sets lastKey = "world"
        }
        // Now the player goes offline
        when(player.getPlayer()).thenReturn(null);

        // A deposit while offline must route to their last world ("world")
        EconomyResponse r = economy.depositPlayer(player, 75.0);
        assertTrue(r.transactionSuccess());
        assertEquals(75.0, store.getStorageObject(playerUUID).getMoney("world"), 0.0001);
        verify(delegate, never()).depositPlayer(any(Player.class), anyDouble());
    }

    @Test
    public void testOfflineUnknownWorldDelegates() {
        // Brand new player, offline, no lastKey -> cannot resolve -> delegate
        when(player.getPlayer()).thenReturn(null);
        when(delegate.depositPlayer(player, 5.0))
                .thenReturn(new EconomyResponse(5, 5, ResponseType.SUCCESS, null));
        economy.depositPlayer(player, 5.0);
        verify(delegate).depositPlayer(player, 5.0);
    }
}
