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
package com.mcmiddleearth.mcme.pvp.Util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcmiddleearth.mcme.pvp.PVPPlugin;
import lombok.Getter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 *
 * @author Donovan <dallen@dallen.xyz>
 */
public class DBmanager {
    
    @Getter private static ObjectMapper JSonParser = new ObjectMapper();

    public static boolean saveObj(Object obj, File loc, String name){
        if(!loc.exists()){
            loc.mkdirs();
        }
        if(!(new File(loc + PVPPlugin.getFileSep()+ "backup").exists())){
            new File(loc + PVPPlugin.getFileSep()+ "backup").mkdirs();
        }
        boolean success = true;
        File locStart = new File(loc + PVPPlugin.getFileSep() + name + ".new");
        File locBackup = new File(loc + PVPPlugin.getFileSep()+ "backup" + PVPPlugin.getFileSep() + name);
        File locEnd = new File(loc + PVPPlugin.getFileSep() + name);
        try {
            JSonParser.writeValue(locStart, obj);
        } catch (IOException ex) {
            success = false;
        } finally {
            if (success) {
                if (locEnd.exists()) {
                    if (locBackup.exists()) {
                        locBackup.delete();
                    }
                    locEnd.renameTo(locBackup);
                }
                locStart.renameTo(locEnd);
            }
        }
        return success;
    }
    
    public static Object loadObj(Class type, File loc){
        if(!loc.exists()){
            loc.mkdirs();
            return false;
        }
        Object obj;
        try {
            obj = JSonParser.readValue(loc, type);
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
        return obj;
    }
    
    public static Object loadObj(Class type, String path){
        File loc = new File(PVPPlugin.getPluginDirectory() + PVPPlugin.getFileSep() + path);
        if(!loc.exists()){
            loc.mkdirs();
            return false;
        }
        Object obj;
        try {
            obj = JSonParser.readValue(loc, type);
        } catch (Exception ex) {
//            ex.printStackTrace();
            return false;
        }
        return obj;
    }
    
    public static HashMap<String, Object> loadAllObj(Class Type, File loc){
        if(!loc.exists()){
            loc.mkdirs();
            return null;
        }
        HashMap<String, Object> rtn = new HashMap<String, Object>();
        for(File f : loc.listFiles()){
            rtn.put(f.getName(), loadObj(Type, f));
        }
        return rtn;
    }
}
