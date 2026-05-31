package com.wasteofplastic.invswitcher.commands;

import com.wasteofplastic.invswitcher.InvSwitcher;
import com.wasteofplastic.invswitcher.economy.InvEconomy;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

/**
 * Shared base for InvSwitcher's economy commands.
 * @author tastybento
 */
public abstract class AbstractMoneyCommand extends CompositeCommand {

    protected final InvSwitcher addon;

    /**
     * Constructor for a top-level command registered into a game mode's command tree. The
     * InvSwitcher addon must be passed explicitly, otherwise the command would inherit the
     * game mode addon from its parent.
     */
    protected AbstractMoneyCommand(InvSwitcher addon, CompositeCommand parent, String label, String... aliases) {
        super(addon, parent, label, aliases);
        this.addon = getAddon();
    }

    /**
     * Constructor for a nested sub-command; inherits the addon from its parent.
     */
    protected AbstractMoneyCommand(CompositeCommand parent, String label, String... aliases) {
        super(parent, label, aliases);
        this.addon = getAddon();
    }

    /**
     * @return the per-world economy, or null if money is disabled or Vault is absent
     */
    protected InvEconomy economy() {
        return addon.getEconomy();
    }

    /**
     * Get the economy or send the "no economy" error to the user.
     * @param user - command sender
     * @return the economy, or null if unavailable (an error has been sent)
     */
    protected InvEconomy requireEconomy(User user) {
        InvEconomy eco = economy();
        if (eco == null) {
            user.sendMessage("invswitcher.errors.no-economy");
        }
        return eco;
    }

    /**
     * Parse a strictly-positive money amount, sending an error message on failure.
     * @param user - command sender
     * @param arg - the argument to parse
     * @return the amount, or null if it could not be parsed or was not positive
     */
    protected Double parseAmount(User user, String arg) {
        double amount;
        try {
            amount = Double.parseDouble(arg);
        } catch (NumberFormatException e) {
            user.sendMessage("invswitcher.errors.not-a-number", TextVariables.NUMBER, arg);
            return null;
        }
        if (amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            user.sendMessage("invswitcher.errors.must-be-positive");
            return null;
        }
        return amount;
    }
}
