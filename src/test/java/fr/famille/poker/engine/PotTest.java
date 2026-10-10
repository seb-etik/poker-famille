package fr.famille.poker.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class PotTest {

    @Test
    void equalContributionsMakeASinglePot() {
        List<Pot> pots = Pot.split(Map.of(0, 100, 1, 100, 2, 100), Set.of(0, 1, 2));

        assertEquals(List.of(new Pot(300, List.of(0, 1, 2))), pots);
    }

    @Test
    void foldedChipsStayInThePotWithoutEligibility() {
        List<Pot> pots = Pot.split(Map.of(0, 10, 1, 5, 2, 10), Set.of(0, 2));

        assertEquals(List.of(new Pot(25, List.of(0, 2))), pots);
    }

    @Test
    void eachAllInLevelOpensASidePot() {
        List<Pot> pots = Pot.split(Map.of(0, 400, 1, 400, 2, 150), Set.of(0, 1, 2));

        assertEquals(List.of(new Pot(450, List.of(0, 1, 2)), new Pot(500, List.of(0, 1))), pots);
    }

    @Test
    void threeLevelsMakeThreePots() {
        List<Pot> pots = Pot.split(Map.of(0, 100, 1, 300, 2, 500, 3, 500), Set.of(0, 1, 2, 3));

        assertEquals(List.of(
                new Pot(400, List.of(0, 1, 2, 3)),
                new Pot(600, List.of(1, 2, 3)),
                new Pot(400, List.of(2, 3))), pots);
    }

    @Test
    void foldedChipsAboveAnAllInFeedTheSidePot() {
        List<Pot> pots = Pot.split(Map.of(0, 100, 1, 300, 2, 300), Set.of(0, 2));

        assertEquals(List.of(new Pot(300, List.of(0, 2)), new Pot(400, List.of(2))), pots);
    }

    @Test
    void foldedChipsAboveEveryContenderGoToTheLastPot() {
        List<Pot> pots = Pot.split(Map.of(0, 100, 1, 300), Set.of(0));

        assertEquals(List.of(new Pot(400, List.of(0))), pots);
    }

    @Test
    void noChipsNoPot() {
        assertEquals(List.of(), Pot.split(Map.of(0, 0, 1, 0), Set.of(0, 1)));
    }
}
