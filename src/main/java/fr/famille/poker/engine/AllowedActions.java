package fr.famille.poker.engine;

import java.util.Set;

/**
 * What the player to act may do. {@code callAmount} is the chips a call adds; {@code minAmount}
 * and {@code maxAmount} bound a bet or raise, as a total for the round (see {@link Action}).
 */
public record AllowedActions(Set<ActionType> types, int callAmount, int minAmount, int maxAmount) {

    public AllowedActions {
        types = Set.copyOf(types);
    }

    static AllowedActions none() {
        return new AllowedActions(Set.of(), 0, 0, 0);
    }
}
