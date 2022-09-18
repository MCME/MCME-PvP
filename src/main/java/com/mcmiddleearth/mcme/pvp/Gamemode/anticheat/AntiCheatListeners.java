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
package com.mcmiddleearth.mcme.pvp.Gamemode.anticheat;

import com.mcmiddleearth.mcme.pvp.PVP.Team;
import com.mcmiddleearth.mcme.pvp.command.PVPCommand;
import com.mcmiddleearth.mcme.pvp.Permissions;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;

/**
 *
 * @author Eric
 */
public class AntiCheatListeners implements Listener{
    
    //Prevent trolling or cheating with commands
    @EventHandler
    public static void onPlayerCommand(PlayerCommandPreprocessEvent e){
        
        if(truncateAtFirstSpace(e.getMessage()).equals("/kill")){
            
            String firstArg = getFirstArg(e.getMessage());
            
            if(firstArg.equalsIgnoreCase(e.getPlayer().getName()) || firstArg.equals("")){
                return;
            }
        }
        
        String command = truncateAtFirstSpace(e.getMessage());
        Player cs = e.getPlayer();
        
        if(command.equalsIgnoreCase("/toggledownfall")){
            cs.sendMessage(ChatColor.GRAY + "Rain has been disabled, as it causes lag for some players");
            e.setCancelled(true);
        }
        if(command.equalsIgnoreCase("/stop") && !e.getPlayer().getName().equals("q220")){
            cs.sendMessage(ChatColor.RED + "You can't stop the server!");
            e.setCancelled(true);
        }
        
        if((e.getPlayer().getName().equals("DSESGH") || 
                e.getPlayer().getName().equals("Dallen") || 
                e.getPlayer().getName().equals("q220") || 
                e.getPlayer().getName().equals("Finrod_Amandil") ||
                e.getPlayer().getName().equals("barteldvn") ||
                e.getPlayer().getName().equals("DynoDaring")) && !command.equalsIgnoreCase("/stop")){
            return;
        }
        
        if(command.equalsIgnoreCase("/deop") ||
                command.equalsIgnoreCase("/gamerule") ||
                command.equalsIgnoreCase("/plugup") ||
                command.equalsIgnoreCase("/reload") ||
                command.equalsIgnoreCase("/restart") ||
                command.equalsIgnoreCase("/say") ||
                command.equalsIgnoreCase("/summon") ||
                command.equalsIgnoreCase("/op") ||
                command.equalsIgnoreCase("/restart") ||
                command.equalsIgnoreCase("/reload") ||
                command.equalsIgnoreCase("/scoreboard") ||
                command.equalsIgnoreCase("/slay")){
            cs.sendMessage(ChatColor.RED + "You are not able to perform this command");
            e.setCancelled(true);
            
        }
        
        if(command.equalsIgnoreCase("/kill")){
            cs.sendMessage(ChatColor.RED + "You can only use /kill on yourself!");
            e.setCancelled(true);
        }
        
        if(command.equalsIgnoreCase("/effect") || 
                command.equalsIgnoreCase("/enchant") ||
                command.equalsIgnoreCase("/i") ||
                command.equalsIgnoreCase("/execute")){
            cs.sendMessage(ChatColor.RED + "You trying to cheat?!?");
            e.setCancelled(true);
        }
        
        if(PVPCommand.getRunningGame() != null){
            if(command.equalsIgnoreCase("/fill") || 
                    command.equalsIgnoreCase("/blockdata") ||
                    command.equalsIgnoreCase("/clear") ||
                    command.equalsIgnoreCase("/gamemode") ||
                    command.equalsIgnoreCase("/give") ||
                    command.equalsIgnoreCase("/setblock") ||
                    command.equalsIgnoreCase("/xp") ||
                    command.equalsIgnoreCase("/worldjump") ||
                    command.equalsIgnoreCase("/mvtp") ||
                    command.equalsIgnoreCase("/mv")){
                cs.sendMessage(ChatColor.RED + "You can't do that during a game!");
                e.setCancelled(true);
            }
            
            if(command.equalsIgnoreCase("/tp") && !Team.getSpectator().getMembers().contains(e.getPlayer())){
                cs.sendMessage(ChatColor.RED + "You can't do that during a game!");
                e.setCancelled(true);
            }
            
            if(command.equalsIgnoreCase("/locker")){
                cs.sendMessage(ChatColor.RED + "End the game or wait for it to end!");
                e.setCancelled(true);
            }
            
            if(Team.getSpectator().getMembers().contains(cs)){
                if(command.equalsIgnoreCase("/me") ||
                        command.equalsIgnoreCase("/tell") ||
                        command.equalsIgnoreCase("/msg") ||
                        command.equalsIgnoreCase("/tellraw") ||
                        command.equalsIgnoreCase("/title")||
                        command.equalsIgnoreCase("/w")||
                        command.equalsIgnoreCase("/r")){
                    cs.sendMessage(ChatColor.RED + "You can't do that during a game!");
                    e.setCancelled(true);
                }
                
            }
            
        }
        
    }
    
    //Prevent spectators from giving info to players
    @EventHandler
    public static void onPlayerChat(AsyncPlayerChatEvent e){
        if(PVPCommand.getRunningGame() != null && Team.getSpectator().getMembers().contains(e.getPlayer())){
            
            if(e.getPlayer().hasPermission(Permissions.RUN.getPermissionNode())){
                return;
            }
            
            for(Player p : Team.getSpectator().getMembers()){
                
                p.sendMessage(ChatColor.GRAY + "Spectator " + e.getPlayer().getName() + ": " + ChatColor.WHITE + e.getMessage());
                
            }
            
            e.setCancelled(true);
        }
    }
    
    //Prevent speedhacks
    /*@EventHandler
    public static void onPlayerMove(PlayerMoveEvent e){
        
        if(Team.getSpectators().contains(e.getPlayer()) || (e.getPlayer().isOp() && PVPCommandCore.getRunningGame() == null)){
            return;
        }
        
        Location from = e.getFrom();
        Location to = e.getTo();
        
        double xZDistance = Math.sqrt(Math.pow(to.getX() - from.getX(), 2) + Math.pow(to.getZ() - from.getZ(), 2));
        double yDistance = to.getY() - from.getY();
        
        if(xZDistance >= 0.61 || xZDistance <= -0.61){
            if(Team.getInfected().contains(e.getPlayer()) && xZDistance <= 0.68 && xZDistance >= -0.68){
                return;
            }
            else{
                System.out.println(e.getPlayer().getName() + " moved too fast, with an xz distance of " + xZDistance);
                e.getPlayer().sendMessage(ChatColor.RED + "You moved too fast!");
                e.setTo(from);
            }
            
        }
        
        if(yDistance >= .51){
            System.out.println(e.getPlayer().getName() + " moved too fast, with a y distance of " + yDistance);
            e.getPlayer().sendMessage(ChatColor.RED + "You moved too fast!");
            e.setTo(from);
        }
        
    }*/
    
    private static HashMap<String, Long> lastInteract = new HashMap<>();

    public static HashMap<String, Long> getLastInteract() {
        return lastInteract;
    }

//    @EventHandler
//    public static void onPlayerClick(PlayerInteractEvent e){
//
//        if(e.getAction().equals(Action.RIGHT_CLICK_BLOCK) || e.getAction().equals(Action.LEFT_CLICK_AIR) || e.getAction().equals(Action.LEFT_CLICK_BLOCK) || e.getAction().equals(Action.RIGHT_CLICK_AIR)){
//            if(!lastInteract.containsKey(e.getPlayer().getName())){
//                lastInteract.put(e.getPlayer().getName(), (System.currentTimeMillis() - 100));
//            }
//
//            if(System.currentTimeMillis() - lastInteract.get(e.getPlayer().getName()) <= 85){
//                e.setCancelled(true);
//            }
//
//            if(lastInteract.keySet().contains(e.getPlayer().getName())){
//                lastInteract.remove(e.getPlayer().getName());
//                lastInteract.put(e.getPlayer().getName(), System.currentTimeMillis());
//            }else{
//                lastInteract.put(e.getPlayer().getName(), System.currentTimeMillis());
//            }
//        }
//
//    }
    
    private static String truncateAtFirstSpace(String s){
        
        char[] chars = s.toCharArray();
        String newString = "";
        
        for(char c : chars){
            
            if(c != ' '){
                newString += String.valueOf(c);
            }
            else{
                return newString;
            }
            
        }
        return newString;
    }
    private static String getFirstArg(String s){
        
        char[] chars = s.toCharArray();
        
        boolean hitFirstSpace = false;
        
        String firstArg = "";
        
        for(char c : chars){
            if(c == ' ' && !hitFirstSpace){
                hitFirstSpace = true;
            }
            
            else if(c == ' ' && hitFirstSpace){
                return firstArg;
            }
            
            else if (c != ' ' && hitFirstSpace){
                firstArg += String.valueOf(c);
            }
        }
        
        return firstArg;
        
    }
}
/* 
13
13
*/

/* sprint jump no potion
14
14
*/

