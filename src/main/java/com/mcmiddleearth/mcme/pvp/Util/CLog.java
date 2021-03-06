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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mcmiddleearth.mcme.pvp.PVPPlugin;
import com.mcmiddleearth.mcme.pvp.maps.MapEditor;
import java.util.logging.Level;

/**
 *
 * @author Donovan <dallen@dallen.xyz>
 */
public class CLog {
    public static void println(Object obj){
        if(PVPPlugin.isDebug()){
            if(obj instanceof String){
                System.out.println(obj);
            }else{
                try {
                    System.out.println(com.mcmiddleearth.mcme.pvp.Util.DBmanager.getJSonParser().writeValueAsString(obj));
                } catch (JsonProcessingException ex) {
                    java.util.logging.Logger.getLogger(MapEditor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
