package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.mode.loadout.Kit;
import com.mcmiddleearth.pvp.mode.loadout.KitItem;
import com.mcmiddleearth.pvp.team.TeamSpec;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;

/** Turns a {@link Kit} into items in a player's inventory. */
public final class KitItems {

    private KitItems() {
    }

    /** Clears the inventory (armor and offhand included) and puts every item of the kit into its slot. */
    public static void give(Player player, Kit kit, TeamSpec color) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        for (KitItem item : kit.items()) {
            put(inventory, item.slot(), item(item, color));
        }
    }

    /**
     * One kit item: its material and amount, unbreakable when the kit says so, and leather dyed with
     * {@code color}'s armor color when the item is dyed and a color is given (null: undyed).
     */
    public static ItemStack item(KitItem item, TeamSpec color) {
        ItemStack stack = new ItemStack(Material.valueOf(item.material()), item.amount());
        stack.editMeta(meta -> {
            if (item.unbreakable()) {
                meta.setUnbreakable(true);
            }
            if (item.dyed() && color != null && meta instanceof LeatherArmorMeta leather) {
                leather.setColor(Color.fromRGB(color.armorRgb()));
            }
        });
        return stack;
    }

    private static void put(PlayerInventory inventory, String slot, ItemStack stack) {
        switch (slot) {
            case "head" -> inventory.setHelmet(stack);
            case "chest" -> inventory.setChestplate(stack);
            case "legs" -> inventory.setLeggings(stack);
            case "feet" -> inventory.setBoots(stack);
            case "offhand" -> inventory.setItemInOffHand(stack);
            default -> inventory.setItem(Integer.parseInt(slot), stack);
        }
    }
}
