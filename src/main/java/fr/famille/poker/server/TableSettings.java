package fr.famille.poker.server;

import fr.famille.poker.engine.BlindSchedule;

/**
 * Chosen when the table is created.
 *
 * @param handsPerLevel hands between two blind increases; 0 keeps the blinds fixed
 * @param increasePercent how much the blinds go up at each level
 */
public record TableSettings(int smallBlind, int bigBlind, int startingStack, int handsPerLevel, int increasePercent) {

    public static final TableSettings DEFAULT = new TableSettings(5, 10, 1000, 10, 50);

    public TableSettings {
        if (smallBlind <= 0 || bigBlind < smallBlind || startingStack < bigBlind) {
            throw new IllegalArgumentException("Invalid table settings: " + smallBlind + "/" + bigBlind + ", " + startingStack);
        }
        if (handsPerLevel < 0 || handsPerLevel > 100) {
            throw new IllegalArgumentException("Invalid hands per level: " + handsPerLevel);
        }
        if (handsPerLevel > 0 && (increasePercent < 10 || increasePercent > 200)) {
            throw new IllegalArgumentException("Invalid blind increase: " + increasePercent + " %");
        }
    }

    /** Fixed blinds. */
    public TableSettings(int smallBlind, int bigBlind, int startingStack) {
        this(smallBlind, bigBlind, startingStack, 0, 0);
    }

    public BlindSchedule blindSchedule() {
        return new BlindSchedule(smallBlind, bigBlind, handsPerLevel, increasePercent);
    }
}
