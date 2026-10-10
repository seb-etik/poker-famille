package fr.famille.poker.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CardTest {

    @Test
    void parsesShortCode() {
        assertEquals(new Card(Rank.ACE, Suit.SPADES), Card.of("AS"));
        assertEquals(new Card(Rank.TEN, Suit.DIAMONDS), Card.of("TD"));
        assertEquals(new Card(Rank.NINE, Suit.HEARTS), Card.of("9H"));
        assertEquals(new Card(Rank.TWO, Suit.CLUBS), Card.of("2C"));
    }

    @Test
    void codeRoundTripsForAll52Cards() {
        for (Rank rank : Rank.values()) {
            for (Suit suit : Suit.values()) {
                Card card = new Card(rank, suit);
                assertEquals(card, Card.of(card.code()));
                assertEquals(card.code(), card.toString());
            }
        }
    }

    @Test
    void rankValuesGoFromTwoToAce() {
        assertEquals(2, Rank.TWO.value());
        assertEquals(10, Rank.TEN.value());
        assertEquals(14, Rank.ACE.value());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"A", "ASX", "1S", "AX", "as", "10S"})
    void rejectsInvalidCode(String code) {
        assertThrows(IllegalArgumentException.class, () -> Card.of(code));
    }
}
