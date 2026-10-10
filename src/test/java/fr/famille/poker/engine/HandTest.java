package fr.famille.poker.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class HandTest {

    private static final int SMALL_BLIND = 5;
    private static final int BIG_BLIND = 10;

    @Nested
    class Start {

        @Test
        void postsBlindsLeftOfTheButtonAndDealsTwoCardsEach() {
            List<Seat> seats = seats(1000, 1000, 1000);
            Hand hand = start(seats, 0, deck("AS KS QS JS TS 9S"));

            assertEquals(Stage.PRE_FLOP, hand.stage());
            assertEquals(List.of(0, 5, 10), bets(seats));
            assertEquals(List.of(1000, 995, 990), stacks(seats));
            assertEquals(cards("AS JS"), seats.get(1).holeCards());
            assertEquals(cards("KS TS"), seats.get(2).holeCards());
            assertEquals(cards("QS 9S"), seats.get(0).holeCards());
            assertEquals(List.of(), hand.board());
            assertEquals(OptionalInt.of(0), hand.toAct());
        }

        @Test
        void tellsWhichSeatsPostTheBlinds() {
            Hand hand = start(List.of(seat(1, 1000), seat(4, 1000), seat(6, 1000)), 6, new Deck());

            assertEquals(1, hand.smallBlindSeat());
            assertEquals(4, hand.bigBlindSeat());
        }

        @Test
        void headsUpTheButtonIsTheSmallBlind() {
            Hand hand = start(List.of(seat(3, 1000), seat(5, 1000)), 3, new Deck());

            assertEquals(3, hand.smallBlindSeat());
            assertEquals(5, hand.bigBlindSeat());
        }

        @Test
        void firstToActPreflopSitsLeftOfTheBigBlind() {
            Hand hand = start(seats(1000, 1000, 1000, 1000), 0, new Deck());

            assertEquals(OptionalInt.of(3), hand.toAct());
        }

        @Test
        void headsUpButtonPostsSmallBlindActsFirstPreflopAndLastAfter() {
            List<Seat> seats = List.of(seat(3, 1000), seat(5, 1000));
            Hand hand = start(seats, 3, new Deck());

            assertEquals(List.of(5, 10), bets(seats));
            assertEquals(OptionalInt.of(3), hand.toAct());

            hand.act(3, Action.call());
            hand.act(5, Action.check());

            assertEquals(Stage.FLOP, hand.stage());
            assertEquals(OptionalInt.of(5), hand.toAct());
        }

        @Test
        void shortStackPostsWhatItHasAndIsAllIn() {
            List<Seat> seats = seats(1000, 3, 1000);
            start(seats, 0, new Deck());

            assertEquals(3, seats.get(1).bet());
            assertEquals(SeatStatus.ALL_IN, seats.get(1).status());
        }

        @Test
        void needsTwoPlayersWithChips() {
            assertThrows(IllegalArgumentException.class, () -> start(seats(1000), 0, new Deck()));
            assertThrows(IllegalArgumentException.class, () -> start(seats(1000, 0), 0, new Deck()));
        }
    }

    @Nested
    class BettingRounds {

        @Test
        void callingAroundDealsTheFlopAndLeftOfButtonSpeaksFirst() {
            List<Seat> seats = seats(1000, 1000, 1000);
            Hand hand = start(seats, 0, new Deck());

            hand.act(0, Action.call());
            hand.act(1, Action.call());
            hand.act(2, Action.check());

            assertEquals(Stage.FLOP, hand.stage());
            assertEquals(3, hand.board().size());
            assertEquals(List.of(0, 0, 0), bets(seats));
            assertEquals(List.of(new Pot(30, List.of(0, 1, 2))), hand.pots());
            assertEquals(OptionalInt.of(1), hand.toAct());
        }

        @Test
        void bigBlindMayCheckOrRaiseWhenLimpedTo() {
            Hand hand = start(seats(1000, 1000, 1000), 0, new Deck());
            hand.act(0, Action.call());
            hand.act(1, Action.call());

            AllowedActions allowed = hand.allowedActions();

            assertEquals(EnumSet.of(ActionType.CHECK, ActionType.RAISE), allowed.types());
            assertEquals(20, allowed.minAmount());
            assertEquals(1000, allowed.maxAmount());
        }

        @Test
        void checkingDownDealsTurnThenRiver() {
            Hand hand = start(seats(1000, 1000, 1000), 0, new Deck());
            callPreflop(hand);

            checkAround(hand, 1, 2, 0);
            assertEquals(Stage.TURN, hand.stage());
            assertEquals(4, hand.board().size());

            checkAround(hand, 1, 2, 0);
            assertEquals(Stage.RIVER, hand.stage());
            assertEquals(5, hand.board().size());
        }

        @Test
        void outOfTurnActionIsRejectedAndChangesNothing() {
            List<Seat> seats = seats(1000, 1000, 1000);
            Hand hand = start(seats, 0, new Deck());

            assertThrows(IllegalActionException.class, () -> hand.act(1, Action.call()));

            assertEquals(OptionalInt.of(0), hand.toAct());
            assertEquals(List.of(1000, 995, 990), stacks(seats));
        }

        @Test
        void cannotCheckFacingABet() {
            Hand hand = start(seats(1000, 1000, 1000), 0, new Deck());

            assertThrows(IllegalActionException.class, () -> hand.act(0, Action.check()));
        }

        @Test
        void cannotCallOrFoldWhenCheckingIsFree() {
            Hand hand = start(seats(1000, 1000, 1000), 0, new Deck());
            callPreflop(hand);

            assertEquals(EnumSet.of(ActionType.CHECK, ActionType.BET), hand.allowedActions().types());
            assertThrows(IllegalActionException.class, () -> hand.act(1, Action.call()));
            assertThrows(IllegalActionException.class, () -> hand.act(1, Action.fold()));
        }

        @Test
        void foldingToTheBigBlindEndsTheHand() {
            List<Seat> seats = seats(1000, 1000, 1000);
            Hand hand = start(seats, 0, new Deck());

            hand.act(0, Action.fold());
            hand.act(1, Action.fold());

            assertTrue(hand.isComplete());
            assertEquals(OptionalInt.empty(), hand.toAct());
            assertEquals(List.of(1000, 995, 1005), stacks(seats));
            assertEquals(Map.of(), hand.result().orElseThrow().shownHands());
            // The big blind's 5 chips nobody matched go back to it: it wins a pot of 10.
            assertEquals(10, hand.result().orElseThrow().winnings(2));
        }

        @Test
        void uncalledBetGoesBackToItsOwner() {
            List<Seat> seats = seats(1000, 1000, 1000);
            Hand hand = start(seats, 0, new Deck());

            hand.act(0, Action.raise(30));
            hand.act(1, Action.fold());
            hand.act(2, Action.fold());

            assertEquals(List.of(1015, 995, 990), stacks(seats));
        }

        @Test
        void noActionOnceTheHandIsOver() {
            Hand hand = start(seats(1000, 1000), 0, new Deck());
            hand.act(0, Action.fold());

            assertThrows(IllegalActionException.class, () -> hand.act(1, Action.check()));
            assertEquals(Set.of(), hand.allowedActions().types());
        }
    }

    @Nested
    class MinimumRaise {

        @Test
        void preflopMinimumRaiseIsTwiceTheBigBlind() {
            Hand hand = start(seats(1000, 1000, 1000), 0, new Deck());

            AllowedActions allowed = hand.allowedActions();
            assertEquals(EnumSet.of(ActionType.FOLD, ActionType.CALL, ActionType.RAISE), allowed.types());
            assertEquals(10, allowed.callAmount());
            assertEquals(20, allowed.minAmount());
            assertEquals(1000, allowed.maxAmount());

            assertThrows(IllegalActionException.class, () -> hand.act(0, Action.raise(19)));
            hand.act(0, Action.raise(20));

            assertEquals(30, hand.allowedActions().minAmount());
        }

        @Test
        void reRaiseMustBeAtLeastTheLastRaise() {
            Hand hand = start(seats(1000, 1000, 1000), 0, new Deck());

            hand.act(0, Action.raise(50));

            assertEquals(90, hand.allowedActions().minAmount());
            assertThrows(IllegalActionException.class, () -> hand.act(1, Action.raise(89)));
        }

        @Test
        void minimumBetAfterTheFlopIsTheBigBlind() {
            Hand hand = start(seats(1000, 1000, 1000), 0, new Deck());
            callPreflop(hand);

            assertEquals(10, hand.allowedActions().minAmount());
            assertEquals(990, hand.allowedActions().maxAmount());
            assertThrows(IllegalActionException.class, () -> hand.act(1, Action.bet(5)));

            hand.act(1, Action.bet(10));

            assertEquals(20, hand.allowedActions().minAmount());
        }

        @Test
        void cannotBetMoreThanTheStack() {
            Hand hand = start(seats(1000, 1000, 1000), 0, new Deck());

            assertThrows(IllegalActionException.class, () -> hand.act(0, Action.raise(1001)));
        }
    }

    @Nested
    class AllIn {

        @Test
        void shortStackMayBetLessThanTheBigBlind() {
            List<Seat> seats = seats(1000, 17, 1000);
            Hand hand = start(seats, 0, new Deck());
            callPreflop(hand);

            assertEquals(7, hand.allowedActions().minAmount());
            assertEquals(7, hand.allowedActions().maxAmount());
            hand.act(1, Action.bet(7));

            assertEquals(SeatStatus.ALL_IN, seats.get(1).status());
            assertEquals(17, hand.allowedActions().minAmount());
        }

        @Test
        void callingWithLessThanTheBetPutsAllIn() {
            List<Seat> seats = seats(1000, 1000, 50);
            Hand hand = start(seats, 0, new Deck());
            hand.act(0, Action.raise(100));
            hand.act(1, Action.fold());

            assertEquals(40, hand.allowedActions().callAmount());
            hand.act(2, Action.call());

            assertEquals(SeatStatus.ALL_IN, seats.get(2).status());
        }

        @Test
        void allInBelowMinimumRaiseDoesNotReopenBettingForWhoAlreadyActed() {
            List<Seat> seats = seats(1000, 1000, 160);
            Hand hand = start(seats, 0, new Deck());
            callPreflop(hand);

            hand.act(1, Action.bet(100));
            hand.act(2, Action.raise(150));

            // Seat 0 has not acted yet: full options, minimum raise still based on the 100 bet.
            AllowedActions forSeat0 = hand.allowedActions();
            assertEquals(EnumSet.of(ActionType.FOLD, ActionType.CALL, ActionType.RAISE), forSeat0.types());
            assertEquals(250, forSeat0.minAmount());
            hand.act(0, Action.call());

            // Seat 1 already acted: it can only call the extra 50 or fold.
            assertEquals(EnumSet.of(ActionType.FOLD, ActionType.CALL), hand.allowedActions().types());
            assertEquals(50, hand.allowedActions().callAmount());
            assertThrows(IllegalActionException.class, () -> hand.act(1, Action.raise(400)));
            hand.act(1, Action.call());

            assertEquals(Stage.TURN, hand.stage());
            assertEquals(OptionalInt.of(1), hand.toAct());
        }

        @Test
        void cannotRaiseWhenEveryoneElseIsAllIn() {
            Hand hand = start(seats(1000, 500, 1000), 0, new Deck());
            hand.act(0, Action.fold());
            hand.act(1, Action.raise(500));

            assertEquals(EnumSet.of(ActionType.FOLD, ActionType.CALL), hand.allowedActions().types());
        }

        @Test
        void sidePotAppearsInPotsDuringTheHand() {
            List<Seat> seats = seats(1000, 1000, 50);
            Hand hand = start(seats, 0, new Deck());
            hand.act(0, Action.call());
            hand.act(1, Action.call());
            hand.act(2, Action.raise(50));
            hand.act(0, Action.call());
            hand.act(1, Action.call());

            assertEquals(List.of(new Pot(150, List.of(0, 1, 2))), hand.pots());

            hand.act(1, Action.bet(100));
            hand.act(0, Action.call());

            assertEquals(Stage.TURN, hand.stage());
            assertEquals(List.of(new Pot(150, List.of(0, 1, 2)), new Pot(200, List.of(0, 1))), hand.pots());
        }

        @Test
        void allInPreflopRunsOutTheBoardToShowdown() {
            List<Seat> seats = List.of(seat(0, 1000), seat(1, 500));
            Hand hand = start(seats, 0, deck("AH 2C AD 7D 3S 8C 9D JH 4S"));

            hand.act(0, Action.raise(1000));
            hand.act(1, Action.call());

            assertTrue(hand.isComplete());
            assertEquals(cards("3S 8C 9D JH 4S"), hand.board());
            assertEquals(List.of(500, 1000), stacks(seats));
        }
    }

    @Nested
    class Showdown {

        @Test
        void bestHandWinsThePot() {
            List<Seat> seats = seats(1000, 1000, 1000);
            Hand hand = start(seats, 0, deck("AH KH 2C AD KD 7D 3S 8C 9D JH 4S"));
            callPreflop(hand);
            checkAround(hand, 1, 2, 0);
            checkAround(hand, 1, 2, 0);
            checkAround(hand, 1, 2, 0);

            assertTrue(hand.isComplete());
            HandResult result = hand.result().orElseThrow();
            assertEquals(Set.of(0, 1, 2), result.shownHands().keySet());
            assertEquals(HandCategory.ONE_PAIR, result.shownHands().get(1).category());
            assertEquals(30, result.winnings(1));
            assertEquals(List.of(990, 1020, 990), stacks(seats));
        }

        @Test
        void tiedHandsSplitThePotAndOddChipGoesLeftOfTheButton() {
            List<Seat> seats = seats(1000, 1000, 1000);
            Hand hand = start(seats, 0, deck("2C 3C 4C 2D 3D 4D AS KD QH JC TS"));
            hand.act(0, Action.call());
            hand.act(1, Action.fold());
            hand.act(2, Action.check());
            checkAround(hand, 2, 0);
            checkAround(hand, 2, 0);
            checkAround(hand, 2, 0);

            HandResult result = hand.result().orElseThrow();
            assertEquals(Set.of(0, 2), result.shownHands().keySet());
            assertEquals(12, result.winnings(0));
            assertEquals(13, result.winnings(2));
            assertEquals(List.of(1002, 995, 1003), stacks(seats));
        }

        @Test
        void threeWayAllInWithSidePot() {
            // Seat 0: button, deep stack, high card. Seat 1: small blind, pair of aces.
            // Seat 2: big blind, short stack, three kings.
            List<Seat> seats = seats(1000, 400, 150);
            Hand hand = start(seats, 0, deck("AH KD QS AD KC JD KS 7D 2C 9H 3S"));

            hand.act(0, Action.raise(30));
            hand.act(1, Action.call());
            hand.act(2, Action.call());
            assertEquals(List.of(970, 370, 120), stacks(seats));

            hand.act(1, Action.check());
            hand.act(2, Action.bet(120));
            hand.act(0, Action.raise(400));
            assertEquals(EnumSet.of(ActionType.FOLD, ActionType.CALL), hand.allowedActions().types());
            hand.act(1, Action.call());

            assertTrue(hand.isComplete());
            assertEquals(List.of(new Pot(450, List.of(0, 1, 2)), new Pot(500, List.of(0, 1))), hand.pots());
            HandResult result = hand.result().orElseThrow();
            assertEquals(450, result.winnings(2));
            assertEquals(500, result.winnings(1));
            assertEquals(0, result.winnings(0));
            assertEquals(List.of(600, 500, 450), stacks(seats));
        }

        @Test
        void chipsAreNeverCreatedNorLost() {
            List<Seat> seats = seats(1000, 400, 150);
            Hand hand = start(seats, 0, new Deck(new java.util.Random(7)));
            hand.act(0, Action.raise(1000));
            hand.act(1, Action.call());
            hand.act(2, Action.call());

            assertTrue(hand.isComplete());
            assertEquals(1550, stacks(seats).stream().mapToInt(Integer::intValue).sum());
            assertFalse(hand.result().orElseThrow().shownHands().isEmpty());
        }
    }

    private static Hand start(List<Seat> seats, int buttonSeat, Deck deck) {
        return Hand.start(seats, buttonSeat, SMALL_BLIND, BIG_BLIND, deck);
    }

    /** Seats numbered from 0 with the given stacks. */
    private static List<Seat> seats(int... stacks) {
        return java.util.stream.IntStream.range(0, stacks.length).mapToObj(i -> seat(i, stacks[i])).toList();
    }

    private static Seat seat(int number, int stack) {
        Seat seat = new Seat(number);
        seat.sit(new Player("id-" + number, "Joueur " + number, "avatar", stack));
        return seat;
    }

    /** Three players, button on seat 0: everyone calls the big blind, which checks. */
    private static void callPreflop(Hand hand) {
        hand.act(0, Action.call());
        hand.act(1, Action.call());
        hand.act(2, Action.check());
    }

    private static void checkAround(Hand hand, int... seatNumbers) {
        for (int seatNumber : seatNumbers) {
            hand.act(seatNumber, Action.check());
        }
    }

    private static Deck deck(String codes) {
        return Deck.withTopCards(cards(codes));
    }

    private static List<Card> cards(String codes) {
        return Arrays.stream(codes.split(" ")).map(Card::of).toList();
    }

    private static List<Integer> bets(List<Seat> seats) {
        return seats.stream().map(Seat::bet).toList();
    }

    private static List<Integer> stacks(List<Seat> seats) {
        return seats.stream().map(seat -> seat.player().stack()).toList();
    }
}
