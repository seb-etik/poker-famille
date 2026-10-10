package fr.famille.poker.server;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.famille.poker.server.message.ServerMessage;
import io.quarkus.logging.Log;
import io.quarkus.websockets.next.OpenConnections;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Which WebSocket connection each player of each table uses, and sending JSON to them. */
@ApplicationScoped
public class ConnectionRegistry {

    public record Binding(String tableCode, String playerId) {
    }

    private final Map<String, Binding> byConnection = new ConcurrentHashMap<>();
    private final Map<Binding, String> byPlayer = new ConcurrentHashMap<>();

    @Inject
    OpenConnections openConnections;

    @Inject
    ObjectMapper mapper;

    /** Ties a player to a connection; returns the connection they used before, if any. */
    public Optional<String> bind(String tableCode, String playerId, String connectionId) {
        Binding binding = new Binding(tableCode, playerId);
        Binding previousOfConnection = byConnection.put(connectionId, binding);
        if (previousOfConnection != null && !previousOfConnection.equals(binding)) {
            byPlayer.remove(previousOfConnection, connectionId);
        }
        String previous = byPlayer.put(binding, connectionId);
        return Optional.ofNullable(previous).filter(id -> !id.equals(connectionId));
    }

    /**
     * Forgets a closed connection. Returns its player only if it was still their current connection:
     * a connection replaced by a newer one must not mark the player as disconnected.
     */
    public Optional<Binding> unbind(String connectionId) {
        Binding binding = byConnection.remove(connectionId);
        if (binding != null && byPlayer.remove(binding, connectionId)) {
            return Optional.of(binding);
        }
        return Optional.empty();
    }

    public Optional<String> playerOf(String connectionId) {
        return Optional.ofNullable(byConnection.get(connectionId)).map(Binding::playerId);
    }

    public void forgetTable(String tableCode) {
        byPlayer.keySet().removeIf(binding -> binding.tableCode().equals(tableCode));
        byConnection.values().removeIf(binding -> binding.tableCode().equals(tableCode));
    }

    public void send(String tableCode, String playerId, ServerMessage message) {
        Optional.ofNullable(byPlayer.get(new Binding(tableCode, playerId)))
                .flatMap(openConnections::findByConnectionId)
                .ifPresent(connection -> sendTo(connection, message));
    }

    public void close(String connectionId) {
        openConnections.findByConnectionId(connectionId)
                .ifPresent(connection -> connection.close().subscribe().with(ok -> { }, e -> { }));
    }

    public void sendTo(WebSocketConnection connection, ServerMessage message) {
        String json;
        try {
            json = mapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot write " + message, e);
        }
        connection.sendText(json).subscribe().with(
                ok -> { },
                failure -> Log.debugf("Could not send to %s: %s", connection.id(), failure.getMessage()));
    }
}
