package fr.famille.poker.server.message;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/** Something to animate, sent just before the state it leads to. */
public sealed interface EventMessage extends ServerMessage {

    @JsonProperty("name")
    String name();

    @JsonTypeName("event")
    record HandStarted(int handNumber, int buttonSeat) implements EventMessage {
        @Override
        public String name() {
            return "handStarted";
        }
    }

    /** The blinds go up from this hand on. */
    @JsonTypeName("event")
    record BlindsUp(int level, int smallBlind, int bigBlind) implements EventMessage {
        @Override
        public String name() {
            return "blindsUp";
        }
    }

    /** {@code amount}: the seat's total bet for the round once the action is done. */
    @JsonTypeName("event")
    record Action(int seat, String action, int amount) implements EventMessage {
        @Override
        public String name() {
            return "action";
        }
    }

    /** {@code cards}: only the cards just dealt. */
    @JsonTypeName("event")
    record Board(String stage, List<String> cards) implements EventMessage {
        @Override
        public String name() {
            return "board";
        }
    }

    @JsonTypeName("event")
    record Showdown() implements EventMessage {
        @Override
        public String name() {
            return "showdown";
        }
    }

    @JsonTypeName("event")
    record Win(List<StateMessage.PayoutView> payouts) implements EventMessage {
        @Override
        public String name() {
            return "win";
        }
    }
}
