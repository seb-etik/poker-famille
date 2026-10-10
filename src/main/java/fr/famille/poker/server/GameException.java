package fr.famille.poker.server;

import fr.famille.poker.server.message.ErrorCode;

/** A refused request; its message is shown to the player as is, in French. */
public class GameException extends RuntimeException {

    private final ErrorCode code;

    public GameException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
