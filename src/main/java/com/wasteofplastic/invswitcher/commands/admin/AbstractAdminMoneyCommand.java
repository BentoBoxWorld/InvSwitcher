package com.wasteofplastic.invswitcher.commands.admin;

import com.wasteofplastic.invswitcher.commands.AbstractMoneyCommand;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

/**
 * Shared base for admin economy sub-commands of the form {@code <command> <player> <amount>}.
 * @author tastybento
 */
public abstract class AbstractAdminMoneyCommand extends AbstractMoneyCommand {

    protected AbstractAdminMoneyCommand(CompositeCommand parent, String label, String... aliases) {
        super(parent, label, aliases);
    }

    /**
     * Resolve a target player by name, sending an error message if unknown.
     * @param user - command sender
     * @param name - player name
     * @return the target user, or null if unknown
     */
    protected User resolveTarget(User user, String name) {
        User target = getPlayers().getUser(name);
        if (target == null || target.getUniqueId() == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, name);
            return null;
        }
        return target;
    }
}
