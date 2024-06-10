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
package com.mcmiddleearth.mcme.pvp.Gamemode;

import com.mcmiddleearth.mcme.pvp.maps.Map;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Donovan <dallen@dallen.xyz>
 */
public interface Gamemode {
    
    void Start(Map m, int parameter);
    
    ArrayList<Player> getPlayers();
    
    com.mcmiddleearth.mcme.pvp.Gamemode.BasePluginGamemode.GameState getState();
    
    ArrayList<String> getNeededPoints();
    
    void End(Map m);
    
    boolean midgamePlayerJoin(Player p);
    
    String requiresParameter();
    
    boolean isMidgameJoin();

    void incrementPlayerDeaths(Player player);

    HashMap<Player, Double> getTopKDMap();

    HashMap<Player, Integer> getTopDeathsMap();

    HashMap<Player, Integer> getTopKillsMap();

    boolean isFrozen(Player player);

    void incrementPlayerKills(Player player);
}
