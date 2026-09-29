package com.mcmiddleearth.pvp.game;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Players who join together and always end up on the same team: one player, or a party. Each player once. */
public record JoinUnit(List<PlayerRef> members) {

    public JoinUnit {
        members = List.copyOf(Objects.requireNonNull(members, "members"));
        if (members.isEmpty()) {
            throw new IllegalArgumentException("a join unit needs at least one player");
        }
        // A player listed twice would be added to the game twice, which the roster refuses halfway through.
        Set<UUID> ids = new HashSet<>();
        for (PlayerRef member : members) {
            if (!ids.add(member.id())) {
                throw new IllegalArgumentException("a join unit lists " + member.name() + " twice");
            }
        }
    }

    public static JoinUnit solo(PlayerRef player) {
        return new JoinUnit(List.of(player));
    }

    public static JoinUnit party(List<PlayerRef> members) {
        return new JoinUnit(members);
    }

    public int size() {
        return members.size();
    }
}
