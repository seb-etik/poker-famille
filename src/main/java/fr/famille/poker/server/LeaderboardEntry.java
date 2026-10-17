package fr.famille.poker.server;

/** One line of the family leaderboard; {@code nickname} and {@code avatar} come from the player's last game. */
public record LeaderboardEntry(String nickname, String avatar, int wins, int games, int rank) {
}
