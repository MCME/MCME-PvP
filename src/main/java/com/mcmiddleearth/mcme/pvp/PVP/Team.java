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
package com.mcmiddleearth.mcme.pvp.PVP;

import com.mcmiddleearth.mcme.pvp.Handlers.BukkitTeamHandler;
import com.mcmiddleearth.mcme.pvp.Handlers.ChatHandler;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;

/**
 *
 * @author Eric
 */
public class Team { 
    
        private ArrayList<Location> capturedPoints = new ArrayList<>();
        private ArrayList<Player> members = new ArrayList<>();
        private ArrayList<Player> allMembers = new ArrayList<>();
        private String prefix;
        private ChatColor color;
        private Teams t;
        @Getter private static Team red = new Team("Red", org.bukkit.ChatColor.RED, Teams.RED);
        @Getter private static Team blue = new Team("Blue", org.bukkit.ChatColor.BLUE, Teams.BLUE);
        @Getter private static Team blacks = new Team("Blacks", ChatColor.BLACK, Teams.BLACKS);
        @Getter private static Team greens = new Team("Greens", ChatColor.DARK_GREEN, Teams.GREENS);
        @Getter private static Team spectator = new Team("Spectator", org.bukkit.ChatColor.GRAY, Teams.SPECTATORS);
        @Getter private static Team survivor = new Team("Survivor", org.bukkit.ChatColor.BLUE, Teams.SURVIVORS);
        @Getter private static Team infected = new Team("Infected", org.bukkit.ChatColor.DARK_RED, Teams.INFECTED);
        @Getter private static Team runner = new Team("Runner", org.bukkit.ChatColor.BLUE, Teams.RUNNER);
        @Getter private static Team death = new Team("Death", org.bukkit.ChatColor.BLACK, Teams.DEATH);
        
        public Team(String prefix, ChatColor color, Teams t){
            this.prefix = prefix;
            this.color = color;
            this.t = t;
        }
        
        public enum Teams {
            RED,BLUE,BLACKS,GREENS,INFECTED,SURVIVORS,RUNNER,DEATH,SPECTATORS;
        }
        
        public void add(Player p){
            Team.removeFromTeam(p);
            members.add(p);
            
            ChatHandler.getPlayerPrefixes().put(p.getName(), (color + prefix));
            ChatHandler.getPlayerColors().put(p.getName(), color);
            p.setDisplayName(color + p.getName());
            BukkitTeamHandler.addToBukkitTeam(p, color);
            if(!allMembers.contains(p)){
                allMembers.add(p);
            }
            
            switch(t){
                case RED:
                    p.sendMessage(color + "You are on the Red Team!");
                    break;
                case BLUE:
                    p.sendMessage(color + "You are on the Blue Team!");
                    break;
                case BLACKS:
                    p.sendMessage(color+"You’ve joined the Blacks.");
                    break;
                case GREENS:
                    p.sendMessage(color+"You´ve joined the Greens.");
                    break;
                case SPECTATORS:
                    p.sendMessage(color + "You are Spectating!");
                    p.sendMessage(ChatColor.GRAY + "As a spectator, game participants won't see your chat.");
                    break;
                case SURVIVORS:
                    p.sendMessage(color + "You are a Survivor!");
                    break;
                case INFECTED:
                    p.sendMessage(color + "You are Infected!");
                    break;
                case RUNNER:
                    p.sendMessage(color + "You are a runner, don't stand RUN!");
                    break;
                case DEATH:
                    p.sendMessage(color + "You are Death, kill the runners!");
            }
            
            if(p.getName().length() < 14){
                p.setPlayerListName(color + p.getName());
            }
            else{
                String newName = p.getName().substring(0,13);
                p.setPlayerListName(color + newName);
            }
            
            if(t == Teams.SPECTATORS && p.getGameMode() != GameMode.SPECTATOR){
                p.setGameMode(GameMode.SPECTATOR);
                p.setFlying(true);
            }
            else if(p.getGameMode() != GameMode.ADVENTURE){
                p.setGameMode(GameMode.ADVENTURE);
                p.setFlying(false);
            }
            
            if(t == Teams.INFECTED){
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,100000,1));
            }
        }
        
        public int size(){
            return members.size();
        }
        
        public static void removeFromTeam(Player p){
            if(red.getMembers().contains(p)){
                red.getMembers().remove(p);
            }
            else if(blue.getMembers().contains(p)){
                blue.getMembers().remove(p);
            }
            else if(blacks.getMembers().contains(p)){
                blacks.getMembers().remove(p);
            }
            else if(greens.getMembers().contains(p)){
                greens.getMembers().remove(p);
            }
            else if(spectator.getMembers().contains(p)){
                spectator.getMembers().remove(p);
            }
            else if(survivor.getMembers().contains(p)){
                survivor.getMembers().remove(p);
            }
            else if(infected.getMembers().contains(p)){
                infected.getMembers().remove(p);
                p.sendMessage("Removed from infected!");
                p.removePotionEffect(PotionEffectType.SPEED);
            } else if(runner.getMembers().contains(p)){
                runner.getMembers().remove(p);
            } else if(death.getMembers().contains(p)){
                death.getMembers().remove(p);
            }
            
            if(!p.isDead()){
                p.setHealth(20);
            }
           
            BukkitTeamHandler.removeFromBukkitTeam(p);
            p.setDisplayName(ChatColor.WHITE + p.getName());
            p.setPlayerListName(p.getName());
            ChatHandler.getPlayerColors().put(p.getName(), ChatColor.WHITE);
            ChatHandler.getPlayerPrefixes().remove(p.getName());
            
        }
    public static boolean areTeamMates(Player p1, Player p2){
        
        if(red.getMembers().contains(p1) && red.getMembers().contains(p2)){
            return true;
        }
        else if(greens.getMembers().contains(p1) && greens.getMembers().contains(p2)){
            return true;
        }
        else if(blacks.getMembers().contains(p1) && blacks.getMembers().contains(p2)){
            return true;
        }
        else if(blue.getMembers().contains(p1) && blue.getMembers().contains(p2)){
            return true;
        }
        else if(spectator.getMembers().contains(p1) && spectator.getMembers().contains(p2)){
            return true;
        }
        else if(survivor.getMembers().contains(p1) && survivor.getMembers().contains(p2)){
            return true;
        }
        else if(infected.getMembers().contains(p1) && infected.getMembers().contains(p2)){
            return true;
        } else if(runner.getMembers().contains(p1) && runner.getMembers().contains(p2)){
            return true;
        }
        else{
            return false;
        }
        
    }

    public static void resetAllTeams(){
        for(Player p : infected.getMembers()){
            p.removePotionEffect(PotionEffectType.SPEED);
        }
        
        red.getMembers().clear();
        blue.getMembers().clear();
        greens.getMembers().clear();
        blacks.getMembers().clear();
        spectator.getMembers().clear();
        survivor.getMembers().clear();
        infected.getMembers().clear();
        runner.getMembers().clear();
        death.getMembers().clear();

        red.getCapturedPoints().clear();
        blue.getCapturedPoints().clear();
        
        red.getAllMembers().clear();
        blue.getAllMembers().clear();
        greens.getAllMembers().clear();
        blacks.getAllMembers().clear();
        spectator.getAllMembers().clear();
        survivor.getAllMembers().clear();
        infected.getAllMembers().clear();
        runner.getAllMembers().clear();
        death.getAllMembers().clear();
        
    }

    public ArrayList<Location> getCapturedPoints() {
        return capturedPoints;
    }

    public ArrayList<Player> getMembers() {
        return members;
    }

    public ArrayList<Player> getAllMembers() {
        return allMembers;
    }
}
