package fr.famille.poker.engine;

import java.util.Objects;

/**
 * What a player wants to do. For {@code BET} and {@code RAISE}, {@code amount} is the total bet
 * for the round once the action is done ("raise to 60"), not the amount added.
 */
public record Action(ActionType type, int amount) {

    public Action {
        Objects.requireNonNull(type, "type");
    }

    public static Action fold() {
        return new Action(ActionType.FOLD, 0);
    }

    public static Action check() {
        return new Action(ActionType.CHECK, 0);
    }

    public static Action call() {
        return new Action(ActionType.CALL, 0);
    }

    public static Action bet(int amount) {
        return new Action(ActionType.BET, amount);
    }

    public static Action raise(int amount) {
        return new Action(ActionType.RAISE, amount);
    }
}
