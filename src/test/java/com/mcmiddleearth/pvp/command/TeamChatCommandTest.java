package com.mcmiddleearth.pvp.command;

import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.integration.LegacyColors;
import com.mcmiddleearth.pvp.platform.GameEventRouter;
import com.mcmiddleearth.pvp.team.TeamSpec;
import com.mcmiddleearth.pvp.testing.PaperFixture;
import com.mcmiddleearth.pvp.testing.TestArenas;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** /t: team chat within one game. */
class TeamChatCommandTest {

    @TempDir
    Path dir;

    private PaperFixture fixture;
    private TeamChatCommand teamChat;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        fixture = PaperFixture.create(server, dir, TestArenas.tdm("AS-TDM", 0, 2), TestArenas.ffa("AS-FFA", 100, 4));
        teamChat = new TeamChatCommand(fixture.services());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void aMessageReachesTheSendersTeamOnly() {
        List<PlayerMock> players = List.of(fixture.player("Alice"), fixture.player("Bob"), fixture.player("Carol"),
                fixture.player("Dave"));
        Game game = running("AS-TDM", players);
        PlayerMock sender = players.getFirst();
        List<PlayerMock> team = players.stream().filter(p -> color(game, p).equals(color(game, sender))).toList();
        List<PlayerMock> others = players.stream().filter(p -> !team.contains(p)).toList();

        teamChat.send(sender, "hold the bridge");

        for (PlayerMock member : team) {
            assertEquals(List.of("[TEAM] Alice: hold the bridge"), PaperFixture.chat(member));
        }
        for (PlayerMock other : others) {
            assertEquals(List.of(), PaperFixture.chat(other));
        }
    }

    @Test
    void theLineIsInTheTeamColorAndTagsInTheMessageStayText() {
        PlayerMock alice = fixture.player("Alice");
        Game game = running("AS-TDM", List.of(alice, fixture.player("Bob")));

        teamChat.send(alice, "<red>go</red>");

        String legacy = LegacyComponentSerializer.legacySection().serialize(alice.nextComponentMessage());
        assertTrue(legacy.startsWith(LegacyColors.code(color(game, alice).color()) + "[TEAM] Alice"), legacy);
        assertTrue(legacy.endsWith("<red>go</red>"), legacy);
    }

    @Test
    void anEliminatedPlayerHasNoTeamChat() {
        fixture.register(new GameEventRouter(fixture.services(), fixture.plugin()));
        List<PlayerMock> players = List.of(fixture.player("Alice"), fixture.player("Bob"), fixture.player("Carol"),
                fixture.player("Dave"));
        Game game = running("AS-TDM", players);
        PlayerMock victim = players.getFirst();
        PlayerMock teammate = players.stream()
                .filter(p -> !p.equals(victim) && color(game, p).equals(color(game, victim)))
                .findFirst().orElseThrow();
        PlayerMock attacker = players.stream()
                .filter(p -> !color(game, p).equals(color(game, victim)))
                .findFirst().orElseThrow();

        victim.simulateDamage(100, attacker);
        players.forEach(PaperFixture::chat);

        teamChat.send(victim, "help");
        assertEquals(List.of("You're not on a team."), PaperFixture.chat(victim));

        teamChat.send(teammate, "fall back");
        assertEquals(List.of(), PaperFixture.chat(victim));
    }

    @Test
    void withoutATeamThereIsNoTeamChat() {
        PlayerMock lobby = fixture.player("Lobby");
        PlayerMock ffa = fixture.player("Erin");
        PlayerMock spectator = fixture.player("Frank");
        Game game = running("AS-FFA", List.of(ffa, fixture.player("Gina")));
        fixture.services().games().spectate(PaperFixture.ref(spectator), game);
        PaperFixture.chat(ffa);
        PaperFixture.chat(spectator);

        for (PlayerMock player : List.of(lobby, ffa, spectator)) {
            teamChat.send(player, "hello");
            assertEquals(List.of("You're not on a team."), PaperFixture.chat(player), player.getName());
        }
    }

    private Game running(String arena, List<PlayerMock> players) {
        Game game = fixture.open(arena);
        fixture.join(game, players.toArray(PlayerMock[]::new));
        fixture.startAndRun(game);
        players.forEach(PaperFixture::chat);
        return game;
    }

    private static TeamSpec color(Game game, PlayerMock player) {
        return game.session(player.getUniqueId()).orElseThrow().color().orElseThrow();
    }
}
