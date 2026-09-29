package com.mcmiddleearth.pvp.archfixture;

import io.papermc.paper.dialog.Dialog;

import java.util.List;

/** Uses a Paper dialog type on purpose, so ArchitectureTest can show that the rule fails. */
public final class OpensDialogs {

    private OpensDialogs() {
    }

    public static Dialog first(List<Dialog> dialogs) {
        return dialogs.getFirst();
    }
}
