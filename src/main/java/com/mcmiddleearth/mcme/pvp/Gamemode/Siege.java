package com.mcmiddleearth.mcme.pvp.Gamemode;

import com.mcmiddleearth.mcme.pvp.Handlers.GearHandler;
import com.mcmiddleearth.mcme.pvp.PVP.PlayerStat;
import com.mcmiddleearth.mcme.pvp.PVP.Team;
import com.mcmiddleearth.mcme.pvp.PVPPlugin;
import com.mcmiddleearth.mcme.pvp.Util.EventLocation;
import com.mcmiddleearth.mcme.pvp.command.PVPCommand;
import com.mcmiddleearth.mcme.pvp.maps.Map;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Jubo
 */
public class Siege extends BasePluginGamemode {

    /*
    TODO:
     set up spawn for spectator x
     OT spawn is broken x
     spawn message two times x
     ctf text for OT    x
     no neutral zones   x
     tick at 2 %    x
     multiply tick by people    x
     better spawn system for ffa. iterate through spawns rathen than random x
     greens randomness in matchmaking
     OT doesnt recap    x
     bossbar going up for red team  cant recreate it
     CTF respawn timer doesnt work  x
     no auto tick upwards   x
     lower base time but time is added when captures (40 % rounded up)  x
     stop overtime when blue isnt in the cap zone anymore   x
     you wont be set into spectator mode when dying, still in spectator team    x
     cap message is spammed when dying in it probably   x
     does recapping work (except for last zone)     x
     auto cap for recapturing doesnt work as intended i think       x
     ctf: OT when one point at blue and zero at red. red won when they capped the point x
     eiter remove glass when capping or move it more up
     Change Team Colors:    x
       Blue:Green       x
       Red:Black        x
     Message in ActionBar that red started recapping
     look at balancing during start and midgamejoin again   x
     OT: after recapping time is too high
     cover beacons that arent capturable
     fix freezeplayer (collision is weird) make them invis and collision off for 7 seconds at the start
     dont have them tp in the air

     if(loc.getBlockY() >= 85){
                    if(!SGHandlers.redTeamCaptureDef.contains(player)) SGHandlers.redTeamCaptureDef.add(player);
                }else{
                    SGHandlers.redTeamCaptureDef.remove(player);
                }

     */

    private int time = 2;

    private int addedTime = 1;

    private int tempOTtime = 0;

    private boolean pvpRegistered = false;

    private final ArrayList<String> NeededPoints = new ArrayList<String>(Arrays.asList(new String[]{
            "RedSpawn1",
            "RedSpawn2",
            "BlueSpawn1",
            "BlueSpawn2",
            "CapturePoint1",
            "CapturePoint2"
    }));

    private List<String> capturePointNames = new ArrayList<>();

    private int area = 1;
    private int maxArea = 4;

    private int redScore = 3;
    private int blueScore = 0;

    private final int flagTick = 2;
    //private final int flagTick = 12;

    private final int capturePointRadius = 10;

    private final int respawnTime = 5;

    private final int arrowTime = 8;

    private GameState state;

    Map map;

    private boolean midgameJoin = true;

    private int count;

    private Objective Points;

    private GamemodeHandlers SGHandlers;

    private BossBar bar;

    private String overtime = "";
    private boolean overtimeBool = false;
    private boolean blueOvertimeBool = false;
    private boolean redOvertimeBool = false;
    private boolean overtimeFinished = false;

    private List<Player> redTeam = new ArrayList<>();
    private List<Player> blueTeam = new ArrayList<>();
    private java.util.Map<Player,Integer> deathList = new HashMap<>();

    public Siege(){ state = GameState.IDLE; }

    Runnable tickCQ = new Runnable() {
        @Override
        public void run() {
            time--;
            if(time < 60 ){
                Points.setDisplayName(overtime+"Time: "+ time + "s");
            }else{
                Points.setDisplayName(overtime+"Time: "+(time / 60) + "m "+time%60+"s");
            }
            if(time == 30){
                for(Player player : Bukkit.getOnlinePlayers()){
                    player.sendMessage(ChatColor.GREEN+"30 seconds remaining!");
                }
            }else if(time <= 10 && time > 1){
                for(Player p : Bukkit.getOnlinePlayers()){
                    p.sendMessage(ChatColor.GREEN + String.valueOf(time) + " seconds remaining!");
                }
            }else if(time == 1){
                for(Player p : Bukkit.getOnlinePlayers()){
                    p.sendMessage(ChatColor.GREEN + String.valueOf(time) + " second remaining!");
                }
            }
            if(blueOvertimeBool && overtimeBool && SGHandlers.blueTeamCaptureAttack.isEmpty()) redTeamWin();
            if(time == 0){
                if(redOvertimeBool && !overtimeFinished) blueTeamWin();
                else if(!overtimeBool && !SGHandlers.blueTeamCaptureAttack.isEmpty() && area == maxArea-1){
                    overtime = "Overtime: ";
                    time = 121;
                    overtimeBool = true;
                    blueOvertimeBool = true;
                    for(Player p : Bukkit.getOnlinePlayers()){
                        p.sendMessage(ChatColor.GREEN + "The game is going into overtime! Blacks: Try to capture this point!");
                    }
                }else{
                    redTeamWin();
                }
            }
        }
    };

    Runnable flagPoints = new Runnable() {
        @Override
        public void run() {
            boolean captured = false;
            if(area == maxArea){
                if(!overtimeBool){
                    overtime = "Overtime: ";
                    tempOTtime = time + 1 - 120;
                    time = 121;
                    overtimeBool = true;
                    redOvertimeBool = true;
                    bar.setVisible(false);
                    for(Player p : Bukkit.getOnlinePlayers()){
                        p.sendMessage(ChatColor.GREEN + "The game is going into overtime! Greens: Try to recapture the point.");
                    }
                }else if(!redOvertimeBool ^ overtimeFinished){
                    blueTeamWin();
                }
            }
            if(SGHandlers.capAmount.containsKey("CapturePoint"+area) && SGHandlers.capAmount.get("CapturePoint"+area) >= 100){
                captured = true;
                for(Player player : Bukkit.getOnlinePlayers()){
                    player.sendMessage(ChatColor.BLACK+"The Blacks captured the next point.");
                }
                SGHandlers.capAmount.replace("CapturePoint"+area,100);
                Block b = SGHandlers.points.get("CapturePoint"+area).getBlock().getRelative(0,1,0);
                b.setType(Material.AIR);
                b.setType(Material.BLUE_STAINED_GLASS);
                if(SGHandlers.capAmount.containsKey("CapturePoint"+(area-1))){
                    SGHandlers.capAmount.replace("CapturePoint"+(area-1),100);
                    Block b2 = SGHandlers.points.get("CapturePoint"+(area-1)).getBlock().getRelative(0,1,0);
                    b2.setType(Material.AIR);
                    b2.setType(Material.BLUE_STAINED_GLASS);
                }
                blueScore++;
                redScore--;
                area++;
                Points.getScore(ChatColor.BLACK+"The Blacks' Flags:").setScore(blueScore);
                Points.getScore(ChatColor.DARK_GREEN+"The Greens' Flags:").setScore(redScore);
                SGHandlers.blueTeamCaptureAttack.clear();
                SGHandlers.redTeamCaptureAttack.clear();
                if(capturePointNames.contains("Capture Point "+area)){
                    bar.setTitle(capturePointNames.get(area-1));
                }
                bar.setColor(BarColor.GREEN);
                bar.setProgress(0);
                sendCaptureSound();
                time = time + addedTime;
            }else if(SGHandlers.capAmount.containsKey("CapturePoint"+(area-1))&& SGHandlers.capAmount.get("CapturePoint"+(area-1)) <= 0){
                if(overtimeBool && redOvertimeBool && !overtimeFinished){
                    time = tempOTtime;
                    bar.setVisible(true);
                    overtime = "";
                    overtimeFinished = true;
                }
                if(SGHandlers.capAmount.containsKey("CapturePoint"+area)){
                    SGHandlers.capAmount.replace("CapturePoint"+area,0);
                    Block b2 = SGHandlers.points.get("CapturePoint"+area).getBlock().getRelative(0,1,0);
                    b2.setType(Material.AIR);
                    b2.setType(Material.RED_STAINED_GLASS);
                }
                captured = true;
                for(Player player : Bukkit.getOnlinePlayers()){
                    player.sendMessage(ChatColor.DARK_GREEN+"The Greens recaptured the point.");
                }
                SGHandlers.capAmount.replace("CapturePoint"+(area-1),0);
                Block b = SGHandlers.points.get("CapturePoint"+(area-1)).getBlock().getRelative(0,1,0);
                b.setType(Material.AIR);
                b.setType(Material.RED_STAINED_GLASS);
                blueScore--;
                redScore++;
                area--;
                Points.getScore(ChatColor.BLACK+"The Blacks' Flags:").setScore(blueScore);
                Points.getScore(ChatColor.DARK_GREEN+"The Greens' Flags:").setScore(redScore);
                SGHandlers.blueTeamCaptureDef.clear();
                SGHandlers.redTeamCaptureDef.clear();
                bar.setTitle(capturePointNames.get(area-1));
                bar.setColor(BarColor.GREEN);
                bar.setProgress(0);
                sendCaptureSound();
            }else if(SGHandlers.capAmount.containsKey("CapturePoint"+area) && SGHandlers.capAmount.get("CapturePoint"+area) == 0){
                bar.setProgress(0);
                bar.setColor(BarColor.GREEN);
                Block b = SGHandlers.points.get("CapturePoint"+(area)).getBlock().getRelative(0,1,0);
                b.setType(Material.AIR);
                b.setType(Material.RED_STAINED_GLASS);
            }else if(SGHandlers.capAmount.containsKey("CapturePoint"+area) && SGHandlers.capAmount.get("CapturePoint"+area) > 0){
                bar.setColor(BarColor.WHITE);
            }
            if(!captured) {
                if(SGHandlers.capAmount.containsKey("CapturePoint"+area)) {
                    if (SGHandlers.redTeamCaptureAttack.isEmpty() && SGHandlers.blueTeamCaptureAttack.isEmpty()) {
                        int capAmount = SGHandlers.capAmount.get("CapturePoint" + area);
                        if (capAmount == 0 || capAmount == 100) {
                            //Bukkit.getPlayer("Jubo").sendMessage("Jubo");
                        } else {
                            SGHandlers.capAmount.replace("CapturePoint" + area, SGHandlers.capAmount.get("CapturePoint" + area) - flagTick);
                        }
                    }
                    int flagTickAttack = flagTick * Math.abs(SGHandlers.blueTeamCaptureAttack.size() - SGHandlers.redTeamCaptureAttack.size());
                    if (SGHandlers.blueTeamCaptureAttack.size() < SGHandlers.redTeamCaptureAttack.size()) {
                        if (SGHandlers.capAmount.get("CapturePoint" + area) != 0) {
                            SGHandlers.capAmount.replace("CapturePoint" + area, SGHandlers.capAmount.get("CapturePoint" + area) - flagTickAttack);
                        }
                    } else if (SGHandlers.blueTeamCaptureAttack.size() == SGHandlers.redTeamCaptureAttack.size()) {
                        //do nothing
                    } else if (SGHandlers.blueTeamCaptureAttack.size() > SGHandlers.redTeamCaptureAttack.size()) {
                        SGHandlers.capAmount.replace("CapturePoint" + area, SGHandlers.capAmount.get("CapturePoint" + area) + flagTickAttack);
                    }
                }
                if(SGHandlers.capAmount.containsKey("CapturePoint"+(area-1))) {
                    if(SGHandlers.redTeamCaptureDef.isEmpty() && SGHandlers.blueTeamCaptureDef.isEmpty()){
                        int capAmount = SGHandlers.capAmount.get("CapturePoint"+(area-1));
                        if(capAmount == 100){
                            //do nothing
                        }else {
                            SGHandlers.capAmount.replace("CapturePoint"+(area-1), SGHandlers.capAmount.get("CapturePoint"+(area-1))+flagTick);
                        }
                    }
                    int flagTickDef = flagTick * Math.abs(SGHandlers.blueTeamCaptureDef.size() - SGHandlers.redTeamCaptureDef.size());
                    if(area == maxArea){
                        if (SGHandlers.blueTeamCaptureDef.size() < SGHandlers.redTeamCaptureDef.size()) {
                            SGHandlers.capAmount.replace("CapturePoint" + (area - 1), SGHandlers.capAmount.get("CapturePoint" + (area - 1)) - flagTickDef);
                        } else if (SGHandlers.blueTeamCaptureDef.size() == SGHandlers.redTeamCaptureDef.size()) {
                            //do nothing
                        } else if (SGHandlers.blueTeamCaptureDef.size() > SGHandlers.redTeamCaptureDef.size()) {
                            if (SGHandlers.capAmount.get("CapturePoint" + (area - 1)) != 100) {
                                SGHandlers.capAmount.replace("CapturePoint" + (area - 1), SGHandlers.capAmount.get("CapturePoint" + (area - 1)) + flagTickDef);
                            }
                        }
                    }else{      // if(SGHandlers.capAmount.get("CapturePoint"+area) <= 0)
                        if (SGHandlers.blueTeamCaptureDef.size() < SGHandlers.redTeamCaptureDef.size()) {
                            SGHandlers.capAmount.replace("CapturePoint" + (area - 1), SGHandlers.capAmount.get("CapturePoint" + (area - 1)) - flagTickDef);
                        } else if (SGHandlers.blueTeamCaptureDef.size() == SGHandlers.redTeamCaptureDef.size()) {
                            //do nothing
                        } else if (SGHandlers.blueTeamCaptureDef.size() > SGHandlers.redTeamCaptureDef.size()) {
                            if (SGHandlers.capAmount.get("CapturePoint" + (area - 1)) != 100) {
                                SGHandlers.capAmount.replace("CapturePoint" + (area - 1), SGHandlers.capAmount.get("CapturePoint" + (area - 1)) + flagTickDef);
                            }
                        }
                    }
                }
                if(SGHandlers.capAmount.containsKey("CapturePoint"+area)) {
                    for (Player player : SGHandlers.blueTeamCaptureAttack) {
                        if(SGHandlers.capAmount.get("CapturePoint" + area) > 100){
                            player.sendMessage(ChatColor.GRAY + "Cap at 100%");
                        }else if(SGHandlers.capAmount.get("CapturePoint" + area) < 0){
                            player.sendMessage(ChatColor.GRAY + "Cap at 0%");
                        }else{
                            player.sendMessage(ChatColor.GRAY + "Cap at " + SGHandlers.capAmount.get("CapturePoint" + area) + "%");
                        }
                    }
                    for (Player player : SGHandlers.redTeamCaptureAttack) {
                        if(SGHandlers.capAmount.get("CapturePoint" + area) > 100){
                            player.sendMessage(ChatColor.GRAY + "Cap at 100%");
                        }else if(SGHandlers.capAmount.get("CapturePoint" + area) < 0){
                            player.sendMessage(ChatColor.GRAY + "Cap at 0%");
                        }else{
                            player.sendMessage(ChatColor.GRAY + "Cap at " + SGHandlers.capAmount.get("CapturePoint" + area) + "%");
                        }
                    }
                }
                for(Player player : SGHandlers.blueTeamCaptureDef){
                    if(SGHandlers.capAmount.get("CapturePoint" + (area-1)) > 100){
                        player.sendMessage(ChatColor.GRAY + "Cap at 100%");
                    }else if(SGHandlers.capAmount.get("CapturePoint" + (area-1)) < 0){
                        player.sendMessage(ChatColor.GRAY + "Cap at 0%");
                    }else{
                        player.sendMessage(ChatColor.GRAY+"Cap at "+ SGHandlers.capAmount.get("CapturePoint"+(area-1))+"%");
                    }
                }
                for(Player player : SGHandlers.redTeamCaptureDef){
                    if(SGHandlers.capAmount.get("CapturePoint" + (area-1)) > 100){
                        player.sendMessage(ChatColor.GRAY + "Cap at 100%");
                    }else if(SGHandlers.capAmount.get("CapturePoint" + (area-1)) < 0){
                        player.sendMessage(ChatColor.GRAY + "Cap at 0%");
                    }else{
                        player.sendMessage(ChatColor.GRAY+"Cap at "+ SGHandlers.capAmount.get("CapturePoint"+(area-1))+"%");
                    }
                }
                if(SGHandlers.capAmount.containsKey("CapturePoint"+area)){
                    if((Math.abs((double) SGHandlers.capAmount.get("CapturePoint"+area)/100)<= 1.0)
                        && (Math.abs((double) SGHandlers.capAmount.get("CapturePoint"+area)/100) >= 0)){
                        bar.setProgress(Math.abs((double) SGHandlers.capAmount.get("CapturePoint"+area)/100));
                    }else if(Math.abs((double) SGHandlers.capAmount.get("CapturePoint"+area)/100)> 1.0){
                        bar.setProgress(1.0);
                    }else{
                        bar.setProgress(0);
                    }
                }
            }
        }
    };

    Runnable arrowHandler = () -> {
        for(Player player : Bukkit.getOnlinePlayers()){
            if(!Team.getSpectator().getMembers().contains(player)){
                if(!player.getInventory().contains(Material.ARROW)){
                    player.getInventory().setItem(8,new ItemStack(Material.ARROW,1));
                }else if(player.getInventory().getItem(8) == null){
                    player.getInventory().remove(Material.ARROW);
                    player.getInventory().setItem(8,new ItemStack(Material.ARROW,1));
                }else if(player.getInventory().getItem(8).getAmount() < 24){
                    player.getInventory().addItem(new ItemStack(Material.ARROW,1));
                }
            }
        }
    };

    Runnable respawnTimer = () -> {
        for(Player player : deathList.keySet()){
            if(deathList.get(player) == 0){
                deathList.remove(player);
                if(redTeam.contains(player)){
                    addToTeam(player,Team.Teams.GREENS);
                }else if(blueTeam.contains(player)){
                    addToTeam(player,Team.Teams.BLACKS);
                }
            }else{
                player.sendMessage(ChatColor.GREEN + "Respawn in "+deathList.get(player));
                deathList.replace(player,deathList.get(player)-1);
            }
        }
    };

    Runnable playerMove = new Runnable() {
        @Override
        public void run() {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Location loc = player.getLocation();
                if (map.getImportantPoints().containsKey("CapturePoint" + area)) {
                    if (loc.distance(map.getImportantPoints().get("CapturePoint" + area).toBukkitLoc()) < capturePointRadius
                            && (loc.getBlock().getRelative(0, -2, 0).getType() == Material.HONEY_BLOCK
                            || loc.getBlock().getRelative(0, -3, 0).getType() == Material.HONEY_BLOCK
                            || loc.getBlock().getRelative(0, -4, 0).getType() == Material.HONEY_BLOCK)) {
                        if (Team.getGreens().getMembers().contains(player)) {
                            if (!SGHandlers.redTeamCaptureAttack.contains(player)) SGHandlers.redTeamCaptureAttack.add(player);
                        } else if (Team.getBlacks().getMembers().contains(player)) {
                            if (!SGHandlers.blueTeamCaptureAttack.contains(player)) SGHandlers.blueTeamCaptureAttack.add(player);
                        }
                    } else
                        /*
                        if (loc.distance(map.getImportantPoints().get("CapturePoint" + area).toBukkitLoc()) < capturePointRadius
                            && (loc.getBlock().getRelative(0, -2, 0).getType() != Material.HONEY_BLOCK
                            || loc.getBlock().getRelative(0, -3, 0).getType() != Material.HONEY_BLOCK
                            || loc.getBlock().getRelative(0, -4, 0).getType() != Material.HONEY_BLOCK))

                         */
                    {
                        if (Team.getGreens().getMembers().contains(player)) {
                            SGHandlers.redTeamCaptureAttack.remove(player);
                        } else if (Team.getBlacks().getMembers().contains(player)) {
                            SGHandlers.blueTeamCaptureAttack.remove(player);
                        }
                    }
                }
                if (area != 1) {
                    if (loc.distance(map.getImportantPoints().get("CapturePoint" + (area - 1)).toBukkitLoc()) < capturePointRadius
                            && (loc.getBlock().getRelative(0, -2, 0).getType() == Material.HONEY_BLOCK
                            || loc.getBlock().getRelative(0, -3, 0).getType() == Material.HONEY_BLOCK
                            || loc.getBlock().getRelative(0, -4, 0).getType() == Material.HONEY_BLOCK)) {
                        if (Team.getGreens().getMembers().contains(player)) {
                            if (!SGHandlers.redTeamCaptureDef.contains(player)) SGHandlers.redTeamCaptureDef.add(player);
                        } else if (Team.getBlacks().getMembers().contains(player)) {
                            if (!SGHandlers.blueTeamCaptureDef.contains(player)) SGHandlers.blueTeamCaptureDef.add(player);
                        }
                    } else /*if (loc.distance(map.getImportantPoints().get("CapturePoint" + (area - 1)).toBukkitLoc()) < capturePointRadius
                            && (loc.getBlock().getRelative(0, -2, 0).getType() != Material.HONEY_BLOCK
                            || loc.getBlock().getRelative(0, -3, 0).getType() != Material.HONEY_BLOCK
                            || loc.getBlock().getRelative(0, -4, 0).getType() != Material.HONEY_BLOCK))
                            */
                    {
                        if (Team.getGreens().getMembers().contains(player)) {
                            SGHandlers.redTeamCaptureDef.remove(player);
                        } else if (Team.getBlacks().getMembers().contains(player)) {
                            SGHandlers.blueTeamCaptureDef.remove(player);
                        }
                    }
                }
            }
        }
    };

    @Override
    public void Start(Map m, int parameter){
        kdSort();
        count = PVPPlugin.getCountdownTime();
        state = GameState.COUNTDOWN;
        super.Start(m,parameter);
        this.map = m;
        time = parameter;

        if(!map.getImportantPoints().keySet().containsAll(NeededPoints)){
            for(Player p : players){
                p.sendMessage(ChatColor.RED + "Game cannot start! Not all needed points have been added!");
            }
            End(m);
        }

        if(!pvpRegistered){
            SGHandlers = new GamemodeHandlers();
            PluginManager pm = PVPPlugin.getServerInstance().getPluginManager();
            pm.registerEvents(SGHandlers,PVPPlugin.getPlugin());
            pvpRegistered = true;
        }
        for(Location l : SGHandlers.points.values()){
            l.getBlock().setType(Material.BEACON);

            l.getBlock().getRelative(0, -1, -1).setType(Material.IRON_BLOCK);
            l.getBlock().getRelative(0, -1, 0).setType(Material.IRON_BLOCK);
            l.getBlock().getRelative(0, -1, 1).setType(Material.IRON_BLOCK);
            l.getBlock().getRelative(1, -1, -1).setType(Material.IRON_BLOCK);
            l.getBlock().getRelative(1, -1, 0).setType(Material.IRON_BLOCK);
            l.getBlock().getRelative(1, -1, 1).setType(Material.IRON_BLOCK);
            l.getBlock().getRelative(-1, -1, -1).setType(Material.IRON_BLOCK);
            l.getBlock().getRelative(-1, -1, 0).setType(Material.IRON_BLOCK);
            l.getBlock().getRelative(-1, -1, 1).setType(Material.IRON_BLOCK);
            l.getBlock().getRelative(0,1,0).setType(Material.RED_STAINED_GLASS);
        }
        for(Player p: players){
            if(Team.getBlacks().size() < Team.getGreens().size()){
                Team.getBlacks().add(p);
                p.teleport(m.getImportantPoints().get("BlueSpawn1").toBukkitLoc().add(0,2,0));
                freezePlayer(p,140);
                blueTeam.add(p);
            }else if (Team.getGreens().size() <= Team.getBlacks().size()){
                Team.getGreens().add(p);
                p.teleport(m.getImportantPoints().get("RedSpawn1").toBukkitLoc().add(0,2,0));
                freezePlayer(p,140);
                redTeam.add(p);
            }
        }
        for(Player player : Bukkit.getServer().getOnlinePlayers()){
            if(!Team.getGreens().getMembers().contains(player) && !Team.getBlacks().getMembers().contains(player)){
                Team.getSpectator().add(player);
                player.teleport(m.getSpawn().toBukkitLoc().add(0, 2, 0));
            }
        }

        Bukkit.getScheduler().scheduleSyncRepeatingTask(PVPPlugin.getPlugin(), new Runnable() {
            @Override
            public void run() {
                if(count == 0){
                    if(state == GameState.RUNNING){
                        return;
                    }
                    bar = Bukkit.createBossBar(ChatColor.WHITE+capturePointNames.get(area-1), BarColor.GREEN, BarStyle.SOLID);
                    bar.setProgress(0);
                    bar.setVisible(true);

                    Bukkit.getScheduler().scheduleSyncRepeatingTask(PVPPlugin.getPlugin(),tickCQ,0,20);
                    Bukkit.getScheduler().scheduleSyncRepeatingTask(PVPPlugin.getPlugin(),flagPoints,0,20);
                    Bukkit.getScheduler().scheduleSyncRepeatingTask(PVPPlugin.getPlugin(),arrowHandler,0,20*arrowTime);
                    Bukkit.getScheduler().scheduleSyncRepeatingTask(PVPPlugin.getPlugin(),respawnTimer,0,20);
                    Bukkit.getScheduler().scheduleSyncRepeatingTask(PVPPlugin.getPlugin(),playerMove,0,20);

                    Points = getScoreboard().registerNewObjective("Score","dummy");
                    Points.setDisplayName("Time: " + time + "m");
                    time *= 60;
                    addedTime = (int) (time * 0.4);
                    Points.getScore(ChatColor.BLACK+"The Blacks' Flags:").setScore(blueScore);
                    Points.getScore(ChatColor.DARK_GREEN+"The Greens' Flags:").setScore(redScore);
                    Points.setDisplaySlot(DisplaySlot.SIDEBAR);

                    for(Player p : Bukkit.getServer().getOnlinePlayers()){
                        p.sendMessage(ChatColor.GREEN + "Game Start!");
                        p.setScoreboard(getScoreboard());
                    }

                    for(Player p : Team.getGreens().getMembers()){
                        GearHandler.giveGear(p, ChatColor.DARK_GREEN, GearHandler.SpecialGear.NONE);
                        bar.addPlayer(p);
                    }
                    for(Player p : Team.getBlacks().getMembers()){
                        GearHandler.giveGear(p, ChatColor.BLACK, GearHandler.SpecialGear.NONE);
                        bar.addPlayer(p);
                    }
                    state = GameState.RUNNING;
                    count = -1;
                    for(Player p : players){
                        p.sendMessage(ChatColor.GRAY + "Use " + ChatColor.GREEN + "/unstuck" + ChatColor.GRAY + " if you're stuck in a block!");
                    }
                }else if(count != -1){
                    for(Player p : Bukkit.getServer().getOnlinePlayers()){
                        p.sendMessage(ChatColor.GREEN+"Game begins in "+count);
                    }
                    count--;
                }
            }
        },40,20);
    }

    public void End(Map m){
        state = GameState.IDLE;
        for(Location l: SGHandlers.points.values()){
            l.getBlock().setType(Material.AIR);
            l.getBlock().getRelative(0, 1, 0).setType(Material.AIR);
        }
        for(String key : SGHandlers.capAmount.keySet()){
            SGHandlers.capAmount.replace(key,0);
        }
        SGHandlers.redTeamCaptureAttack.clear();
        SGHandlers.blueTeamCaptureAttack.clear();
        SGHandlers.redTeamCaptureDef.clear();
        SGHandlers.blueTeamCaptureDef.clear();
        area = 1;
        redScore = SGHandlers.areaTemp;
        blueScore = 0;
        bar.removeAll();
        overtime = "";
        blueOvertimeBool = false;
        overtimeBool = false;
        redOvertimeBool = false;
        overtimeFinished = false;
        redTeam.clear();
        blueTeam.clear();
        deathList.clear();
        getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
        m.playerLeaveAll();
        PVPCommand.queueNextGame();
        super.End(m);
    }

    private void sendCaptureSound(){
        for(Player player : Team.getRed().getMembers()){
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL,10.0F,1.0F);
        }
        for(Player player : Team.getBlue().getMembers()){
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL,10.0F,1.0F);
        }
    }

    private void redTeamWin(){
        for(Player player : Bukkit.getOnlinePlayers()){
            player.sendMessage(ChatColor.DARK_GREEN+"Game over!");
            player.sendMessage(ChatColor.DARK_GREEN+"The Greens win!");
        }
        PlayerStat.addGameWon(Team.Teams.RED);
        PlayerStat.addGameLost(Team.Teams.BLUE);
        PlayerStat.addGameSpectatedAll();
        End(map);
    }

    private void blueTeamWin(){
        for(Player player : Bukkit.getOnlinePlayers()){
            player.sendMessage(ChatColor.BLACK+"Game over!");
            player.sendMessage(ChatColor.BLACK+"The Blacks win!");
        }
        PlayerStat.addGameWon(Team.Teams.BLUE);
        PlayerStat.addGameLost(Team.Teams.RED);
        PlayerStat.addGameSpectatedAll();
        End(map);
    }

    @Override
    public void playerLeave(Player p){ Team.removeFromTeam(p);}

    public boolean midgamePlayerJoin(Player p){
        if(state == GameState.RUNNING || state == GameState.COUNTDOWN){

            if(deathList.containsKey(p)) return false;
            if(Team.getGreens().getAllMembers().contains(p)){
                addToTeam(p, Team.Teams.GREENS);
            }
            else if(Team.getBlacks().getAllMembers().contains(p)){
                addToTeam(p, Team.Teams.BLACKS);
            }

            else{
                if(Team.getGreens().size() > Team.getBlacks().size()){
                    addToTeam(p, Team.Teams.BLACKS);
                }
                else{
                    addToTeam(p, Team.Teams.GREENS);
                }
            }
            super.midgamePlayerJoin(p);
            return true;
        }
        else{
            return false;
        }
    }

    private void addToTeam(Player player, Team.Teams team){
        if(team == Team.Teams.GREENS){
            Team.getGreens().add(player);
            if(map.getImportantPoints().containsKey("RedSpawn"+area)){
                player.teleport(map.getImportantPoints().get("RedSpawn"+area).toBukkitLoc().add(0,2,0));
            }else if(map.getImportantPoints().containsKey("RedSpawn"+(area-1))){
                player.teleport(map.getImportantPoints().get("RedSpawn"+(area-1)).toBukkitLoc().add(0,2,0));
            }
            GearHandler.giveGear(player,ChatColor.DARK_GREEN, GearHandler.SpecialGear.NONE);
            if(!redTeam.contains(player))redTeam.add(player);
        }else{
            Team.getBlacks().add(player);
            if(map.getImportantPoints().containsKey("BlueSpawn"+area)){
                player.teleport(map.getImportantPoints().get("BlueSpawn"+area).toBukkitLoc().add(0,2,0));
            }else if(map.getImportantPoints().containsKey("BlueSpawn"+(area-1))){
                player.teleport(map.getImportantPoints().get("BlueSpawn"+(area-1)).toBukkitLoc().add(0,2,0));
            }
            GearHandler.giveGear(player,ChatColor.BLACK, GearHandler.SpecialGear.NONE);
            if(!blueTeam.contains(player))blueTeam.add(player);
        }
    }

    public String requiresParameter(){
        return "time in minutes";
    }

    private class GamemodeHandlers implements Listener {
        private HashMap<String, Location> points = new HashMap<>();
        private HashMap<String, Integer> capAmount = new HashMap<>();
        private List<Player> redTeamCaptureAttack = new ArrayList<>();
        private List<Player> blueTeamCaptureAttack = new ArrayList<>();
        private List<Player> redTeamCaptureDef = new ArrayList<>();
        private List<Player> blueTeamCaptureDef = new ArrayList<>();
        int areaTemp = 0;

        public GamemodeHandlers() {
            int i = 1;
            for (java.util.Map.Entry<String, EventLocation> e : map.getImportantPoints().entrySet()) {
                if (e.getKey().contains("Point")) {
                    points.put(e.getKey(), e.getValue().toBukkitLoc());
                    capAmount.put(e.getKey(), 0);
                    areaTemp = areaTemp + 1;
                    capturePointNames.add("Capture Point " + areaTemp);
                }
            }
            maxArea = areaTemp + 1;
            redScore = areaTemp;
        }


        @EventHandler
        public void onPlayerDeath(PlayerDeathEvent event) {
            if (state == GameState.RUNNING) {
                if (event.getEntity() instanceof Player) {
                    Player player = (Player) event.getEntity();
                    if (Team.getGreens().getMembers().contains(player)) {
                        redTeamCaptureAttack.remove(player);
                        redTeamCaptureDef.remove(player);
                    } else if (Team.getBlacks().getMembers().contains(player)) {
                        blueTeamCaptureAttack.remove(player);
                        blueTeamCaptureDef.remove(player);
                    }
                    deathList.put(player,respawnTime);
                    Team.getSpectator().add(event.getEntity());
                }
            }
        }

        @EventHandler
        public void onPlayerLeave(PlayerQuitEvent event) {
            if (state == GameState.RUNNING || state == GameState.COUNTDOWN) {
                Team.removeFromTeam(event.getPlayer());
                deathList.remove(event.getPlayer());
                blueTeam.remove(event.getPlayer());
                redTeam.remove(event.getPlayer());
                if (Team.getGreens().size() <= 0) {
                    blueTeamWin();
                }
                if (Team.getBlacks().size() <= 0) {
                    redTeamWin();
                }
            }
        }

        @EventHandler
        public void onPlayerRespawn(PlayerRespawnEvent event) {
            if (state == GameState.RUNNING) {
                if (map.getImportantPoints().containsKey("BlueSpawn" + area)) {
                    if (Team.getBlacks().getMembers().contains(event.getPlayer())) {
                        event.setRespawnLocation(map.getImportantPoints().get("BlueSpawn" + area).toBukkitLoc().add(0, 2, 0));
                    } else if (Team.getGreens().getMembers().contains(event.getPlayer())) {
                        event.setRespawnLocation(map.getImportantPoints().get("RedSpawn" + area).toBukkitLoc().add(0, 2, 0));
                    }else{
                        event.setRespawnLocation(map.getSpawn().toBukkitLoc().add(0,2,0));
                    }
                }else if (map.getImportantPoints().containsKey("BlueSpawn" + (area - 1))) {
                    if (Team.getBlacks().getMembers().contains(event.getPlayer())) {
                        event.setRespawnLocation(map.getImportantPoints().get("BlueSpawn" + (area - 1)).toBukkitLoc().add(0, 2, 0));
                    } else if (Team.getGreens().getMembers().contains(event.getPlayer())) {
                        event.setRespawnLocation(map.getImportantPoints().get("RedSpawn" + (area - 1)).toBukkitLoc().add(0, 2, 0));
                    }else{
                        event.setRespawnLocation(map.getSpawn().toBukkitLoc().add(0,2,0));
                    }
                }
            }
        }
    }

    @Override
    public ArrayList<String> getNeededPoints() { return NeededPoints; }

    @Override
    public GameState getState(){ return state; }

    public Objective getPoints(){ return Points; }

    @Override
    public boolean isMidgameJoin(){ return midgameJoin; }
}
