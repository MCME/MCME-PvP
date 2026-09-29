package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.Permissions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerLockTest {

    @Test
    void startsInTheConfiguredState() {
        assertTrue(new ServerLock(true).locked());
        assertFalse(new ServerLock(false).locked());
    }

    @Test
    void locksAndUnlocks() {
        ServerLock lock = new ServerLock(false);
        lock.lock();
        assertTrue(lock.locked());
        lock.unlock();
        assertFalse(lock.locked());
    }

    @Test
    void keepsOutLockedPlayersWithoutThePermission() {
        ServerLock lock = new ServerLock(true);

        assertTrue(lock.keepsOut(permission -> false));
        assertFalse(lock.keepsOut(Permissions.JOIN_LOCKED::equals));
    }

    @Test
    void unlockedKeepsOutNobody() {
        ServerLock lock = new ServerLock(false);

        assertFalse(lock.keepsOut(permission -> false));
    }
}
