package fr.famille.poker.server;

import java.time.Duration;

import io.quarkus.logging.Log;
import io.vertx.core.Vertx;

/** {@link Scheduler} on Vert.x timers. Tasks run on the event loop and must not block for long. */
final class VertxScheduler implements Scheduler {

    private final Vertx vertx;

    VertxScheduler(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public Cancellable schedule(Duration delay, Runnable task) {
        long timerId = vertx.setTimer(Math.max(1, delay.toMillis()), ignored -> {
            try {
                task.run();
            } catch (RuntimeException e) {
                Log.error("Scheduled task failed", e);
            }
        });
        return () -> vertx.cancelTimer(timerId);
    }
}
