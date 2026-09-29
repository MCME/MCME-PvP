package com.mcmiddleearth.pvp.integration;

import com.mcmiddleearth.pvp.Permissions;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.team.TeamSpec;
import com.mcmiddleearth.pvp.testing.PaperFixture;
import com.mcmiddleearth.pvp.testing.TestArenas;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** %mcmepvp_prefix% and %mcmepvp_color% give exactly 1.1.2's texts. */
class PvpExpansionTest {

    @TempDir
    Path dir;

    private ServerMock server;
    private PaperFixture fixture;
    private PvpExpansion expansion;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        fixture = PaperFixture.create(server, dir, TestArenas.tdm("AS-TDM", 0, 2), TestArenas.ffa("AS-FFA", 100, 4),
                TestArenas.tdm("AS-TDM3", 200, 3));
        expansion = new PvpExpansion(fixture.services().games(), "3.0.0-TEST");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void keepsThe112Identity() {
        assertEquals("mcmepvp", expansion.getIdentifier());
        assertEquals("Eriol_Eandur", expansion.getAuthor());
        assertEquals("3.0.0-TEST", expansion.getVersion());
        assertTrue(expansion.persist());
    }

    @Test
    void aPlayerWaitingForTheCountdownIsAParticipant() {
        PlayerMock alice = fixture.player("Alice");
        fixture.join(fixture.open("AS-TDM"), alice);

        assertEquals("§aParticipant Alice§r: ", expansion.onRequest(alice, "prefix"));
        assertEquals("§a", expansion.onRequest(alice, "color"));
    }

    @Test
    void teamMembersShowTheirTeam() {
        List<PlayerMock> players = List.of(fixture.player("Alice"), fixture.player("Bob"), fixture.player("Carol"));
        Game game = fixture.services().games().open("AS-TDM3", Map.of("teamCount", 3), "Tester");
        fixture.join(game, players.toArray(PlayerMock[]::new));
        fixture.startAndRun(game);

        for (PlayerMock player : players) {
            TeamSpec team = game.session(player.getUniqueId()).orElseThrow().color().orElseThrow();
            String code = LegacyColors.code(team.color());
            assertEquals(code + team.name() + " " + player.getName() + "§r: ", expansion.onRequest(player, "prefix"));
            assertEquals(code, expansion.onRequest(player, "color"));
        }
        List<String> prefixes = players.stream().map(p -> expansion.onRequest(p, "prefix")).sorted().toList();
        assertTrue(prefixes.stream().anyMatch(p -> p.startsWith("§cRed ")), prefixes.toString());
        assertTrue(prefixes.stream().anyMatch(p -> p.startsWith("§9Blue ")), prefixes.toString());
        assertTrue(prefixes.stream().anyMatch(p -> p.startsWith("§aGreen ")), prefixes.toString());
    }

    @Test
    void aStaffMemberInAGameShowsTheirTeamNotStaff() {
        PlayerMock dave = fixture.player("Dave");
        dave.addAttachment(fixture.plugin(), Permissions.ADMIN, true);
        List<PlayerMock> players = List.of(dave, fixture.player("Alice"), fixture.player("Bob"));
        Game game = fixture.services().games().open("AS-TDM3", Map.of("teamCount", 3), "Tester");
        fixture.join(game, players.toArray(PlayerMock[]::new));
        fixture.startAndRun(game);

        TeamSpec team = game.session(dave.getUniqueId()).orElseThrow().color().orElseThrow();
        String code = LegacyColors.code(team.color());
        assertEquals(code + team.name() + " Dave§r: ", expansion.onRequest(dave, "prefix"));
        assertEquals(code, expansion.onRequest(dave, "color"));
    }

    @Test
    void freeForAllPlayersShowTheirPersonalColor() {
        PlayerMock alice = fixture.player("Alice");
        Game game = fixture.open("AS-FFA");
        fixture.join(game, alice, fixture.player("Bob"));
        fixture.startAndRun(game);

        String code = LegacyColors.code(game.session(alice.getUniqueId()).orElseThrow().color().orElseThrow().color());
        assertEquals(code + "Player Alice§r: ", expansion.onRequest(alice, "prefix"));
        assertEquals(code, expansion.onRequest(alice, "color"));
    }

    @Test
    void spectatorsAndTheEliminatedAreSpectators() {
        PlayerMock carol = fixture.player("Carol");
        List<PlayerMock> players = List.of(fixture.player("Alice"), fixture.player("Bob"), fixture.player("Dave"));
        Game game = fixture.open("AS-TDM");
        fixture.join(game, players.toArray(PlayerMock[]::new));
        fixture.startAndRun(game);
        fixture.services().games().spectate(PaperFixture.ref(carol), game);
        PlayerMock loner = players.stream().filter(p -> teamSize(game, p) == 1).findFirst().orElseThrow();
        PlayerMock victim = players.stream().filter(p -> teamSize(game, p) == 2).findFirst().orElseThrow();
        game.handleDeath(victim.getUniqueId(), loner.getUniqueId());

        assertEquals("§7Spectator Carol§r: ", expansion.onRequest(carol, "prefix"));
        assertEquals("§7", expansion.onRequest(carol, "color"));
        assertEquals("§7Spectator " + victim.getName() + "§r: ", expansion.onRequest(victim, "prefix"));
        assertEquals("§7", expansion.onRequest(victim, "color"));
    }

    @Test
    void outsideGamesStaffAndLobbyPlayersDiffer() {
        PlayerMock admin = fixture.player("Dave");
        admin.addAttachment(fixture.plugin(), Permissions.ADMIN, true);
        PlayerMock manager = fixture.player("Erin");
        manager.addAttachment(fixture.plugin(), Permissions.STAFF_RUN, true);
        PlayerMock frank = fixture.player("Frank");

        assertEquals("§6PvP Staff Dave§r: ", expansion.onRequest(admin, "prefix"));
        assertEquals("§6", expansion.onRequest(admin, "color"));
        assertEquals("§6Manager Erin§r: ", expansion.onRequest(manager, "prefix"));
        assertEquals("§7", expansion.onRequest(manager, "color"));
        assertEquals("§7Lobby Frank§r: ", expansion.onRequest(frank, "prefix"));
        assertEquals("§7", expansion.onRequest(frank, "color"));
    }

    @Test
    void nullAndOfflinePlayersGetTheLegacyAnswer() {
        PlayerMock gone = fixture.player("Gone");
        gone.disconnect();

        assertEquals("null player", expansion.onRequest(null, "prefix"));
        assertEquals("null player", expansion.onRequest(gone, "prefix"));
        assertEquals("null player", expansion.onRequest(server.getOfflinePlayer(gone.getUniqueId()), "color"));
    }

    @Test
    void otherPlaceholdersAreEmpty() {
        assertEquals("", expansion.onRequest(fixture.player("Alice"), "kills"));
    }

    private static int teamSize(Game game, PlayerMock player) {
        return game.session(player.getUniqueId()).orElseThrow().team().orElseThrow().members().size();
    }
}
