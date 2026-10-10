package fr.famille.poker.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** Finds the best five-card hand among 5 to 7 cards. */
public final class HandEvaluator {

    private static final int HAND_SIZE = 5;
    private static final int MAX_CARDS = 7;

    private HandEvaluator() {
    }

    public static HandRank evaluate(Collection<Card> cards) {
        List<Card> all = List.copyOf(cards);
        if (all.size() < HAND_SIZE || all.size() > MAX_CARDS) {
            throw new IllegalArgumentException("Expected 5 to 7 cards, got " + all.size());
        }
        if (new HashSet<>(all).size() != all.size()) {
            throw new IllegalArgumentException("Duplicate cards: " + all);
        }

        // At most C(7,5) = 21 combinations: trying them all keeps the code obvious.
        HandRank best = null;
        for (int mask = 0; mask < 1 << all.size(); mask++) {
            if (Integer.bitCount(mask) != HAND_SIZE) {
                continue;
            }
            List<Card> five = new ArrayList<>(HAND_SIZE);
            for (int i = 0; i < all.size(); i++) {
                if ((mask & 1 << i) != 0) {
                    five.add(all.get(i));
                }
            }
            HandRank candidate = evaluateFive(five);
            if (best == null || candidate.compareTo(best) > 0) {
                best = candidate;
            }
        }
        return best;
    }

    private static HandRank evaluateFive(List<Card> cards) {
        Map<Rank, Integer> counts = new EnumMap<>(Rank.class);
        for (Card card : cards) {
            counts.merge(card.rank(), 1, Integer::sum);
        }
        // Biggest group first, then highest rank: "K K K 4 4", "A A 9 9 K", "A J 8 4 2".
        Comparator<Rank> byGroup = Comparator.<Rank>comparingInt(counts::get)
                .thenComparing(Comparator.naturalOrder())
                .reversed();
        List<Rank> groups = counts.keySet().stream().sorted(byGroup).toList();
        List<Card> ordered = cards.stream().sorted(Comparator.comparing(Card::rank, byGroup)).toList();

        boolean flush = cards.stream().map(Card::suit).distinct().count() == 1;
        Rank straightHigh = straightHigh(groups);
        int largestGroup = counts.get(groups.getFirst());

        // Five distinct ranks: a straight can be neither a full house nor four of a kind,
        // and a straight that is also a flush is a straight flush.
        if (straightHigh != null) {
            HandCategory category = flush ? HandCategory.STRAIGHT_FLUSH : HandCategory.STRAIGHT;
            List<Card> straightCards = straightHigh == Rank.FIVE ? aceLast(ordered) : ordered;
            return new HandRank(category, List.of(straightHigh), straightCards);
        }
        if (largestGroup == 4) {
            return new HandRank(HandCategory.FOUR_OF_A_KIND, groups, ordered);
        }
        if (largestGroup == 3 && groups.size() == 2) {
            return new HandRank(HandCategory.FULL_HOUSE, groups, ordered);
        }
        if (flush) {
            return new HandRank(HandCategory.FLUSH, groups, ordered);
        }
        if (largestGroup == 3) {
            return new HandRank(HandCategory.THREE_OF_A_KIND, groups, ordered);
        }
        if (largestGroup == 2 && groups.size() == 3) {
            return new HandRank(HandCategory.TWO_PAIR, groups, ordered);
        }
        if (largestGroup == 2) {
            return new HandRank(HandCategory.ONE_PAIR, groups, ordered);
        }
        return new HandRank(HandCategory.HIGH_CARD, groups, ordered);
    }

    /** Highest card of the straight formed by five distinct ranks sorted high to low, or null. */
    private static Rank straightHigh(List<Rank> ranks) {
        if (ranks.size() != HAND_SIZE) {
            return null;
        }
        if (ranks.getFirst().value() - ranks.getLast().value() == HAND_SIZE - 1) {
            return ranks.getFirst();
        }
        // The wheel A-2-3-4-5: the ace plays low, so the straight is five-high.
        if (ranks.equals(List.of(Rank.ACE, Rank.FIVE, Rank.FOUR, Rank.THREE, Rank.TWO))) {
            return Rank.FIVE;
        }
        return null;
    }

    private static List<Card> aceLast(List<Card> cards) {
        List<Card> reordered = new ArrayList<>(cards.subList(1, cards.size()));
        reordered.add(cards.getFirst());
        return reordered;
    }
}
