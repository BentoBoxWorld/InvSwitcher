package com.wasteofplastic.invswitcher.commands.admin;

import java.util.List;

import com.wasteofplastic.invswitcher.economy.InvEconomy;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

/**
 * Shows an admin another player's balance for the world that player is currently in (or was last in).
 * @author tastybento
 */
public class AdminBalanceCommand extends AbstractAdminMoneyCommand {

    public AdminBalanceCommand(CompositeCommand parent) {
        super(parent, "balance", "bal");
    }

    @Override
    public void setup() {
        this.setPermission("invswitcher.admin.eco.balance");
        this.setParametersHelp("invswitcher.commands.admin.balance.parameters");
        this.setDescription("invswitcher.commands.admin.balance.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() != 1) {
            this.showHelp(this, user);
            return false;
        }
        InvEconomy eco = economy();
        if (eco == null) {
            user.sendMessage("invswitcher.errors.no-economy");
            return false;
        }
        User target = resolveTarget(user, args.get(0));
        if (target == null) {
            return false;
        }
        user.sendMessage("invswitcher.commands.admin.balance.balance",
                TextVariables.NAME, target.getName(),
                TextVariables.NUMBER, eco.format(eco.getBalance(target.getOfflinePlayer(), getWorld().getName())));
        return true;
    }
}
