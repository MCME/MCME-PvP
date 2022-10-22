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
import org.bukkit.event.player.PlayerMoveEvent;
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
     better PlayerMoveEvent
     msgs for RespawnTimer  x
     test RespawnTimer  (what happens if you do /pvp join when dead)
     Reset Beacon Colors, still some weird issue    x
     check for Honeyblocks below the cap areas instead of just radius   x

     better way:
     public void initializePlayerMoveRunnable() {
        getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Location playerLocation = player.getLocation();

                }
            }
        }, 0L, 20L);
     */

    private int time = 15;

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

    private final int flagTick = 5;

    private final int capturePointRadius = 10;

    private GameState state;

    Map map;

    private boolean midgameJoin = true;

    private int count;

    private Objective Points;

    private GamemodeHandlers GMHandlers;

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
            if(time == 0){
                if(redOvertimeBool && !overtimeFinished) blueTeamWin();
                else if(!overtimeBool && !GMHandlers.blueTeamCaptureAttack.isEmpty() && area == maxArea-1){
                    overtime = "Overtime: ";
                    time = 121;
                    overtimeBool = true;
                    blueOvertimeBool = true;
                    for(Player p : Bukkit.getOnlinePlayers()){
                        p.sendMessage(ChatColor.GREEN + "The game is going into overtime! Blue Team: Try to capture this point!");
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
                    tempOTtime = time + 1;
                    time = 121;
                    overtimeBool = true;
                    redOvertimeBool = true;
                    bar.setVisible(false);
                    for(Player p : Bukkit.getOnlinePlayers()){
                        p.sendMessage(ChatColor.GREEN + "The game is going into overtime! Red Team: Try to recapture the point.");
                    }
                }else if(!redOvertimeBool ^ overtimeFinished){
                    blueTeamWin();
                }
            }
            if(GMHandlers.capAmount.containsKey("CapturePoint"+area) && GMHandlers.capAmount.get("CapturePoint"+area) >= 100){
                captured = true;
                for(Player player : Bukkit.getOnlinePlayers()){
                    player.sendMessage(ChatColor.BLUE+"Blue team captured the next point.");
                }
                Block b = GMHandlers.points.get("CapturePoint"+area).getBlock().getRelative(0,1,0);
                b.setType(Material.AIR);
                b.setType(Material.BLUE_STAINED_GLASS);
                if(GMHandlers.capAmount.containsKey("CapturePoint"+(area-1))){
                    GMHandlers.capAmount.replace("CapturePoint"+(area-1),100);
                    Block b2 = GMHandlers.points.get("CapturePoint"+(area-1)).getBlock().getRelative(0,1,0);
                    b2.setType(Material.AIR);
                    b2.setType(Material.BLUE_STAINED_GLASS);
                }
                blueScore++;
                redScore--;
                area++;
                Points.getScore(ChatColor.BLUE+"Blue team flags:").setScore(blueScore);
                Points.getScore(ChatColor.RED+"Red team flags:").setScore(redScore);
                GMHandlers.blueTeamCaptureAttack.clear();
                GMHandlers.redTeamCaptureAttack.clear();
                if(capturePointNames.contains("Capture Point "+area)){
                    bar.setTitle(capturePointNames.get(area-1));
                }
                bar.setColor(BarColor.RED);
                bar.setProgress(1.0);
                sendCaptureSound();
            }else if(GMHandlers.capAmount.containsKey("CapturePoint"+(area-1))&&GMHandlers.capAmount.get("CapturePoint"+(area-1)) <= -100){
                if(overtimeBool && redOvertimeBool && !overtimeFinished){
                    time = tempOTtime;
                    bar.setVisible(true);
                    overtime = "";
                    overtimeFinished = true;
                }
                if(GMHandlers.capAmount.containsKey("CapturePoint"+area)){
                    GMHandlers.capAmount.replace("CapturePoint"+area,-100);
                    Block b2 = GMHandlers.points.get("CapturePoint"+area).getBlock().getRelative(0,1,0);
                    b2.setType(Material.AIR);
                    b2.setType(Material.RED_STAINED_GLASS);
                }
                captured = true;
                for(Player player : Bukkit.getOnlinePlayers()){
                    player.sendMessage(ChatColor.RED+"Red team recaptured the point.");
                }
                Block b = GMHandlers.points.get("CapturePoint"+(area-1)).getBlock().getRelative(0,1,0);
                b.setType(Material.AIR);
                b.setType(Material.RED_STAINED_GLASS);
                blueScore--;
                redScore++;
                area--;
                Points.getScore(ChatColor.BLUE+"Blue team flags:").setScore(blueScore);
                Points.getScore(ChatColor.RED+"Red team flags:").setScore(redScore);
                GMHandlers.blueTeamCaptureDef.clear();
                GMHandlers.redTeamCaptureDef.clear();
                bar.setTitle(capturePointNames.get(area-1));
                bar.setColor(BarColor.RED);
                bar.setProgress(1.0);
                sendCaptureSound();
            }else if(GMHandlers.capAmount.containsKey("CapturePoint"+area) && GMHandlers.capAmount.get("CapturePoint"+area) == 0){
                Block b = GMHandlers.points.get("CapturePoint"+area).getBlock().getRelative(0,1,0);
                b.setType(Material.AIR);
                if(bar.getColor() == BarColor.BLUE) bar.setColor(BarColor.RED);
                else if(bar.getColor() == BarColor.RED) bar.setColor(BarColor.BLUE);
                bar.setProgress(0.0);
            }else if(GMHandlers.capAmount.containsKey("CapturePoint"+area) && GMHandlers.capAmount.get("CapturePoint"+area) >= 0){
                bar.setColor(BarColor.BLUE);
            }else if(GMHandlers.capAmount.containsKey("CapturePoint"+area) && GMHandlers.capAmount.get("CapturePoint"+area) <= 0){
                bar.setColor(BarColor.RED);
            }else if(GMHandlers.capAmount.containsKey("CapturePoint"+(area-1)) && GMHandlers.capAmount.get("CapturePoint"+(area-1)) >= 0){
                bar.setColor(BarColor.BLUE);
            }else if(GMHandlers.capAmount.containsKey("CapturePoint"+(area-1)) && GMHandlers.capAmount.get("CapturePoint"+(area-1)) <= 0){
                bar.setColor(BarColor.RED);
            }
            else if(GMHandlers.capAmount.containsKey("CapturePoint"+(area-1))&&GMHandlers.capAmount.get("CapturePoint"+(area-1)) == 0){
                Block b = GMHandlers.points.get("CapturePoint"+(area-1)).getBlock().getRelative(0,1,0);
                b.setType(Material.AIR);
            }
            if(!captured) {
                if(GMHandlers.capAmount.containsKey("CapturePoint"+area)) {
                    if (GMHandlers.redTeamCaptureAttack.isEmpty() && GMHandlers.blueTeamCaptureAttack.isEmpty()) {
                        int capAmount = GMHandlers.capAmount.get("CapturePoint" + area);
                        if (capAmount == 0 || capAmount == 100 || capAmount == -100) {
                            //Bukkit.getPlayer("Jubo").sendMessage("Jubo");
                        } else if (capAmount <= 50 && capAmount > 0) {
                            GMHandlers.capAmount.replace("CapturePoint" + area, GMHandlers.capAmount.get("CapturePoint" + area) - flagTick);
                        } else if (capAmount >= 50 && capAmount < 100) {
                            GMHandlers.capAmount.replace("CapturePoint" + area, GMHandlers.capAmount.get("CapturePoint" + area) + flagTick);
                        } else if (capAmount >= -50 && capAmount < 0) {
                            GMHandlers.capAmount.replace("CapturePoint" + area, GMHandlers.capAmount.get("CapturePoint" + area) + flagTick);
                        } else if (capAmount <= -50 && capAmount > -100) {
                            GMHandlers.capAmount.replace("CapturePoint" + area, GMHandlers.capAmount.get("CapturePoint" + area) - flagTick);
                        } else if (capAmount > 0) {
                            GMHandlers.capAmount.replace("CapturePoint" + area, GMHandlers.capAmount.get("CapturePoint" + area) - flagTick);
                        } else if (capAmount < 0) {
                            GMHandlers.capAmount.replace("CapturePoint" + area, GMHandlers.capAmount.get("CapturePoint" + area) + flagTick);
                        }
                    }
                    int flagTickAttack = flagTick;
                    if (GMHandlers.blueTeamCaptureAttack.size() < GMHandlers.redTeamCaptureAttack.size()) {
                        if (GMHandlers.capAmount.get("CapturePoint" + area) != -100) {
                            GMHandlers.capAmount.replace("CapturePoint" + area, GMHandlers.capAmount.get("CapturePoint" + area) - flagTickAttack);
                        }
                    } else if (GMHandlers.blueTeamCaptureAttack.size() == GMHandlers.redTeamCaptureAttack.size()) {
                        //do nothing
                    } else if (GMHandlers.blueTeamCaptureAttack.size() > GMHandlers.redTeamCaptureAttack.size()) {
                        GMHandlers.capAmount.replace("CapturePoint" + area, GMHandlers.capAmount.get("CapturePoint" + area) + flagTickAttack);
                    }
                }
                if(GMHandlers.capAmount.containsKey("CapturePoint"+(area-1))) {
                    if(GMHandlers.redTeamCaptureDef.isEmpty() && GMHandlers.blueTeamCaptureDef.isEmpty()){
                        int capAmount = GMHandlers.capAmount.get("CapturePoint"+(area-1));
                        if(capAmount == 0 || capAmount == 100 || capAmount == -100){
                            //do nothing
                        }else if(capAmount <= 50 && capAmount > 0){
                            GMHandlers.capAmount.replace("CapturePoint"+(area-1),GMHandlers.capAmount.get("CapturePoint"+(area-1))-flagTick);
                        }else if(capAmount >= 50 && capAmount < 100){
                            GMHandlers.capAmount.replace("CapturePoint"+(area-1),GMHandlers.capAmount.get("CapturePoint"+(area-1))+flagTick);
                        }else if(capAmount >= -50 && capAmount < 0){
                            GMHandlers.capAmount.replace("CapturePoint"+(area-1),GMHandlers.capAmount.get("CapturePoint"+(area-1))+flagTick);
                        } else if(capAmount <= -50 && capAmount > -100){
                            GMHandlers.capAmount.replace("CapturePoint"+(area-1),GMHandlers.capAmount.get("CapturePoint"+(area-1))-flagTick);
                        }else if(capAmount > 0){
                            GMHandlers.capAmount.replace("CapturePoint"+(area-1),GMHandlers.capAmount.get("CapturePoint"+(area-1))-flagTick);
                        }else if(capAmount < 0){
                            GMHandlers.capAmount.replace("CapturePoint"+(area-1),GMHandlers.capAmount.get("CapturePoint"+(area-1))+flagTick);
                        }
                    }
                    int flagTickDef = flagTick;
                    if(area == maxArea){
                        if (GMHandlers.blueTeamCaptureDef.size() < GMHandlers.redTeamCaptureDef.size()) {
                            GMHandlers.capAmount.replace("CapturePoint" + (area - 1), GMHandlers.capAmount.get("CapturePoint" + (area - 1)) - flagTickDef);
                        } else if (GMHandlers.blueTeamCaptureDef.size() == GMHandlers.redTeamCaptureDef.size()) {
                            //do nothing
                        } else if (GMHandlers.blueTeamCaptureDef.size() > GMHandlers.redTeamCaptureDef.size()) {
                            if (GMHandlers.capAmount.get("CapturePoint" + (area - 1)) != 100) {
                                GMHandlers.capAmount.replace("CapturePoint" + (area - 1), GMHandlers.capAmount.get("CapturePoint" + (area - 1)) + flagTickDef);
                            }
                        }
                    }else if(GMHandlers.capAmount.get("CapturePoint"+area) <= 0) {
                        if (GMHandlers.blueTeamCaptureDef.size() < GMHandlers.redTeamCaptureDef.size()) {
                            GMHandlers.capAmount.replace("CapturePoint" + (area - 1), GMHandlers.capAmount.get("CapturePoint" + (area - 1)) - flagTickDef);
                        } else if (GMHandlers.blueTeamCaptureDef.size() == GMHandlers.redTeamCaptureDef.size()) {
                            //do nothing
                        } else if (GMHandlers.blueTeamCaptureDef.size() > GMHandlers.redTeamCaptureDef.size()) {
                            if (GMHandlers.capAmount.get("CapturePoint" + (area - 1)) != 100) {
                                GMHandlers.capAmount.replace("CapturePoint" + (area - 1), GMHandlers.capAmount.get("CapturePoint" + (area - 1)) + flagTickDef);
                            }
                        }
                    }
                }
                if(GMHandlers.capAmount.containsKey("CapturePoint"+area)) {
                    for (Player player : GMHandlers.blueTeamCaptureAttack) {
                        if (GMHandlers.capAmount.get("CapturePoint" + area) >= 0) {
                            player.sendMessage(ChatColor.BLUE + "Cap at " + GMHandlers.capAmount.get("CapturePoint" + area) + "%");
                        } else {
                            player.sendMessage(ChatColor.RED + "Cap at " + GMHandlers.capAmount.get("CapturePoint" + area) * -1 + "%");
                        }
                    }
                    for (Player player : GMHandlers.redTeamCaptureAttack) {
                        if (GMHandlers.capAmount.get("CapturePoint" + area) >= 0) {
                            player.sendMessage(ChatColor.BLUE + "Cap at " + GMHandlers.capAmount.get("CapturePoint" + area) + "%");
                        } else {
                            player.sendMessage(ChatColor.RED + "Cap at " + GMHandlers.capAmount.get("CapturePoint" + area) * -1 + "%");
                        }
                    }
                }
                for(Player player : GMHandlers.blueTeamCaptureDef){
                    if(GMHandlers.capAmount.get("CapturePoint"+(area-1)) >= 0){
                        player.sendMessage(ChatColor.BLUE+"Cap at "+GMHandlers.capAmount.get("CapturePoint"+(area-1))+"%");
                    }else{
                        player.sendMessage(ChatColor.RED+"Cap at "+GMHandlers.capAmount.get("CapturePoint"+(area-1))*-1+"%");
                    }
                }
                for(Player player : GMHandlers.redTeamCaptureDef){
                    if(GMHandlers.capAmount.get("CapturePoint"+(area-1)) >= 0){
                        player.sendMessage(ChatColor.BLUE+"Cap at "+GMHandlers.capAmount.get("CapturePoint"+(area-1))+"%");
                    }else{
                        player.sendMessage(ChatColor.RED+"Cap at "+GMHandlers.capAmount.get("CapturePoint"+(area-1))*-1+"%");
                    }
                }
                if(GMHandlers.capAmount.containsKey("CapturePoint"+area)){
                    bar.setProgress(Math.abs((double)GMHandlers.capAmount.get("CapturePoint"+area)/100));
                }
            }
        }
    };

    Runnable arrowHandler = new Runnable() {
        @Override
        public void run() {
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
        }
    };

    Runnable respawnTimer = new Runnable() {
        @Override
        public void run() {
            for(Player player : deathList.keySet()){
                if(deathList.get(player) == 0){
                    deathList.remove(player);
                    if(redTeam.contains(player)){
                        Team.getRed().add(player);
                        addToTeam(player,Team.Teams.RED);
                    }else if(blueTeam.contains(player)){
                        Team.getBlue().add(player);
                        addToTeam(player,Team.Teams.BLUE);
                    }
                }else{
                    player.sendMessage(ChatColor.GREEN + "Respawn in "+deathList.get(player));
                    deathList.replace(player,deathList.get(player)-1);
                }
            }
        }
    };

    @Override
    public void Start(Map m, int parameter){
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
            GMHandlers = new GamemodeHandlers();
            PluginManager pm = PVPPlugin.getServerInstance().getPluginManager();
            pm.registerEvents(GMHandlers,PVPPlugin.getPlugin());
            pvpRegistered = true;
        }
        for(Location l : GMHandlers.points.values()){
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
        for(Player p : Bukkit.getOnlinePlayers()) {
            if(players.contains(p)){
                if (Team.getBlue().size() <= Team.getRed().size()){
                    Team.getBlue().add(p);
                    p.teleport(m.getImportantPoints().get("BlueSpawn1").toBukkitLoc().add(0,2,0));
                    freezePlayer(p,140);
                    blueTeam.add(p);
                }else if(Team.getRed().size() < Team.getBlue().size()){
                    Team.getRed().add(p);
                    p.teleport(m.getImportantPoints().get("RedSpawn1").toBukkitLoc().add(0,2,0));
                    freezePlayer(p,140);
                    redTeam.add(p);
                }
            } else{
                Team.getSpectator().add(p);
                p.teleport(m.getSpawn().toBukkitLoc().add(0,2,0));
            }
        }

        Bukkit.getScheduler().scheduleSyncRepeatingTask(PVPPlugin.getPlugin(), new Runnable() {
            @Override
            public void run() {
                if(count == 0){
                    if(state == GameState.RUNNING){
                        return;
                    }
                    bar = Bukkit.createBossBar(ChatColor.WHITE+capturePointNames.get(area-1), BarColor.RED, BarStyle.SEGMENTED_20);
                    bar.setProgress(1.0);
                    bar.setVisible(true);

                    Bukkit.getScheduler().scheduleSyncRepeatingTask(PVPPlugin.getPlugin(),tickCQ,0,20);
                    Bukkit.getScheduler().scheduleSyncRepeatingTask(PVPPlugin.getPlugin(),flagPoints,0,20);
                    Bukkit.getScheduler().scheduleSyncRepeatingTask(PVPPlugin.getPlugin(),arrowHandler,0,20*5);
                    Bukkit.getScheduler().scheduleSyncRepeatingTask(PVPPlugin.getPlugin(),respawnTimer,0,20);

                    Points = getScoreboard().registerNewObjective("Score","dummy");
                    Points.setDisplayName("Time: " + time + "m");
                    time *= 60;
                    Points.getScore(ChatColor.BLUE+"Blue team flags:").setScore(blueScore);
                    Points.getScore(ChatColor.RED+"Red team flags:").setScore(redScore);
                    Points.setDisplaySlot(DisplaySlot.SIDEBAR);

                    for(Player p : Bukkit.getServer().getOnlinePlayers()){
                        p.sendMessage(ChatColor.GREEN + "Game Start!");
                        p.setScoreboard(getScoreboard());
                    }

                    for(Player p : Team.getRed().getMembers()){
                        GearHandler.giveGear(p, ChatColor.RED, GearHandler.SpecialGear.NONE);
                        bar.addPlayer(p);
                    }
                    for(Player p : Team.getBlue().getMembers()){
                        GearHandler.giveGear(p, ChatColor.BLUE, GearHandler.SpecialGear.NONE);
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
        for(Location l: GMHandlers.points.values()){
            l.getBlock().setType(Material.AIR);
            l.getBlock().getRelative(0, 1, 0).setType(Material.AIR);
        }
        for(String key : GMHandlers.capAmount.keySet()){
            GMHandlers.capAmount.replace(key,-100);
        }
        GMHandlers.redTeamCaptureAttack.clear();
        GMHandlers.blueTeamCaptureAttack.clear();
        GMHandlers.redTeamCaptureDef.clear();
        GMHandlers.blueTeamCaptureDef.clear();
        area = 1;
        redScore = GMHandlers.areaTemp;
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
            player.sendMessage(ChatColor.RED+"Game over!");
            player.sendMessage(ChatColor.RED+"Red team wins!");
        }
        PlayerStat.addGameWon(Team.Teams.RED);
        PlayerStat.addGameLost(Team.Teams.BLUE);
        PlayerStat.addGameSpectatedAll();
        End(map);
    }

    private void blueTeamWin(){
        for(Player player : Bukkit.getOnlinePlayers()){
            player.sendMessage(ChatColor.BLUE+"Game over!");
            player.sendMessage(ChatColor.BLUE+"Blue team wins!");
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

            if(Team.getRed().getAllMembers().contains(p)){
                addToTeam(p, Team.Teams.RED);
            }
            else if(Team.getBlue().getAllMembers().contains(p)){
                addToTeam(p, Team.Teams.BLUE);
            }

            else{
                if(Team.getRed().size() >= Team.getBlue().size()){
                    addToTeam(p, Team.Teams.BLUE);
                }
                else{
                    addToTeam(p, Team.Teams.RED);
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
        if(team == Team.Teams.RED){
            Team.getRed().add(player);
            player.teleport(map.getImportantPoints().get("RedSpawn"+String.valueOf(area)).toBukkitLoc().add(0,2,0));
            GearHandler.giveGear(player,ChatColor.RED, GearHandler.SpecialGear.NONE);
            redTeam.add(player);
        }else{
            Team.getBlue().add(player);
            player.teleport(map.getImportantPoints().get("BlueSpawn"+String.valueOf(area)).toBukkitLoc().add(0,2,0));
            GearHandler.giveGear(player,ChatColor.BLUE, GearHandler.SpecialGear.NONE);
            blueTeam.add(player);
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
                    capAmount.put(e.getKey(), -100);
                    areaTemp = areaTemp + 1;
                    capturePointNames.add("Capture Point " + areaTemp);
                }
            }
            maxArea = areaTemp + 1;
            redScore = areaTemp;
        }

        @EventHandler
        public void onPlayerMove(PlayerMoveEvent event) {
            if (state == GameState.RUNNING) {
                int y;
                Player player = event.getPlayer();
                Location loc = player.getLocation();
                if (map.getImportantPoints().containsKey("CapturePoint" + area)) {
                    y = map.getImportantPoints().get("CapturePoint" + area).getY();
                    if (loc.distance(map.getImportantPoints().get("CapturePoint" + area).toBukkitLoc()) < capturePointRadius
                        && (loc.getBlock().getRelative(0,-2,0).getType() == Material.HONEY_BLOCK
                            || loc.getBlock().getRelative(0,-3,0).getType() == Material.HONEY_BLOCK
                            || loc.getBlock().getRelative(0,-4,0).getType() == Material.HONEY_BLOCK)) {
                        if (Team.getRed().getMembers().contains(event.getPlayer())) {
                            if (!redTeamCaptureAttack.contains(player)) redTeamCaptureAttack.add(player);
                        } else if (Team.getBlue().getMembers().contains(event.getPlayer())) {
                            if (!blueTeamCaptureAttack.contains(player)) blueTeamCaptureAttack.add(player);
                        }
                    } else if (loc.distance(map.getImportantPoints().get("CapturePoint" + area).toBukkitLoc()) < capturePointRadius
                            && (loc.getBlock().getRelative(0,-2,0).getType() != Material.HONEY_BLOCK
                            || loc.getBlock().getRelative(0,-3,0).getType() != Material.HONEY_BLOCK
                            || loc.getBlock().getRelative(0,-4,0).getType() != Material.HONEY_BLOCK)) {
                        if (Team.getRed().getMembers().contains(event.getPlayer())) {
                            redTeamCaptureAttack.remove(player);
                        } else if (Team.getBlue().getMembers().contains(event.getPlayer())) {
                            blueTeamCaptureAttack.remove(player);
                        }
                    }
                }
                if (area != 1) {
                    y = map.getImportantPoints().get("CapturePoint" + (area - 1)).getY();
                    if (loc.distance(map.getImportantPoints().get("CapturePoint" + (area - 1)).toBukkitLoc()) < capturePointRadius
                            && (loc.getBlock().getRelative(0,-2,0).getType() == Material.HONEY_BLOCK
                            || loc.getBlock().getRelative(0,-3,0).getType() == Material.HONEY_BLOCK
                            || loc.getBlock().getRelative(0,-4,0).getType() == Material.HONEY_BLOCK)) {
                        if (Team.getRed().getMembers().contains(event.getPlayer())) {
                            if (!redTeamCaptureDef.contains(player)) redTeamCaptureDef.add(player);
                        } else if (Team.getBlue().getMembers().contains(event.getPlayer())) {
                            if (!blueTeamCaptureDef.contains(player)) blueTeamCaptureDef.add(player);
                        }
                    } else if (loc.distance(map.getImportantPoints().get("CapturePoint" + (area - 1)).toBukkitLoc()) < capturePointRadius
                            && (loc.getBlock().getRelative(0,-2,0).getType() != Material.HONEY_BLOCK
                            || loc.getBlock().getRelative(0,-3,0).getType() != Material.HONEY_BLOCK
                            || loc.getBlock().getRelative(0,-4,0).getType() != Material.HONEY_BLOCK)) {
                        if (Team.getRed().getMembers().contains(event.getPlayer())) {
                            redTeamCaptureDef.remove(player);
                        } else if (Team.getBlue().getMembers().contains(event.getPlayer())) {
                            blueTeamCaptureDef.remove(player);
                        }
                    }
                }
                /*
                if(player.getLocation().getBlockY() > 90){
                    if(!redTeamCaptureDef.contains(player)) redTeamCaptureDef.add(player);
                    //Bukkit.getPlayer("Jubo").sendMessage("Test");
                }else{
                    redTeamCaptureDef.remove(player);
                    //Bukkit.getPlayer("Jubo").sendMessage("Test2");
                }
                 */
            }
        }

        @EventHandler
        public void onPlayerDeath(PlayerDeathEvent event) {
            if (state == GameState.RUNNING) {
                if (event.getEntity() instanceof Player) {
                    Player player = (Player) event.getEntity();
                    if (Team.getRed().getMembers().contains(player)) {
                        redTeamCaptureAttack.remove(player);
                        redTeamCaptureDef.remove(player);
                    } else if (Team.getBlue().getMembers().contains(player)) {
                        blueTeamCaptureAttack.remove(player);
                        blueTeamCaptureDef.remove(player);
                    }
                    //Logger.getLogger("TestDeath");
                    deathList.put(player,5);
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
                if (Team.getRed().size() <= 0) {
                    blueTeamWin();
                }
                if (Team.getBlue().size() <= 0) {
                    redTeamWin();
                }
            }
        }

        @EventHandler
        public void onPlayerRespawn(PlayerRespawnEvent event) {
            if (state == GameState.RUNNING) {
                if (map.getImportantPoints().containsKey("BlueSpawn" + area)) {
                    if (Team.getBlue().getMembers().contains(event.getPlayer())) {
                        event.setRespawnLocation(map.getImportantPoints().get("BlueSpawn" + area).toBukkitLoc().add(0, 2, 0));
                    } else if (Team.getRed().getMembers().contains(event.getPlayer())) {
                        event.setRespawnLocation(map.getImportantPoints().get("RedSpawn" + area).toBukkitLoc().add(0, 2, 0));
                    }
                }else if (map.getImportantPoints().containsKey("BlueSpawn" + (area - 1))) {
                    if (Team.getBlue().getMembers().contains(event.getPlayer())) {
                        event.setRespawnLocation(map.getImportantPoints().get("BlueSpawn" + (area - 1)).toBukkitLoc().add(0, 2, 0));
                    } else if (Team.getRed().getMembers().contains(event.getPlayer())) {
                        event.setRespawnLocation(map.getImportantPoints().get("RedSpawn" + (area - 1)).toBukkitLoc().add(0, 2, 0));
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
