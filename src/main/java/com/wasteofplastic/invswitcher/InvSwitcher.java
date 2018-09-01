package com.wasteofplastic.invswitcher;


import com.wasteofplastic.invswitcher.listeners.PlayerTeleportListener;

import world.bentobox.bentobox.api.addons.Addon;

public class InvSwitcher extends Addon {
    private Store store;


    @Override
    public void onEnable() {
        // Create the store
        store = new Store(this);
        // Register the listeners
        getServer().getPluginManager().registerEvents(new PlayerTeleportListener(this), getPlugin());
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
