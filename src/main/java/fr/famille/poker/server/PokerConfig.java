package fr.famille.poker.server;

import java.time.Duration;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "poker")
public interface PokerConfig {

    @WithDefault("30s")
    Duration actionTimeout();

    @WithDefault("1s")
    Duration absentPlayerDelay();

    @WithDefault("5s")
    Duration pauseAfterFold();

    @WithDefault("8s")
    Duration pauseAfterShowdown();

    /** A table nobody is connected to is dropped after this delay. */
    @WithDefault("30m")
    Duration idleTableTimeout();
}
