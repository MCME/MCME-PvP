package com.mcmiddleearth.pvp.ui;

import com.mcmiddleearth.pvp.Permissions;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.game.Role;
import com.mcmiddleearth.pvp.testing.RecordingNavigator;
import com.mcmiddleearth.pvp.testing.TestArenas;
import com.mcmiddleearth.pvp.testing.UiFixture;
import com.mcmiddleearth.pvp.text.Msg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.mcmiddleearth.pvp.testing.UiTesting.button;
import static com.mcmiddleearth.pvp.testing.UiTesting.click;
import static com.mcmiddleearth.pvp.testing.UiTesting.labels;
import static com.mcmiddleearth.pvp.testing.UiTesting.plain;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainMenuViewTest {

    @TempDir
    Path dir;

    private final RecordingNavigator navigator = new RecordingNavigator();
    private UiFixture ui;
    private MainMenuView view;
    private Viewer alice;
    private Viewer staff;

    @BeforeEach
    void setUp() {
        ui = new UiFixture(dir, TestArenas.tdm("AS-TDM", 0, 2), TestArenas.ffa("AS-FFA", 100, 4));
        ConfirmView confirm = new ConfirmView(ui.services.messages(), navigator);
        view = new MainMenuView(ui.services, navigator, new StartGameView(ui.services, navigator), confirm,
                new InfoViews(ui.services));
        alice = ui.player("Alice");
        staff = ui.staff("Staff");
    }

    @Test
    void outsideAGameEveryJoinableGameHasAJoinButton() {
        Game game = ui.open("AS-TDM");
        MenuScreen menu = view.build(alice);
        assertEquals("PvP", plain(menu.title()));
        assertEquals(List.of("Pick a game to join, or spectate one."), plain(menu.body()));
        assertEquals(List.of("Join AS-TDM · Team Deathmatch · 0/" + game.maxPlayers() + " · waiting",
                "Spectate", "Rules", "My stats", "Lobby", "Close"), labels(menu));
        assertEquals(ButtonStyle.PRIMARY, menu.buttons().get(0).style());
    }

    @Test
    void withoutGamesTheMenuSaysSo() {
        MenuScreen menu = view.build(alice);
        assertEquals(List.of("No games are open right now."), plain(menu.body()));
        assertEquals(List.of("Spectate", "Rules", "My stats", "Lobby", "Close"), labels(menu));
    }

    @Test
    void staffButtonsNeedTheirPermissions() {
        assertFalse(labels(view.build(alice)).contains("Start a game"));
        assertFalse(labels(view.build(alice)).contains("Kick all"));
        Viewer kickOnly = new Viewer(staff.id(), "KickOnly", Permissions.STAFF_KICK::equals);
        assertFalse(labels(view.build(kickOnly)).contains("Start a game"));
        assertFalse(labels(view.build(kickOnly)).contains("Kick all"));
        Viewer runner = new Viewer(staff.id(), "Runner", Permissions.STAFF_RUN::equals);
        assertTrue(labels(view.build(runner)).contains("Start a game"));
        assertTrue(labels(view.build(runner)).contains("Kick all"));
        assertEquals(List.of("Start a game", "Kick all", "Close"), labels(view.build(staff)).subList(4, 7));
    }

    @Test
    void startAGameOpensTheArenaPicker() {
        assertTrue(click(button(view.build(staff), "Start a game"), staff).isEmpty());
        assertEquals("Start a game", plain(navigator.lastScreen().title()));
    }

    @Test
    void kickAllAsksFirstThenRunsTheCommandOnce() {
        assertTrue(click(button(view.build(staff), "Kick all"), staff).isEmpty());
        assertTrue(navigator.commands().isEmpty());
        ConfirmScreen question = assertInstanceOf(ConfirmScreen.class, navigator.lastScreen());
        assertEquals("Send everyone to the main server?", plain(question.title()));
        assertTrue(question.yes().action().singleUse());
        assertTrue(click(question.yes(), staff).isEmpty());
        assertEquals(List.of("pvpadmin kickall"), navigator.commands());
    }

    @Test
    void joiningFromTheMenuJoinsTheGame() {
        Game game = ui.open("AS-TDM");
        assertTrue(click(button(view.build(alice), "Join AS-TDM"), alice).isEmpty());
        assertEquals(game, ui.games().gameOf(alice.id()).orElseThrow());
    }

    @Test
    void aJoinButtonForAClosedGameAnswersThatTheGameEnded() {
        Game game = ui.open("AS-TDM");
        MenuScreen menu = view.build(alice);
        ui.games().stop(game);
        assertEquals(Msg.ERROR_GAME_ENDED, click(button(menu, "Join AS-TDM"), alice).orElseThrow().messageKey());
        assertTrue(ui.games().gameOf(alice.id()).isEmpty());
    }

    @Test
    void insideAGameTheMenuOffersLeaveRulesAndScores() {
        Game game = ui.open("AS-TDM");
        ui.join(game, alice);
        MenuScreen menu = view.build(alice);
        assertEquals("Team Deathmatch on AS-TDM", plain(menu.title()));
        assertEquals(List.of("Leave game", "Rules", "Teams and scores", "Close"), labels(menu));
        assertTrue(click(button(menu, "Rules"), alice).isEmpty());
        assertEquals("Team Deathmatch rules", plain(navigator.lastScreen().title()));
        assertTrue(click(button(menu, "Teams and scores"), alice).isEmpty());
        assertEquals("Teams and scores", plain(navigator.lastScreen().title()));
    }

    @Test
    void whileItsResultShowsTheMenuStillShowsTheEndedGame() {
        Game game = ui.open("AS-TDM");
        ui.join(game, alice, ui.player("Bob"));
        ui.games().startCountdown(game);
        ui.advanceSeconds(7);
        ui.games().stop(game);
        ui.open("AS-FFA");

        MenuScreen menu = view.build(alice);

        assertEquals("Team Deathmatch on AS-TDM", plain(menu.title()));
        assertEquals(List.of("Leave game", "Rules", "Teams and scores", "Close"), labels(menu));
    }

    @Test
    void leaveTakesThePlayerOutOnce() {
        Game game = ui.open("AS-TDM");
        ui.join(game, alice);
        Button leave = button(view.build(alice), "Leave game");
        assertTrue(click(leave, alice).isEmpty());
        assertTrue(ui.games().gameOf(alice.id()).isEmpty());
        assertEquals(Msg.ERROR_LEFT_THAT_GAME, click(leave, alice).orElseThrow().messageKey());
    }

    @Test
    void leaveOnAClosedGameAnswersThatItEnded() {
        Game game = ui.open("AS-TDM");
        ui.join(game, alice);
        MenuScreen menu = view.build(alice);
        ui.games().stop(game);
        assertEquals(Msg.ERROR_GAME_ENDED, click(button(menu, "Leave game"), alice).orElseThrow().messageKey());
    }

    @Test
    void leaveOnAGameTheViewerMovedOnFromAnswersThatTheyAlreadyLeft() {
        Game tdm = ui.open("AS-TDM");
        ui.join(tdm, alice, ui.player("Bob"));
        ui.games().startCountdown(tdm);
        ui.advanceSeconds(7);
        Button leave = button(view.build(alice), "Leave game");
        ui.games().stop(tdm);
        Game ffa = ui.open("AS-FFA");
        ui.join(ffa, alice);

        assertEquals(Msg.ERROR_LEFT_THAT_GAME, click(leave, alice).orElseThrow().messageKey());
    }

    @Test
    void spectateListsTheGamesToWatch() {
        Game game = ui.open("AS-TDM");
        assertTrue(click(button(view.build(alice), "Spectate"), alice).isEmpty());
        MenuScreen list = assertInstanceOf(MenuScreen.class, navigator.lastScreen());
        assertEquals("Spectate a game", plain(list.title()));
        assertEquals(List.of("AS-TDM · Team Deathmatch · 0 players", "Back"), labels(list));
        assertTrue(click(button(list, "AS-TDM"), alice).isEmpty());
        assertEquals(Role.SPECTATOR, game.session(alice.id()).orElseThrow().role());
        assertTrue(click(list.exit(), alice).isEmpty());
        MenuScreen backToMenu = assertInstanceOf(MenuScreen.class, navigator.lastScreen());
        assertEquals("Team Deathmatch on AS-TDM", plain(backToMenu.title()));
    }

    @Test
    void spectateListShowsTheSingularForOnePlayer() {
        Game game = ui.open("AS-TDM");
        ui.join(game, ui.player("Bob"));

        MenuScreen list = assertInstanceOf(MenuScreen.class, view.spectateList());

        assertEquals(List.of("AS-TDM · Team Deathmatch · 1 player", "Back"), labels(list));
    }

    @Test
    void spectateWithoutGamesIsANotice() {
        NoticeScreen list = assertInstanceOf(NoticeScreen.class, view.spectateList());
        assertEquals(List.of("No games are open right now."), plain(list.body()));
        assertEquals(List.of("Back"), labels(list));
    }

    @Test
    void lobbySendsThePlayerThere() {
        assertTrue(click(button(view.build(alice), "Lobby"), alice).isEmpty());
        assertEquals(List.of(alice.id()), ui.sentToLobby);
        assertEquals(List.of("You're back in the lobby."), navigator.messages());
    }

    @Test
    void myStatsShowsTheViewersOwnStats() {
        assertTrue(click(button(view.build(alice), "My stats"), alice).isEmpty());
        assertEquals(List.of("No PvP stats for Alice."), plain(navigator.lastScreen().body()));
    }

    @Test
    void staffInAGameAlsoGetStartAndKickAll() {
        Game game = ui.open("AS-TDM");
        ui.join(game, staff);
        MenuScreen menu = view.build(staff);
        assertEquals(List.of("Leave game", "Rules", "Teams and scores", "Start a game", "Kick all", "Close"),
                labels(menu));
    }

    @Test
    void teamsAndScoresOfAClosedGameAnswersThatItEnded() {
        Game game = ui.open("AS-TDM");
        ui.join(game, alice);
        MenuScreen menu = view.build(alice);
        ui.games().stop(game);
        assertEquals(Msg.ERROR_GAME_ENDED,
                click(button(menu, "Teams and scores"), alice).orElseThrow().messageKey());
    }

    @Test
    void withOnlyAnEndedGameTheMenuOffersNoJoinAndSaysSo() {
        Game game = ui.open("AS-TDM");
        ui.join(game, ui.player("Bob"), ui.player("Carol"));
        ui.games().startCountdown(game);
        ui.advanceSeconds(7);
        ui.games().stop(game);
        assertEquals(Phase.ENDING, game.phase());

        MenuScreen menu = view.build(alice);

        assertEquals(List.of("No games are open right now."), plain(menu.body()));
        assertTrue(labels(menu).stream().noneMatch(label -> label.startsWith("Join")));
        assertEquals(List.of("Spectate", "Rules", "My stats", "Lobby", "Close"), labels(menu));
    }

    @Test
    void aFullWaitingGameIsStillOfferedToWatch() {
        Game game = ui.open("AS-TDM", Map.of("maxPlayers", 2));
        ui.join(game, ui.player("Bob"), ui.player("Carol"));
        assertEquals(Phase.WAITING, game.phase());

        MenuScreen menu = view.build(alice);

        assertEquals(List.of("No game takes players right now, but you can spectate one."), plain(menu.body()));
        assertTrue(labels(menu).stream().noneMatch(label -> label.startsWith("Join")));
        MenuScreen list = assertInstanceOf(MenuScreen.class, view.spectateList());
        assertEquals(List.of("AS-TDM · Team Deathmatch · 2 players", "Back"), labels(list));
    }

    @Test
    void theLobbyButtonRefusesAViewerWhoJoinedAGame() {
        Button lobby = button(view.build(alice), "Lobby");
        Game game = ui.open("AS-TDM");
        ui.join(game, alice);

        assertEquals(Msg.ERROR_ALREADY_IN_GAME, click(lobby, alice).orElseThrow().messageKey());
        assertTrue(ui.sentToLobby.isEmpty());
    }

    @Test
    void theRulesButtonOutsideAGameListsTheModes() {
        assertTrue(click(button(view.build(alice), "Rules"), alice).isEmpty());
        assertEquals(plain(new InfoViews(ui.services).rulesList().title()), plain(navigator.lastScreen().title()));
    }
}
