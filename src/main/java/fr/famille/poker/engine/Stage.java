package fr.famille.poker.engine;

public enum Stage {
    PRE_FLOP,
    FLOP,
    TURN,
    RIVER,
    /** Pots paid, after a showdown or when everyone else folded. */
    COMPLETE
}
