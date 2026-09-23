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
package com.mcmiddleearth.mcme.pvp.Handlers;

import com.mcmiddleearth.mcme.pvp.Gamemode.BasePluginGamemode;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.entity.Player;

/**
 *
 * @author Eric
 */
public class BukkitTeamHandler {
    private static org.bukkit.scoreboard.Team aqua;
    private static org.bukkit.scoreboard.Team blue;
    private static org.bukkit.scoreboard.Team darkAqua;
    private static org.bukkit.scoreboard.Team darkGreen;
    private static org.bukkit.scoreboard.Team darkPurple;
    private static org.bukkit.scoreboard.Team darkRed;
    private static org.bukkit.scoreboard.Team gold;
    private static org.bukkit.scoreboard.Team gray;
    private static org.bukkit.scoreboard.Team green;
    private static org.bukkit.scoreboard.Team lightPurple;
    private static org.bukkit.scoreboard.Team red;
    private static org.bukkit.scoreboard.Team yellow;
     
    
    public static void configureBukkitTeams(){
        aqua = colorTeam("aqua", NamedTextColor.AQUA);
        blue = colorTeam("blue", NamedTextColor.BLUE);
        darkAqua = colorTeam("darkaqua", NamedTextColor.DARK_AQUA);
        darkGreen = colorTeam("darkgreen", NamedTextColor.DARK_GREEN);
        darkPurple = colorTeam("darkpurple", NamedTextColor.DARK_PURPLE);
        darkRed = colorTeam("darkred", NamedTextColor.DARK_RED);
        gold = colorTeam("gold", NamedTextColor.GOLD);
        gray = colorTeam("gray", NamedTextColor.GRAY);
        green = colorTeam("green", NamedTextColor.GREEN);
        lightPurple = colorTeam("lightpurple", NamedTextColor.LIGHT_PURPLE);
        red = colorTeam("red", NamedTextColor.RED);
        yellow = colorTeam("yellow", NamedTextColor.YELLOW);
    }

    /**
     * Returns the named team, registering it if it does not exist yet, with its color set.
     * The color used to be set by dispatching "scoreboard teams option <team> color <color>": that
     * syntax is from before 1.13, so the command never worked since, and on Paper 26.2 the failed
     * dispatch throws and aborts onEnable.
     */
    private static org.bukkit.scoreboard.Team colorTeam(String name, NamedTextColor color){
        org.bukkit.scoreboard.Team team = BasePluginGamemode.getScoreboard().getTeam(name);
        if(team == null){
            team = BasePluginGamemode.getScoreboard().registerNewTeam(name);
        }
        team.color(color);
        return team;
    }
    
    public static void addToBukkitTeam(Player p, ChatColor c){
        
        switch(c){
            case AQUA:
                aqua.addPlayer(p);
                break;
            case BLUE:
                blue.addPlayer(p);
                break;
            case DARK_AQUA:
                darkAqua.addPlayer(p);
                break;
            case DARK_GREEN:
                darkGreen.addPlayer(p);
                break;
            case DARK_PURPLE:
                darkPurple.addPlayer(p);
                break;
            case DARK_RED:
                darkRed.addPlayer(p);
                break;
            case GOLD:
                gold.addPlayer(p);
                break;
            case GRAY:
                gray.addPlayer(p);
                break;
            case GREEN:
                green.addPlayer(p);
                break;
            case LIGHT_PURPLE:
                lightPurple.addPlayer(p);
                break;
            case RED:
                red.addPlayer(p);
                break;
            case YELLOW:
                yellow.addPlayer(p);
                break;
        }
    }
    
    public static void removeFromBukkitTeam(Player p){
        if(aqua != null){
            if(aqua.hasPlayer(p)){
                aqua.removePlayer(p);
            }
        }
        if(blue != null){
            if(blue.hasPlayer(p)){
                blue.removePlayer(p);
            }
        }
        if(darkAqua != null){
            if(darkAqua.hasPlayer(p)){
                darkAqua.removePlayer(p);
            }
        }
        if(darkGreen != null){
            if(darkGreen.hasPlayer(p)){
                darkGreen.removePlayer(p);
            }
        }
        if(darkPurple != null){
            if(darkPurple.hasPlayer(p)){
                darkPurple.removePlayer(p);
            }
        }
        if(darkRed != null){
            if(darkRed.hasPlayer(p)){
                darkRed.removePlayer(p);
            }
        }
        if(gold != null){
            if(gold.hasPlayer(p)){
                gold.removePlayer(p);
            }
        }
        if(gray != null){
            if(gray.hasPlayer(p)){
                gray.removePlayer(p);
            }
        }
        if(green != null){
            if(green.hasPlayer(p)){
                green.removePlayer(p);
            }
        }
        if(lightPurple != null){
            if(lightPurple.hasPlayer(p)){
                lightPurple.removePlayer(p);
            }
        }
        if(red != null){
            if(red.hasPlayer(p)){
                red.removePlayer(p);
            }
        }
        if(yellow != null){
            if(yellow.hasPlayer(p)){
                yellow.removePlayer(p);
            }
        }
    }

    public static Color getTeamColor(Player p){
        if(aqua.hasPlayer(p)) return Color.fromRGB(0x55FFFF);
        if(blue.hasPlayer(p)) return Color.fromRGB(0x0000AA);
        if(darkAqua.hasPlayer(p)) return Color.fromRGB(0x00AAAA);
        if(darkGreen.hasPlayer(p)) return Color.fromRGB(0x00AA00);
        if(darkPurple.hasPlayer(p)) return Color.fromRGB(0xAA00AA);
        if(darkRed.hasPlayer(p)) return Color.fromRGB(0xAA0000);
        if(gold.hasPlayer(p)) return Color.fromRGB(0xFFAA00);
        if(gray.hasPlayer(p)) return Color.fromRGB(0xAAAAAA);
        if(green.hasPlayer(p)) return Color.fromRGB(0x55FF55);
        if(lightPurple.hasPlayer(p)) return Color.fromRGB(0xFF55FF);
        if(red.hasPlayer(p)) return Color.fromRGB(0xFF5555);;
        if(yellow.hasPlayer(p)) return Color.fromRGB(0xFFFFFF);
        return null;
    }
}
