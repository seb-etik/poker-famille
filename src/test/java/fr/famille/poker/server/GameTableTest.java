package fr.famille.poker.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import fr.famille.poker.engine.Deck;
import fr.famille.poker.server.message.ErrorCode;
import fr.famille.poker.server.message.EventMessage;
import fr.famille.poker.server.message.ServerMessage;
import fr.famille.poker.server.message.StateMessage;
import fr.famille.poker.server.message.StateMessage.SeatView;

class GameTableTest {

    private static final Timings TIMINGS = new Timings(
            Duration.ofSeconds(30), Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(8));

    private final RecordingOutbox outbox = new RecordingOutbox();
    private final ManualScheduler scheduler = new ManualScheduler();
    private final GameTable table = new GameTable("ABCD", new TableSettings(5, 10, 1000), TIMINGS,
            outbox, scheduler, () -> new Deck(new Random(42)),
            Clock.fixed(Instant.parse("2026-10-10T20:00:00Z"), ZoneOffset.UTC));

    @Test
    void firstPlayerToJoinIsTheHost() {
        table.join("alice", "Alice", "fox");
        table.join("bob", "Bob", "bear");

        StateMessage forAlice = outbox.lastState("alice");
        assertTrue(forAlice.you().isHost());
        assertNull(forAlice.you().seat());
        assertEquals("waiting", forAlice.table().status());
        assertFalse(outbox.lastState("bob").you().isHost());
    }

    @Test
    void rejectsAnInvalidNickname() {
        assertError(ErrorCode.INVALID_MESSAGE, () -> table.join("alice", " ", "fox"));
        assertError(ErrorCode.INVALID_MESSAGE, () -> table.join("alice", "A".repeat(21), "fox"));
    }

    @Test
    void sittingGivesTheStartingStack() {
        table.join("alice", "Alice", "fox");
        table.sit("alice", 3);

        SeatView seat = outbox.lastState("alice").seats().get(3);
        assertEquals("Alice", seat.nickname());
        assertEquals(1000, seat.stack());
        assertEquals(3, outbox.lastState("alice").you().seat());
    }

    @Test
    void refusesInvalidSeating() {
        assertError(ErrorCode.NOT_JOINED, () -> table.sit("alice", 0));
        table.join("alice", "Alice", "fox");
        table.join("bob", "Bob", "bear");
        table.sit("alice", 0);

        assertError(ErrorCode.SEAT_TAKEN, () -> table.sit("bob", 0));
        assertError(ErrorCode.SEAT_TAKEN, () -> table.sit("bob", 8));
        assertError(ErrorCode.ALREADY_SEATED, () -> table.sit("alice", 1));
    }

    @Test
    void onlyTheHostStartsAndNeedsTwoPlayers() {
        table.join("alice", "Alice", "fox");
        table.join("bob", "Bob", "bear");
        table.sit("alice", 0);

        assertError(ErrorCode.NOT_ENOUGH_PLAYERS, () -> table.start("alice"));
        table.sit("bob", 1);
        assertError(ErrorCode.NOT_HOST, () -> table.start("bob"));
    }

    @Test
    void startingDealsAndAnnouncesTheHandBeforeTheState() {
        seatAliceAndBob();
        outbox.clear();

        table.start("alice");

        List<ServerMessage> forBob = outbox.messages("bob");
        assertEquals(new EventMessage.HandStarted(1, 0), forBob.getFirst());
        StateMessage state = assertInstanceOf(StateMessage.class, forBob.getLast());
        assertEquals("playing", state.table().status());
        assertEquals("preflop", state.hand().stage());
        assertEquals(0, state.hand().toActSeat());
        assertEquals(30_000L, state.hand().remainingMs());
    }

    @Test
    void everyoneSeesOnlyTheirOwnCards() {
        seatAliceAndBob();
        table.join("carol", "Carol", "owl");
        table.start("alice");

        StateMessage forAlice = outbox.lastState("alice");
        assertEquals(2, forAlice.seats().get(0).cards().size());
        assertNull(forAlice.seats().get(1).cards());
        assertTrue(forAlice.seats().get(1).hasCards());

        StateMessage forBob = outbox.lastState("bob");
        assertNull(forBob.seats().get(0).cards());
        assertEquals(2, forBob.seats().get(1).cards().size());

        StateMessage forCarol = outbox.lastState("carol");
        assertNull(forCarol.seats().get(0).cards());
        assertNull(forCarol.seats().get(1).cards());
    }

    @Test
    void onlyThePlayerToActGetsAllowedActions() {
        seatAliceAndBob();
        table.start("alice");

        StateMessage.AllowedActionsView allowed = outbox.lastState("alice").allowedActions();
        assertEquals(List.of("fold", "call", "raise"), allowed.types());
        assertEquals(5, allowed.callAmount());
        assertNull(outbox.lastState("bob").allowedActions());
    }

    @Test
    void outOfTurnActionIsRefused() {
        seatAliceAndBob();
        table.start("alice");

        GameException error = assertError(ErrorCode.ILLEGAL_ACTION, () -> table.act("bob", "check", null));
        assertEquals("Ce n'est pas ton tour.", error.getMessage());
    }

    @Test
    void unknownActionIsAnInvalidMessage() {
        seatAliceAndBob();
        table.start("alice");

        assertError(ErrorCode.INVALID_MESSAGE, () -> table.act("alice", "bluff", null));
        assertError(ErrorCode.INVALID_MESSAGE, () -> table.act("alice", "raise", null));
    }

    @Test
    void actionsAreBroadcastWithTheTotalBet() {
        seatAliceAndBob();
        table.start("alice");
        outbox.clear();

        table.act("alice", "call", null);

        assertEquals(new EventMessage.Action(0, "call", 10), outbox.messages("bob").getFirst());
    }

    @Test
    void foldEndsTheHandShowsTheResultAndDealsTheNextOneAfterAPause() {
        seatAliceAndBob();
        table.start("alice");

        table.act("alice", "fold", null);

        StateMessage state = outbox.lastState("bob");
        assertEquals("complete", state.hand().stage());
        assertEquals(Map.of(1, 10), state.lastResult().payouts().getFirst().winners());
        assertTrue(state.lastResult().shownHands().isEmpty());
        assertEquals(Duration.ofSeconds(5), scheduler.pending().delay());

        scheduler.runPending();

        StateMessage next = outbox.lastState("bob");
        assertEquals(2, next.hand().number());
        assertEquals(1, next.hand().buttonSeat());
        assertNull(next.lastResult());
    }

    @Test
    void showdownRevealsTheCardsToEveryone() {
        seatAliceAndBob();
        table.start("alice");
        table.act("alice", "call", null);
        while (outbox.lastState("alice").hand().toActSeat() != null) {
            String toAct = outbox.lastState("alice").hand().toActSeat() == 0 ? "alice" : "bob";
            table.act(toAct, "check", null);
        }

        StateMessage forAlice = outbox.lastState("alice");
        assertEquals(2, forAlice.seats().get(1).cards().size());
        assertEquals(2, forAlice.lastResult().shownHands().size());
        assertTrue(outbox.messages("alice").contains(new EventMessage.Showdown()));
        assertEquals(Duration.ofSeconds(8), scheduler.pending().delay());
    }

    @Test
    void timerChecksOrFoldsForThePlayer() {
        seatAliceAndBob();
        table.start("alice");
        assertEquals(Duration.ofSeconds(30), scheduler.pending().delay());

        scheduler.runPending();

        assertTrue(outbox.messages("bob").contains(new EventMessage.Action(0, "fold", 5)));
        assertEquals("complete", outbox.lastState("bob").hand().stage());
    }

    @Test
    void disconnectedPlayerIsPlayedForAfterAShortDelay() {
        seatAliceAndBob();
        table.start("alice");

        table.disconnect("alice");

        assertFalse(outbox.lastState("bob").seats().get(0).connected());
        assertEquals(Duration.ofSeconds(1), scheduler.pending().delay());
    }

    @Test
    void reconnectingKeepsTheSeatAndTheCards() {
        seatAliceAndBob();
        table.start("alice");
        List<String> cards = outbox.lastState("alice").seats().get(0).cards();

        table.disconnect("alice");
        table.join("alice", "Alice", "fox");

        StateMessage state = outbox.lastState("alice");
        assertEquals(0, state.you().seat());
        assertEquals(cards, state.seats().get(0).cards());
        assertTrue(state.seats().get(0).connected());
        assertEquals(Duration.ofSeconds(30), scheduler.pending().delay());
    }

    @Test
    void leavingDuringAHandFreesTheSeatAtTheEndAndKeepsTheStack() {
        seatAliceAndBob();
        table.start("alice");

        table.leave("bob");
        assertNotNull(outbox.lastState("alice").seats().get(1));

        table.act("alice", "fold", null);
        assertNull(outbox.lastState("alice").seats().get(1));
        assertEquals("waiting", outbox.lastState("alice").table().status());

        table.sit("bob", 3);
        assertEquals(1005, outbox.lastState("bob").seats().get(3).stack());
    }

    @Test
    void hostRoleMovesWhenTheHostLeaves() {
        seatAliceAndBob();

        table.leave("alice");

        assertEquals(1, outbox.lastState("bob").table().hostSeat());
        assertTrue(outbox.lastState("bob").you().isHost());
    }

    @Test
    void gameStopsWhenOnlyOnePlayerHasChips() {
        seatAliceAndBob();
        table.start("alice");

        table.act("alice", "raise", 1000);
        table.act("bob", "call", null);

        StateMessage state = outbox.lastState("alice");
        assertEquals("complete", state.hand().stage());
        assertEquals("waiting", state.table().status());
        assertTrue(state.seats().stream().anyMatch(seat -> seat.stack() == 0));
        assertNull(scheduler.pendingOrNull());
    }

    private void seatAliceAndBob() {
        table.join("alice", "Alice", "fox");
        table.join("bob", "Bob", "bear");
        table.sit("alice", 0);
        table.sit("bob", 1);
    }

    private static GameException assertError(ErrorCode code, Runnable action) {
        GameException error = assertThrows(GameException.class, action::run);
        assertEquals(code, error.code());
        return error;
    }

    static final class RecordingOutbox implements Outbox {

        private final Map<String, List<ServerMessage>> sent = new HashMap<>();

        @Override
        public void send(String playerId, ServerMessage message) {
            sent.computeIfAbsent(playerId, id -> new ArrayList<>()).add(message);
        }

        List<ServerMessage> messages(String playerId) {
            return sent.getOrDefault(playerId, List.of());
        }

        StateMessage lastState(String playerId) {
            return messages(playerId).reversed().stream()
                    .filter(StateMessage.class::isInstance)
                    .map(StateMessage.class::cast)
                    .findFirst()
                    .orElseThrow();
        }

        void clear() {
            sent.clear();
        }
    }

    static final class ManualScheduler implements Scheduler {

        record Task(Duration delay, Runnable action, boolean[] cancelled) implements Cancellable {
            @Override
            public void cancel() {
                cancelled[0] = true;
            }
        }

        private final List<Task> tasks = new ArrayList<>();

        @Override
        public Cancellable schedule(Duration delay, Runnable action) {
            Task task = new Task(delay, action, new boolean[1]);
            tasks.add(task);
            return task;
        }

        Task pendingOrNull() {
            return tasks.stream().filter(task -> !task.cancelled()[0]).reduce((first, second) -> second).orElse(null);
        }

        Task pending() {
            Task task = pendingOrNull();
            assertNotNull(task, "no pending task");
            return task;
        }

        void runPending() {
            Task task = pending();
            task.cancel();
            task.action().run();
        }
    }
}
