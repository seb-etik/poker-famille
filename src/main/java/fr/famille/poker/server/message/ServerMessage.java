package fr.famille.poker.server.message;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/** Messages the server sends; each implementation names its {@code type} with {@code @JsonTypeName}. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public sealed interface ServerMessage permits StateMessage, EventMessage, ErrorMessage, PongMessage {
}
