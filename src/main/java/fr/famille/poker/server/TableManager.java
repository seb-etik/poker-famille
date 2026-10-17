package fr.famille.poker.server;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import fr.famille.poker.engine.Deck;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/** The tables in memory, by code. A table nobody is connected to is dropped after a while. */
@ApplicationScoped
public class TableManager {

    private static final long PURGE_PERIOD_MS = 60_000;

    private final Map<String, GameTable> tables = new ConcurrentHashMap<>();
    private final TableCodes codes = new TableCodes(new SecureRandom());

    @Inject
    PokerConfig config;

    @Inject
    Vertx vertx;

    @Inject
    ConnectionRegistry connections;

    @Inject
    ResultStore results;

    void onStart(@Observes StartupEvent event) {
        vertx.setPeriodic(PURGE_PERIOD_MS, ignored -> purgeAbandonedTables());
    }

    public GameTable create(TableSettings settings) {
        Timings timings = new Timings(config.actionTimeout(), config.absentPlayerDelay(),
                config.pauseAfterFold(), config.pauseAfterShowdown());
        while (true) {
            String code = codes.next();
            GameTable table = new GameTable(code, settings, timings,
                    (playerId, message) -> connections.send(code, playerId, message),
                    new VertxScheduler(vertx), Deck::new, Clock.systemUTC(), results);
            if (tables.putIfAbsent(code, table) == null) {
                Log.infof("Table %s created", code);
                return table;
            }
        }
    }

    public Optional<GameTable> find(String code) {
        return code == null ? Optional.empty() : Optional.ofNullable(tables.get(code.toUpperCase(Locale.ROOT)));
    }

    void purgeAbandonedTables() {
        tables.values().removeIf(table -> {
            if (!table.isAbandoned(config.idleTableTimeout())) {
                return false;
            }
            table.close();
            connections.forgetTable(table.code());
            Log.infof("Table %s dropped after inactivity", table.code());
            return true;
        });
    }
}
