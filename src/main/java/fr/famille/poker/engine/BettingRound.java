package fr.famille.poker.engine;

import java.util.HashSet;
import java.util.Set;

/**
 * One betting round: the bet to match, the minimum raise, who has acted since the last raise,
 * and who may still raise.
 */
final class BettingRound {

    private int currentBet;
    private int minRaise;
    private final Set<Seat> actedSinceLastRaise = new HashSet<>();
    private final Set<Seat> raiseClosed = new HashSet<>();

    BettingRound(int currentBet, int minRaise) {
        this.currentBet = currentBet;
        this.minRaise = minRaise;
    }

    int currentBet() {
        return currentBet;
    }

    /** Smallest raise increment: the big blind, then the size of the last full raise. */
    int minRaise() {
        return minRaise;
    }

    boolean hasActed(Seat seat) {
        return actedSinceLastRaise.contains(seat);
    }

    boolean mayRaise(Seat seat) {
        return !raiseClosed.contains(seat);
    }

    void recordAction(Seat seat) {
        actedSinceLastRaise.add(seat);
        raiseClosed.add(seat);
    }

    /**
     * Raises the bet to {@code newBet}. Everyone must act again, but only a full raise lets
     * players who already acted raise again: an all-in for less than the minimum raise does not.
     */
    void recordRaise(int newBet) {
        int increment = newBet - currentBet;
        if (increment >= minRaise) {
            minRaise = increment;
            raiseClosed.clear();
        }
        currentBet = newBet;
        actedSinceLastRaise.clear();
    }
}
