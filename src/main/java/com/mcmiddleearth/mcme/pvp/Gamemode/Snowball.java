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

import com.mcmiddleearth.mcme.pvp.Handlers.BukkitTeamHandler;
import com.mcmiddleearth.mcme.pvp.Handlers.ChatHandler;
import com.mcmiddleearth.mcme.pvp.Handlers.GearHandler;
import com.mcmiddleearth.mcme.pvp.Handlers.GearHandler.SpecialGear;
import com.mcmiddleearth.mcme.pvp.PVP.PlayerStat;
import com.mcmiddleearth.mcme.pvp.PVP.Team;
import com.mcmiddleearth.mcme.pvp.PVPPlugin;
import com.mcmiddleearth.mcme.pvp.Util.EventLocation;
import com.mcmiddleearth.mcme.pvp.command.PVPCommand;
import com.mcmiddleearth.mcme.pvp.maps.Map;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 *
 * @author Maski98
 */
public class Snowball extends BasePluginGamemode {

    private boolean pvpRegistered = false;

    @Getter
    private final ArrayList<String> NeededPoints = new ArrayList<>(Arrays.asList("PlayerSpawn"));

    private GameState state;

    Map map;

    private int count;

    private Objective Points;

    private GamemodeHandlers SnowBall;

    private HashMap<String, String> playerDeaths = new HashMap<>();

    private HashMap<String, ChatColor> hasPlayed = new HashMap<String, ChatColor>();

    private HashMap<Player, Long> healing = new HashMap<>();

    private final Integer snowBallTime = 5;

    private boolean midgameJoin = true;
    private int time;


    private final ChatColor[] chatColors = new ChatColor[]{
            ChatColor.AQUA,
            ChatColor.BLUE,
            ChatColor.DARK_AQUA,
            ChatColor.DARK_GREEN,
            ChatColor.DARK_PURPLE,
            ChatColor.DARK_RED,
            ChatColor.GOLD,
            ChatColor.GREEN,
            ChatColor.LIGHT_PURPLE,
            ChatColor.RED,
            ChatColor.YELLOW
    };

    private EventLocation[] spawns;

    //TODO:
    // teleport after game end  x
    // spawn protection for 1 second
    // add better spawn system for this x
    // oitq remove and give arrow back in oitq to stop people from shooting (does that work?)

    public Snowball(){
        state = GameState.IDLE;
    }


    Runnable tickSB = new BukkitRunnable(){
            @Override
            public void run() {
                time--;

                if(time < 60 ){
                    Points.setDisplayName("Time: "+ time + "s");
                }else{
                    Points.setDisplayName("Time: "+(time / 60) + "m "+time%60+"s");
                }

                if(time == 30){

                    for(Player p : Bukkit.getOnlinePlayers()){
                        p.sendMessage(ChatColor.GREEN + "30 seconds remaining!");
                    }

                }
                else if(time <= 10 && time > 1){

                    for(Player p : Bukkit.getOnlinePlayers()){
                        p.sendMessage(ChatColor.GREEN + String.valueOf(time) + " seconds remaining!");
                    }

                }
                else if(time == 1){

                    for(Player p : Bukkit.getOnlinePlayers()){
                        p.sendMessage(ChatColor.GREEN + String.valueOf(time) + " second remaining!");
                    }

                }

                if(time <= 0){
                    End(map);
                }

                boolean healed = false;

                for(Player p : healing.keySet()){

                    if(System.currentTimeMillis() < healing.get(p)){
                        p.setHealth(20);
                        healed = true;
                    }

                }
                if(!healed){
                    healing.clear();
                }
            }
        };

    Runnable healer = new Runnable(){

        public void run(){
            boolean healed = false;

            for(Player p : healing.keySet()){


                if(System.currentTimeMillis() < healing.get(p)){
                    p.setHealth(20);
                    healed = true;
                }

            }
            if(!healed){
                healing.clear();
            }
        }

    };


    Runnable snowballHandler = new Runnable() {
        @Override
        public void run() {
            for(Player player : Bukkit.getOnlinePlayers()){
                if(!Team.getSpectator().getMembers().contains(player)){
                    if(!player.getInventory().contains(Material.SNOWBALL)){
                        player.getInventory().setItem(0,new ItemStack(Material.SNOWBALL,3));
                    }else if(player.getInventory().getItem(0) == null){
                        player.getInventory().remove(Material.SNOWBALL);
                        player.getInventory().setItem(0,new ItemStack(Material.SNOWBALL,3));
                    }else {
                        player.getInventory().addItem(new ItemStack(Material.SNOWBALL,3));
                    }
                }
            }
        }
    };



    @Override
    public void Start(Map m, int parameter){
        super.Start(m, parameter);
        map = m;
        count = PVPPlugin.getCountdownTime();
        time = parameter;
        state = GameState.COUNTDOWN;
        spawns = map.getImportantPoints().values().toArray(new EventLocation[0]);
        if(!map.getImportantPoints().keySet().containsAll(NeededPoints)){
            for(Player p : players){
                p.sendMessage(ChatColor.RED + "Game cannot start! Not all needed points have been added!");
            }
            End(m);
        }

        if(!pvpRegistered){
            SnowBall = new GamemodeHandlers();
            PluginManager pm = PVPPlugin.getServerInstance().getPluginManager();
            pm.registerEvents(SnowBall, PVPPlugin.getPlugin());
            pvpRegistered = true;
        }

        int c = 0;
        for(Player p : Bukkit.getServer().getOnlinePlayers()){
            if(players.contains(p)){
                p.teleport(spawns[c].toBukkitLoc().add(0, 1, 0));
                freezePlayer(p, 140);
                if(spawns.length == (c + 1)){
                    c = 0;
                }
                else{
                    c++;
                }
            }
            else{
                Team.getSpectator().add(p);
                p.teleport(map.getSpawn().toBukkitLoc());
            }

        }

        new BukkitRunnable(){
            @Override
            public void run() {

                if(count == 0){
                    if(state == GameState.RUNNING){
                        cancel();
                    }
                    state = GameState.RUNNING;
                    int k = 0;
                    Points = getScoreboard().registerNewObjective("Kills", "dummy", "Time: " + time + "m");
                    time *= 60;
                    Points.setDisplaySlot(DisplaySlot.SIDEBAR);

                    Bukkit.getScheduler().scheduleSyncRepeatingTask(PVPPlugin.getPlugin(),snowballHandler,0,20*snowBallTime);
                    Bukkit.getScheduler().scheduleSyncRepeatingTask(PVPPlugin.getPlugin(),tickSB,0,20);
                    Bukkit.getScheduler().scheduleSyncRepeatingTask(PVPPlugin.getPlugin(),healer,0,20);

                    SnowBall.spawn = new Random().nextInt(spawns.length-1);

                    for(Player p : Bukkit.getServer().getOnlinePlayers()){
                        p.sendMessage(ChatColor.GREEN + "Game Start!");
                        p.setScoreboard(getScoreboard());
                    }

                    for(Player p : players){

                        p.setGameMode(GameMode.ADVENTURE);

                        ChatHandler.getPlayerPrefixes().put(p.getName(), chatColors[k] + "Player");
                        ChatHandler.getPlayerColors().put(p.getName(), chatColors[k]);
                        hasPlayed.put(p.getName(), chatColors[k]);

                        Points.getScore(ChatHandler.getPlayerColors().get(p.getName()) + p.getName()).setScore(0);

                        if(p.getName().length() < 14){
                            p.setPlayerListName(chatColors[k] + p.getName());
                        }
                        else{
                            String newName = p.getName().substring(0,13);
                            p.setPlayerListName(chatColors[k] + newName);
                        }
                        GearHandler.giveGear(p, chatColors[k], SpecialGear.SNOW);
                        BukkitTeamHandler.addToBukkitTeam(p, chatColors[k]);

                        if(chatColors.length == (k+1)){
                            k = 0;
                        }
                        else{
                            k++;
                        }
                    }
                    count = -1;

                    for(Player p : players){
                        p.sendMessage(ChatColor.GRAY + "Use " + ChatColor.GREEN + "/unstuck" + ChatColor.GRAY + " if you're stuck in a block!");
                    }
                }
                else if(count != -1){
                    for(Player p : Bukkit.getOnlinePlayers()){
                        p.sendMessage(ChatColor.GREEN + "Game begins in " + count);
                    }
                    count--;
                }
                else{
                    cancel();
                }
            }
        }.runTaskTimer(PVPPlugin.getPlugin(),40, 20);
    }

    public void End(Map m){
        PlayerStat.addGameSpectatedAll();
        state = GameState.IDLE;
        hasPlayed.clear();

        ArrayList<String> mostDeaths = new ArrayList<String>();
        int mostDeathsNum = 0;
        String killMessage = "";

        ArrayList<String> mostKills = new ArrayList<String>();
        int mostKillsNum = 0;
        String deathMessage = "";

        ArrayList<String> highestKd = new ArrayList<String>();
        double highestKdNum = 0;
        String kDMessage = "";

        for(Player p : players){

            if(playerDeaths.containsKey(p.getName())){
                if(Integer.parseInt(playerDeaths.get(p.getName())) > mostDeathsNum){
                    mostDeaths.clear();
                    mostDeathsNum = Integer.parseInt(playerDeaths.get(p.getName()));
                    mostDeaths.add(p.getName());
                }
                else if(Integer.parseInt(playerDeaths.get(p.getName())) == mostDeathsNum){
                    mostDeaths.add(p.getName());
                }
            }
            if(Points.getScore(ChatHandler.getPlayerColors().get(p.getName()) + p.getName()).getScore() > mostKillsNum){
                mostKills.clear();
                mostKillsNum = Points.getScore(ChatHandler.getPlayerColors().get(p.getName()) + p.getName()).getScore();
                mostKills.add(p.getName());
            }
            else if(Points.getScore(ChatHandler.getPlayerColors().get(p.getName()) + p.getName()).getScore() == mostKillsNum){
                mostKills.add(p.getName());
            }
            try{
                if(Double.valueOf(Points.getScore(ChatHandler.getPlayerColors().get(p.getName()) + p.getName()).getScore()) / Double.parseDouble(playerDeaths.get(p.getName())) > highestKdNum && highestKdNum != -1){
                    highestKd.clear();
                    highestKdNum =  Points.getScore(ChatHandler.getPlayerColors().get(p.getName()) + p.getName()).getScore() / Double.parseDouble(playerDeaths.get(p.getName()));
                    highestKd.add(p.getName());
                }
                else if(Double.valueOf(Points.getScore(ChatHandler.getPlayerColors().get(p.getName()) + p.getName()).getScore()) / Double.parseDouble(playerDeaths.get(p.getName())) == highestKdNum){
                    highestKd.add(p.getName());
                }
            }catch(NullPointerException e){
                if(highestKdNum != -1){
                    highestKd.clear();
                    highestKdNum = -1;
                    highestKd.add(p.getName());
                }
                else if (highestKdNum == -1){
                    highestKd.add(p.getName());
                }
            }

        }

        int loops = 0;
        for(String playerName : mostDeaths){
            if(mostDeaths.size() == 1 && loops == 0){
                deathMessage = ChatHandler.getPlayerColors().get(playerName) + playerName + ChatColor.GREEN + " with " + mostDeathsNum;
            }
            else if(loops == (mostDeaths.size() - 1)){
                deathMessage += ChatColor.GREEN + "and " + ChatHandler.getPlayerColors().get(playerName) + playerName + ChatColor.GREEN + " with " + mostDeathsNum;
            }
            else{
                deathMessage += ChatHandler.getPlayerColors().get(playerName) + playerName + ChatColor.GREEN + ", ";
                loops++;
            }
        }

        loops = 0;
        for(String playerName : mostKills){
            PlayerStat.getPlayerStats().get(playerName).addGameWon();
            if(mostKills.size() == 1 && loops == 0){
                killMessage = ChatHandler.getPlayerColors().get(playerName) + playerName + ChatColor.GREEN + " with " + mostKillsNum;
            }
            else if(loops == (mostKills.size() - 1)){
                killMessage += ChatColor.GREEN + "and " + ChatHandler.getPlayerColors().get(playerName) + playerName + ChatColor.GREEN + " with " + mostKillsNum;
            }
            else{
                killMessage += ChatHandler.getPlayerColors().get(playerName) + playerName + ChatColor.GREEN + ", ";
                loops++;
            }
        }

        loops = 0;
        String highestKdNumString;
        DecimalFormat df = new DecimalFormat("#0.00");
        if(highestKdNum == -1.0){
            highestKdNumString = "infinity";
        }
        else{
            highestKdNumString = df.format(highestKdNum);
        }
        for(String playerName : highestKd){
            if(highestKd.size() == 1 && loops == 0){
                kDMessage = ChatHandler.getPlayerColors().get(playerName) + playerName + ChatColor.GREEN + " with " + highestKdNumString;
            }
            else if(loops == (highestKd.size() - 1)){
                kDMessage += ChatColor.GREEN + "and " + ChatHandler.getPlayerColors().get(playerName) + playerName + ChatColor.GREEN + " with " + highestKdNumString;
            }
            else{
                kDMessage += ChatHandler.getPlayerColors().get(playerName) + playerName + ChatColor.GREEN + ", ";
                loops++;
            }
        }

        for(Player p : Bukkit.getOnlinePlayers()){
            p.sendMessage(ChatColor.GREEN + "Most Kills: " + killMessage);
            p.sendMessage(ChatColor.GREEN + "Most Deaths: " + deathMessage);
            p.sendMessage(ChatColor.GREEN + "Highest KD: " + kDMessage);
        }

        for(Player p : Bukkit.getOnlinePlayers()){
            BukkitTeamHandler.removeFromBukkitTeam(p);
        }

        getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
        m.playerLeaveAll();
        playerDeaths.clear();

        mostDeaths.clear();
        mostKills.clear();
        highestKd.clear();
        PVPCommand.queueNextGame();
        super.End(m);
    }

    @Override
    public boolean midgamePlayerJoin(Player p){
        Random random = new Random();
        ChatColor color;
        Team.removeFromTeam(p);
        if(!hasPlayed.containsKey(p.getName())){
            color = chatColors[random.nextInt(chatColors.length)];
            ChatHandler.getPlayerColors().put(p.getName(), color);
            ChatHandler.getPlayerPrefixes().put(p.getName(), color + "Player");
            if (state == GameState.RUNNING) Points.getScore(ChatHandler.getPlayerColors().get(p.getName()) + p.getName()).setScore(0);
            hasPlayed.put(p.getName(), color);

        }
        else{
            color = hasPlayed.get(p.getName());
            ChatHandler.getPlayerColors().put(p.getName(), color);
        }

        if(p.getName().length() < 14){
            p.setPlayerListName(color + p.getName());
        }
        else{
            String newName = p.getName().substring(0,13);
            p.setPlayerListName(color + newName);
        }

        p.teleport(spawns[SnowBall.spawn++].toBukkitLoc().add(0, 1, 0));
        p.setGameMode(GameMode.ADVENTURE);
        p.setScoreboard(getScoreboard());

        GearHandler.giveGear(p, color, SpecialGear.SNOW);
        BukkitTeamHandler.addToBukkitTeam(p, color);

        super.midgamePlayerJoin(p);

        return true;
    }

    public String requiresParameter(){
        return "time in minutes";
    }

    private class GamemodeHandlers implements Listener{

        int spawn = 0;

        @EventHandler
        public void onPlayerDeath(PlayerDeathEvent e){
            int tempDeaths;
            if(Points.getScore(ChatHandler.getPlayerColors().get(e.getEntity().getKiller().getName()) + e.getEntity().getKiller().getName()).getScore() == 100){
                End(map);
                e.getEntity().teleport(PVPPlugin.getLobby());
            }
            if(e.getEntity().getKiller() != null && state == GameState.RUNNING){
                Points.getScore(ChatHandler.getPlayerColors().get(e.getEntity().getKiller().getName()) + e.getEntity().getKiller().getName()).setScore(Points.getScore(ChatHandler.getPlayerColors().get(e.getEntity().getKiller().getName()) + e.getEntity().getKiller().getName()).getScore() + 1);
                PlayerInventory killerInv = e.getEntity().getKiller().getInventory();
                ItemStack Snowball = new ItemStack(Material.SNOWBALL, 3);
                if(killerInv.contains(Snowball.getType())){
                    killerInv.addItem(Snowball);
                }else{
                    killerInv.setItem(0,Snowball);
                }
                if(playerDeaths.containsKey(e.getEntity().getName())){
                    tempDeaths = Integer.parseInt(playerDeaths.get(e.getEntity().getName()));
                    playerDeaths.remove(e.getEntity().getName());
                    playerDeaths.put(e.getEntity().getName(), String.valueOf(tempDeaths + 1));
                }else{
                    playerDeaths.put(e.getEntity().getName(), "1");
                }
                if(Points.getScore(ChatHandler.getPlayerColors().get(e.getEntity().getKiller().getName()) + e.getEntity().getKiller().getName()).getScore() == 100){
                    End(map);
                    e.getEntity().teleport(PVPPlugin.getLobby());
                }
            }

        }

        @EventHandler
        public void onPlayerRespawn(PlayerRespawnEvent e){

            if(state == GameState.RUNNING && players.contains(e.getPlayer())){
                //Random random = new Random();
                e.getPlayer().getInventory().remove(Material.SNOWBALL);
                e.getPlayer().getInventory().addItem(new ItemStack(Material.SNOWBALL, 8));
                //e.setRespawnLocation(spawns[random.nextInt(spawns.length)].toBukkitLoc().add(0, 1, 0));

                e.setRespawnLocation(spawns[spawn++].toBukkitLoc().add(0,1,0));
                if(spawn >= spawns.length){
                    spawn = 0;
                }

                healing.put(e.getPlayer(), System.currentTimeMillis() + 7500);
            }
            Logger.getLogger("PVP").log(Level.INFO, e.getRespawnLocation().toString());
        }
    }

    @Override
    public ArrayList<String> getNeededPoints() {
        return NeededPoints;
    }

    @Override
    public GameState getState() {
        return state;
    }

    public Objective getPoints() {
        return Points;
    }

    @Override
    public boolean isMidgameJoin() {
        return midgameJoin;
    }
}
