package com.wasteofplastic.invswitcher.commands.user;

import java.util.List;

import com.wasteofplastic.invswitcher.InvSwitcher;
import com.wasteofplastic.invswitcher.commands.AbstractMoneyCommand;
import com.wasteofplastic.invswitcher.economy.InvEconomy;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

/**
 * Shows a player their balance for the world they are currently in.
 * @author tastybento
 */
public class BalanceCommand extends AbstractMoneyCommand {

    public BalanceCommand(InvSwitcher addon, CompositeCommand parent) {
        super(addon, parent, "balance", "bal", "money");
    }

    @Override
    public void setup() {
        this.setOnlyPlayer(true);
        this.setPermission("invswitcher.balance");
        this.setDescription("invswitcher.commands.balance.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        InvEconomy eco = economy();
        if (eco == null) {
            user.sendMessage("invswitcher.errors.no-economy");
            return false;
        }
        double balance = eco.getBalance(user.getPlayer());
        user.sendMessage("invswitcher.commands.balance.balance", TextVariables.NUMBER, eco.format(balance));
        return true;
    }
}
