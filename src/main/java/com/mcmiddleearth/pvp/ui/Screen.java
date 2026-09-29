package com.mcmiddleearth.pvp.ui;

import net.kyori.adventure.text.Component;

import java.util.List;

/**
 * A dialog screen, built fresh by the views for each opening and drawn by the Paper layer. {@code body} holds the
 * text lines above the buttons.
 */
public sealed interface Screen permits MenuScreen, FormScreen, ConfirmScreen, NoticeScreen {

    Component title();

    List<Component> body();
}
