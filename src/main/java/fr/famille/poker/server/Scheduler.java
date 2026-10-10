package fr.famille.poker.server;

import java.time.Duration;

/** Runs a task later: action timers and pauses between hands. */
@FunctionalInterface
public interface Scheduler {

    Cancellable schedule(Duration delay, Runnable task);

    @FunctionalInterface
    interface Cancellable {
        void cancel();
    }
}
