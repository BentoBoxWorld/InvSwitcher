package com.wasteofplastic.invswitcher.commands.user;

import java.util.List;

import com.wasteofplastic.invswitcher.InvSwitcher;
import com.wasteofplastic.invswitcher.commands.AbstractMoneyCommand;
import com.wasteofplastic.invswitcher.economy.InvEconomy;

import net.milkbowl.vault.economy.EconomyResponse;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

/**
 * Pays another player from the sender's current-world balance. The recipient is credited in the
 * world they are currently in (or were last in), so the payment is world-correct even if they are
 * offline or elsewhere.
 * @author tastybento
 */
public class PayCommand extends AbstractMoneyCommand {

    public PayCommand(InvSwitcher addon, CompositeCommand parent) {
        super(addon, parent, "pay");
    }

    @Override
    public void setup() {
        this.setOnlyPlayer(true);
        this.setPermission("invswitcher.pay");
        this.setParametersHelp("invswitcher.commands.pay.parameters");
        this.setDescription("invswitcher.commands.pay.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() != 2) {
            this.showHelp(this, user);
            return false;
        }
        InvEconomy eco = requireEconomy(user);
        if (eco == null) {
            return false;
        }
        User target = getPlayers().getUser(args.get(0));
        if (target == null || target.getUniqueId() == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        if (target.getUniqueId().equals(user.getUniqueId())) {
            user.sendMessage("invswitcher.errors.cannot-pay-self");
            return false;
        }
        Double amount = parseAmount(user, args.get(1));
        if (amount == null) {
            return false;
        }
        // Pay within this command's game mode economy (its world), regardless of where either
        // player is currently standing.
        String world = getWorld().getName();
        if (!eco.has(user.getPlayer(), world, amount)) {
            user.sendMessage("invswitcher.errors.insufficient-funds");
            return false;
        }
        EconomyResponse withdrawal = eco.withdrawPlayer(user.getPlayer(), world, amount);
        if (!withdrawal.transactionSuccess()) {
            user.sendMessage("invswitcher.errors.insufficient-funds");
            return false;
        }
        eco.depositPlayer(target.getOfflinePlayer(), world, amount);
        user.sendMessage("invswitcher.commands.pay.sent",
                TextVariables.NUMBER, eco.format(amount), TextVariables.NAME, target.getName());
        if (target.isOnline()) {
            target.sendMessage("invswitcher.commands.pay.received",
                    TextVariables.NUMBER, eco.format(amount), TextVariables.NAME, user.getName());
        }
        return true;
    }
}
