package com.wasteofplastic.invswitcher.commands.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.wasteofplastic.invswitcher.commands.AbstractMoneyCommand;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

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

    /**
     * Send a message reporting the target's balance for the given world, with [name] and [number].
     * @param user - command sender
     * @param messageKey - locale key of the message
     * @param target - the target player
     * @param world - the world to report the balance for
     */
    protected void sendBalanceMessage(User user, String messageKey, User target, String world) {
        user.sendMessage(messageKey, TextVariables.NAME, target.getName(),
                TextVariables.NUMBER, economy().format(economy().getBalance(target.getOfflinePlayer(), world)));
    }

    /**
     * Tab-complete the player parameter (the first argument of these commands) with the names of
     * online players. The command tree is {@code <admin> eco <sub> <player> [amount]}, so the
     * player slot is the third token in the dispatched args.
     */
    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        if (args.size() == 3) {
            return Optional.of(Util.tabLimit(new ArrayList<>(Util.getOnlinePlayerList(user)), args.get(2)));
        }
        return Optional.empty();
    }
}
