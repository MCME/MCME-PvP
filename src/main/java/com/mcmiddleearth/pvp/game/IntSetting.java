package com.mcmiddleearth.pvp.game;

/** A whole-number setting and its widest range. The range for one arena can be narrower (see {@link SettingSpec}). */
public record IntSetting(String id, int min, int max, int step) implements Setting {
}
