package fr.famille.poker.engine;

public enum SeatStatus {
    /** In the hand and still able to bet. */
    ACTIVE,
    FOLDED,
    /** In the hand with no chips left: no more decisions until the showdown. */
    ALL_IN,
    /** Not dealt in this hand: empty seat, no chips, or arrived during the hand. */
    SITTING_OUT
}
