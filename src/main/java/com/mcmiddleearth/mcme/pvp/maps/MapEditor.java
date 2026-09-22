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

import com.mcmiddleearth.mcme.pvp.PVPPlugin;
import com.mcmiddleearth.mcme.pvp.Util.DBmanager;
import com.mcmiddleearth.mcme.pvp.Util.EventLocation;
import com.mcmiddleearth.mcme.pvp.command.PVPCommand;
import com.mcmiddleearth.pluginutil.message.FancyMessage;
import com.mcmiddleearth.pluginutil.message.MessageType;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Donovan <dallen@dallen.xyz> NicoviçTheSixth
 */
public class MapEditor{
    public static void MapCreator(String map, Player p){
        p.sendMessage(ChatColor.GREEN + "Creating new map");
        Map.maps.put(map, new Map(p.getLocation(), map));
        System.out.println(map);
        Map m = Map.maps.get(map);
        p.sendMessage(map + " spawn is: " + m.getSpawn().getX() + " " + m.getSpawn().getY() + " " + m.getSpawn().getZ());
        sendMapMessage(map, m, p);
        PVPCommand.reloadMaplist();
        saveMap(map);
    }

    public static void MapNameEdit(String map, String name, Player p){
        Map m = Map.maps.get(map);
        m.setName(name);
        Map.maps.put(name, m);
        Map.maps.remove(map);
        File f = new File(PVPPlugin.getMapDirectory() + PVPPlugin.getFileSep() + "maps" + PVPPlugin.getFileSep() + map);
        f.delete();
        sendMapMessage(name, m, p);
        PVPCommand.reloadMaplist();
        saveMap(map);
    }

    public static void MapTitleEdit(String map, String title, Player p){
        Map m = com.mcmiddleearth.mcme.pvp.maps.Map.maps.get(map);
        m.setTitle(title);
        sendMapMessage(map, m, p);
        saveMap(map);
    }

    public static void MapGamemodeSet(String map, String gamemode, Player p){
        Map m = Map.maps.get(map);
        m.setGmType(gamemode);
        m.bindGamemode();
        sendMapMessage(map, m, p);
        saveMap(map);
    }

    public static void MapMaxSet(String map, String amount, Player p){
        Map m = com.mcmiddleearth.mcme.pvp.maps.Map.maps.get(map);
        m.setMax(Integer.parseInt(amount));
        sendMapMessage(map, m, p);
        saveMap(map);
    }

    public static void MapRPSet(String map, String rp, Player p){
        Map m = Map.maps.get(map);
        switch(rp) {
            case "eriador":
                m.setResourcePackURL("http://www.mcmiddleearth.com/content/Eriador.zip");
                break;
            case "rohan":
                m.setResourcePackURL("http://www.mcmiddleearth.com/content/Rohan.zip");
                break;
            case "lothlorien":
                m.setResourcePackURL("http://www.mcmiddleearth.com/content/Lothlorien.zip");
                break;
            case "gondor":
                m.setResourcePackURL("http://www.mcmiddleearth.com/content/Gondor.zip");
                break;
            case "moria":
                m.setResourcePackURL("http://www.mcmiddleearth.com/content/Moria.zip");
                break;
            case "mordor":
                m.setResourcePackURL("http://www.mcmiddleearth.com/content/Mordor.zip");
                break;
        }
        sendMapMessage(map, m, p);
        saveMap(map);
    }
    public static void MapAreaSet(String map, Player p){
        Map m = Map.maps.get(map);
        BukkitPlayer bukkitP = new BukkitPlayer(p);//PVPCore.getWorldEditPlugin(), PVPCore.getWorldEditPlugin().getServerInterface(), p);
        LocalSession session = PVPPlugin.getWorldEditPlugin().getWorldEdit().getSessionManager().get(bukkitP);

        try{
            Region r = session.getSelection(new BukkitWorld(p.getWorld()));
            if(r.getHeight() < 250){
                p.sendMessage(ChatColor.RED + "I think you forgot to do //expand vert!");
            }
            else{
                List<BlockVector2> wePoints = r.polygonize(1000);
                ArrayList<EventLocation> bPoints = new ArrayList<>();

                for(BlockVector2 point : wePoints){
                    bPoints.add(new EventLocation(new Location(p.getWorld(), point.getX(), 1, point.getZ())));
                }

                m.setRegionPoints(bPoints);
                m.initializeRegion();
                p.sendMessage(ChatColor.YELLOW + "Area set!");
            }
        }
        catch(IncompleteRegionException e){
            p.sendMessage(ChatColor.RED + "You don't have a region selected!");
        }
        saveMap(map);
    }

    public static void PointDelete(String map, String point, Player p){
        Map.maps.get(map).getImportantPoints().remove(point);
        sendSpawnMessage(map, p);
        saveMap(map);
    }

    public static void PointCreate(String map, String point, Player p){
        Map.maps.get(map).getImportantPoints().put(point, new EventLocation(p.getLocation().add(0, -1, 0)));
        sendSpawnMessage(map, p);
        saveMap(map);
    }

    public static void PointLocEdit(String map, String point, Player p){
        Map.maps.get(map).getImportantPoints().replace(point, new EventLocation(p.getLocation().add(0, -1, 0)));
        sendSpawnMessage(map, p);
        saveMap(map);
    }

    private static void saveMap(String map){
        Map.maps.get(map).setCurr(0);
        DBmanager.saveObj(Map.maps.get(map),new File(PVPPlugin.getPluginDirectory()+PVPPlugin.getFileSep()+"maps"),map);
    }

    public static void ShowSpawns(String map, Player p){
        Map m = Map.maps.get(map);
        EventLocation loc;
        for(String name : m.getImportantPoints().keySet()){
            loc = m.getImportantPoints().get(name);
            ArmorStand marker = (ArmorStand) loc.toBukkitLoc().getWorld().spawnEntity(loc.toBukkitLoc().add(0, 1, 0), EntityType.ARMOR_STAND);
            marker.setGravity(false);
            marker.setCustomName(name);
            marker.setCustomNameVisible(true);
            marker.setGlowing(true);
            marker.setMarker(true);
        }
    }

    public static void HideSpawns(Player p){
        ArmorStand toDelete;
        for(Entity marker : PVPPlugin.getLobby().getWorld().getEntities())
            if(marker.getType() == EntityType.ARMOR_STAND){
                toDelete = (ArmorStand) marker;
                if(toDelete.isMarker())
                    toDelete.remove();
            }
    }

    public static void sendMapMessage (String map, Map m, Player p){
        FancyMessage message = new FancyMessage(MessageType.INFO, PVPPlugin.getMessageUtil());
        message.addFancy("Map name: " + m.getName() + "\n",
                "/mapeditor "+(map)+" name "+ map,
                "Click to edit. Don't change the leading '/mapEditor <map> name'.");
        message.addFancy("Map title: " + m.getTitle() + "\n",
                "/mapeditor "+(map)+" title "+ m.getTitle(),
                "Click to edit. Don't change the leading '/mapEditor <map> title'.");
        message.addFancy("Map gamemode: " + m.getGmType() + "\n",
                "/mapeditor "+(map)+" gm "+ m.getGmType(),
                "Click to edit. Don't change the leading '/mapEditor <map> gm'.");
        message.addFancy("Map max players: " + m.getMax() + "\n",
                "/mapeditor "+(map)+" max "+ m.getMax(),
                "Click to edit. Don't change the leading '/mapEditor <map> max'.");
        message.addFancy("Map rp: " + m.getResourcePackURL() + "\n",
                "/mapeditor "+(map)+" rp ",
                "Click to edit. Don't change the leading '/mapEditor <map> rp'.");
        message.addFancy("Map area set: " + !m.getRegionPoints().isEmpty() + "\n",
                "/mapeditor "+(map)+" setarea",
                "Click to edit. Don't change the command.");
        message.addFancy("Map spawns: " + m.getImportantPoints().size() + "\n",
                "/mapeditor "+(map)+" listspawns",
                "Click to view spawns. Don't change the leading '/mapEditor <map> listspawns'.\n");
        if(m.getGm() != null)
            message.addSimple("Map has all spawns: " + m.getImportantPoints().keySet().containsAll(m.getGm().getNeededPoints()) + "\n");
        else
            message.addSimple("Map has all spawns: false \n");
        message.send(p);
    }

    public static void sendSpawnMessage(String map, Player p){
        FancyMessage message = new FancyMessage(MessageType.INFO, PVPPlugin.getMessageUtil());
        Map m = Map.maps.get(map);
        String coordinate;
        message.addSimple("Map " + (map) + " on " + m.getTitle() + "\n");
        if(m.getGmType() == null)
            message.addFancy("Map has no gamemode. \n",
                    "/mapeditor "+(map)+" gm "+ m.getGmType(),
                    "Click to edit. Don't change the leading '/mapEditor <map> gm'.");
        else {
            HashMap<String, EventLocation> spawns = m.getImportantPoints();
            if (spawns.size() != 0) {
                for (String i : spawns.keySet()) {
                    message.addFancy(i + " " + spawns.get(i).getX() + " " + spawns.get(i).getY() + " " + spawns.get(i).getZ() + "\n",
                            "/mapeditor " + (map) + " spawn " + (i) + " ",
                            "Click to edit. Don't change the leading '/mapeditor <map> spawn <spawn>'.");
                }
                if (m.getGm().getNeededPoints().contains("PlayerSpawn"))
                    message.addFancy("Add spawn. \n",
                            "/mapeditor " + (map) + " spawn PlayerSpawn" + spawns.size() + " create",
                            "Click to create a spawn, don't change the command, just press enter.");
                for (String i : m.getGm().getNeededPoints())
                    if (!spawns.containsKey(i))
                        message.addFancy("Missing spawns, click to add " + i + ".\n",
                                "/mapeditor " + (map) + " spawn " + i + " create",
                                "Click to create a spawn, don't change the command, just press enter.");
            } else {
                if (m.getGm().getNeededPoints().contains("PlayerSpawn")) {
                    message.addFancy("Contains no spawns, click to add spawn.\n",
                            "/mapeditor " + (map) + " spawn PlayerSpawn create",
                            "Click to create a spawn, don't change the command, just press enter.");
                } else {
                    for (String i : m.getGm().getNeededPoints())
                        message.addFancy("Contains no spawns, click to add " + i + ".\n",
                                "/mapeditor " + (map) + " spawn " + i + " create",
                                "Click to create a spawn, don't change the command, just press enter.");
                }
            }
        }
        message.send(p);
    }
}