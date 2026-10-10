package fr.famille.poker.server;

import fr.famille.poker.server.message.ServerMessage;

/** Sends a message to a player, wherever they are connected; silently dropped if they are not. */
@FunctionalInterface
public interface Outbox {

    void send(String playerId, ServerMessage message);
}
