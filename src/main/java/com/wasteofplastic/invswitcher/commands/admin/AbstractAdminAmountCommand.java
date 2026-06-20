package com.wasteofplastic.invswitcher.commands.admin;

import java.util.List;

import org.bukkit.OfflinePlayer;

import com.wasteofplastic.invswitcher.economy.InvEconomy;

import net.milkbowl.vault.economy.EconomyResponse;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;

/**
 * Base for admin economy commands of the form {@code <command> <player> <amount>} (give, take,
 * set). Handles the shared validation, applies the subclass's money operation against the
 * command's game mode world, and reports the resulting balance. Subclasses only define the
 * operation and the success message.
 * @author tastybento
 */
public abstract class AbstractAdminAmountCommand extends AbstractAdminMoneyCommand {

    protected AbstractAdminAmountCommand(CompositeCommand parent, String label, String... aliases) {
        super(parent, label, aliases);
    }

    /**
     * Perform the money operation on the target for the given world.
     * @param eco - the economy
     * @param target - the target player
     * @param world - the command's game mode world name
     * @param amount - the parsed, positive amount
     * @return the economy response (a non-success response reports insufficient funds)
     */
    protected abstract EconomyResponse apply(InvEconomy eco, OfflinePlayer target, String world, double amount);

    /**
     * @return the locale key of the success message, which receives [name] and [number]
     */
    protected abstract String successKey();

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
        User target = resolveTarget(user, args.get(0));
        if (target == null) {
            return false;
        }
        Double amount = parseAmount(user, args.get(1));
        if (amount == null) {
            return false;
        }
        String world = getWorld().getName();
        EconomyResponse response = apply(eco, target.getOfflinePlayer(), world, amount);
        if (!response.transactionSuccess()) {
            user.sendMessage("invswitcher.errors.insufficient-funds");
            return false;
        }
        // Report the balance returned by the transaction itself. Re-reading here would reload an
        // offline target fresh from the database before the asynchronous save has flushed, showing
        // the stale pre-transaction balance.
        sendBalanceMessage(user, successKey(), target, response.balance);
        return true;
    }
}
