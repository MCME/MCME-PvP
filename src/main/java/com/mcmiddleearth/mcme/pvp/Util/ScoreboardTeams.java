/*
 * This file is part of MCME-pvp.
 *
 * MCME-pvp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MCME-pvp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MCME-pvp.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */
package com.mcmiddleearth.mcme.pvp.Util;

import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Scoreboard team helpers.
 */
public final class ScoreboardTeams {

    private ScoreboardTeams() {
    }

    /**
     * Returns the team with this name, registering it first if the scoreboard does not have it.
     * {@link Scoreboard#registerNewTeam} throws for a name that is already taken, for example a
     * team that the main scoreboard kept from an earlier start.
     */
    public static Team getOrRegister(Scoreboard scoreboard, String name) {
        Team existing = scoreboard.getTeam(name);
        return existing != null ? existing : scoreboard.registerNewTeam(name);
    }
}
