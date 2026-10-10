package fr.famille.poker.engine;

public enum Suit {
    CLUBS('C'),
    DIAMONDS('D'),
    HEARTS('H'),
    SPADES('S');

    private final char symbol;

    Suit(char symbol) {
        this.symbol = symbol;
    }

    public char symbol() {
        return symbol;
    }

    public static Suit fromSymbol(char symbol) {
        for (Suit suit : values()) {
            if (suit.symbol == symbol) {
                return suit;
            }
        }
        throw new IllegalArgumentException("Unknown suit: " + symbol);
    }
}
