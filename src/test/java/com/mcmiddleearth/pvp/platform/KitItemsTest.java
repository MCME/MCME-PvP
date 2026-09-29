package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.mode.loadout.Kit;
import com.mcmiddleearth.pvp.mode.loadout.KitItem;
import com.mcmiddleearth.pvp.mode.loadout.Kits;
import com.mcmiddleearth.pvp.team.Palette;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitItemsTest {

    private ServerMock server;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        player = server.addPlayer("Alice");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void theStandardKitReplacesTheInventory() {
        player.getInventory().setItem(5, new ItemStack(Material.DIRT));

        KitItems.give(player, Kits.STANDARD, Palette.RED);

        PlayerInventory inventory = player.getInventory();
        assertEquals(Material.LEATHER_HELMET, inventory.getHelmet().getType());
        assertEquals(Material.LEATHER_CHESTPLATE, inventory.getChestplate().getType());
        assertEquals(Material.LEATHER_LEGGINGS, inventory.getLeggings().getType());
        assertEquals(Material.LEATHER_BOOTS, inventory.getBoots().getType());
        assertEquals(Material.IRON_SWORD, inventory.getItem(0).getType());
        assertEquals(Material.BOW, inventory.getItem(1).getType());
        assertEquals(Material.ARROW, inventory.getItem(8).getType());
        assertEquals(24, inventory.getItem(8).getAmount());
        assertNull(inventory.getItem(5));
    }

    @Test
    void armorTakesTheTeamColorAndOnlyTheArrowsCanBreak() {
        KitItems.give(player, Kits.STANDARD, Palette.BLUE);

        PlayerInventory inventory = player.getInventory();
        for (ItemStack armor : List.of(inventory.getHelmet(), inventory.getChestplate(), inventory.getLeggings(),
                inventory.getBoots())) {
            LeatherArmorMeta meta = (LeatherArmorMeta) armor.getItemMeta();
            assertEquals(Color.fromRGB(0x3C44AA), meta.getColor());
            assertTrue(meta.isUnbreakable());
        }
        assertTrue(inventory.getItem(0).getItemMeta().isUnbreakable());
        assertTrue(inventory.getItem(1).getItemMeta().isUnbreakable());
        assertFalse(inventory.getItem(8).getItemMeta().isUnbreakable());
    }

    @Test
    void aPersonalColorDyesTheArmor() {
        ItemStack helmet = KitItems.item(new KitItem("head", "LEATHER_HELMET", 1, true, true), Palette.PERSONAL.get(0));

        assertEquals(Color.fromRGB(0x3AB3DA), ((LeatherArmorMeta) helmet.getItemMeta()).getColor());
    }

    @Test
    void withoutAColorLeatherKeepsItsOwnColor() {
        ItemStack helmet = KitItems.item(new KitItem("head", "LEATHER_HELMET", 1, true, true), null);

        assertEquals(server.getItemFactory().getDefaultLeatherColor(), ((LeatherArmorMeta) helmet.getItemMeta()).getColor());
    }

    @Test
    void slotsAndAmountsComeFromTheKit() {
        Kit kit = new Kit("test", List.of(
                new KitItem("offhand", "SHIELD", 1, false, false),
                new KitItem("3", "ARROW", 5, false, false)));

        KitItems.give(player, kit, null);

        assertEquals(Material.SHIELD, player.getInventory().getItemInOffHand().getType());
        assertEquals(5, player.getInventory().getItem(3).getAmount());
    }
}
