package fr.famille.poker.server;

import java.text.Normalizer;
import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The family leaderboard, computed from the history of games each time it is read. */
public final class Leaderboard {

    static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    private Leaderboard() {
    }

    /** A player is their nickname, compared without case, accents or surrounding spaces. */
    public static String key(String nickname) {
        String withoutAccents = Normalizer.normalize(nickname.strip(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT);
    }

    public static List<GameRecord> currentMonth(List<GameRecord> games, Clock clock) {
        YearMonth month = YearMonth.now(clock.withZone(PARIS));
        return games.stream().filter(game -> YearMonth.from(game.endedAt().atZone(PARIS)).equals(month)).toList();
    }

    /** Sorted by wins, then win percentage, then games; players tied on all three share their rank. */
    public static List<LeaderboardEntry> compute(List<GameRecord> games) {
        Map<String, Tally> tallies = new LinkedHashMap<>();
        games.stream()
                .filter(game -> game.players().stream().map(Leaderboard::key).distinct().count() >= 2)
                .sorted(Comparator.comparing(GameRecord::endedAt))
                .forEach(game -> {
                    String winner = key(game.winner());
                    for (String player : game.players()) {
                        Tally tally = tallies.computeIfAbsent(key(player), k -> new Tally());
                        tally.games++;
                        tally.nickname = player.strip();
                        tally.avatar = game.avatars().getOrDefault(player, tally.avatar);
                        if (key(player).equals(winner)) {
                            tally.wins++;
                        }
                    }
                });

        List<Tally> sorted = tallies.values().stream()
                .sorted(Comparator.comparingInt((Tally t) -> t.wins).reversed()
                        .thenComparing(Comparator.comparingDouble(Tally::percentage).reversed())
                        .thenComparing(Comparator.comparingInt((Tally t) -> t.games).reversed())
                        .thenComparing(t -> key(t.nickname)))
                .toList();

        List<LeaderboardEntry> entries = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            Tally tally = sorted.get(i);
            boolean tied = i > 0 && sorted.get(i - 1).wins == tally.wins && sorted.get(i - 1).games == tally.games;
            int rank = tied ? entries.get(i - 1).rank() : i + 1;
            entries.add(new LeaderboardEntry(tally.nickname, tally.avatar, tally.wins, tally.games, rank));
        }
        return entries;
    }

    private static final class Tally {
        String nickname;
        String avatar;
        int wins;
        int games;

        double percentage() {
            return games == 0 ? 0 : (double) wins / games;
        }
    }
}
