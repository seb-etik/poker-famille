package fr.famille.poker.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class HandEvaluatorTest {

    @Nested
    class Categories {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
            "AS KD 9H 7C 4S 3D 2H, HIGH_CARD",
            "AS AD 9H 7C 4S 3D 2H, ONE_PAIR",
            "AS AD 9H 9C 4S 3D 2H, TWO_PAIR",
            "AS AD AH 9C 4S 3D 2H, THREE_OF_A_KIND",
            "9S 8D 7H 6C 5S 2D KH, STRAIGHT",
            "AH JH 8H 4H 2H KS QD, FLUSH",
            "KS KD KH 4C 4S 2D 9H, FULL_HOUSE",
            "7S 7D 7H 7C AS 2D 3H, FOUR_OF_A_KIND",
            "9H 8H 7H 6H 5H AS AD, STRAIGHT_FLUSH",
            "AS KS QS JS TS 2D 3H, STRAIGHT_FLUSH",
        })
        void detectsCategoryAmongSevenCards(String cards, HandCategory expected) {
            assertEquals(expected, evaluate(cards).category());
        }

        @ParameterizedTest
        @ValueSource(strings = {"AS AD 9H 7C 4S", "AS AD 9H 7C 4S 3D"})
        void acceptsFiveOrSixCards(String cards) {
            assertEquals(HandCategory.ONE_PAIR, evaluate(cards).category());
        }

        @Test
        void categoriesAreStrictlyOrdered() {
            List<HandRank> ascending = List.of(
                    evaluate("AS KD 9H 7C 4S"),
                    evaluate("2S 2D 3H 4C 6S"),
                    evaluate("2S 2D 3H 3C 4S"),
                    evaluate("2S 2D 2H 3C 4S"),
                    evaluate("AS 2D 3H 4C 5S"),
                    evaluate("2H 3H 4H 5H 7H"),
                    evaluate("2S 2D 2H 3C 3S"),
                    evaluate("2S 2D 2H 2C 3S"),
                    evaluate("AH 2H 3H 4H 5H"));

            for (int i = 1; i < ascending.size(); i++) {
                assertBeats(ascending.get(i), ascending.get(i - 1));
            }
        }
    }

    @Nested
    class LowStraight {

        @Test
        void wheelIsAFiveHighStraight() {
            HandRank wheel = evaluate("AS 2D 3H 4C 5S KD 9H");

            assertEquals(HandCategory.STRAIGHT, wheel.category());
            assertEquals(ranks("5"), wheel.tiebreakers());
        }

        @Test
        void sixHighStraightBeatsWheel() {
            assertBeats(evaluate("2S 3D 4H 5C 6S KD 9H"), evaluate("AS 2D 3H 4C 5S KD 9H"));
        }

        @Test
        void sixCardRunFromAceIsSixHigh() {
            assertEquals(ranks("6"), evaluate("AS 2D 3H 4C 5S 6D 9H").tiebreakers());
        }

        @Test
        void steelWheelIsAFiveHighStraightFlush() {
            HandRank steelWheel = evaluate("AH 2H 3H 4H 5H KS KD");

            assertEquals(HandCategory.STRAIGHT_FLUSH, steelWheel.category());
            assertEquals(ranks("5"), steelWheel.tiebreakers());
        }

        @Test
        void wheelPutsTheAceLast() {
            assertEquals(cards("5S 4C 3H 2D AS"), evaluate("AS 2D 3H 4C 5S").bestCards());
        }

        @Test
        void straightsDoNotWrapAround() {
            assertEquals(HandCategory.HIGH_CARD, evaluate("QS KD AH 2C 3S 7D 8H").category());
        }
    }

    @Nested
    class Kickers {

        @ParameterizedTest(name = "{0} > {1}")
        @CsvSource({
            // high card: down to the fifth card
            "AS KD 9H 7C 4S, AS KD 9H 7C 3S",
            // pair: higher pair, then kickers
            "KS KD 2H 3C 4S, QS QD AH JC 9S",
            "AS AD KH 7C 4S, AH AC QH 7D 4D",
            "AS AD KH 7C 4S, AH AC KD 7D 3D",
            // two pair: top pair, then bottom pair, then kicker
            "AS AD 2H 2C 3S, KS KD QH QC JS",
            "AS AD 9H 9C 2S, AH AC 8H 8D KS",
            "AS AD 9H 9C KS, AH AC 9D 9S QS",
            // three of a kind: trips, then kickers
            "8S 8D 8H 2C 3S, 7S 7D 7H AC KS",
            "7S 7D 7H AC 2S, 7S 7D 7C KC QS",
            // straight: highest card
            "TS 9D 8H 7C 6S, 9S 8D 7H 6C 5S",
            // flush: down to the fifth card
            "AH JH 8H 4H 3H, AS JS 8S 4S 2S",
            "AH JH 8H 4H 3H, KS QS JS 9S 7S",
            // full house: trips first, then pair
            "3S 3D 3H 2C 2S, 2S 2D 2H AC AS",
            "KS KD KH QC QS, KS KD KH JC JS",
            // four of a kind: quads, then kicker
            "8S 8D 8H 8C 2S, 7S 7D 7H 7C AS",
            "7S 7D 7H 7C AS, 7S 7D 7H 7C KS",
            // straight flush: highest card
            "TH 9H 8H 7H 6H, 9S 8S 7S 6S 5S",
            "AS KS QS JS TS, KH QH JH TH 9H",
        })
        void strongerHandWins(String stronger, String weaker) {
            assertBeats(evaluate(stronger), evaluate(weaker));
        }

        @Test
        void pairTiebreakersArePairThenKickers() {
            assertEquals(ranks("A K 7 4"), evaluate("AS AD KH 7C 4S").tiebreakers());
        }
    }

    @Nested
    class BestFiveOfSeven {

        @Test
        void threePairsKeepTheTwoBestAndTheBestKicker() {
            HandRank hand = evaluate("AS AD KH KC QS QD 2H");

            assertEquals(HandCategory.TWO_PAIR, hand.category());
            assertEquals(ranks("A K Q"), hand.tiebreakers());
        }

        @Test
        void twoThreeOfAKindMakeAFullHouse() {
            HandRank hand = evaluate("AS AD AH KC KS KD 2H");

            assertEquals(HandCategory.FULL_HOUSE, hand.category());
            assertEquals(ranks("A K"), hand.tiebreakers());
        }

        @Test
        void fullHouseTakesTheHighestPair() {
            assertEquals(ranks("K 4"), evaluate("KS KD KH 4C 4S 2D 2H").tiebreakers());
        }

        @Test
        void quadsTakeTheHighestKicker() {
            assertEquals(ranks("7 A"), evaluate("7S 7D 7H 7C AS AD AH").tiebreakers());
        }

        @Test
        void flushAndStraightWithDifferentCardsIsOnlyAFlush() {
            HandRank hand = evaluate("9H 8H 7S 6H 5H 2H KD");

            assertEquals(HandCategory.FLUSH, hand.category());
            assertEquals(ranks("9 8 6 5 2"), hand.tiebreakers());
        }

        @Test
        void sixSuitedCardsKeepTheFiveHighest() {
            HandRank hand = evaluate("AH KH 9H 7H 5H 3H 2S");

            assertEquals(HandCategory.FLUSH, hand.category());
            assertEquals(new HashSet<>(cards("AH KH 9H 7H 5H")), new HashSet<>(hand.bestCards()));
        }

        @Test
        void pairInsideAStraightStillMakesAStraight() {
            HandRank hand = evaluate("9S 9D 8H 7C 6S 5D 2H");

            assertEquals(HandCategory.STRAIGHT, hand.category());
            assertEquals(ranks("9"), hand.tiebreakers());
        }

        @Test
        void bestCardsStartWithTheMainGroup() {
            List<Rank> bestRanks = evaluate("4C KS 2D KD 4S KH 2H").bestCards().stream().map(Card::rank).toList();

            assertEquals(ranks("K K K 4 4"), bestRanks);
        }
    }

    @Nested
    class Ties {

        @Test
        void boardPlaysForBothPlayers() {
            String board = "AS KD QH JC TS ";

            assertTie(evaluate(board + "2C 3D"), evaluate(board + "4H 5S"));
        }

        @Test
        void kickersBeyondFiveCardsDoNotCount() {
            String board = "AS AD KH QC JS ";

            assertTie(evaluate(board + "2C 3D"), evaluate(board + "4H 5S"));
        }

        @Test
        void suitsNeverBreakTies() {
            assertTie(evaluate("AS KS 9D 7C 4H"), evaluate("AD KD 9S 7H 4C"));
        }

        @Test
        void sameFlushRanksInDifferentSuitsTie() {
            assertTie(evaluate("AH JH 8H 4H 3H"), evaluate("AS JS 8S 4S 3S"));
        }
    }

    @Nested
    class InvalidInput {

        @ParameterizedTest
        @ValueSource(strings = {"AS KD 9H 7C", "AS KD 9H 7C 4S 3D 2H 5C", "AS AS 9H 7C 4S"})
        void rejectsWrongCountOrDuplicates(String cards) {
            assertThrows(IllegalArgumentException.class, () -> evaluate(cards));
        }

        @Test
        void rejectsNull() {
            assertThrows(NullPointerException.class, () -> HandEvaluator.evaluate(null));
        }
    }

    private static HandRank evaluate(String cards) {
        return HandEvaluator.evaluate(cards(cards));
    }

    private static List<Card> cards(String codes) {
        return Arrays.stream(codes.trim().split("\\s+")).map(Card::of).toList();
    }

    private static List<Rank> ranks(String symbols) {
        return Arrays.stream(symbols.split(" ")).map(s -> Rank.fromSymbol(s.charAt(0))).toList();
    }

    private static void assertBeats(HandRank stronger, HandRank weaker) {
        assertTrue(stronger.compareTo(weaker) > 0, stronger + " should beat " + weaker);
        assertTrue(weaker.compareTo(stronger) < 0, weaker + " should lose to " + stronger);
    }

    private static void assertTie(HandRank a, HandRank b) {
        assertEquals(0, a.compareTo(b), a + " should tie " + b);
    }
}
