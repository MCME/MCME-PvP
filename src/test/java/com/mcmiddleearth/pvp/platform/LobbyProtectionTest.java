package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.testing.PaperFixture;
import com.mcmiddleearth.pvp.testing.TestArenas;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.LivingEntityMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 1.1.2's protected lobby for players outside games, behind the lobbyProtection switch. */
class LobbyProtectionTest {

    @TempDir
    Path dir;

    private ServerMock server;
    private PaperFixture fixture;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void lobbyPlayersTakeNoDamage() {
        init(PvpConfig.defaults());
        PlayerMock dave = fixture.player("Dave");
        PlayerMock erin = fixture.player("Erin");

        assertTrue(fire(dave, DamageCause.FALL).isCancelled());
        assertTrue(fire(dave, DamageCause.FIRE).isCancelled());
        assertTrue(dave.simulateDamage(5, erin).isCancelled());
        assertEquals(20, dave.getHealth());
    }

    @Test
    void theVoidAndKillStillWork() {
        init(PvpConfig.defaults());
        PlayerMock dave = fixture.player("Dave");

        assertFalse(fire(dave, DamageCause.VOID).isCancelled());
        assertFalse(fire(dave, DamageCause.KILL).isCancelled());
    }

    @Test
    void lavaAndFireHurtWhenLavaDamageIsOn() {
        init(PvpConfig.defaults());
        PlayerMock dave = fixture.player("Dave");

        assertFalse(fire(dave, DamageCause.LAVA).isCancelled());
        assertFalse(fire(dave, DamageCause.FIRE_TICK).isCancelled());
    }

    @Test
    void lavaAndFireAreHarmlessWhenLavaDamageIsOff() {
        init(PaperFixture.config(false, true));
        PlayerMock dave = fixture.player("Dave");

        assertTrue(fire(dave, DamageCause.LAVA).isCancelled());
        assertTrue(fire(dave, DamageCause.FIRE_TICK).isCancelled());
    }

    @Test
    void lobbyPlayersCannotHurtAnythingByHandOrBow() {
        init(PvpConfig.defaults());
        PlayerMock dave = fixture.player("Dave");
        Location at = dave.getLocation();
        LivingEntityMock zombie = (LivingEntityMock) at.getWorld().spawn(at, Zombie.class);
        Arrow arrow = at.getWorld().spawn(at, Arrow.class);
        arrow.setShooter(dave);

        assertTrue(zombie.simulateDamage(5, dave).isCancelled());
        assertTrue(zombie.simulateDamage(5, arrow).isCancelled());
    }

    @Test
    void aCreativeLobbyPlayerCanDamageDecorationsAndMobs() {
        init(PvpConfig.defaults());
        PlayerMock dave = fixture.player("Dave");
        dave.setGameMode(GameMode.CREATIVE);
        Location at = dave.getLocation();
        LivingEntityMock zombie = (LivingEntityMock) at.getWorld().spawn(at, Zombie.class);
        LivingEntityMock stand = (LivingEntityMock) at.getWorld().spawn(at, ArmorStand.class);

        assertFalse(zombie.simulateDamage(5, dave).isCancelled());
        assertFalse(stand.simulateDamage(5, dave).isCancelled());
    }

    @Test
    void aCreativeLobbyPlayerStillCannotDamageAPlayer() {
        init(PvpConfig.defaults());
        PlayerMock dave = fixture.player("Dave");
        dave.setGameMode(GameMode.CREATIVE);
        PlayerMock erin = fixture.player("Erin");
        PlayerMock alice = fixture.player("Alice");
        fixture.join(fixture.open("AS-TDM"), alice);

        assertTrue(erin.simulateDamage(5, dave).isCancelled());
        assertTrue(alice.simulateDamage(5, dave).isCancelled());
    }

    @Test
    void anAdventureLobbyPlayerIsStillBlocked() {
        init(PvpConfig.defaults());
        PlayerMock dave = fixture.player("Dave");
        dave.setGameMode(GameMode.ADVENTURE);
        Location at = dave.getLocation();
        LivingEntityMock zombie = (LivingEntityMock) at.getWorld().spawn(at, Zombie.class);

        assertTrue(zombie.simulateDamage(5, dave).isCancelled());
    }

    @Test
    void lobbyPlayersDoNotGetHungry() {
        init(PvpConfig.defaults());

        assertEquals(20, hunger(fixture.player("Dave")).getFoodLevel());
    }

    @Test
    void playersInAGameAreLeftToTheRouter() {
        init(PvpConfig.defaults());
        PlayerMock alice = fixture.player("Alice");
        fixture.join(fixture.open("AS-TDM"), alice);
        Location at = alice.getLocation();
        LivingEntityMock zombie = (LivingEntityMock) at.getWorld().spawn(at, Zombie.class);

        assertFalse(fire(alice, DamageCause.FALL).isCancelled());
        assertEquals(5, hunger(alice).getFoodLevel());
        assertFalse(zombie.simulateDamage(5, alice).isCancelled());
    }

    @Test
    void aLobbyPlayerCannotHitAPlayerInAGame() {
        init(PvpConfig.defaults());
        PlayerMock alice = fixture.player("Alice");
        PlayerMock dave = fixture.player("Dave");
        fixture.join(fixture.open("AS-TDM"), alice);

        assertTrue(alice.simulateDamage(5, dave).isCancelled());
    }

    @Test
    void switchedOffItTouchesNobody() {
        init(PaperFixture.config(true, false));
        PlayerMock dave = fixture.player("Dave");
        PlayerMock erin = fixture.player("Erin");

        assertFalse(fire(dave, DamageCause.FALL).isCancelled());
        assertFalse(erin.simulateDamage(5, dave).isCancelled());
        assertEquals(5, hunger(dave).getFoodLevel());
    }

    private void init(PvpConfig config) {
        fixture = PaperFixture.create(server, dir, config, TestArenas.tdm("AS-TDM", 0, 2));
        fixture.register(new LobbyProtection(fixture.services()));
    }

    /** Environmental damage; Paper only offers deprecated constructors for it, so tests build it this way. */
    @SuppressWarnings("deprecation")
    private EntityDamageEvent fire(Player victim, DamageCause cause) {
        EntityDamageEvent event = new EntityDamageEvent(victim, cause, DamageSource.builder(DamageType.GENERIC).build(), 2);
        server.getPluginManager().callEvent(event);
        return event;
    }

    private FoodLevelChangeEvent hunger(Player player) {
        FoodLevelChangeEvent event = new FoodLevelChangeEvent(player, 5);
        server.getPluginManager().callEvent(event);
        return event;
    }
}
