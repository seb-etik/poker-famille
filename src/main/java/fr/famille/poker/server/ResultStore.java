package fr.famille.poker.server;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * The history of the games won to the end, kept in memory: the leaderboard starts again when the server
 * restarts (a redeploy). No persistence on purpose: a volume on the cluster would cost more than it is worth.
 */
@ApplicationScoped
public class ResultStore implements ResultBook {

    private final Clock clock;
    private final List<GameRecord> games = new ArrayList<>();

    public ResultStore() {
        this(Clock.systemUTC());
    }

    ResultStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public synchronized Standing recordWin(GameRecord game) {
        games.add(game);
        List<LeaderboardEntry> month = Leaderboard.compute(Leaderboard.currentMonth(games, clock));
        String winner = Leaderboard.key(game.winner());
        LeaderboardEntry entry = month.stream()
                .filter(e -> Leaderboard.key(e.nickname()).equals(winner))
                .findFirst()
                .orElseThrow();
        return new Standing(entry.wins(), entry.rank(), month.subList(0, Math.min(3, month.size())));
    }

    public synchronized List<LeaderboardEntry> leaderboard(LeaderboardPeriod period) {
        List<GameRecord> selected = period == LeaderboardPeriod.MONTH ? Leaderboard.currentMonth(games, clock) : games;
        return Leaderboard.compute(selected);
    }
}
