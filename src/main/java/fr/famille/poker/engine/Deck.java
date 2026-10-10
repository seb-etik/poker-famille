package fr.famille.poker.engine;

import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/** The 52 cards, shuffled once at creation, then drawn from the top. */
public final class Deck {

    private final Deque<Card> cards;

    /** Shuffles with {@link SecureRandom}: the only constructor meant for real games. */
    public Deck() {
        this(new SecureRandom());
    }

    /** Shuffles with the given source, so tests can use a seeded {@link Random}. */
    public Deck(Random random) {
        List<Card> all = new ArrayList<>(52);
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                all.add(new Card(rank, suit));
            }
        }
        Collections.shuffle(all, random);
        this.cards = new ArrayDeque<>(all);
    }

    public Card draw() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("The deck is empty");
        }
        return cards.pop();
    }

    public int remaining() {
        return cards.size();
    }
}
