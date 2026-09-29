package com.mcmiddleearth.pvp.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The sessions of one game in join order, and the units that joined together (parties stay together at the
 * countdown).
 */
final class Roster {

    private final Map<UUID, PlayerSession> sessions = new LinkedHashMap<>();
    private final List<List<UUID>> units = new ArrayList<>();

    /** A new session for the player, or their earlier one present again. */
    PlayerSession activate(PlayerRef player) {
        PlayerSession session = sessions.get(player.id());
        if (session == null) {
            session = new PlayerSession(player, sessions.size());
            sessions.put(player.id(), session);
        } else if (session.present()) {
            throw new IllegalStateException(player.name() + " is already in this game");
        } else {
            session.reactivate(player);
        }
        return session;
    }

    void addUnit(JoinUnit unit) {
        units.add(new ArrayList<>(unit.members().stream().map(PlayerRef::id).toList()));
    }

    void leaveUnit(UUID player) {
        units.forEach(unit -> unit.remove(player));
        units.removeIf(List::isEmpty);
    }

    /** The units, with only their present players; present players outside any unit count as units of one. */
    List<List<PlayerSession>> playingUnits() {
        List<List<PlayerSession>> result = new ArrayList<>();
        Set<UUID> covered = new HashSet<>();
        for (List<UUID> unit : units) {
            List<PlayerSession> members = unit.stream()
                    .map(sessions::get)
                    .filter(session -> session != null && session.present() && session.role() == Role.PLAYER)
                    .toList();
            if (!members.isEmpty()) {
                result.add(members);
                members.forEach(member -> covered.add(member.id()));
            }
        }
        players().stream()
                .filter(session -> !covered.contains(session.id()))
                .forEach(session -> result.add(List.of(session)));
        return result;
    }

    Optional<PlayerSession> session(UUID player) {
        return Optional.ofNullable(sessions.get(player));
    }

    Optional<PlayerSession> present(UUID player) {
        return session(player).filter(PlayerSession::present);
    }

    List<PlayerSession> all() {
        return List.copyOf(sessions.values());
    }

    List<PlayerSession> present() {
        return sessions.values().stream().filter(PlayerSession::present).toList();
    }

    List<PlayerSession> players() {
        return sessions.values().stream().filter(s -> s.present() && s.role() == Role.PLAYER).toList();
    }

    List<PlayerSession> spectators() {
        return sessions.values().stream().filter(s -> s.present() && s.role() == Role.SPECTATOR).toList();
    }
}
