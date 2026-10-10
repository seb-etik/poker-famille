package fr.famille.poker.engine;

import java.util.List;
import java.util.Map;

/** End of a hand: who won each pot, and the hands shown (empty when everyone else folded). */
public record HandResult(List<Payout> payouts, Map<Integer, HandRank> shownHands) {

    public HandResult {
        payouts = List.copyOf(payouts);
        shownHands = Map.copyOf(shownHands);
    }

    /** Chips a seat won from the pots, all pots together. */
    public int winnings(int seatNumber) {
        return payouts.stream().mapToInt(payout -> payout.shares().getOrDefault(seatNumber, 0)).sum();
    }
}
