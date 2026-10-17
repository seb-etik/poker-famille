package fr.famille.poker.server;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * A game won to the end, as stored in results.json. Nicknames are kept as typed; they are compared
 * through {@link Leaderboard#key(String)}.
 */
public record GameRecord(Instant endedAt, List<String> players, String winner, Map<String, String> avatars) {

    public GameRecord {
        players = List.copyOf(players);
        avatars = avatars == null ? Map.of() : Map.copyOf(avatars);
    }
}
