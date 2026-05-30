package com.wasteofplastic.invswitcher;


import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;

import com.wasteofplastic.invswitcher.commands.admin.AdminMoneyCommand;
import com.wasteofplastic.invswitcher.commands.user.BalanceCommand;
import com.wasteofplastic.invswitcher.commands.user.PayCommand;
import com.wasteofplastic.invswitcher.economy.InvEconomy;
import com.wasteofplastic.invswitcher.listeners.PlayerListener;

import net.milkbowl.vault.economy.Economy;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.configuration.Config;
import world.bentobox.bentobox.database.DatabaseSetup.DatabaseType;
import world.bentobox.bentobox.hooks.VaultHook;

/**
 * Inventory switcher for worlds. Switches advancements too.
 *
 * @author tastybento
 *
 */
public class InvSwitcher extends Addon {

    private Store store;

    private Settings settings;

    private final Config<Settings> config = new Config<>(this, Settings.class);

    private Set<World> worlds = new HashSet<>();

    private InvEconomy economy;

    @Override
    public void onLoad() {
        // Save default config.yml
        this.saveDefaultConfig();
        // Load the plugin's config
        this.loadSettings();
    }

    @Override
    public void allLoaded() {
        // Load worlds
        worlds = getSettings().getWorlds()
                .stream()
                .map(String::toLowerCase)
                .map(Bukkit::getWorld)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (worlds.isEmpty()) {
            logWarning("Did not hook into any worlds - disabling addon!");
            this.setState(State.DISABLED);
            return;
        }
        // Add nethers and ends
        Set<World> netherEnds = new HashSet<>();
        log("Hooking into the following worlds:");
        worlds.forEach(w -> {
            log(w.getName());
            World nether = Bukkit.getWorld(w.getName() + "_nether");
            if (nether != null) {
                netherEnds.add(nether);
                log(nether.getName());
            }
            World end = Bukkit.getWorld(w.getName() + "_the_end");
            if (end != null) {
                netherEnds.add(end);
                log(end.getName());
            }
        });
        worlds.addAll(netherEnds);
        // Create the store
        store = new Store(this);
        // Register the listeners
        registerListener(new PlayerListener(this));
        // Set up the per-world economy. Deferred by a tick so that the underlying economy
        // plugin (e.g. EssentialsX) has finished registering its own provider with Vault,
        // letting us capture it as the delegate for unmanaged worlds.
        if (settings.isMoney()) {
            if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
                logError("options.money is enabled but the Vault plugin is not installed - per-world money disabled.");
            } else {
                Bukkit.getScheduler().runTask(getPlugin(), this::setupEconomy);
            }
        }
    }

    /**
     * Captures the previously-registered Vault economy (to delegate unmanaged worlds to) and
     * registers InvSwitcher's own per-world economy at the highest priority so it intercepts
     * every economy call. Runs once.
     */
    private void setupEconomy() {
        if (economy != null) {
            return;
        }
        // Capture the existing provider BEFORE we register ourselves, skipping our own type
        // so a re-run can never capture itself into a delegation loop.
        Economy delegate = null;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp != null && !(rsp.getProvider() instanceof InvEconomy)) {
            delegate = rsp.getProvider();
        }
        if (delegate == null) {
            logWarning("No previous economy was found - InvSwitcher will be the only economy. "
                    + "Worlds it does not manage will share a single balance.");
        } else {
            log("Per-world economy enabled - delegating unmanaged worlds to " + delegate.getName());
        }
        economy = new InvEconomy(this, delegate);
        Bukkit.getServicesManager().register(Economy.class, economy, getPlugin(), ServicePriority.Highest);

        // BentoBox captured its VaultHook during early hook registration, before we registered -
        // so it (and addons that use it, e.g. Bank, Level, Upgrades) still points at the previous
        // economy. Re-run the hook so it re-reads the now-highest provider (us). The VaultHook is a
        // single shared instance held by those addons, so refreshing it updates them too.
        refreshBentoBoxVaultHook();

        // Register commands and placeholders with the game modes whose worlds we manage
        PhManager phManager = new PhManager(this);
        getPlugin().getAddonsManager().getGameModeAddons().stream()
                .filter(gm -> worlds.contains(gm.getOverWorld()))
                .forEach(gm -> {
                    gm.getPlayerCommand().ifPresent(pc -> {
                        new BalanceCommand(this, pc);
                        new PayCommand(this, pc);
                    });
                    gm.getAdminCommand().ifPresent(ac -> new AdminMoneyCommand(this, ac));
                    if (!phManager.registerPlaceholders(gm)) {
                        logWarning("Could not register economy placeholders - no PlaceholderManager available.");
                    }
                    log("Per-world economy hooking into " + gm.getDescription().getName());
                });
    }

    @Override
    public void onEnable() {
        // Verify that we're not running on a YAML database
        if (this.getPlugin().getSettings().getDatabaseType().equals(DatabaseType.YAML)) {
            this.setState(State.DISABLED);
            this.logError("This addon is incompatible with YAML database. Please use another type, like JSON.");
        }
    }


    @Override
    public void onDisable() {
        // Unregister our economy so a reload does not stack providers
        if (economy != null) {
            Bukkit.getServicesManager().unregister(Economy.class, economy);
            economy = null;
            // Re-point BentoBox's VaultHook at whatever economy remains (e.g. EssentialsC)
            refreshBentoBoxVaultHook();
        }
        // save cache
        if (store != null) {
            getStore().saveOnShutdown();
        }

    }

    /**
     * Re-runs BentoBox's VaultHook so it re-reads the highest-priority economy currently
     * registered with the services manager. BentoBox addons such as Bank hold this same hook
     * instance, so they pick up the change without needing to re-hook themselves.
     */
    private void refreshBentoBoxVaultHook() {
        getPlugin().getVault().ifPresent(VaultHook::hook);
    }

    /**
     * @return the per-world economy, or null if money is disabled or Vault is absent
     */
    public InvEconomy getEconomy() {
        return economy;
    }


    /**
     * @return the store
     */
    public Store getStore() {
        return store;
    }

    /**
     * This method loads addon configuration settings in memory.
     */
    private void loadSettings() {
        this.settings = config.loadConfigObject();

        if (this.settings == null) {
            // Disable
            this.logError("InvSwitcher settings could not load! Addon disabled.");
            this.setState(State.DISABLED);
            return;
        }
        // Save new version
        this.config.saveConfigObject(settings);
    }

    public Settings getSettings() {
        return this.settings;
    }

    /**
     * Get the hooked worlds
     * @return the worlds
     */
    public Set<World> getWorlds() {
        return worlds;
    }


}
