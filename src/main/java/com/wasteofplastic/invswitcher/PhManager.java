package com.wasteofplastic.invswitcher;

import com.wasteofplastic.invswitcher.economy.InvEconomy;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.user.User;

/**
 * Registers PlaceholderAPI placeholders for a player's per-world balance.
 * @author tastybento
 */
public class PhManager {

    private final BentoBox plugin;
    private final InvSwitcher addon;

    public PhManager(InvSwitcher addon) {
        this.addon = addon;
        this.plugin = addon.getPlugin();
    }

    /**
     * Register the balance placeholders for a game mode.
     * @param gm - game mode addon
     * @return true if registered, false if no placeholder manager is available
     */
    public boolean registerPlaceholders(GameModeAddon gm) {
        if (plugin.getPlaceholdersManager() == null) {
            return false;
        }
        String prefix = gm.getDescription().getName().toLowerCase() + "_invswitcher_";
        // Raw balance number for the player's current world
        plugin.getPlaceholdersManager().registerPlaceholder(addon, prefix + "balance",
                user -> balance(user, false));
        // Formatted balance (currency name + decimals) for the player's current world
        plugin.getPlaceholdersManager().registerPlaceholder(addon, prefix + "balance_formatted",
                user -> balance(user, true));
        return true;
    }

    private String balance(User user, boolean formatted) {
        InvEconomy eco = addon.getEconomy();
        if (eco == null || user == null || !user.isPlayer()) {
            return "";
        }
        double bal = eco.getBalance(user.getPlayer());
        return formatted ? eco.format(bal) : String.valueOf(bal);
    }
}
