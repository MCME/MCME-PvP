package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.Permissions;

import java.util.function.Predicate;

/**
 * Whether PvP is locked. While it is locked, only players with {@code pvp.join.locked} may stay on the server.
 * Plain state, read and changed on the server thread. 1.1.2 started locked after every restart ({@code startLocked}).
 */
public final class ServerLock {

    private boolean locked;

    public ServerLock(boolean locked) {
        this.locked = locked;
    }

    public boolean locked() {
        return locked;
    }

    public void lock() {
        locked = true;
    }

    public void unlock() {
        locked = false;
    }

    /**
     * Locked, and {@code hasPermission} says no to {@code pvp.join.locked}: joining, spectating and staying are
     * all refused the same way.
     */
    public boolean keepsOut(Predicate<String> hasPermission) {
        return locked && !hasPermission.test(Permissions.JOIN_LOCKED);
    }
}
