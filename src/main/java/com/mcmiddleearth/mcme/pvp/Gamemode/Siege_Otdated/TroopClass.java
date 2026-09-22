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
package com.mcmiddleearth.mcme.pvp.Gamemode.Siege_Otdated;

import java.util.ArrayList;
import org.bukkit.inventory.Inventory;

/**
 *
 * @author Eric
 */
public class TroopClass {
    
    public static enum Class {
        ARCHER, INFANTRY, CAVALRY
    }
    
    private ArrayList<String> members = new ArrayList<>();
    
    private Inventory gearInv;
    
    public TroopClass(Class c){
        switch (c){
            case ARCHER:
                
                break;
            case INFANTRY:
                break;
            case CAVALRY:
                break;
        }
    }
    
    public ArrayList<String> get(){
        ArrayList<String> tempMembers = new ArrayList<>();
        for(String s : members){
            tempMembers.add(s);
        }
        return tempMembers;
    }
    
    public void addMember(String playerName){
        members.add(playerName);
    }
    
    public void removeMember(String playerName){
        members.remove(playerName);
    }
    
    public void clear(){
        members.clear();
    }
}
