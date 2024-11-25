package com.wasteofplastic.invswitcher;


import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.World;

import com.wasteofplastic.invswitcher.listeners.PlayerListener;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.configuration.Config;
import world.bentobox.bentobox.database.DatabaseSetup.DatabaseType;

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
        // save cache
        if (store != null) {
            getStore().saveOnShutdown();
        }

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
