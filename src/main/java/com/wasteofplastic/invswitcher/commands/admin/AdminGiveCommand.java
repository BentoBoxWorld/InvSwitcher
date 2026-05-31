package com.wasteofplastic.invswitcher.commands.admin;

import org.bukkit.OfflinePlayer;

import com.wasteofplastic.invswitcher.economy.InvEconomy;

import net.milkbowl.vault.economy.EconomyResponse;
import world.bentobox.bentobox.api.commands.CompositeCommand;

/**
 * Gives money to a player in this command's game mode world.
 * @author tastybento
 */
public class AdminGiveCommand extends AbstractAdminAmountCommand {

    public AdminGiveCommand(CompositeCommand parent) {
        super(parent, "give");
    }

    @Override
    public void setup() {
        this.setPermission("invswitcher.admin.eco.give");
        this.setParametersHelp("invswitcher.commands.admin.give.parameters");
        this.setDescription("invswitcher.commands.admin.give.description");
    }

    @Override
    protected EconomyResponse apply(InvEconomy eco, OfflinePlayer target, String world, double amount) {
        return eco.depositPlayer(target, world, amount);
    }

    @Override
    protected String successKey() {
        return "invswitcher.commands.admin.give.success";
    }
}
