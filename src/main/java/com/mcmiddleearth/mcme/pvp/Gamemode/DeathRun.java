package com.mcmiddleearth.mcme.pvp.Gamemode;

import com.mcmiddleearth.mcme.pvp.Handlers.GearHandler;
import com.mcmiddleearth.mcme.pvp.Handlers.GearHandler.SpecialGear;
import com.mcmiddleearth.mcme.pvp.PVP.PlayerStat;
import com.mcmiddleearth.mcme.pvp.PVP.Team;
import com.mcmiddleearth.mcme.pvp.PVPPlugin;
import com.mcmiddleearth.mcme.pvp.Util.EventLocation;
import com.mcmiddleearth.mcme.pvp.command.PVPCommand;
import com.mcmiddleearth.mcme.pvp.maps.Map;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class DeathRun extends com.mcmiddleearth.mcme.pvp.Gamemode.BasePluginGamemode {
    private boolean pvpRegistered = false;

    @Getter
    private final ArrayList<String> NeededPoints = new ArrayList<>(Arrays.asList(new String[]{
            "RunnerSpawn",
            "DeathSpawn",
            "VictoryPoint"
    }));

    private GameState state;

    Map map;

    private int count;

    private Objective Points;

    private GamemodeHandlers DRHandlers;

    private int time;
    private ArrayList<Player> winners = new ArrayList<>();

    public DeathRun() {
        state = GameState.IDLE;
    }

    Runnable tick = new Runnable() {
        @Override
        public void run() {
            time--;

            if(time < 60 ){
                Points.setDisplayName("Time: "+ time + "s");
            }else{
                Points.setDisplayName("Time: "+(time / 60) + "m "+time%60+"s");
            }

            if (time == 30) {

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(ChatColor.GREEN + "30 seconds remaining!");
                }

            } else if (time <= 10 && time > 1) {

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(ChatColor.GREEN + String.valueOf(time) + " seconds remaining!");
                }

            } else if (time == 1) {

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(ChatColor.GREEN + String.valueOf(time) + " second remaining!");
                }

            }

            if (time == 0) {
                if(winners.size() != 0) {
                    sendWinMessage();
                    PlayerStat.addGameWon(Team.Teams.RUNNER);
                    PlayerStat.addGameLost(Team.Teams.DEATH);
                }
                else {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendMessage(ChatColor.RED + "Game over!");
                        p.sendMessage(ChatColor.RED + "Death Wins!");
                    }
                    PlayerStat.addGameWon(Team.Teams.DEATH);
                    PlayerStat.addGameLost(Team.Teams.RUNNER);
                }
                PlayerStat.addGameSpectatedAll();
                End(map);
            }
        }
    };

    @Override
    public void Start(Map m, int parameter) {
        count = PVPPlugin.getCountdownTime();
        state = GameState.COUNTDOWN;
        super.Start(m, parameter);
        this.map = m;
        time = parameter;

        Random rand = new Random();

        if (!map.getImportantPoints().keySet().containsAll(NeededPoints)) {
            for (Player p : players) {
                p.sendMessage(ChatColor.RED + "Game cannot start! Not all needed points have been added!");
            }
            End(m);
        }

        if (!pvpRegistered) {
            DRHandlers = new GamemodeHandlers();
            PluginManager pm = PVPPlugin.getServerInstance().getPluginManager();
            pm.registerEvents(DRHandlers, PVPPlugin.getPlugin());
            pvpRegistered = true;
        }

        for (Location l : DRHandlers.points) {
            l.getBlock().setType(Material.BEACON);
        }

        int c = 0;
        int death = rand.nextInt(players.size());
        for (Player p : players) {
            if (c == death) {
                Team.getDeath().add(p);
                p.teleport(m.getImportantPoints().get("DeathSpawn").toBukkitLoc());
            } else {
                Team.getRunner().add(p);
                p.teleport(m.getImportantPoints().get("RunnerSpawn").toBukkitLoc());
                freezePlayer(p, 140);
            }
            c++;
        }

        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            if (!Team.getRunner().getMembers().contains(player) && !Team.getDeath().getMembers().contains(player)) {
                Team.getSpectator().add(player);
                player.teleport(m.getSpawn().toBukkitLoc().add(0, 2, 0));
            }
        }
        Bukkit.getScheduler().scheduleSyncRepeatingTask(PVPPlugin.getPlugin(), new Runnable() {
            @Override
            public void run() {
                if (count == 0) {
                    if (state == GameState.RUNNING) {
                        for(Player p : Team.getDeath().getMembers())
                            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2));
                        return;
                    }

                    Bukkit.getScheduler().scheduleSyncRepeatingTask(PVPPlugin.getPlugin(), tick, 0, 20);

                    Points = getScoreboard().registerNewObjective("Remaining", "dummy");
                    Points.setDisplayName("Time: " + time + "m");
                    time *= 60;
                    Points.getScore(ChatColor.BLUE + "Runners:").setScore(Team.getRunner().size());
                    Points.setDisplaySlot(DisplaySlot.SIDEBAR);

                    for (Player p : Bukkit.getServer().getOnlinePlayers()) {
                        p.sendMessage(ChatColor.GREEN + "Game Start!");
                    }

                    for (Player p : Team.getRunner().getMembers()) {
                        p.setScoreboard(getScoreboard());
                    }
                    for (Player p : Team.getDeath().getMembers()) {
                        GearHandler.giveGear(p, ChatColor.BLACK, SpecialGear.NONE);
                    }
                    state = GameState.RUNNING;
                    count = -1;

                    for (Player p : players) {
                        p.sendMessage(ChatColor.GRAY + "Use " + ChatColor.GREEN + "/unstuck" + ChatColor.GRAY + " if you're stuck in a block!");
                    }

                } else if (count != -1) {
                    for (Player p : Bukkit.getServer().getOnlinePlayers()) {
                        p.sendMessage(ChatColor.GREEN + "Game begins in " + count);
                    }
                    count--;
                }
            }

        }, 40, 20);
    }

    @Override
    public void End(Map m) {
        state = GameState.IDLE;

        getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
        m.playerLeaveAll();
        winners.clear();
        PVPCommand.queueNextGame();
        super.End(m);
    }

    public String requiresParameter() {
        return "time in minutes";
    }

    public boolean isMidgameJoin() {
        if (time >= 120) {
            return true;
        } else {
            return false;
        }
    }

    private class GamemodeHandlers implements Listener {

        private ArrayList<Location> points = new ArrayList<>();

        public GamemodeHandlers() {
            for (java.util.Map.Entry<String, EventLocation> e : map.getImportantPoints().entrySet()) {
                if (e.getKey().contains("Point")) {
                    points.add(e.getValue().toBukkitLoc());
                }
            }
        }

        @EventHandler
        public void onPlayerInteract(PlayerInteractEvent e) {
            if (state == GameState.RUNNING && players.contains(e.getPlayer()) &&
                    e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                Player p = e.getPlayer();
                e.setUseInteractedBlock(Event.Result.DENY);
                if (e.getClickedBlock().getType().equals(Material.BEACON) && Team.getRunner().getMembers().contains(p)) {
                    if (!winners.contains(p)) {
                        winners.add(p);
                        for (Player pl: Bukkit.getServer().getOnlinePlayers()) {
                            pl.sendMessage(ChatColor.BLUE + p.getDisplayName() + " has reached the goal!");
                        }
                        Points.getScore(ChatColor.BLUE + "Runners:").setScore(Team.getRunner().size() - 1);
                        Team.getRunner().getMembers().remove(p);
                        if(Team.getRunner().size() == 0){
                            sendWinMessage();
                            End(map);
                        }
                        if (state == GameState.RUNNING) {
                            Team.getSpectator().add(p);
                        }
                    }
                }
            }
        }


        @EventHandler
        public void onPlayerDeath(PlayerDeathEvent e) {
            if (e.getEntity() instanceof Player && state == GameState.RUNNING) {
                Player p = e.getEntity();
                if (Team.getRunner().getMembers().contains(p)) {
                    Points.getScore(ChatColor.BLUE + "Runners:").setScore(Team.getRunner().size() - 1);
                    e.setDeathMessage(ChatColor.BLUE + p.getName() + ChatColor.GRAY + " has died");
                    Team.getRunner().getMembers().remove(p);
                    if (state == GameState.RUNNING) {
                        Team.getSpectator().add(p);
                    }
                }
                if (Team.getRunner().size() < 1 && winners.size() == 0) {

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendMessage(ChatColor.RED + "Game over!");
                        player.sendMessage(ChatColor.RED + "Death Wins!");

                    }
                    PlayerStat.addGameWon(Team.Teams.DEATH);
                    PlayerStat.addGameLost(Team.Teams.RUNNER);
                    PlayerStat.addGameSpectatedAll();
                    End(map);
                } else if (Team.getRunner().size() == 0) {
                    PlayerStat.addGameLost(Team.Teams.DEATH);
                    PlayerStat.addGameWon(Team.Teams.RUNNER);
                    sendWinMessage();
                    End(map);
                }
                if (Team.getDeath().getMembers().contains(p)) {
                    e.setDeathMessage(ChatColor.BLACK + p.getName() + ChatColor.GRAY + " has died");
                }
            }
        }


        @EventHandler
        public void onPlayerRespawn(PlayerRespawnEvent e) {
            final Player p = e.getPlayer();
            if (state == GameState.RUNNING && players.contains(e.getPlayer())) {
                if (Team.getDeath().getMembers().contains(p)) {
                    e.setRespawnLocation(map.getImportantPoints().get("DeathSpawn").toBukkitLoc());
                    GearHandler.giveGear(p, ChatColor.BLACK, SpecialGear.NONE);
                } else {
                    e.setRespawnLocation(map.getSpawn().toBukkitLoc());
                    e.getPlayer().getInventory().clear();
                    e.getPlayer().getInventory().setArmorContents(new ItemStack[] {new ItemStack(Material.AIR), new ItemStack(Material.AIR),
                            new ItemStack(Material.AIR), new ItemStack(Material.AIR)});
                }
            }
        }
    }
        @EventHandler
        public void onPlayerLeave(PlayerQuitEvent e) {
            final Player p = e.getPlayer();
            if(state == GameState.RUNNING || state == GameState.COUNTDOWN){
                Team.removeFromTeam(p);
                Points.getScore(ChatColor.BLUE + "Runners:").setScore(Team.getRunner().size());
                if(Team.getRunner().size() <= 0){

                    for(Player player : Bukkit.getOnlinePlayers()){
                        player.sendMessage(ChatColor.RED + "Game over!");
                        player.sendMessage(ChatColor.RED + "Death Wins!");
                    }
                    PlayerStat.addGameWon(Team.Teams.DEATH);
                    PlayerStat.addGameLost(Team.Teams.RUNNER);
                    PlayerStat.addGameSpectatedAll();
                    End(map);
                }
            }
        }

        @Override
        public boolean midgamePlayerJoin(Player p) {
            if (time >= 120 && !Team.getRunner().getAllMembers().contains(p)) {
                Team.getRunner().add(p);
                p.teleport(map.getImportantPoints().get("RunnerSpawn").toBukkitLoc().add(0, 2, 0));
                Points.getScore(ChatColor.BLUE + "Runners:").setScore(Team.getRunner().size());
                super.midgamePlayerJoin(p);
                return true;
            } else {
                return false;
            }
        }

    public void sendWinMessage(){
            String remainingPlayers = "";
            int loopnum = 0;
            for (Player pl : winners) {
                if (winners.size() > 1 && loopnum == (winners.size() - 1)) {

                    remainingPlayers += (", and " + pl.getName());
                } else if (winners.size() == 1 || loopnum == 0) {
                    remainingPlayers += (" " + pl.getName());
                } else {
                    remainingPlayers += (", " + pl.getName());
                }

                loopnum++;
            }
            for (Player pl : Bukkit.getOnlinePlayers()) {
                pl.sendMessage(ChatColor.BLUE + "Game over!");
                if (winners.size() == 1)
                    pl.sendMessage(ChatColor.BLUE + remainingPlayers + " wins!");
                else
                    pl.sendMessage(ChatColor.BLUE + remainingPlayers + " win!");
            }
    }

    @Override
    public GameState getState() {
        return state;
    }

    public Objective getPoints() {
        return Points;
    }
}
