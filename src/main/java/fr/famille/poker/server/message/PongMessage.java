package fr.famille.poker.server.message;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("pong")
public record PongMessage() implements ServerMessage {
}
