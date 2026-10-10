package fr.famille.poker.server;

import java.time.Duration;

/**
 * @param actionTimeout time a connected player has to act
 * @param absentPlayerDelay delay before playing for a disconnected or leaving player
 * @param pauseAfterFold pause before the next hand when everyone else folded
 * @param pauseAfterShowdown pause before the next hand after a showdown
 */
public record Timings(Duration actionTimeout, Duration absentPlayerDelay, Duration pauseAfterFold,
        Duration pauseAfterShowdown) {
}
