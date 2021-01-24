package com.wasteofplastic.invswitcher;


import com.wasteofplastic.invswitcher.listeners.PlayerListener;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.database.DatabaseSetup.DatabaseType;

public class InvSwitcher extends Addon {
    private Store store;


    @Override
    public void onEnable() {
        // Verify that we're not running on a YAML database
        if (this.getPlugin().getSettings().getDatabaseType().equals(DatabaseType.YAML)) {
            this.setState(State.DISABLED);
            this.logError("This addon is incompatible with YAML database. Please use another type, like JSON.");
            return;
        }
        // Create the store
        store = new Store(this);
        // Register the listeners
        registerListener(new PlayerListener(this));
    }


    @Override
    public void onDisable()
    {
    }


    /**
     * @return the store
     */
    public Store getStore() {
        return store;
    }

}
