package fr.famille.poker.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** A pot and the seats that can win it: the main pot, or a side pot opened by an all-in. */
public record Pot(int amount, List<Integer> eligibleSeats) {

    public Pot {
        eligibleSeats = List.copyOf(eligibleSeats);
    }

    /**
     * Splits the chips put in during a hand into a main pot and side pots, one per all-in level.
     *
     * @param contributions chips put in by each seat, folded seats included
     * @param contenders seats that have not folded: the only ones who can win
     */
    static List<Pot> split(Map<Integer, Integer> contributions, Set<Integer> contenders) {
        Map<Integer, Integer> bySeat = new TreeMap<>(contributions);
        List<Integer> levels = bySeat.entrySet().stream()
                .filter(entry -> contenders.contains(entry.getKey()) && entry.getValue() > 0)
                .map(Map.Entry::getValue)
                .distinct()
                .sorted()
                .toList();

        List<Pot> pots = new ArrayList<>();
        int previousLevel = 0;
        for (int i = 0; i < levels.size(); i++) {
            int level = levels.get(i);
            boolean lastLevel = i == levels.size() - 1;
            int amount = 0;
            List<Integer> eligible = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : bySeat.entrySet()) {
                int contribution = entry.getValue();
                // Folded chips above every contender's level have nowhere else to go.
                int counted = lastLevel ? contribution : Math.min(contribution, level);
                amount += Math.max(0, counted - previousLevel);
                if (contenders.contains(entry.getKey()) && contribution >= level) {
                    eligible.add(entry.getKey());
                }
            }
            pots.add(new Pot(amount, eligible));
            previousLevel = level;
        }
        return pots;
    }
}
