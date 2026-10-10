package fr.famille.poker.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

/** Plays many hands with random legal actions: the engine must always finish and keep every chip. */
class HandRandomPlayTest {

    private static final int HANDS = 5_000;

    @Test
    void randomHandsAlwaysFinishAndKeepEveryChip() {
        Random random = new Random(2026);
        for (int i = 0; i < HANDS; i++) {
            playOneHand(random, i);
        }
    }

    private static void playOneHand(Random random, int handNumber) {
        int playerCount = 2 + random.nextInt(7);
        List<Seat> seats = new ArrayList<>();
        for (int number = 0; number < playerCount; number++) {
            // Short stacks are frequent so that all-ins and side pots happen a lot.
            int stack = 1 + random.nextInt(random.nextBoolean() ? 60 : 2000);
            Seat seat = new Seat(number);
            seat.sit(new Player("id-" + number, "Joueur " + number, "avatar", stack));
            seats.add(seat);
        }
        int chipsBefore = totalStacks(seats);
        int button = random.nextInt(playerCount);

        Hand hand = Hand.start(seats, button, 5, 10, new Deck(random));
        int actions = 0;
        while (!hand.isComplete()) {
            int seat = hand.toAct().orElseThrow();
            hand.act(seat, randomAction(hand.allowedActions(), random));
            assertTrue(++actions < 500, "hand " + handNumber + " never ends");
        }

        assertEquals(chipsBefore, totalStacks(seats), "chips changed in hand " + handNumber);
        if (!hand.result().orElseThrow().shownHands().isEmpty()) {
            assertEquals(5, hand.board().size(), "showdown without a full board in hand " + handNumber);
        }
        int paid = hand.result().orElseThrow().payouts().stream().mapToInt(payout -> payout.pot().amount()).sum();
        assertEquals(seats.stream().mapToInt(Seat::totalBet).sum(), paid, "pots differ from bets in hand " + handNumber);
    }

    private static Action randomAction(AllowedActions allowed, Random random) {
        List<ActionType> types = new ArrayList<>(allowed.types());
        types.sort(null);
        ActionType type = types.get(random.nextInt(types.size()));
        return switch (type) {
            case FOLD -> Action.fold();
            case CHECK -> Action.check();
            case CALL -> Action.call();
            case BET, RAISE -> {
                // Often the minimum or all in, the two edges of the allowed range.
                int span = allowed.maxAmount() - allowed.minAmount();
                int amount = switch (random.nextInt(3)) {
                    case 0 -> allowed.minAmount();
                    case 1 -> allowed.maxAmount();
                    default -> allowed.minAmount() + random.nextInt(span + 1);
                };
                yield new Action(type, amount);
            }
        };
    }

    private static int totalStacks(List<Seat> seats) {
        return seats.stream().mapToInt(seat -> seat.player().stack()).sum();
    }
}
