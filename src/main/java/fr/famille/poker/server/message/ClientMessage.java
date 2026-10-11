package fr.famille.poker.server.message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/** Messages a client sends, told apart by their {@code type} field (see docs/PROTOCOL.md). */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ClientMessage.Join.class, name = "join"),
    @JsonSubTypes.Type(value = ClientMessage.Sit.class, name = "sit"),
    @JsonSubTypes.Type(value = ClientMessage.Leave.class, name = "leave"),
    @JsonSubTypes.Type(value = ClientMessage.Start.class, name = "start"),
    @JsonSubTypes.Type(value = ClientMessage.Restart.class, name = "restart"),
    @JsonSubTypes.Type(value = ClientMessage.Act.class, name = "action"),
    @JsonSubTypes.Type(value = ClientMessage.Ping.class, name = "ping"),
})
public sealed interface ClientMessage {

    record Join(String playerId, String nickname, String avatar) implements ClientMessage {
    }

    record Sit(Integer seat) implements ClientMessage {
    }

    record Leave() implements ClientMessage {
    }

    record Start() implements ClientMessage {
    }

    record Restart() implements ClientMessage {
    }

    record Act(String action, Integer amount) implements ClientMessage {
    }

    record Ping() implements ClientMessage {
    }
}
