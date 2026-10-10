package fr.famille.poker.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class TableTest {

    @Test
    void hasEightEmptySeats() {
        Table table = new Table("ABCD", 5, 10);

        assertEquals(Table.MAX_SEATS, table.seats().size());
        assertTrue(table.seats().stream().allMatch(Seat::isEmpty));
    }

    @Test
    void seatsAPlayer() {
        Table table = new Table("ABCD", 5, 10);

        table.sit(3, player("alice", 1000));

        assertEquals("alice", table.seats().get(3).player().id());
    }

    @Test
    void refusesATakenSeatAnUnknownSeatOrAPlayerAlreadySeated() {
        Table table = new Table("ABCD", 5, 10);
        Player alice = player("alice", 1000);
        table.sit(3, alice);

        assertThrows(IllegalStateException.class, () -> table.sit(3, player("bob", 1000)));
        assertThrows(IllegalArgumentException.class, () -> table.sit(8, player("bob", 1000)));
        assertThrows(IllegalStateException.class, () -> table.sit(4, alice));
    }

    @Test
    void needsTwoPlayersWithChipsToStartAHand() {
        Table table = new Table("ABCD", 5, 10);
        table.sit(0, player("alice", 1000));
        table.sit(1, player("bob", 0));

        assertThrows(IllegalStateException.class, () -> table.startHand(new Deck()));
    }

    @Test
    void firstButtonGoesToTheFirstSeatedPlayer() {
        Table table = tableWith(2, 5, 7);

        Hand hand = table.startHand(new Deck());

        assertEquals(2, hand.buttonSeat());
    }

    @Test
    void buttonMovesToTheNextPlayerAndWrapsAround() {
        Table table = tableWith(2, 5, 7);

        assertEquals(2, playQuickHand(table).buttonSeat());
        assertEquals(5, playQuickHand(table).buttonSeat());
        assertEquals(7, playQuickHand(table).buttonSeat());
        assertEquals(2, playQuickHand(table).buttonSeat());
    }

    @Test
    void playersWithoutChipsSitOut() {
        Table table = new Table("ABCD", 5, 10);
        table.sit(0, player("alice", 1000));
        table.sit(1, player("bob", 0));
        table.sit(2, player("carol", 1000));

        Hand hand = table.startHand(new Deck());

        assertEquals(List.of(0, 2), hand.seats().stream().map(Seat::number).toList());
        assertEquals(SeatStatus.SITTING_OUT, table.seats().get(1).status());
        assertEquals(List.of(), table.seats().get(1).holeCards());
    }

    @Test
    void buttonSkipsAPlayerWhoWentBust() {
        Table table = new Table("ABCD", 5, 10);
        table.sit(0, player("alice", 1000));
        table.sit(1, player("bob", 1000));
        table.sit(2, player("carol", 1000));
        playQuickHand(table);
        Player bob = table.seats().get(1).player();
        bob.removeChips(bob.stack());

        assertEquals(2, table.startHand(new Deck()).buttonSeat());
    }

    @Test
    void cannotStartAHandWhileOneIsInProgress() {
        Table table = tableWith(0, 1);
        table.startHand(new Deck());

        assertThrows(IllegalStateException.class, () -> table.startHand(new Deck()));
    }

    @Test
    void keepsTheCurrentHand() {
        Table table = tableWith(0, 1);
        assertTrue(table.currentHand().isEmpty());

        Hand hand = table.startHand(new Deck());

        assertEquals(hand, table.currentHand().orElseThrow());
    }

    @Test
    void leavingFreesTheSeat() {
        Table table = tableWith(0, 1);

        Player left = table.leave(1);

        assertEquals("player-1", left.id());
        assertTrue(table.seats().get(1).isEmpty());
    }

    @Test
    void cannotLeaveASeatDealtInTheCurrentHand() {
        Table table = tableWith(0, 1);
        table.startHand(new Deck());

        assertThrows(IllegalStateException.class, () -> table.leave(1));
    }

    @Test
    void mayLeaveASeatSittingOutDuringAHand() {
        Table table = tableWith(0, 1);
        table.sit(2, player("broke", 0));
        table.startHand(new Deck());

        table.leave(2);

        assertTrue(table.seats().get(2).isEmpty());
    }

        /** Starts a hand and folds it at once. */
    private static Hand playQuickHand(Table table) {
        Hand hand = table.startHand(new Deck());
        while (!hand.isComplete()) {
            int seat = hand.toAct().orElseThrow();
            boolean canCheck = hand.allowedActions().types().contains(ActionType.CHECK);
            hand.act(seat, canCheck ? Action.check() : Action.fold());
        }
        return hand;
    }

    private static Table tableWith(int... seatNumbers) {
        Table table = new Table("ABCD", 5, 10);
        for (int number : seatNumbers) {
            table.sit(number, player("player-" + number, 1000));
        }
        return table;
    }

    private static Player player(String id, int stack) {
        return new Player(id, id, "avatar", stack);
    }
}
