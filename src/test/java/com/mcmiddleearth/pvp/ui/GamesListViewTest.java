package com.mcmiddleearth.pvp.ui;

import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.LeaveReason;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mcmiddleearth.pvp.testing.UiTesting.click;
import static com.mcmiddleearth.pvp.testing.UiTesting.link;
import static com.mcmiddleearth.pvp.testing.UiTesting.linkLabels;
import static com.mcmiddleearth.pvp.testing.UiTesting.plain;
import static com.mcmiddleearth.pvp.testing.UiTesting.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GamesListViewTest {

    @TempDir
    Path dir;

    private final RecordingNavigator navigator = new RecordingNavigator();
    private UiFixture ui;
    private GamesListView view;
    private Viewer alice;
    private Viewer bob;
    private Viewer staff;

    @BeforeEach
    void setUp() {
        List<Arena> arenas = new ArrayList<>(List.of(TestArenas.tdm("AS-TDM", 0, 2), TestArenas.ffa("AS-FFA", 100, 4)));
        for (int i = 1; i <= 9; i++) {
            arenas.add(TestArenas.tdm("P" + i, 1000 + i * 100, 2));
        }
        ui = new UiFixture(dir, arenas.toArray(Arena[]::new));
        view = new GamesListView(ui.services, navigator, new ConfirmView(ui.services.messages(), navigator),
                new InfoViews(ui.services));
        alice = ui.player("Alice");
        bob = ui.player("Bob");
        staff = ui.staff("Staff");
    }

    @Test
    void everyOpenGameCanBeJoinedOrWatched() {
        Game game = ui.open("AS-TDM");
        ChatMenu menu = view.page(alice, 1);
        assertEquals("PvP games", plain(menu.header()));
        assertEquals(List.of("AS-TDM · Team Deathmatch · 0/" + game.maxPlayers() + " · waiting [Join] [Spectate]"),
                menu.lines().stream().map(line -> text(line)).toList());
    }

    @Test
    void staffAlsoGetCountdownAndStop() {
        ui.open("AS-TDM");
        assertEquals(List.of("Join", "Spectate", "Countdown", "Stop"), linkLabels(view.page(staff, 1).lines().get(0)));
    }

    @Test
    void withoutGamesTheListSaysSo() {
        ChatMenu menu = view.page(alice, 1);
        assertEquals(List.of("No games right now."), menu.lines().stream().map(line -> text(line)).toList());
        assertNull(menu.next());
    }

    @Test
    void pagesHoldEightGamesAndTurn() {
        for (int i = 1; i <= 9; i++) {
            ui.open("P" + i);
        }
        ChatMenu first = view.page(alice, 1);
        assertEquals(8, first.lines().size());
        assertEquals(2, first.pages());
        assertNull(first.previous());
        assertNotNull(first.next());
        assertTrue(first.next().attempt(alice, FormValues.empty()).isEmpty());
        ChatMenu second = navigator.lastMenu();
        assertEquals(2, second.page());
        assertEquals(1, second.lines().size());
        assertNotNull(second.previous());
        assertEquals(2, view.page(alice, 99).page());
        assertEquals(1, view.page(alice, 0).page());
    }

    @Test
    void joinAndSpectateUseTheGame() {
        Game game = ui.open("AS-TDM");
        ChatLine line = view.page(alice, 1).lines().get(0);
        assertTrue(click(link(line, "Join"), alice).isEmpty());
        assertTrue(click(link(line, "Spectate"), bob).isEmpty());
        assertEquals(game, ui.games().gameOf(alice.id()).orElseThrow());
        assertEquals(Role.SPECTATOR, game.session(bob.id()).orElseThrow().role());
    }

    @Test
    void joinRefusesLikeTheCommandWhilePvpIsLocked() {
        Game game = ui.open("AS-TDM");
        ChatLine line = view.page(alice, 1).lines().get(0);
        ui.services.lock().lock();
        assertEquals(Msg.ERROR_SERVER_LOCKED, click(link(line, "Join"), alice).orElseThrow().messageKey());
        assertTrue(click(link(line, "Join"), staff).isEmpty(), "pvp.join.locked may join");
        assertEquals(game, ui.games().gameOf(staff.id()).orElseThrow());
    }

    @Test
    void spectateRefusesLikeTheCommandWhilePvpIsLocked() {
        Game game = ui.open("AS-TDM");
        ChatLine line = view.page(alice, 1).lines().get(0);
        ui.services.lock().lock();
        assertEquals(Msg.ERROR_SERVER_LOCKED, click(link(line, "Spectate"), alice).orElseThrow().messageKey());
        assertTrue(click(link(line, "Spectate"), staff).isEmpty(), "pvp.join.locked may spectate");
        assertEquals(Role.SPECTATOR, game.session(staff.id()).orElseThrow().role());
    }

    @Test
    void aJoinThatEndsUpSpectatingSaysWhy() {
        Game game = ui.open("AS-FFA", Map.of("midgameJoin", false));
        ChatLine line = view.page(alice, 1).lines().get(0);
        ui.join(game, alice, bob);
        ui.games().startCountdown(game);
        ui.advanceSeconds(7);
        Viewer carol = ui.player("Carol");
        assertTrue(click(link(line, "Join"), carol).isEmpty());
        assertEquals(Role.SPECTATOR, game.session(carol.id()).orElseThrow().role());
        assertEquals(List.of("The game has already started."), navigator.messages());
    }

    @Test
    void anEliminatedPlayerIsOfferedOnlySpectateForThatGame() {
        Game game = ui.open("AS-TDM");
        ui.join(game, alice, bob, ui.player("Carol"), ui.player("Dave"));
        ui.games().startCountdown(game);
        ui.advanceSeconds(7);
        ui.join(game, ui.player("Erin"));
        game.handleDeath(alice.id(), bob.id());
        ui.games().leave(alice.id(), LeaveReason.LEFT);
        // Erin made up for Alice, so the game still takes players, but Alice was eliminated from it.
        ChatLine line = view.page(alice, 1).lines().get(0);

        assertEquals(List.of("Spectate"), linkLabels(line));
        assertTrue(click(link(line, "Spectate"), alice).isEmpty());

        assertEquals(Role.SPECTATOR, game.session(alice.id()).orElseThrow().role());
    }

    @Test
    void aViewerAlreadyInAGameGetsNoJoinOrSpectateAnywhere() {
        Game mine = ui.open("AS-TDM");
        ui.join(mine, alice);
        ui.open("AS-FFA");

        ChatMenu menu = view.page(alice, 1);

        assertEquals(2, menu.lines().size());
        assertTrue(menu.lines().stream().allMatch(line -> linkLabels(line).isEmpty()), menu.lines().toString());
    }

    @Test
    void aViewerWhoseOwnGameIsEndingStillGetsButtonsForOtherGames() {
        Game mine = ui.open("AS-TDM");
        ui.join(mine, alice, bob);
        ui.games().startCountdown(mine);
        ui.advanceSeconds(7);
        ui.games().stop(mine);
        assertEquals(Phase.ENDING, mine.phase());
        ui.open("AS-FFA");

        ChatLine ffaLine = view.page(alice, 1).lines().get(1);

        assertEquals(List.of("Join", "Spectate"), linkLabels(ffaLine));
    }

    @Test
    void aPlayerWhoseGameHasEndedJoinsAnotherFromTheList() {
        Game tdm = ui.open("AS-TDM");
        ui.join(tdm, alice, bob);
        ui.games().startCountdown(tdm);
        ui.advanceSeconds(7);
        ui.games().stop(tdm);
        Game ffa = ui.open("AS-FFA");

        ChatLine line = view.page(alice, 1).lines().get(1);
        assertTrue(click(link(line, "Join"), alice).isEmpty());

        assertEquals(ffa, ui.games().gameOf(alice.id()).orElseThrow());
        assertEquals(tdm, ui.games().gameOf(bob.id()).orElseThrow());
        assertEquals(Phase.ENDING, tdm.phase());
    }

    @Test
    void aButtonForAClosedGameAnswersThatTheGameEnded() {
        Game game = ui.open("AS-TDM");
        ChatLine line = view.page(alice, 1).lines().get(0);
        ui.games().stop(game);
        assertEquals(Msg.ERROR_GAME_ENDED, click(link(line, "Join"), alice).orElseThrow().messageKey());
        assertTrue(ui.games().gameOf(alice.id()).isEmpty());
    }

    @Test
    void stopAsksFirstThenStopsTheGameOnce() {
        Game game = ui.open("AS-TDM");
        assertTrue(click(link(view.page(staff, 1).lines().get(0), "Stop"), staff).isEmpty());
        assertTrue(ui.games().games().contains(game));
        ConfirmScreen question = assertInstanceOf(ConfirmScreen.class, navigator.lastScreen());
        assertEquals("Stop the game on AS-TDM?", plain(question.title()));
        assertEquals(List.of("Everyone in it goes back to the lobby."), plain(question.body()));
        assertTrue(question.yes().action().singleUse());
        assertTrue(navigator.serverLog().isEmpty(), "asking first must not log yet");
        assertTrue(click(question.yes(), staff).isEmpty());
        assertTrue(ui.games().games().isEmpty());
        assertEquals(List.of("Stopped the game on AS-TDM."), navigator.messages());
        assertEquals(List.of("Staff stops the game on AS-TDM from a menu"), navigator.serverLog());
        assertEquals(Msg.ERROR_GAME_ENDED, click(question.yes(), staff).orElseThrow().messageKey());
        assertEquals(List.of("Staff stops the game on AS-TDM from a menu"), navigator.serverLog(),
                "a refused click must not log again");
    }

    @Test
    void countdownStartsTheGame() {
        Game game = ui.open("AS-TDM");
        ui.join(game, alice, bob);
        assertTrue(click(link(view.page(staff, 1).lines().get(0), "Countdown"), staff).isEmpty());
        assertEquals(Phase.COUNTDOWN, game.phase());
        assertEquals(List.of("Countdown started on AS-TDM."), navigator.messages());
        assertEquals(List.of("Staff starts the countdown on AS-TDM from a menu"), navigator.serverLog());
    }

    @Test
    void joinIsOfferedOnlyWhenThePlayerWouldPlay() {
        Game game = ui.open("AS-FFA", Map.of("midgameJoin", false));
        ui.join(game, alice, bob);
        ui.games().startCountdown(game);
        ui.advanceSeconds(7);
        assertEquals(Phase.RUNNING, game.phase());
        assertEquals(List.of("Spectate"), linkLabels(view.page(ui.player("Carol"), 1).lines().get(0)));
    }

    @Test
    void theRowUnderAnOpenedGameHasJoinSpectateAndRules() {
        Game game = ui.open("AS-TDM");
        ChatMenu row = view.opened(game);
        assertNull(row.header());
        assertEquals(List.of("Join", "Spectate", "Rules"), linkLabels(row.lines().get(0)));
        assertTrue(click(link(row.lines().get(0), "Rules"), alice).isEmpty());
        assertEquals("Team Deathmatch rules", plain(navigator.lastScreen().title()));
    }

    @Test
    void theArrivalNoticeListsTheGamesThatAcceptJoins() {
        assertTrue(view.arrival().isEmpty());
        ui.open("AS-TDM");
        ChatMenu notice = view.arrival().orElseThrow();
        assertEquals("PvP games open:", plain(notice.header()));
        assertEquals(1, notice.lines().size());
        assertEquals(List.of("Join", "Spectate"), linkLabels(notice.lines().get(0)));
    }

    @Test
    void theArrivalNoticeListsAtMostEightGamesWithAMoreLink() {
        for (int i = 1; i <= 9; i++) {
            ui.open("P" + i);
        }

        ChatMenu notice = view.arrival().orElseThrow();

        assertEquals(9, notice.lines().size());
        ChatLine more = notice.lines().get(8);
        assertEquals(List.of("More games: /pvp games"), linkLabels(more));
        assertTrue(click(link(more, "More games"), alice).isEmpty());
        assertTrue(navigator.commands().isEmpty(), "it shows the list directly, without running a command");
        ChatMenu shown = navigator.lastMenu();
        assertEquals("PvP games", plain(shown.header()));
        assertEquals(1, shown.page());
    }

    @Test
    void theArrivalNoticeLeavesOutAGameThatHasEnded() {
        Game game = ui.open("AS-TDM");
        ui.join(game, alice, bob);
        ui.games().startCountdown(game);
        ui.advanceSeconds(7);
        ui.games().stop(game);
        assertEquals(Phase.ENDING, game.phase());
        assertTrue(view.arrival().isEmpty());
    }

    @Test
    void theArrivalNoticeListsAGameThatLateJoinersOnlyWatch() {
        Game game = ui.open("AS-FFA", Map.of("midgameJoin", false));
        ui.join(game, alice, bob);
        ui.games().startCountdown(game);
        ui.advanceSeconds(7);
        assertEquals(Phase.RUNNING, game.phase());

        ChatMenu notice = view.arrival().orElseThrow();

        assertEquals(1, notice.lines().size());
        assertEquals(List.of("Spectate"), linkLabels(notice.lines().get(0)));
    }

    @Test
    void aJoinOnAFullRunningGameSpectatesAndSaysTheGameIsFull() {
        Game game = ui.open("AS-TDM", Map.of("maxPlayers", 2));
        ChatLine line = view.page(alice, 1).lines().get(0);
        ui.join(game, alice, bob);
        ui.games().startCountdown(game);
        ui.advanceSeconds(7);
        Viewer carol = ui.player("Carol");
        assertTrue(click(link(line, "Join"), carol).isEmpty());
        assertEquals(Role.SPECTATOR, game.session(carol.id()).orElseThrow().role());
        assertEquals(List.of("The game is full."), navigator.messages());
    }

    @Test
    void aJoinOnAWaitingGameThatFilledUpIsRefusedAsFull() {
        Game game = ui.open("AS-TDM", Map.of("maxPlayers", 2));
        ChatLine line = view.page(alice, 1).lines().get(0);
        ui.join(game, alice, bob);
        Viewer carol = ui.player("Carol");
        assertEquals(Msg.ERROR_GAME_FULL, click(link(line, "Join"), carol).orElseThrow().messageKey());
    }

    @Test
    void previousTurnsBackAPage() {
        for (int i = 1; i <= 9; i++) {
            ui.open("P" + i);
        }
        ChatMenu second = view.page(alice, 2);
        assertTrue(second.previous().attempt(alice, FormValues.empty()).isEmpty());
        assertEquals(1, navigator.lastMenu().page());
    }

    @Test
    void aSpectateButtonForAClosedGameAnswersThatTheGameEnded() {
        Game game = ui.open("AS-TDM");
        ChatLine line = view.page(alice, 1).lines().get(0);
        ui.games().stop(game);
        assertEquals(Msg.ERROR_GAME_ENDED, click(link(line, "Spectate"), alice).orElseThrow().messageKey());
        assertTrue(ui.games().gameOf(alice.id()).isEmpty());
    }
}
