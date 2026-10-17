package fr.famille.poker.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ResultStoreTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-17T10:00:00Z"), ZoneOffset.UTC);

    private static GameRecord win(String winner, String... players) {
        return new GameRecord(CLOCK.instant(), List.of(players), winner, Map.of(winner, "fox"));
    }

    @Test
    void startsEmpty() {
        assertEquals(List.of(), new ResultStore(CLOCK).leaderboard(LeaderboardPeriod.ALL));
    }

    @Test
    void addsEachGameToTheLeaderboard() {
        ResultStore store = new ResultStore(CLOCK);

        store.recordWin(win("Seb", "Seb", "Mamie"));
        store.recordWin(win("Mamie", "Seb", "Mamie"));

        assertEquals(2, store.leaderboard(LeaderboardPeriod.ALL).getFirst().games());
        assertEquals(1, store.leaderboard(LeaderboardPeriod.MONTH).getFirst().wins());
    }

    @Test
    void returnsTheWinnersStandingOfTheMonth() {
        ResultStore store = new ResultStore(CLOCK);
        store.recordWin(win("Mamie", "Seb", "Mamie"));
        store.recordWin(win("Mamie", "Seb", "Mamie"));

        Standing standing = store.recordWin(win("Seb", "Seb", "Mamie"));

        assertEquals(1, standing.wins());
        assertEquals(2, standing.rank());
        assertEquals(List.of("Mamie", "Seb"), standing.top().stream().map(LeaderboardEntry::nickname).toList());
    }
}
