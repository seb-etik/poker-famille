package fr.famille.poker.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class LeaderboardTest {

    private static GameRecord game(String endedAt, String winner, String... players) {
        return new GameRecord(Instant.parse(endedAt), List.of(players), winner, Map.of());
    }

    @Test
    void aNicknameIsComparedWithoutCaseAccentsOrSurroundingSpaces() {
        assertEquals(Leaderboard.key("Mamie"), Leaderboard.key(" mamie  "));
        assertEquals(Leaderboard.key("Zoé"), Leaderboard.key("ZOE"));
    }

    @Test
    void countsWinsAndGamesPerPlayer() {
        List<LeaderboardEntry> entries = Leaderboard.compute(List.of(
                game("2026-10-01T20:00:00Z", "Seb", "Seb", "Mamie"),
                game("2026-10-02T20:00:00Z", "mamie", "Seb", "mamie", "Tessa"),
                game("2026-10-03T20:00:00Z", "Seb", "Seb", "Tessa")));

        assertEquals(List.of("Seb", "mamie", "Tessa"), entries.stream().map(LeaderboardEntry::nickname).toList());
        assertEquals(List.of(2, 1, 0), entries.stream().map(LeaderboardEntry::wins).toList());
        assertEquals(List.of(3, 2, 2), entries.stream().map(LeaderboardEntry::games).toList());
    }

    @Test
    void sortsByWinsThenPercentageThenGames() {
        List<LeaderboardEntry> entries = Leaderboard.compute(List.of(
                // A: 1 win out of 1. B: 1 win out of 2. C: 1 win out of 3 → same wins, by percentage.
                game("2026-10-01T20:00:00Z", "A", "A", "X"),
                game("2026-10-02T20:00:00Z", "B", "B", "X"),
                game("2026-10-03T20:00:00Z", "X", "B", "X"),
                game("2026-10-04T20:00:00Z", "C", "C", "X"),
                game("2026-10-05T20:00:00Z", "X", "C", "X"),
                game("2026-10-06T20:00:00Z", "X", "C", "X")));

        assertEquals(List.of("X", "A", "B", "C"), entries.stream().map(LeaderboardEntry::nickname).toList());
    }

    @Test
    void playersTiedOnEverythingShareTheirRank() {
        List<LeaderboardEntry> entries = Leaderboard.compute(List.of(
                game("2026-10-01T20:00:00Z", "A", "A", "B"),
                game("2026-10-02T20:00:00Z", "B", "A", "B"),
                game("2026-10-03T20:00:00Z", "C", "C", "D")));

        // A, B and C: 1 win out of 2, 2, 1 → C first (100 %), A and B tied, D last.
        assertEquals(List.of(1, 2, 2, 4), entries.stream().map(LeaderboardEntry::rank).toList());
    }

    @Test
    void aGameWithASinglePlayerDoesNotCount() {
        assertEquals(List.of(), Leaderboard.compute(List.of(game("2026-10-01T20:00:00Z", "Seb", "Seb"))));
    }

    @Test
    void showsTheNicknameAndAvatarOfTheLastGame() {
        List<LeaderboardEntry> entries = Leaderboard.compute(List.of(
                new GameRecord(Instant.parse("2026-10-01T20:00:00Z"), List.of("mamie", "Seb"), "mamie",
                        Map.of("mamie", "fox")),
                new GameRecord(Instant.parse("2026-10-05T20:00:00Z"), List.of("Mamie", "Seb"), "Seb",
                        Map.of("Mamie", "lorelei:famille-2"))));

        LeaderboardEntry mamie = entries.stream().filter(e -> e.wins() == 1 && e.nickname().equalsIgnoreCase("mamie"))
                .findFirst().orElseThrow();
        assertEquals("Mamie", mamie.nickname());
        assertEquals("lorelei:famille-2", mamie.avatar());
    }

    @Test
    void theMonthIsTheCalendarMonthInParis() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-17T10:00:00Z"), ZoneOffset.UTC);
        List<GameRecord> games = List.of(
                // 30 September 23:30 UTC is already 1 October in Paris.
                game("2026-09-30T23:30:00Z", "Seb", "Seb", "Mamie"),
                game("2026-09-30T20:00:00Z", "Mamie", "Seb", "Mamie"));

        List<GameRecord> month = Leaderboard.currentMonth(games, clock);

        assertEquals(1, month.size());
        assertEquals("Seb", month.getFirst().winner());
    }
}
