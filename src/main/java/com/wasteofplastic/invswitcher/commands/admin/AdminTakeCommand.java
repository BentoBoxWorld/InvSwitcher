package com.wasteofplastic.invswitcher.commands.admin;

import org.bukkit.OfflinePlayer;

import com.wasteofplastic.invswitcher.economy.InvEconomy;

import net.milkbowl.vault.economy.EconomyResponse;
import world.bentobox.bentobox.api.commands.CompositeCommand;

/**
 * Takes money from a player in this command's game mode world.
 * @author tastybento
 */
public class AdminTakeCommand extends AbstractAdminAmountCommand {

    public AdminTakeCommand(CompositeCommand parent) {
        super(parent, "take");
    }

    @Override
    public void setup() {
        this.setPermission("invswitcher.admin.eco.take");
        this.setParametersHelp("invswitcher.commands.admin.take.parameters");
        this.setDescription("invswitcher.commands.admin.take.description");
    }

    @Override
    protected EconomyResponse apply(InvEconomy eco, OfflinePlayer target, String world, double amount) {
        return eco.withdrawPlayer(target, world, amount);
    }

    @Override
    protected String successKey() {
        return "invswitcher.commands.admin.take.success";
    }
}
