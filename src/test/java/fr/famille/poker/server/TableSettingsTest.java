package fr.famille.poker.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import fr.famille.poker.engine.BlindSchedule;
import fr.famille.poker.engine.Blinds;

class TableSettingsTest {

    @Test
    void byDefaultTheBlindsDoubleEveryFiveHands() {
        BlindSchedule schedule = TableSettings.DEFAULT.blindSchedule();

        assertEquals(1, schedule.level(5));
        assertEquals(2, schedule.level(6));
        List<Blinds> levels = IntStream.rangeClosed(1, 5).mapToObj(schedule::blindsAt).toList();
        assertEquals(List.of(new Blinds(10, 20), new Blinds(20, 40), new Blinds(40, 80), new Blinds(80, 160),
                new Blinds(150, 300)), levels);
    }
}
