package fr.famille.poker.server;

import java.util.List;

/** The winner of a game in the leaderboard of the month, right after the game: shown on the victory screen. */
public record Standing(int wins, int rank, List<LeaderboardEntry> top) {

    public Standing {
        top = List.copyOf(top);
    }
}
