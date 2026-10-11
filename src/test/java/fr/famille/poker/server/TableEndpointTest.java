package fr.famille.poker.server;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.websockets.next.BasicWebSocketConnector;
import io.quarkus.websockets.next.WebSocketClientConnection;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@QuarkusTest
class TableEndpointTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Duration WAIT = Duration.ofSeconds(5);

    @TestHTTPResource("/")
    URI baseUri;

    @Inject
    Instance<BasicWebSocketConnector> connectors;

    @Test
    void createsATableWithAFourLetterCode() {
        String code = createTable();

        assertTrue(code.matches("[A-HJ-NP-Z]{4}"), code);
    }

    @Test
    void createsATableWithItsBlindSettings() {
        String code = given().contentType("application/json")
                .body("{\"smallBlind\": 10, \"bigBlind\": 20, \"startingStack\": 2000,"
                        + " \"blindIncreaseEveryHands\": 5, \"blindIncreasePercent\": 25}")
                .post("/api/tables")
                .then().statusCode(201)
                .extract().path("code");
        Client client = connect(code);
        client.send("{\"type\":\"join\",\"playerId\":\"eve-id\",\"nickname\":\"Eve\",\"avatar\":\"cat\"}");

        JsonNode table = client.awaitState(s -> true).get("table");

        assertEquals(10, table.get("smallBlind").asInt());
        assertEquals(2000, table.get("startingStack").asInt());
        assertEquals(5, table.get("handsUntilNextLevel").asInt());
        assertEquals(15, table.at("/nextBlinds/smallBlind").asInt());
    }

    @Test
    void refusesAnOutOfRangeBlindIncrease() {
        given().contentType("application/json").body("{\"blindIncreaseEveryHands\": 10, \"blindIncreasePercent\": 500}")
                .post("/api/tables")
                .then().statusCode(400);
    }

    @Test
    void refusesInvalidTableSettings() {
        given().contentType("application/json").body("{\"smallBlind\": 20, \"bigBlind\": 10}")
                .post("/api/tables")
                .then().statusCode(400);
    }

    @Test
    void unknownTableIsRefusedAndClosed() {
        Client client = connect("ZZZZ");

        JsonNode error = client.await("error", message -> true);
        assertEquals("TABLE_NOT_FOUND", error.get("code").asText());
        client.awaitClosed();
    }

    @Test
    void answersPingAndRefusesUnreadableOrPrematureMessages() {
        Client client = connect(createTable());

        client.send("{\"type\":\"ping\"}");
        client.await("pong", message -> true);

        client.send("not json");
        assertEquals("INVALID_MESSAGE", client.await("error", message -> true).get("code").asText());

        client.send("{\"type\":\"sit\",\"seat\":0}");
        assertEquals("NOT_JOINED", client.await("error", message -> true).get("code").asText());
    }

    @Test
    void twoPlayersPlayAHandFromStartToShowdown() {
        String code = createTable();
        Client alice = connect(code);
        Client bob = connect(code);
        alice.send("{\"type\":\"join\",\"playerId\":\"alice-id\",\"nickname\":\"Alice\",\"avatar\":\"fox\"}");
        alice.awaitState(state -> true);
        bob.send("{\"type\":\"join\",\"playerId\":\"bob-id\",\"nickname\":\"Bob\",\"avatar\":\"bear\"}");
        alice.send("{\"type\":\"sit\",\"seat\":0}");
        bob.awaitState(state -> state.at("/seats/0").isObject());
        bob.send("{\"type\":\"sit\",\"seat\":1}");
        alice.awaitState(state -> state.at("/seats/1").isObject());

        alice.send("{\"type\":\"start\"}");
        JsonNode handStarted = bob.await("event", event -> event.get("name").asText().equals("handStarted"));
        assertEquals(1, handStarted.get("handNumber").asInt());

        JsonNode last = playToTheEnd(alice, bob);

        assertEquals("complete", last.at("/hand/stage").asText());
        assertEquals(2, last.at("/lastResult/shownHands").size());
        assertEquals(2000, last.at("/seats/0/stack").asInt() + last.at("/seats/1/stack").asInt());
        // Once shown, Bob's cards are visible to Alice.
        assertEquals(2, last.at("/seats/1/cards").size());

        // Before the showdown, nobody ever saw the other's cards.
        for (JsonNode state : alice.states()) {
            if (!state.at("/hand/stage").asText().equals("complete")) {
                assertTrue(state.at("/seats/1/cards").isNull() || state.at("/seats/1").isNull(), state.toString());
            }
        }
        for (JsonNode state : bob.states()) {
            if (!state.at("/hand/stage").asText().equals("complete")) {
                assertTrue(state.at("/seats/0/cards").isNull() || state.at("/seats/0").isNull(), state.toString());
            }
        }
    }

    @Test
    void reconnectingWithTheSamePlayerIdTakesOverTheSeat() {
        String code = createTable();
        Client first = connect(code);
        first.send("{\"type\":\"join\",\"playerId\":\"carol-id\",\"nickname\":\"Carol\",\"avatar\":\"owl\"}");
        first.send("{\"type\":\"sit\",\"seat\":4}");
        first.awaitState(state -> state.at("/you/seat").asInt(-1) == 4);

        Client second = connect(code);
        second.send("{\"type\":\"join\",\"playerId\":\"carol-id\",\"nickname\":\"Carol\",\"avatar\":\"owl\"}");

        JsonNode state = second.awaitState(s -> true);
        assertEquals(4, state.at("/you/seat").asInt());
        assertTrue(state.at("/you/isHost").asBoolean());
        assertTrue(state.at("/seats/4/connected").asBoolean());
        first.awaitClosed();
        assertTrue(second.connection.isOpen());
    }

    /** Whoever must act checks when possible, otherwise calls, until the hand is over. */
    private static JsonNode playToTheEnd(Client alice, Client bob) {
        for (int step = 0; step < 20; step++) {
            JsonNode state = alice.awaitState(s -> s.at("/hand/stage").asText().equals("complete")
                    || s.at("/hand/toActSeat").isInt());
            if (state.at("/hand/stage").asText().equals("complete")) {
                return state;
            }
            // Alice just read the current state; Bob reads his own copy of it.
            Client actor = state.at("/hand/toActSeat").asInt() == 0 ? alice : bob;
            JsonNode actorState = actor == alice ? state : bob.awaitState(s -> s.at("/allowedActions").isObject());
            boolean canCheck = actorState.at("/allowedActions/types").toString().contains("\"check\"");
            actor.send("{\"type\":\"action\",\"action\":\"" + (canCheck ? "check" : "call") + "\"}");
        }
        fail("The hand did not end");
        return null;
    }

    private String createTable() {
        return given().contentType("application/json").body("{}")
                .post("/api/tables")
                .then().statusCode(201)
                .extract().path("code");
    }

    private Client connect(String code) {
        Client client = new Client();
        client.connection = connectors.get()
                .baseUri(baseUri)
                .path("/ws/table/" + code)
                .onTextMessage((connection, text) -> client.received.add(parse(text)))
                .connectAndAwait();
        return client;
    }

    private static JsonNode parse(String text) {
        try {
            return JSON.readTree(text);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A test client that reads its messages in order. */
    private static final class Client {

        WebSocketClientConnection connection;
        final BlockingQueue<JsonNode> received = new LinkedBlockingQueue<>();
        final List<JsonNode> seenStates = new ArrayList<>();

        void send(String json) {
            connection.sendTextAndAwait(json);
        }

        /** Next message of the given type matching the predicate; earlier messages are skipped. */
        JsonNode await(String type, Predicate<JsonNode> predicate) {
            long deadline = System.nanoTime() + WAIT.toNanos();
            while (System.nanoTime() < deadline) {
                JsonNode message;
                try {
                    message = received.poll(50, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
                if (message == null) {
                    continue;
                }
                if (message.get("type").asText().equals("state")) {
                    seenStates.add(message);
                }
                if (message.get("type").asText().equals(type) && predicate.test(message)) {
                    return message;
                }
            }
            fail("No '" + type + "' message received in time");
            return null;
        }

        JsonNode awaitState(Predicate<JsonNode> predicate) {
            return await("state", predicate);
        }

        List<JsonNode> states() {
            return seenStates;
        }

        void awaitClosed() {
            long deadline = System.nanoTime() + WAIT.toNanos();
            while (connection.isOpen() && System.nanoTime() < deadline) {
                Thread.onSpinWait();
            }
            assertFalse(connection.isOpen(), "connection still open");
        }
    }

    @Test
    void stateUsesTheProtocolFieldNames() {
        Client client = connect(createTable());
        client.send("{\"type\":\"join\",\"playerId\":\"dan-id\",\"nickname\":\"Dan\",\"avatar\":\"cat\"}");

        JsonNode state = client.awaitState(s -> true);

        assertNotNull(state.get("you").get("isHost"), state.toString());
        assertEquals(8, state.get("seats").size());
        assertTrue(state.get("hand").isNull());
        assertEquals("waiting", state.at("/table/status").asText());
    }
}
