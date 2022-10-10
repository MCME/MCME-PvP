package com.mcmiddleearth.mcme.pvp.Gamemode;

import com.mcmiddleearth.mcme.pvp.Handlers.GearHandler;
import com.mcmiddleearth.mcme.pvp.PVP.PlayerStat;
import com.mcmiddleearth.mcme.pvp.PVP.Team;
import com.mcmiddleearth.mcme.pvp.PVPPlugin;
import com.mcmiddleearth.mcme.pvp.Util.EventLocation;
import com.mcmiddleearth.mcme.pvp.command.PVPCommand;
import com.mcmiddleearth.mcme.pvp.maps.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
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
public class Conquest extends BasePluginGamemode {

    private int time = 15;

    private boolean pvpRegistered = false;

    private final ArrayList<String> NeededPoints = new ArrayList<String>(Arrays.asList(new String[]{
            "RedSpawn1",
            "RedSpawn2",
            "RedSpawn3",
            "BlueSpawn1",
            "BlueSpawn2",
            "BlueSpawn3",
            "CapturePoint1",
            "CapturePoint2",
            "CapturePoint3"
    }));

    private int area = 1;

    private int redScore = 3;
    private int blueScore = 0;

    private final int capturePointRadius = 3;

    private GameState state;

    Map map;

    private boolean midgameJoin = true;

    private int count;

    private Objective Points;

    private GamemodeHandlers GMHandlers;

    // TODO:
    //  area can be 1,2 or 3
    //  decides which CapturePoint can be accessed
    //  also decides which spawn is used
    //  reworking capturing with PlayerMoveEvent
    //  Maybe even capture faster when more people in area

    public Conquest(){ state = GameState.IDLE; }

    Runnable tickCQ = new Runnable() {
        @Override
        public void run() {
            time--;
            if(time % 60 == 0){
                Points.setDisplayName("Time: "+(time / 60) + "m");
            }else if(time < 60 ){
                Points.setDisplayName("Time: "+ time + "s");
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
                redTeamWin();
            }
        }
    };

    Runnable flagPoints = new Runnable() {
        @Override
        public void run() {
            boolean captured = false;
            if(area == 4){
                blueTeamWin();
            }
            if(GMHandlers.capAmount.get("CapturePoint"+area) == 100){
                //Tea, captured
                area++;
                captured = true;
                for(Player player : Bukkit.getOnlinePlayers()){
                    player.sendMessage(ChatColor.BLUE+"Blue team captured the next point.");
                }
                //Block b = e.getClickedBlock().getLocation().add(0, 1, 0).getBlock();
                Block b = GMHandlers.points.get("CapturePoint"+area).add(0,1,0).getBlock();
                b.setType(Material.BLUE_STAINED_GLASS);
                blueScore--;
                redScore++;
                Points.getScore(ChatColor.BLUE+"Blue team flags:").setScore(blueScore);
                Points.getScore(ChatColor.RED+"Red team flags:").setScore(redScore);
            }else if(GMHandlers.capAmount.containsKey("CapturePoint"+(area-1))&&GMHandlers.capAmount.get("CapturePoint"+(area-1)) == -100){
                //Tea, captured
                area--;
                captured = true;
                for(Player player : Bukkit.getOnlinePlayers()){
                    player.sendMessage(ChatColor.RED+"Red team recaptured the point.");
                }
                Block b = GMHandlers.points.get("CapturePoint"+(area-1)).add(0,1,0).getBlock();
                b.setType(Material.RED_STAINED_GLASS);
                blueScore++;
                redScore--;
                Points.getScore(ChatColor.BLUE+"Blue team flags:").setScore(blueScore);
                Points.getScore(ChatColor.RED+"Red team flags:").setScore(redScore);
            }
            if(!captured) {
                if (GMHandlers.blueTeamCaptureAttack.size() < GMHandlers.redTeamCaptureAttack.size()) {
                    GMHandlers.capAmount.replace("CapturePoint"+area,GMHandlers.capAmount.get("CapturePoint"+area)+3);
                } else if (GMHandlers.blueTeamCaptureAttack.size() == GMHandlers.redTeamCaptureAttack.size()) {
                    //do nothing
                } else if (GMHandlers.blueTeamCaptureAttack.size() > GMHandlers.redTeamCaptureAttack.size()) {
                    GMHandlers.capAmount.replace("CapturePoint"+area,GMHandlers.capAmount.get("CapturePoint"+area)-3);
                }
                if(GMHandlers.capAmount.containsKey("CapturePoint"+(area-1))) {
                    if (GMHandlers.blueTeamCaptureDef.size() < GMHandlers.redTeamCaptureDef.size()) {
                        GMHandlers.capAmount.replace("CapturePoint" + area, GMHandlers.capAmount.get("CapturePoint" + area) + 3);
                    } else if (GMHandlers.blueTeamCaptureDef.size() == GMHandlers.redTeamCaptureDef.size()) {
                        //do nothing
                    } else if (GMHandlers.blueTeamCaptureDef.size() > GMHandlers.redTeamCaptureDef.size()) {
                        GMHandlers.capAmount.replace("CapturePoint" + area, GMHandlers.capAmount.get("CapturePoint" + area) - 3);
                    }
                }
                for(Player player : GMHandlers.blueTeamCaptureAttack){
                    if(GMHandlers.capAmount.get("CapturePoint"+area) >= 0){
                        player.sendMessage(ChatColor.BLUE+"Cap at "+GMHandlers.capAmount.get("CapturePoint"+area)+"%");
                    }else{
                        player.sendMessage(ChatColor.RED+"Cap at "+GMHandlers.capAmount.get("CapturePoint"+area)*-1+"%");
                    }
                }
                for(Player player : GMHandlers.redTeamCaptureAttack){
                    if(GMHandlers.capAmount.get("CapturePoint"+area) >= 0){
                        player.sendMessage(ChatColor.BLUE+"Cap at "+GMHandlers.capAmount.get("CapturePoint"+area)+"%");
                    }else{
                        player.sendMessage(ChatColor.RED+"Cap at "+GMHandlers.capAmount.get("CapturePoint"+area)*-1+"%");
                    }
                }
                for(Player player : GMHandlers.blueTeamCaptureDef){
                    if(GMHandlers.capAmount.get("CapturePoint"+area) >= 0){
                        player.sendMessage(ChatColor.BLUE+"Cap at "+GMHandlers.capAmount.get("CapturePoint"+(area-1))+"%");
                    }else{
                        player.sendMessage(ChatColor.RED+"Cap at "+GMHandlers.capAmount.get("CapturePoint"+(area-1))*-1+"%");
                    }
                }
                for(Player player : GMHandlers.redTeamCaptureDef){
                    if(GMHandlers.capAmount.get("CapturePoint"+area) >= 0){
                        player.sendMessage(ChatColor.BLUE+"Cap at "+GMHandlers.capAmount.get("CapturePoint"+(area-1))+"%");
                    }else{
                        player.sendMessage(ChatColor.RED+"Cap at "+GMHandlers.capAmount.get("CapturePoint"+(area-1))*-1+"%");
                    }
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
        }
        for(Player p : Bukkit.getOnlinePlayers()) {
            if(players.contains(p)){
                if (Team.getBlue().size() <= Team.getBlue().size()){
                    Team.getRed().add(p);
                    p.teleport(m.getImportantPoints().get("Redspawn1").toBukkitLoc().add(0,2,0));
                    freezePlayer(p,140);
                }else if(Team.getBlue().size() < Team.getRed().size()){
                    Team.getBlue().add(p);
                    p.teleport(m.getImportantPoints().get("Bluespawn1").toBukkitLoc().add(0,2,0));
                    freezePlayer(p,140);
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

                    Bukkit.getScheduler().scheduleSyncRepeatingTask(PVPPlugin.getPlugin(),tickCQ,0,20);
                    Bukkit.getScheduler().scheduleSyncRepeatingTask(PVPPlugin.getPlugin(),flagPoints,0,20);

                    Points = getScoreboard().registerNewObjective("Score","dummy");
                    Points.setDisplayName("Time: " + time + "m");
                    time *= 60;
                    Points.getScore(ChatColor.BLUE+"Blue team flags:").setScore(blueScore);
                    Points.getScore(ChatColor.RED+"Red team flags:").setScore(redScore);
                    Points.setDisplaySlot(DisplaySlot.SIDEBAR);
                    state = GameState.RUNNING;
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

        getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
        m.playerLeaveAll();
        PVPCommand.queueNextGame();
        super.End(m);
    }

    private void redTeamWin(){
        for(Player player : Bukkit.getOnlinePlayers()){
            player.sendMessage(ChatColor.BLUE+"Game over!");
            player.sendMessage(ChatColor.BLUE+"Red team wins!");
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
        //work with area
        if(team == Team.Teams.RED){
            Team.getRed().add(player);
            player.teleport(map.getImportantPoints().get("Redspawn"+String.valueOf(area)).toBukkitLoc().add(0,2,0));
            GearHandler.giveGear(player,ChatColor.RED, GearHandler.SpecialGear.NONE);
        }else{
            Team.getBlue().add(player);
            player.teleport(map.getImportantPoints().get("Bluespawn"+String.valueOf(area)).toBukkitLoc().add(0,2,0));
            GearHandler.giveGear(player,ChatColor.BLUE, GearHandler.SpecialGear.NONE);
        }
    }

    public String requiresParameter(){
        return "";
    }

    private class GamemodeHandlers implements Listener {
        private HashMap<String, Location> points = new HashMap<>();
        private HashMap<String,Integer> capAmount = new HashMap<>();
        private List<Player> redTeamCaptureAttack = new ArrayList<>();
        private List<Player> blueTeamCaptureAttack = new ArrayList<>();
        private List<Player> redTeamCaptureDef = new ArrayList<>();
        private List<Player> blueTeamCaptureDef = new ArrayList<>();


        public GamemodeHandlers(){

            for(java.util.Map.Entry<String, EventLocation> e: map.getImportantPoints().entrySet()){
                if(e.getKey().contains("Point")){
                    points.put(e.getKey(),e.getValue().toBukkitLoc());
                    capAmount.put(e.getKey(),-100);
                }
            }
        }

        @EventHandler
        public void onPlayerMove(PlayerMoveEvent event){
            // check if player enters area of beacon
            Player player = event.getPlayer();
            Location loc = player.getLocation();
            if(loc.distance(map.getImportantPoints().get("CapturePoint"+String.valueOf(area)).toBukkitLoc())>capturePointRadius){
                if(Team.getRed().getMembers().contains(event.getPlayer())){
                    redTeamCaptureAttack.add(player);
                }else if(Team.getBlue().getMembers().contains(event.getPlayer())){
                    blueTeamCaptureAttack.add(player);
                }
            }else if(loc.distance(map.getImportantPoints().get("CapturePoint"+String.valueOf(area)).toBukkitLoc())<capturePointRadius){
                if(Team.getRed().getMembers().contains(event.getPlayer())){
                    redTeamCaptureAttack.remove(player);
                }else if(Team.getBlue().getMembers().contains(event.getPlayer())){
                    blueTeamCaptureAttack.remove(player);
                }
            }
            if(area != 1){
                if(loc.distance(map.getImportantPoints().get("CapturePoint"+String.valueOf(area-1)).toBukkitLoc())>capturePointRadius){
                    if(Team.getRed().getMembers().contains(event.getPlayer())){
                        redTeamCaptureDef.add(player);
                    }else if(Team.getBlue().getMembers().contains(event.getPlayer())){
                        blueTeamCaptureDef.add(player);
                    }
                }else if(loc.distance(map.getImportantPoints().get("CapturePoint"+String.valueOf(area-1)).toBukkitLoc())<capturePointRadius){
                    if(Team.getRed().getMembers().contains(event.getPlayer())){
                        redTeamCaptureDef.remove(player);
                    }else if(Team.getBlue().getMembers().contains(event.getPlayer())){
                        blueTeamCaptureDef.remove(player);
                    }
                }
            }
        }

        @EventHandler
        public void onPlayerDeath(PlayerDeathEvent event){
            if(state == GameState.RUNNING){
                if(event.getEntity() instanceof Player){
                    Player player = (Player) event.getEntity();
                    if(Team.getRed().getMembers().contains(player)){
                        redTeamCaptureAttack.remove(player);
                    }else if(Team.getBlue().getMembers().contains(player)){
                        blueTeamCaptureAttack.remove(player);
                    }
                }
            }
        }

        @EventHandler
        public void onPlayerLeave(PlayerQuitEvent event){
            if(state == GameState.RUNNING || state == GameState.COUNTDOWN){
                Team.removeFromTeam(event.getPlayer());
                if(Team.getRed().size() <= 0){ // Maybe needs changing
                    blueTeamWin();
                }
                if(Team.getBlue().size() <= 0){ // Maybe needs changing
                    redTeamWin();
                }
            }
        }

        @EventHandler
        public void onPlayerRespawn(PlayerRespawnEvent event){
            if(state == GameState.RUNNING){
                if(Team.getBlue().getMembers().contains(event.getPlayer())){
                    event.setRespawnLocation(map.getImportantPoints().get("Bluespawn"
                            +String.valueOf(area)).toBukkitLoc().add(0,2,0));
                }else if(Team.getRed().getMembers().contains(event.getPlayer())){
                    event.setRespawnLocation(map.getImportantPoints().get("Redspawn"
                            +String.valueOf(area)).toBukkitLoc().add(0,2,0));
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
