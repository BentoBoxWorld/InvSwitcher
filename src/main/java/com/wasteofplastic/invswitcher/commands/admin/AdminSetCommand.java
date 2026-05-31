package com.wasteofplastic.invswitcher.commands.admin;

import org.bukkit.OfflinePlayer;

import com.wasteofplastic.invswitcher.economy.InvEconomy;

import net.milkbowl.vault.economy.EconomyResponse;
import world.bentobox.bentobox.api.commands.CompositeCommand;

/**
 * Sets a player's balance in this command's game mode world.
 * @author tastybento
 */
public class AdminSetCommand extends AbstractAdminAmountCommand {

    public AdminSetCommand(CompositeCommand parent) {
        super(parent, "set");
    }

    @Override
    public void setup() {
        this.setPermission("invswitcher.admin.eco.set");
        this.setParametersHelp("invswitcher.commands.admin.set.parameters");
        this.setDescription("invswitcher.commands.admin.set.description");
    }

    @Override
    protected EconomyResponse apply(InvEconomy eco, OfflinePlayer target, String world, double amount) {
        return eco.setBalance(target, world, amount);
    }

    @Override
    protected String successKey() {
        return "invswitcher.commands.admin.set.success";
    }
}
