package fr.famille.poker.engine;

/** An action refused by the rules. The message is meant for the player, in French. */
public class IllegalActionException extends RuntimeException {

    public IllegalActionException(String message) {
        super(message);
    }
}
