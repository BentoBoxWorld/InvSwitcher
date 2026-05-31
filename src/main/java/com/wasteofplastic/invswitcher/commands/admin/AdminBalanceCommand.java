package com.wasteofplastic.invswitcher.commands.admin;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;

/**
 * Shows an admin another player's balance for this command's game mode world.
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
        if (requireEconomy(user) == null) {
            return false;
        }
        User target = resolveTarget(user, args.get(0));
        if (target == null) {
            return false;
        }
        sendBalanceMessage(user, "invswitcher.commands.admin.balance.balance", target, getWorld().getName());
        return true;
    }
}
