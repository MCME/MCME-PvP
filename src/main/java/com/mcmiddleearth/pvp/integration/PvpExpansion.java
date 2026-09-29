package com.mcmiddleearth.pvp.integration;

import com.mcmiddleearth.pvp.Permissions;
import com.mcmiddleearth.pvp.game.GameManager;
import com.mcmiddleearth.pvp.game.PlayerSession;
import com.mcmiddleearth.pvp.game.Role;
import com.mcmiddleearth.pvp.team.TeamSpec;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * The PlaceholderAPI expansion {@code mcmepvp} that the chat plugin uses to prefix names: {@code %mcmepvp_prefix%}
 * and {@code %mcmepvp_color%}, with exactly 1.1.2's texts. The value follows the player's session in their game.
 *
 * <p>PlaceholderAPI may call {@link #onRequest} off the server thread, from async chat or tab-list plugins, so
 * each shared value here is read once: a transition mid-call can give a stale label but never an exception.
 */
public final class PvpExpansion extends PlaceholderExpansion {

    private static final String RESET = "§r";
    private static final String GREEN = "§a";
    private static final String GRAY = "§7";
    private static final String GOLD = "§6";

    private final GameManager games;
    private final String version;

    public PvpExpansion(GameManager games, String version) {
        this.games = games;
        this.version = version;
    }

    @Override
    public String getIdentifier() {
        return "mcmepvp";
    }

    @Override
    public String getAuthor() {
        return "Eriol_Eandur";
    }

    @Override
    public String getVersion() {
        return version;
    }

    /** Registered once by the plugin, so PlaceholderAPI must keep it over its own reloads. */
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offline, String params) {
        Player player = offline == null || !offline.isOnline() ? null : offline.getPlayer();
        if (player == null) {
            return "null player";
        }
        return switch (params) {
            case "prefix" -> {
                Label label = label(player);
                yield label.prefix() + " " + player.getName() + RESET + ": ";
            }
            case "color" -> label(player).color();
            default -> "";
        };
    }

    /** What precedes a name in chat ({@code §cRed}) and the color of the chat text after it. */
    private record Label(String prefix, String color) {
    }

    private Label label(Player player) {
        Optional<PlayerSession> session = games.gameOf(player.getUniqueId())
                .flatMap(game -> game.session(player.getUniqueId()));
        if (session.isEmpty()) {
            if (player.hasPermission(Permissions.ADMIN)) {
                return new Label(GOLD + "PvP Staff", GOLD);
            }
            // 1.1.2 gave managers a gold prefix but kept their chat gray.
            return player.hasPermission(Permissions.STAFF_RUN)
                    ? new Label(GOLD + "Manager", GRAY) : new Label(GRAY + "Lobby", GRAY);
        }
        PlayerSession s = session.get();
        if (s.role() == Role.SPECTATOR || !s.alive()) {
            return new Label(GRAY + "Spectator", GRAY);
        }
        // Read once: calling s.color() a second time could see a countdown cancel or a spectate switch in between,
        // turning a present color into an empty one and throwing from get().
        Optional<TeamSpec> color = s.color();
        if (color.isEmpty()) {
            return new Label(GREEN + "Participant", GREEN);
        }
        TeamSpec spec = color.get();
        String code = LegacyColors.code(spec.color());
        return new Label(code + (spec.personal() ? "Player" : spec.name()), code);
    }
}
