package com.wasteofplastic.invswitcher.commands.admin;

import java.util.List;

import com.wasteofplastic.invswitcher.economy.InvEconomy;

import net.milkbowl.vault.economy.EconomyResponse;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

/**
 * Takes money from a player in the world they are currently in (or were last in).
 * @author tastybento
 */
public class AdminTakeCommand extends AbstractAdminMoneyCommand {

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
        String world = getWorld().getName();
        EconomyResponse response = eco.withdrawPlayer(target.getOfflinePlayer(), world, amount);
        if (!response.transactionSuccess()) {
            user.sendMessage("invswitcher.errors.insufficient-funds");
            return false;
        }
        user.sendMessage("invswitcher.commands.admin.take.success",
                TextVariables.NAME, target.getName(),
                TextVariables.NUMBER, eco.format(eco.getBalance(target.getOfflinePlayer(), world)));
        return true;
    }
}
