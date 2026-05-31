package com.wasteofplastic.invswitcher.commands.admin;

import java.util.List;

import com.wasteofplastic.invswitcher.InvSwitcher;
import com.wasteofplastic.invswitcher.commands.AbstractMoneyCommand;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;

/**
 * Admin economy command container: {@code give}, {@code take}, {@code set} and {@code balance}.
 * @author tastybento
 */
public class AdminMoneyCommand extends AbstractMoneyCommand {

    public AdminMoneyCommand(InvSwitcher addon, CompositeCommand parent) {
        super(addon, parent, "eco", "money");
    }

    @Override
    public void setup() {
        this.setPermission("invswitcher.admin.eco");
        this.setDescription("invswitcher.commands.admin.eco.description");
        new AdminBalanceCommand(this);
        new AdminGiveCommand(this);
        new AdminTakeCommand(this);
        new AdminSetCommand(this);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        this.showHelp(this, user);
        return true;
    }
}
