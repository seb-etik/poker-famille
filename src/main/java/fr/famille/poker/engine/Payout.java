package fr.famille.poker.engine;

import java.util.Map;

/** How one pot was shared: chips won by seat number. */
public record Payout(Pot pot, Map<Integer, Integer> shares) {

    public Payout {
        shares = Map.copyOf(shares);
    }
}
