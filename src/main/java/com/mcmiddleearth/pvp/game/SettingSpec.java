package com.mcmiddleearth.pvp.game;

/**
 * A setting as offered for one arena: its range there and its default.
 * {@code min}, {@code max} and {@code step} are 0 for a {@link BoolSetting}; {@code defaultValue} is an Integer or a Boolean.
 */
public record SettingSpec(Setting setting, int min, int max, int step, Object defaultValue) {

    /** A whole-number setting with only one possible value here; menus show it as text instead of a slider. */
    public boolean fixed() {
        return setting instanceof IntSetting && min == max;
    }
}
