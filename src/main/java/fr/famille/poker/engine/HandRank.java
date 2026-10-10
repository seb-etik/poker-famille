package fr.famille.poker.engine;

import java.util.List;
import java.util.Objects;

/**
 * Value of a five-card hand: its category, then the ranks that break ties within that category
 * (for a two pair: top pair, bottom pair, kicker; for a straight: its highest card).
 *
 * <p>Two hands tie when {@link #compareTo} returns 0, even if {@link #bestCards} differ by suit:
 * use {@code compareTo}, not {@code equals}, to compare hands.
 */
public record HandRank(HandCategory category, List<Rank> tiebreakers, List<Card> bestCards)
        implements Comparable<HandRank> {

    public HandRank {
        Objects.requireNonNull(category, "category");
        tiebreakers = List.copyOf(tiebreakers);
        bestCards = List.copyOf(bestCards);
    }

    @Override
    public int compareTo(HandRank other) {
        int byCategory = category.compareTo(other.category);
        if (byCategory != 0) {
            return byCategory;
        }
        for (int i = 0; i < Math.min(tiebreakers.size(), other.tiebreakers.size()); i++) {
            int byRank = tiebreakers.get(i).compareTo(other.tiebreakers.get(i));
            if (byRank != 0) {
                return byRank;
            }
        }
        return 0;
    }
}
