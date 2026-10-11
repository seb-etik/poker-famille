package fr.famille.poker.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.famille.poker.server.message.ClientMessage;
import fr.famille.poker.server.message.ErrorCode;
import fr.famille.poker.server.message.ErrorMessage;
import fr.famille.poker.server.message.PongMessage;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.inject.Inject;

/** Translates WebSocket messages into calls on the {@link GameTable}; holds no game logic. */
@WebSocket(path = "/ws/table/{code}")
public class TableEndpoint {

    @Inject
    TableManager tables;

    @Inject
    ConnectionRegistry connections;

    @Inject
    ObjectMapper mapper;

    @OnOpen
    public void onOpen(WebSocketConnection connection) {
        if (tables.find(connection.pathParam("code")).isEmpty()) {
            refuseUnknownTable(connection);
        }
    }

    @OnTextMessage
    public void onMessage(String text, WebSocketConnection connection) {
        GameTable table = tables.find(connection.pathParam("code")).orElse(null);
        if (table == null) {
            refuseUnknownTable(connection);
            return;
        }
        try {
            handle(table, parse(text), connection);
        } catch (GameException e) {
            connections.sendTo(connection, new ErrorMessage(e.code(), e.getMessage()));
        }
    }

    @OnClose
    public void onClose(WebSocketConnection connection) {
        connections.unbind(connection.id()).ifPresent(binding ->
                tables.find(binding.tableCode()).ifPresent(table -> table.disconnect(binding.playerId())));
    }

    private void handle(GameTable table, ClientMessage message, WebSocketConnection connection) {
        switch (message) {
            case ClientMessage.Ping ping -> connections.sendTo(connection, new PongMessage());
            case ClientMessage.Join join -> {
                connections.bind(table.code(), join.playerId(), connection.id()).ifPresent(connections::close);
                try {
                    table.join(join.playerId(), join.nickname(), join.avatar());
                } catch (GameException e) {
                    connections.unbind(connection.id());
                    throw e;
                }
            }
            case ClientMessage.Sit sit -> table.sit(playerOf(connection), sit.seat());
            case ClientMessage.Leave leave -> table.leave(playerOf(connection));
            case ClientMessage.Start start -> table.start(playerOf(connection));
            case ClientMessage.Restart restart -> table.restart(playerOf(connection));
            case ClientMessage.Act act -> table.act(playerOf(connection), act.action(), act.amount());
        }
    }

    private String playerOf(WebSocketConnection connection) {
        return connections.playerOf(connection.id())
                .orElseThrow(() -> new GameException(ErrorCode.NOT_JOINED, "Il faut d'abord rejoindre la table."));
    }

    private ClientMessage parse(String text) {
        try {
            return mapper.readValue(text, ClientMessage.class);
        } catch (JsonProcessingException e) {
            throw new GameException(ErrorCode.INVALID_MESSAGE, "Message illisible.");
        }
    }

    private void refuseUnknownTable(WebSocketConnection connection) {
        connections.sendTo(connection, new ErrorMessage(ErrorCode.TABLE_NOT_FOUND, "Cette table n'existe pas."));
        connection.close().subscribe().with(ok -> { }, e -> { });
    }
}
