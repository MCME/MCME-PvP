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
package com.mcmiddleearth.mcme.pvp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.mcmiddleearth.mcme.pvp.Gamemode.anticheat.AntiCheatListeners;
import com.mcmiddleearth.mcme.pvp.Handlers.ArrowHandler;
import com.mcmiddleearth.mcme.pvp.Util.CLog;
import com.mcmiddleearth.mcme.pvp.Util.DBmanager;
import com.mcmiddleearth.mcme.pvp.command.PVPCommand;
import com.mcmiddleearth.mcme.pvp.maps.Map;
import com.mcmiddleearth.pluginutil.message.MessageUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author Donovan
 */
public class PVPPlugin extends JavaPlugin{
    private static boolean debug = true;
    @Getter private static PVPPlugin plugin;
    private String spawnWorld;
    private static String FileSep = System.getProperty("file.separator");
    private ArrayList<String> noHunger = new ArrayList<String>();
    private static ObjectMapper objectMapper = new ObjectMapper();
    private static Server serverInstance;
    private static File pluginDirectory;
    private static File playerDirectory;
    @Getter private static File mapDirectory;
    @Getter private static File statDirectory;
    private static boolean blockprotect = false;
    private static Integer minutes_broadcast;
    private CommandDispatcher<Player> commandDispatcher;
    @Getter private static Location Spawn;
    @Getter private static int countdownTime = 5;
    @Getter private static final MessageUtil messageUtil = new MessageUtil();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            try {
                if (args.length > 0) {
                    commandDispatcher.execute(commandDispatcher.parse(String.format("%s %s", label, Joiner.on(" ").join(args)), (Player) sender));
                } else {
                    commandDispatcher.execute(commandDispatcher.parse(label, (Player) sender));
                }
            } catch (CommandSyntaxException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof Player) {
            try {
                CompletableFuture<Suggestions> completionSuggestions = commandDispatcher.getCompletionSuggestions(commandDispatcher.parse(getInput(command, args), (Player) sender));
                return completionSuggestions.get().getList().stream().map(Suggestion::getText).collect(Collectors.toList());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        return new ArrayList<>();
    }
    private String getInput(Command command, String[] args) {
        StringBuilder input = new StringBuilder(command.getName());
        for (String arg : args) {
            input.append(CommandDispatcher.ARGUMENT_SEPARATOR).append(arg);
        }
        return input.toString();
    }

    @Override
    public void onEnable(){
        plugin = this;
        this.saveDefaultConfig();
        this.reloadConfig();
        if(this.getConfig().contains("worlds")){
            for(String s : this.getConfig().getStringList("worlds")){
                Bukkit.getServer().getWorlds().add(Bukkit.getServer().createWorld(new WorldCreator(s)));
            }
        }
        if(this.getConfig().contains("Broadcast_minutes")){
            minutes_broadcast = this.getConfig().getInt("PVP.Broadcast_minutes");
        }
        else
        {
            Logger.getLogger("Logger").log(Level.WARNING, "Broadcast_minutes missing or incorrect");
            minutes_broadcast = 2;
        }
        if(this.getConfig().contains("noHunger")){
            noHunger.addAll(this.getConfig().getStringList("noHunger"));
        }
        this.serverInstance = getServer();
        this.pluginDirectory = getDataFolder();
        CLog.println(pluginDirectory.getPath());
        if (!pluginDirectory.exists()){
            pluginDirectory.mkdir();
        }
        this.playerDirectory = new File(pluginDirectory + System.getProperty("file.separator") + "players");
        if (!playerDirectory.exists()){
            playerDirectory.mkdir();
        }
        this.mapDirectory = new File(pluginDirectory + System.getProperty("file.separator") + "maps");
        if (!mapDirectory.exists()){
            mapDirectory.mkdir();
        }
        this.statDirectory = new File(pluginDirectory + System.getProperty("file.separator") + "stats");
        if (!statDirectory.exists()){
            statDirectory.mkdir();
        }
        this.getCommand("WorldJump").setExecutor(new com.mcmiddleearth.mcme.pvp.CommandCore());
        this.getCommand("World").setExecutor(new com.mcmiddleearth.mcme.pvp.CommandCore());
        this.getCommand("PlugUp").setExecutor(new com.mcmiddleearth.mcme.pvp.CommandCore());
        this.getCommand("t").setExecutor(new com.mcmiddleearth.mcme.pvp.PVP.TeamChat());
        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents(new com.mcmiddleearth.mcme.pvp.ListenerCore(), this);
        pm.registerEvents(new com.mcmiddleearth.mcme.pvp.PVP.PlayerStat.StatListener(), PVPPlugin.getPlugin());
        pm.registerEvents(new com.mcmiddleearth.mcme.pvp.Handlers.ChatHandler(), PVPPlugin.getPlugin());
        pm.registerEvents(new com.mcmiddleearth.mcme.pvp.Handlers.ServerMessageHandler(), PVPPlugin.getPlugin());
        pm.registerEvents(new com.mcmiddleearth.mcme.pvp.Handlers.JoinLeaveHandler(), PVPPlugin.getPlugin());
        pm.registerEvents(new com.mcmiddleearth.mcme.pvp.PVP.Locker(), PVPPlugin.getPlugin());
        pm.registerEvents(new com.mcmiddleearth.mcme.pvp.Handlers.AllGameHandlers(), PVPPlugin.getPlugin());
        pm.registerEvents(new com.mcmiddleearth.mcme.pvp.Handlers.ArrowHandler(), PVPPlugin.getPlugin());
        pm.registerEvents(new com.mcmiddleearth.mcme.pvp.Handlers.GearHandler.Gearpvp(), PVPPlugin.getPlugin());
        pm.registerEvents(new AntiCheatListeners(), PVPPlugin.getPlugin());
        pm.registerEvents(new com.mcmiddleearth.mcme.pvp.Handlers.WeatherHandler(), PVPPlugin.getPlugin());
        Bukkit.getWorld("world").setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        if(getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            //PlaceholderAPI.registerPlaceholderExpansion("mcmePvP", new com.mcmiddleearth.mcme.pvp.Handlers.ChatHandler());
            new com.mcmiddleearth.mcme.pvp.Handlers.ChatHandler().register();
        } else {
            Logger.getGlobal().warning("PlaceholderAPI not enabled");
        }
        HashMap<String, Object> maps = new HashMap<>();
        try{
            maps = DBmanager.loadAllObj(Map.class, this.mapDirectory);
        }
        catch(Exception ex){
        }
        if(maps == null){
            maps = new HashMap<>();
        }
        for(java.util.Map.Entry<String, Object> e : maps.entrySet()){
            try{
                Map m = (Map) e.getValue();
                m.setCurr(0);
                if(m.getGmType() != null){
                    m.bindGamemode();
                }
                if(m.getRegionPoints().size() > 0){
                    m.initializeRegion();
                }
                Map.maps.put(e.getKey(), m);
            }
            catch(Exception ex){
                System.out.println("Error loading map " + e.getKey());
            }
        }
        com.mcmiddleearth.mcme.pvp.Handlers.BukkitTeamHandler.configureBukkitTeams();

        Bukkit.getScheduler().scheduleSyncDelayedTask(PVPPlugin.getPlugin(), new Runnable(){

            @Override
            public void run() {
                Spawn = new Location(Bukkit.getWorld("world"), 344.47, 39, 521.58, 0.3F, -24.15F);

            }
        }, 20);
        this.commandDispatcher = new PVPCommand(this);
    }

    @Override
    public void onDisable(){
        if(PVPCommand.getRunningGame() != null)
            PVPCommand.getRunningGame().getGm().End(PVPCommand.getRunningGame());
        for(String mn : Map.maps.keySet()){
            Map m = Map.maps.get(mn);
            m.setCurr(0);
            DBmanager.saveObj(m, new File(pluginDirectory + PVPPlugin.getFileSep() + "maps"), mn);
        }
    }
    public static WorldEditPlugin getWorldEditPlugin(){
        Plugin p = Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");

        if(p == null){
            return null;
        }
        return (WorldEditPlugin) p;
    }


    public static boolean isDebug() { return debug; }

    public static Integer getMinutes() { return minutes_broadcast; }

    public String getSpawnWorld() { return spawnWorld; }

    public static String getFileSep() { return FileSep; }

    public ArrayList<String> getNoHunger() { return noHunger; }

    public static ObjectMapper getObjectMapper() { return objectMapper; }

    public static Server getServerInstance() { return serverInstance; }

    public static File getPluginDirectory() { return pluginDirectory; }

    public static File getPlayerDirectory() { return playerDirectory; }

    public static boolean isBlockprotect() { return blockprotect; }
}
