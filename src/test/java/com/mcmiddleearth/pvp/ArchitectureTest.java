package com.mcmiddleearth.pvp;

import com.mcmiddleearth.pvp.archfixture.CallsTheServer;
import com.mcmiddleearth.pvp.archfixture.ExtendsBukkitRunnable;
import com.mcmiddleearth.pvp.archfixture.HoldsTheServer;
import com.mcmiddleearth.pvp.archfixture.MutableCounter;
import com.mcmiddleearth.pvp.archfixture.OpensDialogs;
import com.mcmiddleearth.pvp.archfixture.ReadsRegistries;
import com.mcmiddleearth.pvp.archfixture.SchedulesTasks;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The package architecture rules, checked with ArchUnit's plain API in ordinary tests. Each
 * rule is built by a method that takes the packages it governs, so the same rule also runs against small violating
 * classes from the test sources: a rule that cannot fail would otherwise pass unnoticed.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArchitectureTest {

    private static final String ROOT = "com.mcmiddleearth.pvp";
    /** The plain-Java core: it reaches the server only through GameContext and the other seams. */
    private static final String[] CORE = {ROOT + ".game..", ROOT + ".mode..", ROOT + ".arena..", ROOT + ".stats..",
            ROOT + ".team..", ROOT + ".ui.."};
    private static final String UI = ROOT + ".ui..";
    /** Where the deliberately violating test classes live. */
    private static final String FIXTURES = ROOT + ".archfixture..";

    private JavaClasses production;

    @BeforeAll
    void importTheProductionClasses() {
        production = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT);
    }

    @Test
    void onlyProductionClassesAreChecked() {
        assertTrue(production.contain(PvpPlugin.class));
        assertTrue(production.containPackage(ROOT + ".arena"));
        assertFalse(production.contain(MutableCounter.class), "test classes are not imported");
    }

    @Test
    void noStaticFieldIsMutable() {
        noMutableStaticFields().check(production);
    }

    @Test
    void theCoreDoesNotUseTheServerSingletons() {
        noServerSingletons(CORE).check(production);
    }

    @Test
    void theUiModelDoesNotUsePaperDialogTypes() {
        noPaperDialogTypes(UI).check(production);
    }

    @Test
    void theStaticFieldRuleCatchesAMutableCounter() {
        AssertionError e = assertThrows(AssertionError.class,
                () -> noMutableStaticFields().check(only(MutableCounter.class)));

        assertTrue(e.getMessage().contains("MutableCounter.count"), e.getMessage());
    }

    @Test
    void theServerRuleCatchesBukkitAndItsScheduler() {
        assertThrows(AssertionError.class, () -> noServerSingletons(FIXTURES).check(only(CallsTheServer.class)));
        assertThrows(AssertionError.class, () -> noServerSingletons(FIXTURES).check(only(SchedulesTasks.class)));
        assertThrows(AssertionError.class, () -> noServerSingletons(FIXTURES).check(only(ExtendsBukkitRunnable.class)));
        assertThrows(AssertionError.class, () -> noServerSingletons(FIXTURES).check(only(HoldsTheServer.class)));
    }

    @Test
    void thePackageListsNameTheSixCorePackages() {
        assertArrayEquals(new String[] {ROOT + ".game..", ROOT + ".mode..", ROOT + ".arena..", ROOT + ".stats..",
                ROOT + ".team..", ROOT + ".ui.."}, CORE);
        assertEquals(ROOT + ".ui..", UI);
    }

    @Test
    void theDialogRuleCatchesDialogAndRegistryTypes() {
        assertThrows(AssertionError.class, () -> noPaperDialogTypes(FIXTURES).check(only(OpensDialogs.class)));
        assertThrows(AssertionError.class, () -> noPaperDialogTypes(FIXTURES).check(only(ReadsRegistries.class)));
    }

    /** All state is reachable from PvpPlugin.onEnable; constants are static final. */
    private static ArchRule noMutableStaticFields() {
        return fields().that().areStatic().should().beFinal()
                .because("all state is reachable from PvpPlugin.onEnable, never from a static field");
    }

    private static ArchRule noServerSingletons(String... packages) {
        return noClasses().that().resideInAnyPackage(packages)
                .should().dependOnClassesThat()
                        .belongToAnyOf(Bukkit.class, BukkitScheduler.class, BukkitRunnable.class, Server.class)
                .because("the core reaches the server only through GameContext, so it runs without a server in tests")
                .allowEmptyShould(true);
    }

    private static ArchRule noPaperDialogTypes(String... packages) {
        return noClasses().that().resideInAnyPackage(packages)
                .should().dependOnClassesThat().resideInAnyPackage("io.papermc.paper.dialog..", "io.papermc.paper.registry..")
                .because("dialogs cannot be built in tests; only platform.ui turns screens into Paper dialogs")
                .allowEmptyShould(true);
    }

    private static JavaClasses only(Class<?> type) {
        return new ClassFileImporter().importClasses(type);
    }
}
