package com.mcmiddleearth.pvp.ui;

import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.PvpServices;
import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.arena.ArenaRegistry;
import com.mcmiddleearth.pvp.arena.ArenaStore;
import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.GameListeners;
import com.mcmiddleearth.pvp.game.GameManager;
import com.mcmiddleearth.pvp.game.Setting;
import com.mcmiddleearth.pvp.game.SettingSpec;
import com.mcmiddleearth.pvp.mode.GameMode;
import com.mcmiddleearth.pvp.mode.ModeRegistry;
import com.mcmiddleearth.pvp.mode.ModeRules;
import com.mcmiddleearth.pvp.mode.ffa.FreeForAll;
import com.mcmiddleearth.pvp.mode.loadout.Kits;
import com.mcmiddleearth.pvp.mode.loadout.LoadoutPolicy;
import com.mcmiddleearth.pvp.mode.rules.MidgameJoin;
import com.mcmiddleearth.pvp.mode.rules.Respawn;
import com.mcmiddleearth.pvp.mode.rules.SpawnPoints;
import com.mcmiddleearth.pvp.mode.rules.WinConditions;
import com.mcmiddleearth.pvp.mode.tdm.TeamDeathmatch;
import com.mcmiddleearth.pvp.storage.Json;
import com.mcmiddleearth.pvp.team.TeamLayout;
import com.mcmiddleearth.pvp.testing.FakeGameContext;
import com.mcmiddleearth.pvp.testing.RecordingNavigator;
import com.mcmiddleearth.pvp.testing.TestArenas;
import com.mcmiddleearth.pvp.testing.TestMode;
import com.mcmiddleearth.pvp.testing.UiFixture;
import com.mcmiddleearth.pvp.text.Messages;
import com.mcmiddleearth.pvp.text.Msg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import static com.mcmiddleearth.pvp.testing.UiTesting.button;
import static com.mcmiddleearth.pvp.testing.UiTesting.click;
import static com.mcmiddleearth.pvp.testing.UiTesting.labels;
import static com.mcmiddleearth.pvp.testing.UiTesting.plain;
import static com.mcmiddleearth.pvp.testing.UiTesting.submit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartGameViewTest {

    @TempDir
    Path dir;

    private final RecordingNavigator navigator = new RecordingNavigator();
    private UiFixture ui;
    private StartGameView view;
    private Viewer staff;

    @BeforeEach
    void setUp() {
        ui = new UiFixture(dir,
                TestArenas.tdm("AS-TDM", 0, 2),
                TestArenas.ffa("AS-FFA", 0, 4),
                TestArenas.tdm("BR-TDM", 100, 3),
                TestArenas.tdm("OLD", 200, 2).unassigned("the map has no game mode"));
        view = new StartGameView(ui.services, navigator);
        staff = ui.staff("Staff");
    }

    private Arena arena(String id) {
        return ui.services.arenas().get(id).orElseThrow();
    }

    /** Clicks {@code buttonStart} as staff and returns the screen it showed. */
    private Screen clickInto(Screen screen, String buttonStart) {
        assertTrue(click(button(screen, buttonStart), staff).isEmpty());
        return navigator.lastScreen();
    }

    private SettingSpec spec(Arena arena, String settingId) {
        return ui.games().settingsFor(arena).stream()
                .filter(spec -> spec.setting().id().equals(settingId)).findFirst().orElseThrow();
    }

    /** The store behind {@code ui}'s registry, so a test can change an arena file and reload it. */
    private ArenaStore arenaStore() {
        return new ArenaStore(dir.resolve("arenas"), Json.pretty());
    }

    /**
     * A services set of its own, so a test can give {@code tdm} a {@code teamCount} default above the mode's own
     * minimum: {@code UiFixture} always uses {@code PvpConfig.defaults()}, whose bundled default (2) never shows a
     * difference. {@code stats}, {@code lobby}, {@code lock} and {@code proxy} are null: {@link StartGameView} and
     * {@link GameManager} never read them.
     */
    private PvpServices servicesWithTeamCountDefault(Path storeDir, int teamCountDefault, Arena... arenas) {
        PvpConfig defaults = PvpConfig.defaults();
        PvpConfig config = new PvpConfig(defaults.lobby(), defaults.worlds(), defaults.mainServer(), defaults.startLocked(),
                defaults.lavaDamage(), defaults.lobbyProtection(), defaults.countdownSeconds(), defaults.endingSeconds(),
                Map.of("tdm", Map.of("teamCount", teamCountDefault)));
        ArenaRegistry registry = TestArenas.registry(storeDir, arenas);
        ModeRegistry modes = TestArenas.modes();
        GameManager games = new GameManager(registry, modes, config, FakeGameContext.factory(new ArrayList<>()),
                new GameListeners(Logger.getLogger("StartGameViewTest")), id -> 0.0);
        return new PvpServices(config, Messages.bundled(), registry, modes, games, null, null, null, null);
    }

    @Test
    void stepOneListsTheModesWithTheirFreeCounts() {
        MenuScreen modePicker = assertInstanceOf(MenuScreen.class, view.picker());
        assertEquals("Start a game", plain(modePicker.title()));
        assertEquals(List.of("Pick a mode."), plain(modePicker.body()));
        assertEquals(List.of("Team Deathmatch (2 free)", "Free For All (1 free)", "Close"), labels(modePicker));
    }

    @Test
    void stepOneNamesAModeWhoseArenasAreAllBusy() {
        UiFixture withHill = new UiFixture(dir.resolve("with-hill"),
                new ModeRegistry(List.of(new TeamDeathmatch(), new FreeForAll(),
                        new TestMode("hill", TestMode.twoTeams().build()))),
                TestArenas.tdm("AS-TDM", 0, 2), TestArenas.ffa("AS-FFA", 100, 4),
                TestArenas.tdm("HILL", 200, 2).withMode("hill"));
        StartGameView hillView = new StartGameView(withHill.services, navigator);
        withHill.open("HILL");

        MenuScreen modePicker = assertInstanceOf(MenuScreen.class, hillView.picker());
        assertEquals(List.of("Pick a mode.", "mode.hill.name: all arenas busy"), plain(modePicker.body()));
        assertEquals(List.of("Team Deathmatch (1 free)", "Free For All (1 free)", "Close"), labels(modePicker));
    }

    @Test
    void stepTwoListsOnlyThatModesArenasSortedInThreeColumns() {
        MenuScreen arenaPicker = assertInstanceOf(MenuScreen.class, clickInto(view.picker(), "Team Deathmatch"));
        assertEquals("Start a game", plain(arenaPicker.title()));
        assertEquals(List.of("Pick a Team Deathmatch arena."), plain(arenaPicker.body()));
        assertEquals(List.of("AS-TDM", "BR-TDM", "Back"), labels(arenaPicker));
        assertEquals(3, arenaPicker.columns());
    }

    @Test
    void backFromStepTwoReturnsToStepOne() {
        Screen arenaPicker = clickInto(view.picker(), "Team Deathmatch");
        assertTrue(click(button(arenaPicker, "Back"), staff).isEmpty());
        MenuScreen modePicker = assertInstanceOf(MenuScreen.class, navigator.lastScreen());
        assertEquals(List.of("Team Deathmatch (2 free)", "Free For All (1 free)", "Close"), labels(modePicker));
    }

    @Test
    void onlyOneModeFreeSkipsStepOneAndNamesTheBusyArenasAndFullyBusyModes() {
        ui.open("AS-TDM");
        MenuScreen picker = assertInstanceOf(MenuScreen.class, view.picker());
        assertEquals("Start a game", plain(picker.title()));
        assertEquals(List.of("BR-TDM", "Close"), labels(picker));
        assertEquals(List.of(
                "Pick a Team Deathmatch arena.",
                "AS-TDM is busy (game on AS-TDM).",
                "Free For All: all arenas busy"), plain(picker.body()));
    }

    @Test
    void pickingAModeThatBecameFullyBusyIsRefused() {
        Button teamDeathmatch = button(view.picker(), "Team Deathmatch");
        ui.open("AS-TDM");
        ui.open("BR-TDM");
        assertEquals(Msg.ERROR_BUTTON_STALE, click(teamDeathmatch, staff).orElseThrow().messageKey());
        assertTrue(navigator.log().isEmpty());
    }

    @Test
    void withEveryArenaBusyThePickerIsANotice() {
        ui.open("AS-TDM");
        ui.open("BR-TDM");
        NoticeScreen picker = assertInstanceOf(NoticeScreen.class, view.picker());
        assertEquals(List.of(
                "No arena is free to start a game on.",
                "AS-FFA is busy (game on AS-TDM).",
                "AS-TDM is busy (game on AS-TDM).",
                "BR-TDM is busy (game on BR-TDM)."), plain(picker.body()));
    }

    @Test
    void pickingAnArenaOpensItsSettingsForm() {
        Screen arenaPicker = clickInto(view.picker(), "Team Deathmatch");
        assertTrue(click(button(arenaPicker, "BR-TDM"), staff).isEmpty());
        FormScreen form = assertInstanceOf(FormScreen.class, navigator.lastScreen());
        assertEquals("Team Deathmatch on BR-TDM", plain(form.title()));
    }

    @Test
    void pickingAnArenaThatBecameBusyIsRefused() {
        Screen arenaPicker = clickInto(view.picker(), "Team Deathmatch");
        navigator.clear();
        ui.open("BR-TDM");
        assertEquals(Msg.ERROR_ARENA_BLOCKED, click(button(arenaPicker, "BR-TDM"), staff).orElseThrow().messageKey());
        assertTrue(navigator.log().isEmpty());
    }

    @Test
    void pickingARemovedArenaIsRefused() throws IOException {
        Screen arenaPicker = clickInto(view.picker(), "Team Deathmatch");
        Button button = button(arenaPicker, "BR-TDM");
        navigator.clear();
        Files.delete(arenaStore().fileFor("BR-TDM"));
        ui.services.arenas().load();

        assertEquals(Msg.ERROR_UNKNOWN_ARENA, click(button, staff).orElseThrow().messageKey());
        assertTrue(navigator.log().isEmpty());
    }

    @Test
    void formForABusyArenaIsRefused() {
        ui.open("BR-TDM");
        PvpException e = assertThrows(PvpException.class, () -> view.form(arena("BR-TDM")));
        assertEquals(Msg.ERROR_ARENA_BLOCKED, e.messageKey());
    }

    @Test
    void anUnplayableArenaUnderAnOpenGameIsRefusedAsUnplayable() {
        UiFixture blocked = new UiFixture(dir.resolve("blocked"), TestArenas.tdm("AS-TDM", 0, 2),
                TestArenas.tdm("OLD", 0, 2).unassigned("the map has no game mode"));
        StartGameView blockedView = new StartGameView(blocked.services, navigator);
        blocked.open("AS-TDM");

        PvpException e = assertThrows(PvpException.class,
                () -> blockedView.form(blocked.services.arenas().get("OLD").orElseThrow()));
        assertEquals(Msg.ERROR_ARENA_UNASSIGNED, e.messageKey());
    }

    @Test
    void everySettingBecomesAnInputOrATextLine() {
        for (Arena arena : ui.services.arenas().playable()) {
            FormScreen form = view.form(arena);
            assertEquals(ui.games().settingsFor(arena).size(), form.inputs().size() + form.body().size(), arena.id());
        }
    }

    @Test
    void aNewModeGetsItsFormAndRulesWithoutUiCode() {
        GameMode hill = new GameMode() {
            private final ModeRules rules = ModeRules.builder()
                    .teams(TeamLayout.fixed(2))
                    .spawnPoints(SpawnPoints.perTeam())
                    .loadout(LoadoutPolicy.fixed(Kits.STANDARD))
                    .respawn(Respawn.AT_SPAWN)
                    .midgameJoin(MidgameJoin.JOIN)
                    .win(WinConditions.highestAt(Setting.TIME_LIMIT))
                    .build();

            @Override
            public String id() {
                return "hill";
            }

            @Override
            public Set<String> legacyNames() {
                return Set.of("KingOfTheHill");
            }

            @Override
            public ModeRules rules() {
                return rules;
            }
        };
        UiFixture withHill = new UiFixture(dir.resolve("hill"),
                new ModeRegistry(List.of(new TeamDeathmatch(), new FreeForAll(), hill)),
                TestArenas.tdm("HILL", 0, 2).withMode("hill"));
        StartGameView hillView = new StartGameView(withHill.services, navigator);

        FormScreen form = hillView.form(withHill.services.arenas().get("HILL").orElseThrow());

        assertEquals(List.of("timeLimit", "maxPlayers", "midgameJoin"), form.inputs().stream().map(Input::key).toList());
        assertEquals(6, new InfoViews(withHill.services).rulesList().body().size(), "three modes, two lines each");
    }

    @Test
    void aMixedCaseModeIdStillShowsItsProperName() {
        UiFixture mixedCase = new UiFixture(dir.resolve("mixed-case"),
                TestArenas.tdm("MIX-TDM", 0, 2).withMode("TDM"));
        StartGameView mixedView = new StartGameView(mixedCase.services, navigator);

        FormScreen form = mixedView.form(mixedCase.services.arenas().get("MIX-TDM").orElseThrow());

        assertEquals("Team Deathmatch on MIX-TDM", plain(form.title()));
    }

    @Test
    void theTeamSliderIsCappedByTheArenasSpawnSets() {
        FormScreen three = view.form(arena("BR-TDM"));
        Slider teams = assertInstanceOf(Slider.class, three.inputs().get(0));
        assertEquals("teamCount", teams.key());
        assertEquals("Teams", plain(teams.label()));
        assertEquals(List.of(2, 3, 2), List.of(teams.min(), teams.max(), teams.initial()));

        FormScreen two = view.form(arena("AS-TDM"));
        assertEquals(List.of("Teams: 2"), plain(two.body()));
        assertTrue(two.inputs().stream().noneMatch(input -> input.key().equals("teamCount")));
    }

    @Test
    void theFreeForAllFormHasSlidersAndAToggleWithTheDefaults() {
        Arena ffa = arena("AS-FFA");
        FormScreen form = view.form(ffa);
        assertEquals(List.of("timeLimit", "scoreGoal", "maxPlayers", "midgameJoin"),
                form.inputs().stream().map(Input::key).toList());
        Slider time = (Slider) form.inputs().get(0);
        assertEquals("Time limit (minutes)", plain(time.label()));
        assertEquals(List.of(1, 60, 10), List.of(time.min(), time.max(), time.initial()));
        Slider goal = (Slider) form.inputs().get(1);
        assertEquals(List.of(0, 100, 0), List.of(goal.min(), goal.max(), goal.initial()));
        Slider max = (Slider) form.inputs().get(2);
        SettingSpec maxSpec = spec(ffa, "maxPlayers");
        assertEquals(List.of(maxSpec.min(), maxSpec.max(), maxSpec.defaultValue()),
                List.of(max.min(), max.max(), max.initial()));
        Toggle late = (Toggle) form.inputs().get(3);
        assertEquals("Allow joining after the start", plain(late.label()));
        assertTrue(late.initial());
        assertEquals(List.of("Start", "Back"), labels(form));
    }

    @Test
    void startOpensTheGameWithTheSubmittedValues() {
        FormScreen form = view.form(arena("AS-FFA"));
        int maxPlayers = Math.min(8, spec(arena("AS-FFA"), "maxPlayers").max());
        assertTrue(submit(form, staff,
                Map.of("timeLimit", 5, "scoreGoal", 3, "maxPlayers", maxPlayers, "midgameJoin", false)).isEmpty());
        Game game = ui.games().gameOn("AS-FFA").orElseThrow();
        assertEquals(new TreeMap<>(Map.of("timeLimit", 5, "scoreGoal", 3, "maxPlayers", maxPlayers, "midgameJoin", false)),
                new TreeMap<>(game.settings().asMap()));
        assertEquals(List.of("Opened Free For All on AS-FFA."), navigator.messages());
        assertEquals(List.of("Staff opens ffa on AS-FFA from a menu"), navigator.serverLog());
    }

    @Test
    void aClientCannotChangeAFixedSettingOrAddOne() {
        FormScreen form = view.form(arena("AS-TDM"));
        assertTrue(submit(form, staff,
                Map.of("maxPlayers", 4, "midgameJoin", true, "teamCount", 3, "timeLimit", 5)).isEmpty());
        Game game = ui.games().gameOn("AS-TDM").orElseThrow();
        assertEquals(2, game.settings().asMap().get("teamCount"));
    }

    @Test
    void fixedSettingsAreSentAsShownEvenIfTheArenaGainsSpawnsAfterward() throws IOException {
        Path customDir = dir.resolve("config-override");
        PvpServices custom = servicesWithTeamCountDefault(customDir, 3, TestArenas.tdm("CFG-TDM", 0, 2));
        StartGameView customView = new StartGameView(custom, navigator);

        // Two spawn sets: teamCount is fixed, and always at the mode's own minimum (2) regardless of the config
        // default (3).
        FormScreen form = customView.form(custom.arenas().get("CFG-TDM").orElseThrow());
        assertEquals(List.of("Teams: 2"), plain(form.body()));

        // A third spawn set arrives (a /pvpadmin reload, say): teamCount is no longer fixed, and its recomputed
        // default is now the config's 3.
        new ArenaStore(customDir.resolve("arenas"), Json.pretty()).save(TestArenas.tdm("CFG-TDM", 0, 3));
        custom.arenas().load();

        assertTrue(submit(form, staff, Map.of("maxPlayers", 4, "midgameJoin", true)).isEmpty());
        assertEquals(2, custom.games().gameOn("CFG-TDM").orElseThrow().settings().asMap().get("teamCount"));
    }

    @Test
    void aValueOutOfRangeIsRefusedAndNothingOpens() {
        FormScreen form = view.form(arena("AS-FFA"));
        assertEquals(Msg.ERROR_SETTING_RANGE, submit(form, staff,
                Map.of("timeLimit", 61, "scoreGoal", 0, "maxPlayers", 2, "midgameJoin", true)).orElseThrow().messageKey());
        assertTrue(ui.games().games().isEmpty());
        assertTrue(navigator.serverLog().isEmpty());
    }

    @Test
    void aMissingValueIsRefusedAndNothingOpens() {
        FormScreen form = view.form(arena("AS-FFA"));
        assertEquals(Msg.ERROR_SETTING_INVALID, submit(form, staff,
                Map.of("timeLimit", 10, "scoreGoal", 0, "midgameJoin", true)).orElseThrow().messageKey());
        assertTrue(ui.games().games().isEmpty());
    }

    @Test
    void onlyStaffCanStart() {
        FormScreen form = view.form(arena("AS-TDM"));
        assertEquals(Msg.ERROR_NO_PERMISSION, submit(form, ui.player("Alice"),
                Map.of("maxPlayers", 2, "midgameJoin", true)).orElseThrow().messageKey());
        assertTrue(ui.games().games().isEmpty());
    }

    @Test
    void onlyStaffCanOpenTheFormOrGoBack() {
        Viewer alice = ui.player("Alice");
        Screen modePicker = view.picker();
        assertEquals(Msg.ERROR_NO_PERMISSION,
                click(button(modePicker, "Team Deathmatch"), alice).orElseThrow().messageKey());
        Screen arenaPicker = clickInto(modePicker, "Team Deathmatch");
        assertEquals(Msg.ERROR_NO_PERMISSION, click(button(arenaPicker, "BR-TDM"), alice).orElseThrow().messageKey());
        FormScreen form = view.form(arena("AS-TDM"));
        assertEquals(Msg.ERROR_NO_PERMISSION, click(form.back(), alice).orElseThrow().messageKey());
    }

    @Test
    void aFormForAnArenaThatBecameBusyIsRefused() {
        FormScreen form = view.form(arena("BR-TDM"));
        ui.open("BR-TDM");
        assertEquals(Msg.ERROR_ARENA_BLOCKED, submit(form, staff,
                Map.of("teamCount", 2, "maxPlayers", 2, "midgameJoin", true)).orElseThrow().messageKey());
        assertEquals(1, ui.games().games().size());
    }

    @Test
    void startIsRefusedWhenTheArenasModeChangedSinceTheFormWasBuilt() throws IOException {
        FormScreen form = view.form(arena("AS-TDM"));
        arenaStore().save(arena("AS-TDM").withMode("ffa"));
        ui.services.arenas().load();

        assertEquals(Msg.ERROR_BUTTON_STALE, submit(form, staff,
                Map.of("maxPlayers", 2, "midgameJoin", true)).orElseThrow().messageKey());
        assertTrue(ui.games().games().isEmpty());
    }

    @Test
    void backFromTheFormReturnsToStepTwoOfTheSameMode() {
        FormScreen form = view.form(arena("AS-TDM"));
        assertTrue(click(form.back(), staff).isEmpty());
        MenuScreen arenaPicker = assertInstanceOf(MenuScreen.class, navigator.lastScreen());
        assertEquals("Start a game", plain(arenaPicker.title()));
        assertEquals(List.of("AS-TDM", "BR-TDM", "Back"), labels(arenaPicker));
    }
}
