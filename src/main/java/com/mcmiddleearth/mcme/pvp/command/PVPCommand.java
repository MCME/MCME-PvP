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
package com.mcmiddleearth.mcme.pvp.command;

import com.google.common.collect.Lists;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mcmiddleearth.mcme.pvp.Handlers.GearHandler;
import com.mcmiddleearth.mcme.pvp.Gamemode.BasePluginGamemode.GameState;
import com.mcmiddleearth.mcme.pvp.Handlers.BukkitTeamHandler;
import com.mcmiddleearth.mcme.pvp.Handlers.ChatHandler;
import com.mcmiddleearth.mcme.pvp.PVP.PlayerStat;
import com.mcmiddleearth.mcme.pvp.maps.Map;
import com.mcmiddleearth.mcme.pvp.Permissions;
import com.mcmiddleearth.mcme.pvp.maps.MapEditor;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import sun.java2d.loops.Blit;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

import static com.mcmiddleearth.mcme.pvp.Handlers.GearHandler.CustomItem.PIPE;


public class PVPCommand extends CommandDispatcher<Player>{

    private final com.mcmiddleearth.mcme.pvp.PVPPlugin PVPPlugin;
    private static volatile HashMap<String, Map> maps;
    @Getter
    private static volatile Set<String> mapNames;
    @Getter
    private static volatile boolean locked = true;
    @Getter
    private static String Message = "PvP-server Locked";
    private static Queue<Map> gameQueue = new LinkedList<>();
    private static Queue<Integer> parameterQueue = new LinkedList<>();;
    @Getter private static Map nextGame = null;
    private static int parameter;
    @Getter @Setter protected static Map runningGame = null;

    public PVPCommand(com.mcmiddleearth.mcme.pvp.PVPPlugin PVPPlugin1) {
        PVPPlugin = PVPPlugin1;
        reloadMaplist();
        register(LiteralArgumentBuilder.<Player>literal("pvp")
                .then(LiteralArgumentBuilder.<Player>literal("fbt").executes(c->{
                            doCommand("fbt", (Player) c.getSource());
                            return 1;
                        }))
            .then(LiteralArgumentBuilder.<Player>literal("map")
                .then(LiteralArgumentBuilder.<Player>literal("list").executes(c -> {
                    doCommand("mapList", c.getSource());
                    return 1;} ))
                .then(RequiredArgumentBuilder.<Player, String>argument("name", new CommandNewMapArgument()).requires( c -> c.hasPermission(Permissions.PVP_ADMIN.getPermissionNode())).executes(c ->{
                    doCommand("createMap", c.getArgument("name", String.class), c.getSource());
                    return 1; }))
                .then(LiteralArgumentBuilder.<Player>literal("spawn").requires(c -> c.hasPermission(Permissions.JOIN.getPermissionNode()))
                    .then(RequiredArgumentBuilder.<Player, String>argument("name", new CommandMapArgument()).executes(c ->{
                    doCommand("teleport", c.getArgument("name", String.class), c.getSource());
                    return 1;}))))
            .then(LiteralArgumentBuilder.<Player>literal("game")
                .then(LiteralArgumentBuilder.<Player>literal("quickstart").requires( c -> c.hasPermission(Permissions.RUN.getPermissionNode()))
                    .then(RequiredArgumentBuilder.<Player, String>argument( "map", new CommandMapArgument()).executes(c -> {
                        doCommand("createGame", c.getArgument("map", String.class), c.getSource());
                        return 1;} )
                        .then(RequiredArgumentBuilder.<Player,String>argument("variable", new com.mcmiddleearth.mcme.pvp.command.CommandIntVariableArgument()).executes(c -> {
                            doCommand("createVarGame", c.getArgument("map", String.class), c.getArgument("variable", String.class), c.getSource());
                            return 1;} )
                            .then(LiteralArgumentBuilder.<Player>literal("test").executes(c -> {
                                doCommand("createVarTest", c.getArgument("map", String.class), c.getArgument("variable", String.class), c.getSource());
                                return 1;} )))
                        .then(LiteralArgumentBuilder.<Player>literal("test").executes(c -> {
                            doCommand("createTest", c.getArgument("map", String.class), c.getSource());
                            return 1;} ))))
                .then(LiteralArgumentBuilder.<Player>literal("start").requires( c -> c.hasPermission(Permissions.RUN.getPermissionNode())).executes(c -> {
                    doCommand("startGame", c.getSource());
                    return 1;} ))
                .then(LiteralArgumentBuilder.<Player>literal("end").requires( c -> c.hasPermission(Permissions.RUN.getPermissionNode())).executes(c -> {
                    doCommand("endGame", c.getSource());
                    return 1;} ))
                .then(LiteralArgumentBuilder.<Player>literal("getgames").executes(c -> {
                    doCommand("getGames", c.getSource());
                    return 1;} )))
            .then(LiteralArgumentBuilder.<Player>literal("join").executes(c -> {
                doCommand("join", c.getSource());
                return 1;} ))
            .then(LiteralArgumentBuilder.<Player>literal("kick").requires( c -> c.hasPermission(Permissions.KICK.getPermissionNode()))
                .then(RequiredArgumentBuilder.<Player, String>argument("player", new com.mcmiddleearth.mcme.pvp.command.CommandPlayerArgument(PVPPlugin.getServer())).executes(c -> {
                    doCommand("kickPlayer", c.getArgument("player", String.class), c.getSource());
                    return 1;} )))
            .then(LiteralArgumentBuilder.<Player>literal("rules")
                    .then(RequiredArgumentBuilder.<Player, String>argument("gamemode", new CommandStringArgument("infected", "teamslayer", "teamdeathmatch", "ringbearer", "oneinthequiver", "teamconquest", "deathrun", "capturetheflag","egghunt")).executes(c -> {
                        doCommand("rules", c.getArgument("gamemode", String.class), c.getSource());
                        return 1;} )))
            .then(LiteralArgumentBuilder.<Player>literal("pipe").executes(c -> {
                doCommand("pipe", c.getSource());
                return 1;} ))
            .then(LiteralArgumentBuilder.<Player>literal("stats").executes(c -> {
                doCommand("stats", c.getSource());
                return 1;} )
                .then(LiteralArgumentBuilder.<Player>literal("clear").requires( c -> c.hasPermission(Permissions.PVP_ADMIN.getPermissionNode())).executes(c -> {
                    doCommand("statsClear", c.getSource());
                    return 1;} )).executes(c -> {
                        doCommand("stats", c.getSource());
                        return 1;}))
            .then(LiteralArgumentBuilder.<Player>literal("togglevoxel").requires( c -> c.hasPermission(Permissions.PVP_ADMIN.getPermissionNode()))
                .then(RequiredArgumentBuilder.<Player, String> argument("bool", new com.mcmiddleearth.mcme.pvp.command.CommandStringArgument("true", "false"))).executes(c -> {
                doCommand("toggleVoxel", c.getArgument("bool", String.class), c.getSource());
                return 1;} ))
            .then(LiteralArgumentBuilder.<Player>literal("lobby").executes(c -> {
                doCommand("lobby", c.getSource());
                return 1;} ))
            .then(LiteralArgumentBuilder.<Player>literal("broadcast").requires( c -> c.hasPermission(Permissions.RUN.getPermissionNode())).executes(c->{
                doCommand("broadcast", c.getSource());
                return 1;
            }))
        );

        register(LiteralArgumentBuilder.<Player>literal("locker").requires( c -> c.hasPermission(Permissions.RUN.getPermissionNode()))
            .then(LiteralArgumentBuilder.<Player>literal("lock").executes( c -> {
                doCommand("toggleLock", c.getSource());
                return 1; }))
            .then(LiteralArgumentBuilder.<Player>literal("kickall").executes( c -> {
                doCommand("kickall", c.getSource());
                return 1; }))
        );

        register(LiteralArgumentBuilder.<Player>literal("mapeditor").requires( c -> c.hasPermission(Permissions.PVP_ADMIN.getPermissionNode()))
            .then(RequiredArgumentBuilder.<Player, String>argument("map", new CommandMapArgument())
                .then(LiteralArgumentBuilder.<Player>literal("name")
                    .then(RequiredArgumentBuilder.<Player, String>argument("name", new CommandNewMapArgument()).executes(c -> {
                        doCommand("mapEditorName", c.getArgument("map", String.class), c.getArgument("name", String.class), c.getSource());
                        return 1;
                        })))
                .then(LiteralArgumentBuilder.<Player>literal("title")
                    .then(RequiredArgumentBuilder.<Player, String>argument("title", new CommandStringVariableArgument()).executes(c -> {
                            doCommand("mapEditorTitle", c.getArgument("map", String.class), c.getArgument("title", String.class), c.getSource());
                            return 1;
                        })))
                .then(LiteralArgumentBuilder.<Player>literal("gm")
                    .then(RequiredArgumentBuilder.<Player, String>argument("gm", new CommandStringArgument("FreeForAll", "Infected", "OneInTheQuiver", "Ringbearer", "TeamConquest", "TeamDeathmatch", "TeamSlayer", "DeathRun", "CaptureTheFlag","EggHunt")).executes(c -> {
                            doCommand("mapEditorGm", c.getArgument("map", String.class), c.getArgument("gm", String.class), c.getSource());
                            return 1;
                        })))
                .then(LiteralArgumentBuilder.<Player>literal("max")
                    .then(RequiredArgumentBuilder.<Player, String>argument("max", new CommandIntVariableArgument()).executes(c -> {
                            doCommand("mapEditorMax", c.getArgument("map", String.class), c.getArgument("max", String.class), c.getSource());
                            return 1;
                        })))
                .then(LiteralArgumentBuilder.<Player>literal("rp")
                    .then(RequiredArgumentBuilder.<Player, String>argument("rp", new CommandStringArgument("eriador", "rohan", "lothlorien", "gondor", "moria", "mordor")).executes(c -> {
                            doCommand("mapEditorRp", c.getArgument("map", String.class), c.getArgument("rp", String.class), c.getSource());
                            return 1;
                        })))
                 .then(LiteralArgumentBuilder.<Player>literal("setarea").executes( c -> {
                     doCommand("setArea", c.getArgument("map", String.class), c.getSource());
                     return 1;
                 }))
                .then(LiteralArgumentBuilder.<Player>literal("delete").executes( c -> {
                    doCommand("deleteMap", c.getArgument("map", String.class), c.getSource());
                    return 1;
                }))
                .then(LiteralArgumentBuilder.<Player>literal("spawn")
                    .then(RequiredArgumentBuilder.<Player, String>argument( "spawn", new CommandStringVariableArgument())
                        .then(LiteralArgumentBuilder.<Player>literal("delete").executes(c ->{
                            doCommand("deleteSpawn", c.getArgument("map", String.class),c.getArgument("spawn", String.class), c.getSource());
                            return 1;
                        }))
                        .then(LiteralArgumentBuilder.<Player>literal("create").executes( c -> {
                            doCommand("createSpawn", c.getArgument("map", String.class), c.getArgument("spawn", String.class), c.getSource());
                            return 1;
                        }))
                        .then(LiteralArgumentBuilder.<Player>literal("setloc").executes( c -> {
                            doCommand("setSpawnLoc", c.getArgument("map", String.class), c.getArgument("spawn", String.class), c.getSource());
                            return 1;
                        })))
                    .then(LiteralArgumentBuilder.<Player>literal("show").executes( c -> {
                        doCommand("spawnShow", c.getArgument("map", String.class), c.getSource());
                        return 1;
                    }))
                    .then(LiteralArgumentBuilder.<Player>literal("hide").executes( c -> {
                        doCommand("spawnHide", c.getArgument("map", String.class), c.getSource());
                        return 1;
                    }))
                )
                .then(LiteralArgumentBuilder.<Player>literal("listspawns").executes( c -> {
                    doCommand("listSpawns", c.getArgument("map", String.class), c.getSource());
                    return 1;
                }))
                    .then(LiteralArgumentBuilder.<Player>literal("fbt")
                            .then(RequiredArgumentBuilder.<Player, String>argument("fbt", new CommandStringArgument("true","false")).executes(c -> {
                                doCommand("fbt", c.getArgument("map", String.class), c.getArgument("fbt", String.class), c.getSource());
                                return 1;
                            })))
            )
        );
    }

    private void doCommand(String action, Player source) {
        switch (action) {
            case "fbt":
                if(runningGame!=null) {
                    Map m = PVPCommand.getRunningGame();
                    if(m != null) {
                        if (m.getFbt()) {
                            source.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 2));
                        }
                        else source.sendMessage("this map does not support full brightness");
                    }
                    else source.sendMessage("no map currently selected");
                }
                else source.sendMessage("no game currently running");
                break;
            case "mapList":
                for(String m: mapNames)
                    source.sendMessage(ChatColor.GREEN + maps.get(m).getName() + ChatColor.WHITE + " | " + ChatColor.BLUE + maps.get(m).getTitle());
                break;
            case "startGame":
                if(nextGame == null){
                    source.sendMessage(ChatColor.RED + "Can't start! No game is queued!");
                } else if(nextGame.getGm().getPlayers().size() == 0 ){
                    source.sendMessage(ChatColor.RED + "Can't start! No players have joined!");
                } else if(runningGame == null){
                    nextGame.getGm().Start(nextGame, parameter);
                    runningGame = nextGame;
                    nextGame = null;
                    Map m = getRunningGame();
                    if(m.getFbt()){
                        for(Player p : Bukkit.getOnlinePlayers()) {
                            Objects.requireNonNull(p).addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 2));
                        }
                    }
                    else for(Player p : Bukkit.getOnlinePlayers()){
                        p.removePotionEffect(PotionEffectType.NIGHT_VISION);
                    }
                }
                else{
                    source.sendMessage(ChatColor.RED + "Can't start! There's already a game running!");
                }
                MapEditor.HideSpawns(source);
                break;
            case "endGame":
                if(nextGame != null){
                    nextGame.getGm().getPlayers().clear();
                    nextGame = null;
                    for(Player pl : Bukkit.getOnlinePlayers()){
                        ChatHandler.getPlayerColors().put(pl.getName(), ChatColor.WHITE);
                        pl.setPlayerListName(ChatColor.WHITE + pl.getName());
                        pl.setDisplayName(ChatColor.WHITE + pl.getName());
                        BukkitTeamHandler.removeFromBukkitTeam(pl);
                        pl.sendMessage(ChatColor.GRAY + "The queued game was canceled! You'll need to rejoin when another game is queued.");
                    }
                    ChatHandler.getPlayerPrefixes().clear();
                    if(!gameQueue.isEmpty() && !parameterQueue.isEmpty()) {
                        nextGame = gameQueue.poll();
                        parameter = parameterQueue.poll();
                        source.sendMessage("Map: " + nextGame.getTitle() + ", Gamemode: " + nextGame.getGmType() + ", Parameter: "+ parameter + "\nIf you wish to announce the game type /pvp broadcast!");
                    }
                } else if(runningGame != null){

                    for(Player pl : Bukkit.getOnlinePlayers()){
                        pl.sendMessage(ChatColor.GRAY + runningGame.getGmType() + " on " + runningGame.getTitle() + " was ended by a staff!");
                    }
                    runningGame.getGm().End(runningGame);
                }
                else  {
                    source.sendMessage(ChatColor.GRAY + "There is no game to end!");
                }
                break;
            case "getGames":
                if(runningGame != null)
                    source.sendMessage(ChatColor.BLUE + "Now playing: " + runningGame.getGmType() + " on " + runningGame.getTitle());
                if(nextGame != null)
                    source.sendMessage(ChatColor.BLUE + "Next game: " + nextGame.getGmType() + " on " + nextGame.getTitle());
                if(!gameQueue.isEmpty())
                    source.sendMessage(ChatColor.BLUE + "Queued game: " + gameQueue.peek().getGmType() + " on " + gameQueue.peek().getTitle());
                break;
            case "join":
                Map m;

                if(nextGame != null){
                    m = nextGame;
                }
                else if(runningGame != null){
                    m = runningGame;
                }
                else{
                    source.sendMessage(ChatColor.RED + "There is no queued or running game!");
                    break;
                }

                if(!m.getGm().getPlayers().contains(source) && m.getGm().getState() != GameState.COUNTDOWN){
                    if(m.playerJoin(source)){

                        if(m.getGm().getState() == GameState.IDLE){
                            source.setPlayerListName(ChatColor.GREEN + source.getName());
                            source.setDisplayName(ChatColor.GREEN + source.getName());
                            ChatHandler.getPlayerColors().put(source.getName(), ChatColor.GREEN);
                            ChatHandler.getPlayerPrefixes().put(source.getName(), ChatColor.GREEN + "Participant");
                            BukkitTeamHandler.addToBukkitTeam(source, ChatColor.GREEN);
                        }
                    }
                    else{
                        source.sendMessage("Failed to Join Map");
                        break;
                    }
                }
                else if(m.getGm().getState() == GameState.COUNTDOWN){
                    source.sendMessage(ChatColor.RED + "Do " + ChatColor.GREEN + "/pvp join" + ChatColor.RED + " again once the countdown is done!");
                }
                else{
                    source.sendMessage("You are already part of a game");
                    break;
                }
                source.setGameMode(GameMode.CREATIVE);
                source.setGameMode(GameMode.ADVENTURE);
                break;
            case "pipe":
                GearHandler.giveCustomItem(source, PIPE);
                break;
            case "stats":
                PlayerStat ps = PlayerStat.getPlayerStats().get(source.getName());

                source.sendMessage(ChatColor.GREEN + "Showing stats for " + source.getDisplayName());
                source.sendMessage(ChatColor.GRAY + "Kills: " + ps.getKills());
                source.sendMessage(ChatColor.GRAY + "Deaths: " + ps.getDeaths());
                source.sendMessage(ChatColor.GRAY + "KD: " + ps.getKD());
                source.sendMessage(ChatColor.GRAY + "Games Played: " + ps.getGamesPlayed());
                source.sendMessage(ChatColor.GRAY + "    Won: " + ps.getGamesWon());
                source.sendMessage(ChatColor.GRAY + "    Lost: " + ps.getGamesLost());
                source.sendMessage(ChatColor.GRAY + "Games Spectated: " + ps.getGamesSpectated());
                break;
            case "statsClear":
                for(File f : new File(PVPPlugin.getStatDirectory() + PVPPlugin.getFileSep()).listFiles()){
                    f.delete();
                }

                for(PlayerStat pS : PlayerStat.getPlayerStats().values()) {
                    pS.setKills(0);
                    pS.setDeaths(0);
                    pS.setGamesLost(0);
                    pS.setGamesWon(0);
                    pS.setGamesSpectated(0);
                    pS.setGamesPlayed(0);
                    pS.getPlayersKilled().clear();
                }
                break;
            case "lobby":
                source.sendMessage(ChatColor.GREEN + "Sending Signs");
                for(Map map : Map.maps.values()){
                    ItemStack sign = new ItemStack(Material.OAK_WALL_SIGN);
                    ItemMeta im = sign.getItemMeta();
                    im.setDisplayName(map.getName());
                    String gamemode = "none";
                    if(map.getGm() != null){
                        gamemode = map.getGmType();
                    }
                    im.setLore(Arrays.asList(new String[] {map.getTitle(),  gamemode,  String.valueOf(map.getMax())}));
                    sign.setItemMeta(im);
                    source.getInventory().addItem(sign);
                }
                break;
            case "broadcast":
                if(nextGame != null)
                    sendBroadcast(source, nextGame);
                else
                    source.sendMessage("Can't send broadcast, next game is null");
                break;
            case "toggleLock":
                if(locked){
                    source.sendMessage("Server Unlocked!");
                    locked = false;
                }
                else{
                    source.sendMessage("Server Locked!");
                    locked = true;
                    Message = "Server Locked!";
                    for(Player p : Bukkit.getOnlinePlayers()){
                        if(!p.hasPermission(Permissions.JOIN.getPermissionNode())){
                            p.sendMessage(Message);
                            sendPlayerToMain(p);
                        }
                    }
                }
                break;
            case "kickall":
                for(Player p : Bukkit.getOnlinePlayers()){
                    if(!p.hasPermission(Permissions.JOIN.getPermissionNode())){
                        p.sendMessage("A PvP manager kicked all players");
                        sendPlayerToMain(p);
                    }
                }
                source.sendMessage("All players kicked!");
                break;
        }
    }
    private void doCommand(String action, String argument, Player source) {
        switch(action){
            case "createMap":
                MapEditor.MapCreator(argument, source);
                break;
            case "createTest":
                Map m = Map.maps.get(argument);
                if(m.getGm().requiresParameter().equals("none"))
                {
                    if(nextGame == null & runningGame == null) {
                        source.sendMessage("Map: " + m.getTitle() + ", Gamemode: " + m.getGmType());
                        parameter = 0;
                        nextGame = m;
                    }else{
                        source.sendMessage("Map: " + m.getTitle() + ", Gamemode: " + m.getGmType() + " is queued!");
                        gameQueue.add(m);
                        parameterQueue.add(0);
                    }
                }
                else{
                    source.sendMessage(m.getTitle() + " " + m.getGmType() + " requires a variable!");
                }
                break;
            case "toggleVoxel":
                toggleVoxel(argument);
                break;
            case "createGame":
                Map n = Map.maps.get(argument);
                if(n.getGm().requiresParameter().equals("none"))
                {
                    if(nextGame==null && runningGame==null) {
                        source.sendMessage("Map: " + n.getTitle() + ", Gamemode: " + n.getGmType());
                        sendBroadcast(source,n);
                        parameter = 0;
                        nextGame = n;
                    }else{
                        source.sendMessage("Map: " + n.getTitle() + ", Gamemode: " + n.getGmType() + " is queued!");
                        gameQueue.add(n);
                        parameterQueue.add(0);
                    }
                }
                else{
                    source.sendMessage(ChatColor.RED + n.getTitle() + " " + n.getGmType() + " requires a variable.");
                }

                break;
            case "kickPlayer":
                Player kick = Bukkit.getPlayer(argument);
                if(nextGame != null)
                    nextGame.playerLeave(kick);
                if(runningGame != null)
                    runningGame.playerLeave(kick);
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("ConnectOther");
                out.writeUTF(argument);
                out.writeUTF("world");
                source.sendPluginMessage(PVPPlugin.getPlugin(), "BungeeCord", out.toByteArray());
                source.sendMessage(ChatColor.GREEN+"Kicked "+argument+" from the PvP server!");
                break;
            case "rules":
                switch(argument) {
                    case "freeforall":
                        source.sendMessage(ChatColor.GREEN + "Free For All Rules");
                        source.sendMessage(ChatColor.GRAY + "Every man for himself, madly killing everyone! Highest number of kills wins.");
                        break;
                    case "infected":
                        source.sendMessage(ChatColor.GREEN + "Infected Rules");
                        source.sendMessage(ChatColor.GRAY + "Everyone starts as a Survivor, except one person, who is Infected. Infected gets a Speed effect, but has less armor");
                        source.sendMessage(ChatColor.GRAY + "If a Survivor is killed, they become Infected. Infected players have infinite respawns");
                        source.sendMessage(ChatColor.GRAY + "If all Survivors are infected, Infected team wins. If the time runs out with Survivors remaining, Survivors win.");
                        break;
                    case "oneinthequiver":
                        source.sendMessage(ChatColor.GREEN + "One in the Quiver Rules");
                        source.sendMessage(ChatColor.GRAY + "Everyone gets an axe, a bow, and one arrow, which kills in 1 shot if the bow is fully drawn.");
                        source.sendMessage(ChatColor.GRAY + "Every man is fighting for himself. If they get a kill or die, they get another arrow, up to a max of 5 arrows");
                        source.sendMessage(ChatColor.GRAY + "First to 21 kills wins.");
                        break;
                    case "ringbearer":
                        source.sendMessage(ChatColor.GREEN + "Ringbearer Rules");
                        source.sendMessage(ChatColor.GRAY + "Two teams, each with a ringbearer, who gets The One Ring (which of course gives invisibility)");
                        source.sendMessage(ChatColor.GRAY + "As long as the ringbearer is alive, the team can respawn.");
                        source.sendMessage(ChatColor.GRAY + "Once the ringbearer dies, that team cannot respawn. The first team to run out of members loses.");
                        break;
                    case "teamconquest":
                        source.sendMessage(ChatColor.GREEN + "Team Conquest Rules");
                        source.sendMessage(ChatColor.GRAY + "Two teams. There are 3 beacons, which each team can capture by repeatedly right clicking the beacon.");
                        source.sendMessage(ChatColor.GRAY + "Points are awarded on kills, based on the difference between each team's number of beacons.");
                        source.sendMessage(ChatColor.GRAY + "i.e. if Red has 3 beacons and Blue has 0, Red gets 3 point per kill. If Red has 1 and Blue has 2, Red doesn't get points for a kill.");
                        source.sendMessage(ChatColor.GRAY + "First team to a certain point total wins.");
                        break;
                    case "teamdeathmatch":
                        source.sendMessage(ChatColor.GREEN + "Team Deathmatch Rules");
                        source.sendMessage(ChatColor.GRAY + "Two teams, and no respawns. First team to run out of players loses.");
                        break;
                    case "teamslayer":
                        source.sendMessage(ChatColor.GREEN + "Team Slayer Rules");
                        source.sendMessage(ChatColor.GRAY + "Two teams, and infinite respawns. 1 point per kill. First team to a certain point total wins.");
                        break;
                    case "deathrun":
                        source.sendMessage(ChatColor.GREEN + "Death Run Rules");
                        source.sendMessage(ChatColor.GRAY + "One death, and lots of runners. Runners have to reach the end goal before the time limit or getting killed by death.");
                    case "capturetheflag":
                        source.sendMessage(ChatColor.GREEN + "Capture the Flag Rules");
                        source.sendMessage(ChatColor.GRAY + "Capture the enemy flag(banner), by right clicking it, and escort it to your base beacon while protecting your own. To capture the enemy flag, right click on it, it will be placed on your head, then with it on your head right click your spawn, and you score 1 point.");
                    case "egghunt":
                        source.sendMessage(ChatColor.GREEN + "Egg Hunt Rules");
                        source.sendMessage(ChatColor.GRAY + "Around the map, there are several eggs (wool blocks). Each one is worth a different amount of points. To 'take' the egg, right click on it, and a certain points will be added to your score. However, if you die, points will be reducted. Happy Hunting!");
                }
                break;
            case "deleteMap":
                Map.maps.remove(argument);
                File f = new File(PVPPlugin.getMapDirectory() + PVPPlugin.getFileSep() + argument);
                f.delete();
                reloadMaplist();
                source.sendMessage(ChatColor.RED + "Deleted " + argument);
                break;
            case "spawnShow":
                MapEditor.HideSpawns(source);
                MapEditor.ShowSpawns(argument, source);
                break;
            case "spawnHide":
                MapEditor.HideSpawns(source);
                break;
            case "listSpawns":
                MapEditor.sendSpawnMessage(argument, source);
                break;
            case "setArea":
                MapEditor.MapAreaSet(argument, source);
                break;
            case "teleport":
                Location spawn = Map.maps.get(argument).getSpawn().toBukkitLoc();
                source.teleport(spawn);
                source.sendMessage("You have been teleported to " + argument);
                break;
        }
    }
    private void doCommand(String action, String argument1, String argument2, Player source) {
        switch(action) {
            case "createVarTest":
                Map m = Map.maps.get(argument1);
                if(m.getGm().requiresParameter().equals("none"))
                {
                    doCommand("createTest", argument1, source);
                }
                else{
                    if(nextGame == null && runningGame == null) {
                        source.sendMessage("Map: " + m.getTitle() + ", Gamemode: " + m.getGmType() + ", Parameter: "+ argument2);
                        parameter = Integer.parseInt(argument2);
                        nextGame = m;
                    }else{
                        source.sendMessage("Map: " + m.getTitle() + ", Gamemode: " + m.getGmType() + ", Parameter: "+ argument2 + " is queued!");
                        gameQueue.add(m);
                        parameterQueue.add(Integer.parseInt(argument2));
                    }
                }
                break;
            case "createVarGame":
                Map n = Map.maps.get(argument1);
                if(n.getGm().requiresParameter().equals("none"))
                {
                    doCommand("createGame", argument1, source);
                }
                else{
                    if(nextGame == null && runningGame == null) {
                        source.sendMessage("Map: " + n.getTitle() + ", Gamemode: " + n.getGmType() + ", Parameter: "+ argument2);
                        sendBroadcast(source,n);
                        parameter = Integer.parseInt(argument2);
                        nextGame = n;
                    }else{
                        source.sendMessage("Map: " + n.getTitle() + ", Gamemode: " + n.getGmType() + ", Parameter: "+ argument2 + " is queued!");
                        gameQueue.add(n);
                        parameterQueue.add(Integer.parseInt(argument2));
                    }
                }
                break;
            case "mapEditorName":
                MapEditor.MapNameEdit(argument1, argument2, source);
                break;
            case "mapEditorTitle":
                MapEditor.MapTitleEdit(argument1, argument2, source);
                break;
            case "mapEditorGm":
                MapEditor.MapGamemodeSet(argument1, argument2, source);
                break;
            case "mapEditorMax":
                MapEditor.MapMaxSet(argument1, argument2, source);
                break;
            case "mapEditorRp":
                MapEditor.MapRPSet(argument1,argument2,source);
                break;
            case "deleteSpawn":
                MapEditor.PointDelete(argument1, argument2, source);
                break;
            case "createSpawn":
                MapEditor.PointCreate(argument1, argument2, source);
                break;
            case "setSpawnLoc":
                MapEditor.PointLocEdit(argument1, argument2, source);
                break;
            case "fbt":
                MapEditor.BrightnessSet(argument1,argument2,source);
                break;
        }
    }

    private void sendPlayerToMain(Player player) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ConnectOther");
        out.writeUTF(player.getName());
        out.writeUTF("world");
        player.sendPluginMessage(PVPPlugin.getPlugin(), "BungeeCord", out.toByteArray());
    }
    private void sendBroadcast(Player player, Map m) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Message");
        out.writeUTF("ALL");
        out.writeUTF(ChatColor.GRAY + player.getName() + " has started a game\n"
                +ChatColor.GRAY + "Map: " + ChatColor.GREEN + m.getTitle() + ChatColor.GRAY + ", Gamemode: " + ChatColor.GREEN + m.getGmType()+"\n"
                +ChatColor.GRAY + "Use " + ChatColor.GREEN + "/pvp join" + ChatColor.GRAY + " to join the game\n"
                +ChatColor.GRAY + "There are only " + m.getMax() + " slots left\n"
                +ChatColor.GREEN + "Do /pvp rules " + removeSpaces(m.getGmType()) + " if you don't know how this gamemode works!");
        player.sendPluginMessage(PVPPlugin.getPlugin(), "BungeeCord", out.toByteArray());
    }

    public static String removeSpaces(String s){
        String newString = "";

        char[] chars = s.toCharArray();

        for(char c : chars){
            if(c != ' '){ newString += String.valueOf(c); }
        }
        return newString;
    }
    public static void queueNextGame(){
        if(!gameQueue.isEmpty() && !parameterQueue.isEmpty()) {
        nextGame = gameQueue.poll();
        parameter = parameterQueue.poll();
        for(Player p : Bukkit.getOnlinePlayers())
            if(p.hasPermission(Permissions.RUN.getPermissionNode()))
                p.sendMessage("Map: " + nextGame.getTitle() + ", Gamemode: " + nextGame.getGmType() + ", Parameter: "+ parameter +"\nIf you wish to announce the game type /pvp broadcast!");
        }
    }
    public static void toggleVoxel(String argument){

        try{
            if(Bukkit.getPluginManager().getPlugin("VoxelSniper").isEnabled()){
                Bukkit.getPluginManager().disablePlugin(Bukkit.getPluginManager().getPlugin("VoxelSniper"));
            }
            else if(argument.equals("false")){
                Bukkit.getPluginManager().enablePlugin(Bukkit.getPluginManager().getPlugin("VoxelSniper"));
            }
        }
        catch(NullPointerException e){
            System.err.println("VoxelSniper isn't loaded! Ignoring!");
        }
    }
    public static void reloadMaplist(){
        maps = Map.maps;
        mapNames = new HashSet<>(Lists.newArrayList());
        for (String i : maps.keySet()) {
            mapNames.add(i);
        }
        CommandNewMapArgument.UpdateOptions();
        CommandMapArgument.UpdateOptions();
    }

}