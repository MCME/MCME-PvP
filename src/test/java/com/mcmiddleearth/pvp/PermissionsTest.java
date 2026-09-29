package com.mcmiddleearth.pvp;

import org.bukkit.permissions.Permission;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * plugin.yml declares every node the code checks, and 1.1.2's nodes still grant the new ones, so LuckPerms
 * groups keep working. MockBukkit applies permission children on attachments (probe P13), so the grants are
 * tested the way a server resolves them.
 */
class PermissionsTest {

    private ServerMock server;
    private PvpPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        plugin = MockBukkit.load(PvpPlugin.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void everyNodeCheckedInCodeIsDeclaredInPluginYml() {
        Set<String> declared = plugin.getPluginMeta().getPermissions().stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());

        List<String> nodes = nodes();
        assertFalse(nodes.isEmpty());
        for (String node : nodes) {
            assertTrue(declared.contains(node), node + " is not declared in plugin.yml");
        }
    }

    @ParameterizedTest(name = "{0} lists {1}")
    @CsvSource({
            "pvp.join, pvp.join.locked",
            "pvp.gameKick, pvp.staff.kick",
            "pvp.gameCanRun, pvp.staff.run",
            "pvp.mapCreate, pvp.staff.arena",
            "pvp.adminPermission, pvp.admin"
    })
    void eachLegacyNodeListsItsNewNodeAsAChild(String legacyNode, String newNode) {
        Permission permission = server.getPluginManager().getPermission(legacyNode);

        assertNotNull(permission, legacyNode + " is not declared in plugin.yml");
        assertEquals(Boolean.TRUE, permission.getChildren().get(newNode));
    }

    @ParameterizedTest(name = "{0} grants {1}")
    @CsvSource({
            "pvp.join, pvp.join.locked",
            "pvp.gameKick, pvp.staff.kick",
            "pvp.gameCanRun, pvp.staff.run",
            "pvp.mapCreate, pvp.staff.arena",
            "pvp.adminPermission, pvp.admin",
            "pvp.adminPermission, pvp.staff.run",
            "pvp.builder, pvp.join.locked",
            "pvp.moderator, pvp.staff.kick",
            "pvp.manager, pvp.staff.run",
            "pvp.manager, pvp.join.locked",
            "pvp.manager, pvp.staff.kick"
    })
    void aLegacyGrantStillGivesTheNewNode(String legacyNode, String newNode) {
        PlayerMock player = server.addPlayer();
        assertFalse(player.hasPermission(newNode), "a plain player must not have " + newNode);

        player.addAttachment(plugin, legacyNode, true);

        assertTrue(player.hasPermission(newNode));
    }

    @Test
    void adminGrantsEveryNode() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, Permissions.ADMIN, true);

        for (String node : nodes()) {
            assertTrue(player.hasPermission(node), "pvp.admin does not grant " + node);
        }
    }

    @Test
    void everyoneMayPlayButOnlyStaffRunGames() {
        PlayerMock player = server.addPlayer();

        assertTrue(player.hasPermission(Permissions.PLAY));
        assertFalse(player.hasPermission(Permissions.JOIN_LOCKED));
        assertFalse(player.hasPermission(Permissions.STAFF_RUN));
        assertFalse(player.hasPermission(Permissions.STAFF_KICK));
        assertFalse(player.hasPermission(Permissions.STAFF_ARENA));
        assertFalse(player.hasPermission(Permissions.ADMIN));
    }

    @Test
    void operatorsHaveEveryNode() {
        PlayerMock player = server.addPlayer();
        player.setOp(true);

        for (String node : nodes()) {
            assertTrue(player.hasPermission(node), "an operator does not have " + node);
        }
    }

    @Test
    void adminDoesNotListTheLegacyAdminNode() {
        // pvp.adminPermission lists pvp.admin; the reverse would be a cycle, which Bukkit does not detect.
        Permission admin = server.getPluginManager().getPermission(Permissions.ADMIN);

        assertNotNull(admin);
        assertFalse(admin.getChildren().containsKey("pvp.adminPermission"));
    }

    /** The values of every public static String constant of {@link Permissions}. */
    private static List<String> nodes() {
        List<String> nodes = new ArrayList<>();
        for (Field field : Permissions.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class) {
                try {
                    nodes.add((String) field.get(null));
                } catch (IllegalAccessException e) {
                    throw new AssertionError(e);
                }
            }
        }
        return nodes;
    }
}
