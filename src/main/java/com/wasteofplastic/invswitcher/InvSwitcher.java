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
        // Now that worlds are known, register the economy commands and placeholders. The economy
        // provider itself was registered earlier, in onEnable, so it beats shop plugins that cache
        // their Vault provider during their own startup.
        if (economy != null) {
            registerEconomyCommands();
        }
    }

    @Override
    public void onEnable() {
        // Verify that we're not running on a YAML database
        if (this.getPlugin().getSettings().getDatabaseType().equals(DatabaseType.YAML)) {
            this.setState(State.DISABLED);
            this.logError("This addon is incompatible with YAML database. Please use another type, like JSON.");
            return;
        }
        // Register the Vault economy provider as early as possible (here in onEnable, not in
        // allLoaded) so it is in place before economy-consuming plugins (e.g. QuickShop) resolve
        // and cache their provider. The store, worlds and delegate are resolved lazily by
        // InvEconomy, so they do not need to exist yet.
        if (getSettings() != null && getSettings().isMoney()) {
            if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
                logError("options.money is enabled but the Vault plugin is not installed - per-world money disabled.");
            } else {
                registerEconomyProvider();
            }
        }
    }

    /**
     * Creates and registers InvSwitcher's per-world economy at the highest Vault priority so it
     * intercepts every economy call. Runs once. The provider is lazy - it resolves the store and
     * the delegate economy on first use - so this can run before those are ready.
     */
    private void registerEconomyProvider() {
        if (economy != null) {
            return;
        }
        economy = new InvEconomy(this);
        Bukkit.getServicesManager().register(Economy.class, economy, getPlugin(), ServicePriority.Highest);

        // BentoBox captured its VaultHook during early hook registration, before us, so it (and
        // addons that use it, e.g. Bank) still points at the previous economy. Re-run the hook so
        // it re-reads the now-highest provider (us). The VaultHook is a single shared instance held
        // by those addons, so refreshing it updates them too.
        refreshBentoBoxVaultHook();

        // Dump the current economy provider chain so it is clear that we win the registration.
        logEconomyRegistrations();
    }

    /**
     * Registers the economy commands and placeholders against the game modes whose worlds
     * InvSwitcher manages. Called from allLoaded, once worlds are known.
     */
    private void registerEconomyCommands() {
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
     * Logs all registered Vault economy providers (highest priority first) and which one Vault
     * will hand out. Useful for confirming InvSwitcher won the registration and for spotting
     * consumers that cached a different provider before we registered.
     */
    private void logEconomyRegistrations() {
        log("Vault economy providers now registered (used by new lookups):");
        for (RegisteredServiceProvider<Economy> r : Bukkit.getServicesManager().getRegistrations(Economy.class)) {
            log(" - " + r.getProvider().getName() + " (" + r.getProvider().getClass().getName()
                    + ") priority=" + r.getPriority() + " registeredBy=" + r.getPlugin().getName());
        }
        RegisteredServiceProvider<Economy> top = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (top != null) {
            log("Vault.getRegistration(Economy) returns: " + top.getProvider().getName() + " ("
                    + top.getProvider().getClass().getName() + ")");
        }
        log("If a shop/economy plugin still uses the old balance, it cached its provider before now "
                + "and must be loaded after BentoBox (or re-resolve on ServiceRegisterEvent).");
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
