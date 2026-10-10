package fr.famille.poker.engine;

import java.util.Objects;

/** A playing card, written as a short code: rank symbol then suit symbol ("AS", "TD", "9H"). */
public record Card(Rank rank, Suit suit) {

    public Card {
        Objects.requireNonNull(rank, "rank");
        Objects.requireNonNull(suit, "suit");
    }

    public static Card of(String code) {
        if (code == null || code.length() != 2) {
            throw new IllegalArgumentException("Invalid card code: " + code);
        }
        return new Card(Rank.fromSymbol(code.charAt(0)), Suit.fromSymbol(code.charAt(1)));
    }

    public String code() {
        return "" + rank.symbol() + suit.symbol();
    }

    @Override
    public String toString() {
        return code();
    }
}
