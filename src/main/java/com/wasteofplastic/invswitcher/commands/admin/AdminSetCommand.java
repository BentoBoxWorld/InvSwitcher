package com.wasteofplastic.invswitcher.commands.admin;

import java.util.List;

import com.wasteofplastic.invswitcher.economy.InvEconomy;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

/**
 * Sets a player's balance in the world they are currently in (or were last in).
 * @author tastybento
 */
public class AdminSetCommand extends AbstractAdminMoneyCommand {

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
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() != 2) {
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
        Double amount = parseAmount(user, args.get(1));
        if (amount == null) {
            return false;
        }
        eco.setBalance(target.getOfflinePlayer(), amount);
        user.sendMessage("invswitcher.commands.admin.set.success",
                TextVariables.NAME, target.getName(),
                TextVariables.NUMBER, eco.format(eco.getBalance(target.getOfflinePlayer())));
        return true;
    }
}
