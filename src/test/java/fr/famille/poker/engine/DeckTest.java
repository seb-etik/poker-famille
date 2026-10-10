package fr.famille.poker.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class DeckTest {

    @Test
    void contains52DistinctCards() {
        Deck deck = new Deck();
        assertEquals(52, deck.remaining());

        List<Card> drawn = drawAll(deck);

        assertEquals(52, drawn.size());
        assertEquals(52, new HashSet<>(drawn).size());
        assertEquals(0, deck.remaining());
    }

    @Test
    void sameSeedGivesSameOrder() {
        assertEquals(drawAll(new Deck(new Random(42))), drawAll(new Deck(new Random(42))));
    }

    @Test
    void differentSeedsGiveDifferentOrders() {
        assertNotEquals(drawAll(new Deck(new Random(1))), drawAll(new Deck(new Random(2))));
    }

    @Test
    void drawingFromEmptyDeckFails() {
        Deck deck = new Deck();
        drawAll(deck);

        assertThrows(IllegalStateException.class, deck::draw);
    }

    private static List<Card> drawAll(Deck deck) {
        List<Card> cards = new ArrayList<>();
        while (deck.remaining() > 0) {
            cards.add(deck.draw());
        }
        return cards;
    }
}
