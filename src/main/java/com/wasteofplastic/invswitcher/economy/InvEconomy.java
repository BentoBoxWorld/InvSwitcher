package com.wasteofplastic.invswitcher.economy;

import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import com.wasteofplastic.invswitcher.InvSwitcher;
import com.wasteofplastic.invswitcher.Settings;
import com.wasteofplastic.invswitcher.Store;
import com.wasteofplastic.invswitcher.dataobjects.InventoryStorage;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;

/**
 * Vault {@link Economy} implementation that gives every InvSwitcher-managed world its own
 * balance. InvSwitcher registers this provider at the highest priority so it intercepts every
 * economy call server-wide and routes it to the correct world's balance &mdash; even when the
 * target player is offline or in a different world.
 * <p>
 * Worlds that InvSwitcher does not manage are passed through to the economy provider that was
 * registered before InvSwitcher (the {@code delegate}, e.g. EssentialsX), so the rest of the
 * server's economy is unaffected.
 *
 * @author tastybento
 */
public class InvEconomy implements Economy {

    private static final String NEGATIVE_DEPOSIT = "Cannot deposit a negative amount";
    private static final String NEGATIVE_WITHDRAW = "Cannot withdraw a negative amount";
    private static final String INSUFFICIENT_FUNDS = "Insufficient funds";

    private final InvSwitcher addon;
    private final Store store;
    /** The economy provider that was registered before us. May be null if none existed. */
    private final Economy delegate;

    /**
     * @param addon - the InvSwitcher addon
     * @param delegate - the previously-registered economy, or null if none
     */
    public InvEconomy(InvSwitcher addon, Economy delegate) {
        this.addon = addon;
        this.store = addon.getStore();
        this.delegate = delegate;
    }

    private Settings settings() {
        return addon.getSettings();
    }

    /**
     * @return true if unmanaged worlds should be delegated to the previous provider
     */
    private boolean delegating() {
        return settings().isDelegateUnmanagedWorlds() && delegate != null;
    }

    // ------ STATUS / FORMATTING ------

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return "InvSwitcher";
    }

    @Override
    public boolean hasBankSupport() {
        return delegate != null && delegate.hasBankSupport();
    }

    @Override
    public int fractionalDigits() {
        return settings().getFractionalDigits();
    }

    @Override
    public String format(double amount) {
        String name = Math.abs(amount - 1.0D) < 1.0E-9D ? currencyNameSingular() : currencyNamePlural();
        return String.format("%,." + Math.max(0, fractionalDigits()) + "f %s", amount, name);
    }

    @Override
    public String currencyNamePlural() {
        return settings().getCurrencyNamePlural();
    }

    @Override
    public String currencyNameSingular() {
        return settings().getCurrencyNameSingular();
    }

    // ------ CORE BALANCE LOGIC (operates on a single storage object per call) ------

    /**
     * Ensure the key has a balance, seeding it (via import or starting balance) if absent.
     * Mutates {@code s} in memory only; the caller is responsible for persisting.
     */
    private double ensureBalance(OfflinePlayer player, InventoryStorage s, String key) {
        if (!s.hasMoney(key)) {
            s.setMoney(key, seedBalance(player, s));
        }
        return s.getMoney(key);
    }

    /**
     * Determine the initial balance for a player's first-ever managed-world key: either their
     * existing balance imported once from the previous economy, or the configured starting
     * balance.
     */
    private double seedBalance(OfflinePlayer player, InventoryStorage s) {
        Settings settings = settings();
        if (settings.isImportExistingBalances() && delegate != null && !s.isImported()) {
            s.setImported(true);
            return delegate.getBalance(player);
        }
        return settings.getStartingBalance();
    }

    private double readSelf(OfflinePlayer player, String key) {
        InventoryStorage s = store.getStorageObject(player.getUniqueId());
        boolean existed = s.hasMoney(key);
        double balance = ensureBalance(player, s, key);
        if (!existed) {
            store.saveStorage(s);
        }
        return balance;
    }

    private EconomyResponse depositSelf(OfflinePlayer player, String key, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, readSelf(player, key), ResponseType.FAILURE, NEGATIVE_DEPOSIT);
        }
        InventoryStorage s = store.getStorageObject(player.getUniqueId());
        double newBalance = ensureBalance(player, s, key) + amount;
        s.setMoney(key, newBalance);
        store.saveStorage(s);
        return new EconomyResponse(amount, newBalance, ResponseType.SUCCESS, null);
    }

    private EconomyResponse setSelf(OfflinePlayer player, String key, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, readSelf(player, key), ResponseType.FAILURE, NEGATIVE_DEPOSIT);
        }
        InventoryStorage s = store.getStorageObject(player.getUniqueId());
        // Mark imported so a later seed does not re-import on top of an explicitly set balance
        s.setImported(true);
        s.setMoney(key, amount);
        store.saveStorage(s);
        return new EconomyResponse(amount, amount, ResponseType.SUCCESS, null);
    }

    private EconomyResponse withdrawSelf(OfflinePlayer player, String key, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, readSelf(player, key), ResponseType.FAILURE, NEGATIVE_WITHDRAW);
        }
        InventoryStorage s = store.getStorageObject(player.getUniqueId());
        double balance = ensureBalance(player, s, key);
        if (balance < amount) {
            return new EconomyResponse(0, balance, ResponseType.FAILURE, INSUFFICIENT_FUNDS);
        }
        double newBalance = balance - amount;
        s.setMoney(key, newBalance);
        store.saveStorage(s);
        return new EconomyResponse(amount, newBalance, ResponseType.SUCCESS, null);
    }

    // ------ ACCOUNT METHODS (OfflinePlayer) ------

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return true;
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return true;
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        String key = store.getCurrentMoneyKey(player);
        if (key == null) {
            return delegating() ? delegate.getBalance(player) : readSelf(player, Store.DEFAULT_WORLD_KEY);
        }
        return readSelf(player, key);
    }

    @Override
    public double getBalance(OfflinePlayer player, String worldName) {
        String key = store.getMoneyKey(player, Bukkit.getWorld(worldName));
        if (key == null) {
            return delegating() ? delegate.getBalance(player, worldName) : readSelf(player, Store.DEFAULT_WORLD_KEY);
        }
        return readSelf(player, key);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return getBalance(player, worldName) >= amount;
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        String key = store.getCurrentMoneyKey(player);
        if (key == null) {
            return delegating() ? delegate.withdrawPlayer(player, amount)
                    : withdrawSelf(player, Store.DEFAULT_WORLD_KEY, amount);
        }
        return withdrawSelf(player, key, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        String key = store.getMoneyKey(player, Bukkit.getWorld(worldName));
        if (key == null) {
            return delegating() ? delegate.withdrawPlayer(player, worldName, amount)
                    : withdrawSelf(player, Store.DEFAULT_WORLD_KEY, amount);
        }
        return withdrawSelf(player, key, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        String key = store.getCurrentMoneyKey(player);
        if (key == null) {
            return delegating() ? delegate.depositPlayer(player, amount)
                    : depositSelf(player, Store.DEFAULT_WORLD_KEY, amount);
        }
        return depositSelf(player, key, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        String key = store.getMoneyKey(player, Bukkit.getWorld(worldName));
        if (key == null) {
            return delegating() ? delegate.depositPlayer(player, worldName, amount)
                    : depositSelf(player, Store.DEFAULT_WORLD_KEY, amount);
        }
        return depositSelf(player, key, amount);
    }

    /**
     * Set a player's balance for their current (online) or last-known (offline) world. Not part
     * of the Vault interface; used by InvSwitcher's own admin commands.
     * @param player - player
     * @param amount - new balance
     * @return the economy response
     */
    public EconomyResponse setBalance(OfflinePlayer player, double amount) {
        String key = store.getCurrentMoneyKey(player);
        if (key == null) {
            if (delegating()) {
                // Vault has no set operation; emulate it against the delegate's balance
                double delta = amount - delegate.getBalance(player);
                return delta >= 0 ? delegate.depositPlayer(player, delta) : delegate.withdrawPlayer(player, -delta);
            }
            key = Store.DEFAULT_WORLD_KEY;
        }
        return setSelf(player, key, amount);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return true;
    }

    // ------ DEPRECATED NAME-BASED METHODS (delegate up to OfflinePlayer variants) ------

    @Override
    @Deprecated
    public boolean hasAccount(String playerName) {
        return hasAccount(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    @Deprecated
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(Bukkit.getOfflinePlayer(playerName), worldName);
    }

    @Override
    @Deprecated
    public double getBalance(String playerName) {
        return getBalance(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    @Deprecated
    public double getBalance(String playerName, String world) {
        return getBalance(Bukkit.getOfflinePlayer(playerName), world);
    }

    @Override
    @Deprecated
    public boolean has(String playerName, double amount) {
        return has(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    @Deprecated
    public boolean has(String playerName, String worldName, double amount) {
        return has(Bukkit.getOfflinePlayer(playerName), worldName, amount);
    }

    @Override
    @Deprecated
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return withdrawPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    @Deprecated
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(Bukkit.getOfflinePlayer(playerName), worldName, amount);
    }

    @Override
    @Deprecated
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return depositPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    @Deprecated
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(Bukkit.getOfflinePlayer(playerName), worldName, amount);
    }

    @Override
    @Deprecated
    public boolean createPlayerAccount(String playerName) {
        return createPlayerAccount(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    @Deprecated
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(Bukkit.getOfflinePlayer(playerName), worldName);
    }

    // ------ BANK METHODS (passed through to the delegate; not partitioned per world) ------

    private EconomyResponse bankUnsupported() {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "InvSwitcher does not support banks");
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return delegate != null ? delegate.createBank(name, player) : bankUnsupported();
    }

    @Override
    @Deprecated
    public EconomyResponse createBank(String name, String player) {
        return delegate != null ? delegate.createBank(name, player) : bankUnsupported();
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return delegate != null ? delegate.deleteBank(name) : bankUnsupported();
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return delegate != null ? delegate.bankBalance(name) : bankUnsupported();
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return delegate != null ? delegate.bankHas(name, amount) : bankUnsupported();
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return delegate != null ? delegate.bankWithdraw(name, amount) : bankUnsupported();
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return delegate != null ? delegate.bankDeposit(name, amount) : bankUnsupported();
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return delegate != null ? delegate.isBankOwner(name, player) : bankUnsupported();
    }

    @Override
    @Deprecated
    public EconomyResponse isBankOwner(String name, String playerName) {
        return delegate != null ? delegate.isBankOwner(name, playerName) : bankUnsupported();
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return delegate != null ? delegate.isBankMember(name, player) : bankUnsupported();
    }

    @Override
    @Deprecated
    public EconomyResponse isBankMember(String name, String playerName) {
        return delegate != null ? delegate.isBankMember(name, playerName) : bankUnsupported();
    }

    @Override
    public List<String> getBanks() {
        return delegate != null ? delegate.getBanks() : Collections.emptyList();
    }
}
