package com.wasteofplastic.invswitcher;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.Pladdon;

public class InvSwitcherPladdon extends Pladdon {

    @Override
    public Addon getAddon() {
        return new InvSwitcher();
    }
}
