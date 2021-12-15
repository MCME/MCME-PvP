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
import org.bukkit.entity.Player;
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

    @JsonIgnore
    private Gamemode gm;

    private String gmType;
    private EventLocation Spawn;
    private String name;
    private String title;
    private HashMap<String, EventLocation> ImportantPoints = new HashMap<>();
    private ArrayList<EventLocation> regionPoints = new ArrayList<>();
    
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


    public boolean playerJoin(Player player){
        if(Max <= Curr){
            return false;
        }
        
        gm.getPlayers().add(player);
        if(gm.getState() == GameState.IDLE){
            Curr++;
            
            for(Player pl : Bukkit.getOnlinePlayers()){
                pl.sendMessage(ChatColor.GREEN + player.getName() + " Joined!");
            }

        }
        else if(gm.getState() == GameState.RUNNING && gm.midgamePlayerJoin(player)){}
        else if(gm.getState() == GameState.COUNTDOWN && gm.midgamePlayerJoin(player)){}
        else{
            player.sendMessage(ChatColor.YELLOW + "Can't join " + gmType + " midgame!");
        }
        return true;
    }

    public void playerLeave(Player player) {
        ChatHandler.getPlayerPrefixes().remove(player.getName());
        player.setDisplayName(player.getName());
        for (Player allPlayers : gm.getPlayers()) {
            allPlayers.sendMessage(player.getName() + " left");
        }
        if (gm instanceof BasePluginGamemode) {
            ((BasePluginGamemode) gm).playerLeave(player);
        } else {
            gm.getPlayers().remove(player);
        }
        Curr = 0;
        player.getInventory().clear();
    }
    
    public void playerLeaveAll(){
        for(Player player : gm.getPlayers()){
            ChatHandler.getPlayerPrefixes().remove(player.getName());
            player.setDisplayName(player.getName());
            for(Player pl : gm.getPlayers()){
//                pl.sendMessage(p.getName() + " left");
            }
            if(!player.getGameMode().equals(GameMode.SPECTATOR)){
                Curr--;
            }
            player.getInventory().clear();
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

    public Gamemode getGm() {
        return gm;
    }

    public int getMax() {
        return Max;
    }

    public int getCurr() {
        return Curr;
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
