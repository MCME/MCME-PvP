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
package com.mcmiddleearth.mcme.pvp.maps;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mcmiddleearth.mcme.pvp.Gamemode.BasePluginGamemode;
import com.mcmiddleearth.mcme.pvp.Gamemode.BasePluginGamemode.GameState;
import com.mcmiddleearth.mcme.pvp.Gamemode.Gamemode;
import com.mcmiddleearth.mcme.pvp.Handlers.ChatHandler;
import com.mcmiddleearth.mcme.pvp.Util.EventLocation;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;
import lombok.Getter;
import lombok.Setter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Donovan <dallen@dallen.xyz>
 */
public class Map {
    private int Max;

    @JsonIgnore
    private int Curr;

    private EventLocation LobbySign;
    
    @JsonIgnore
    private Gamemode gm;

    private String gmType;
    private EventLocation Spawn;
    private String name;
    private String title;
    private HashMap<String, EventLocation> ImportantPoints = new HashMap<>();
    private ArrayList<EventLocation> regionPoints = new ArrayList<>();
    private Boolean fbt;
    
    @JsonIgnore
    private Region region;

    private String resourcePackURL;

    public static HashMap<String, Map> maps = new HashMap<>();
    
    public Map(){}
    
    public Map(Location spawn){
        this.Spawn = new EventLocation(spawn);
    }
    
    public Map(Location spawn, String name){
        this.Spawn = new EventLocation(spawn);
        this.name = name;
        this.title = name;
    }
    
    public void bindSign(SignChangeEvent sign){
        this.LobbySign = new EventLocation(sign.getBlock().getLocation());
        sign.setLine(0, ChatColor.AQUA + "" + ChatColor.BOLD + title);
        if(gmType.equalsIgnoreCase("Team Deathmatch")){
            sign.setLine(1, ChatColor.BLUE + "" + ChatColor.BOLD + "TDM");
        }else{
            sign.setLine(1, ChatColor.BLUE + "" + ChatColor.BOLD + gmType);
        }
        sign.setLine(2, ChatColor.GREEN + "" + ChatColor.BOLD + "" + Curr+"/"+Max);
    }
    
    public void rebindSign(Location signLoc){
        this.LobbySign = new EventLocation(signLoc.getBlock().getLocation());
        Sign sign = (Sign) signLoc.getBlock().getState();
        sign.setLine(0, ChatColor.AQUA + "" + ChatColor.BOLD + title);
        if(gmType.equalsIgnoreCase("Team Deathmatch")){
            sign.setLine(1, ChatColor.BLUE + "" + ChatColor.BOLD + "TDM");
        }else{
            sign.setLine(1, ChatColor.BLUE + "" + ChatColor.BOLD + gmType);
        }
        sign.setLine(2, ChatColor.GREEN + "" + ChatColor.BOLD + "" + Curr+"/"+Max);
    }
    // Adds players to Map if Max amount of players hasn't been reached and the GameState is IDLE or COUNTDOWN, not RUNNING.
    public boolean playerJoin(Player p){
        if(Max <= Curr){
            return false;
        }
        
        gm.getPlayers().add(p);
        if(gm.getState() == GameState.IDLE || gm.getState() == GameState.COUNTDOWN){
            Curr++;
            
            for(Player pl : Bukkit.getOnlinePlayers()){
                pl.sendMessage(ChatColor.GREEN + p.getName() + " Joined!");
            }
            
            try{
                Sign s = (Sign) LobbySign.toBukkitLoc().getBlock().getState();
                s.setLine(2, ChatColor.GREEN + "" + ChatColor.BOLD + "" + Curr+"/"+Max);
                s.update(true, true);
                LobbySign.toBukkitLoc().getBlock().getState().update();
            }catch(NullPointerException e){
                System.err.println("Couldn't find game signs!");
            }catch(ClassCastException e){
                System.err.println("Signs aren't working! Ignoring!");
            }
        }    
        else if(gm.getState() == GameState.RUNNING && gm.midgamePlayerJoin(p)){}
        else{
            p.sendMessage(ChatColor.YELLOW + "Can't join " + gmType + " midgame!");
        }
        return true;
    }
    
    public void playerLeave(Player p){
        ChatHandler.getPlayerPrefixes().remove(p.getName());
        p.setDisplayName(p.getName());
        for(Player pl : gm.getPlayers()){
            pl.sendMessage(p.getName() + " left");
        }
        if(gm instanceof BasePluginGamemode){
            ((BasePluginGamemode) gm).playerLeave(p);
        }else{
            gm.getPlayers().remove(p);
        }
        Curr = 0;
        try{
            Sign s = (Sign) LobbySign.toBukkitLoc().getBlock().getState();
            s.setLine(2, ChatColor.GREEN + "" + ChatColor.BOLD + "" + Curr+"/"+Max);
            s.update(true, true);
            LobbySign.toBukkitLoc().getBlock().getState().update();
            p.getInventory().clear();
            if(Bukkit.getScoreboardManager().getMainScoreboard().getTeam("players").hasPlayer(p)){
                Bukkit.getScoreboardManager().getMainScoreboard().getTeam("players").removePlayer(p);
            }
        }
        catch(NullPointerException e){
            System.out.println("Couldn't find game signs!");
        }
        catch(ClassCastException e){
            System.err.println("Signs aren't working! Ignoring!");
        }
    }
    
    public void playerLeaveAll(){
        for(Player p : gm.getPlayers()){
            ChatHandler.getPlayerPrefixes().remove(p.getName());
            p.setDisplayName(p.getName());
            for(Player pl : gm.getPlayers()){
//                pl.sendMessage(p.getName() + " left");
            }
            if(!p.getGameMode().equals(GameMode.SPECTATOR)){
                Curr--;
            }
            
            try{
                Sign s = (Sign) LobbySign.toBukkitLoc().getBlock().getState();
                s.setLine(2, ChatColor.GREEN + "" + ChatColor.BOLD + Curr+"/"+Max);
                s.update(true, true);
                LobbySign.toBukkitLoc().getBlock().getState().update();
            }
            catch(NullPointerException e){
                System.err.println("Couldn't find game sign!");
            }
            catch(ClassCastException e){
                System.err.println("Signs aren't working! Ignoring!");
            }
            
            p.getInventory().clear();
            
        }
        gm.getPlayers().clear();
    }
    
    public void bindGamemode(){
        try {
            Class<?> gamemodeClass = Class.forName("com.mcmiddleearth.mcme.pvp.Gamemode." + gmType.replace(" ", ""));
            Constructor<?> ctor = gamemodeClass.getConstructor();
            gm = (Gamemode) ctor.newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | 
                SecurityException | InstantiationException | 
                IllegalAccessException | IllegalArgumentException | 
                InvocationTargetException ex) {
            Logger.getLogger(Map.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    public void initializeRegion(){
        ArrayList<BlockVector2> wePoints = new ArrayList<>();
        World world = Bukkit.getWorld("world");
        
        for(EventLocation e : regionPoints){
            BlockVector2 point = BlockVector2.at(e.getX(), e.getZ());
            wePoints.add(point);
        }
        
        region = new Polygonal2DRegion(new BukkitWorld(world), wePoints, 0, 1000);
        
    }
    
    public static Map findMap(String title, String gamemode){
        for(Map m : maps.values()){
            if(m.getGmType().equalsIgnoreCase(gamemode) && m.getTitle().equalsIgnoreCase(title)){
                return m;
            }
        }
        return null;
    }

    public Gamemode getGm() {
        return gm;
    }

    public int getMax() {
        return Max;
    }

    public int getCurr() {
        return Curr;
    }

    public EventLocation getLobbySign() {
        return LobbySign;
    }

    public String getGmType() {
        return gmType;
    }

    public EventLocation getSpawn() {
        return Spawn;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public HashMap<String, EventLocation> getImportantPoints() {
        return ImportantPoints;
    }

    public ArrayList<EventLocation> getRegionPoints() {
        return regionPoints;
    }

    public Region getRegion() {
        return region;
    }

    public String getResourcePackURL() {
        return resourcePackURL;
    }

    public void setMax(int max) {
        Max = max;
    }

    public void setCurr(int curr) {
        Curr = curr;
    }

    public void setLobbySign(EventLocation lobbySign) {
        LobbySign = lobbySign;
    }

    public void setGm(Gamemode gm) {
        this.gm = gm;
    }

    public void setGmType(String gmType) {
        this.gmType = gmType;
    }

    public void setSpawn(EventLocation spawn) {
        Spawn = spawn;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setFbt(Boolean fbt) {
        this.fbt = fbt;
    }

    public Boolean getFbt(){
        return fbt;
    }

    public void setImportantPoints(HashMap<String, EventLocation> importantPoints) {
        ImportantPoints = importantPoints;
    }

    public void setRegionPoints(ArrayList<EventLocation> regionPoints) {
        this.regionPoints = regionPoints;
    }

    public void setResourcePackURL(String resourcePackURL) {
        this.resourcePackURL = resourcePackURL;
    }
    //    @JsonIgnore
//    public void setRp(String rp){
//        this.rp = rp;
//    }
    
}
