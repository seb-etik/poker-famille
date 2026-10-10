package fr.famille.poker.server.message;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("error")
public record ErrorMessage(ErrorCode code, String message) implements ServerMessage {
}
