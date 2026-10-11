package fr.famille.poker.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class BlindScheduleTest {

    @Test
    void fixedBlindsNeverChange() {
        BlindSchedule fixed = BlindSchedule.fixed(5, 10);

        assertFalse(fixed.increases());
        assertEquals(1, fixed.level(1));
        assertEquals(1, fixed.level(500));
        assertEquals(new Blinds(5, 10), fixed.blindsAt(1));
    }

    @Test
    void aLevelLastsTheGivenNumberOfHands() {
        BlindSchedule schedule = new BlindSchedule(5, 10, 10, 50);

        assertTrue(schedule.increases());
        assertEquals(1, schedule.level(1));
        assertEquals(1, schedule.level(10));
        assertEquals(2, schedule.level(11));
        assertEquals(3, schedule.level(21));
    }

    @Test
    void blindsGoUpByThePercentageRoundedToNiceValues() {
        BlindSchedule schedule = new BlindSchedule(5, 10, 10, 50);

        List<Blinds> levels = IntStream.rangeClosed(1, 8).mapToObj(schedule::blindsAt).toList();

        assertEquals(List.of(
                new Blinds(5, 10),
                new Blinds(8, 16),
                new Blinds(10, 20),
                new Blinds(15, 30),
                new Blinds(25, 50),
                new Blinds(40, 80),
                new Blinds(60, 120),
                new Blinds(100, 200)), levels);
    }

    @Test
    void keepsTheRatioBetweenSmallAndBigBlind() {
        BlindSchedule schedule = new BlindSchedule(10, 25, 10, 50);

        assertEquals(new Blinds(15, 38), schedule.blindsAt(2));
    }

    @Test
    void alwaysGoesUpEvenWithASmallPercentage() {
        BlindSchedule schedule = new BlindSchedule(1, 2, 10, 10);

        assertEquals(new Blinds(2, 4), schedule.blindsAt(2));
        assertEquals(new Blinds(3, 6), schedule.blindsAt(3));
    }

    @Test
    void rejectsInvalidSettings() {
        assertThrows(IllegalArgumentException.class, () -> new BlindSchedule(0, 10, 10, 50));
        assertThrows(IllegalArgumentException.class, () -> new BlindSchedule(20, 10, 10, 50));
        assertThrows(IllegalArgumentException.class, () -> new BlindSchedule(5, 10, -1, 50));
        assertThrows(IllegalArgumentException.class, () -> new BlindSchedule(5, 10, 10, 0));
    }
}
