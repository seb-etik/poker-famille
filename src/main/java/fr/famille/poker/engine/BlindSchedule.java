package fr.famille.poker.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * How the blinds go up: every {@code handsPerLevel} hands (0 = fixed blinds), the small blind
 * grows by {@code increasePercent} %, rounded to a nice value; the big blind keeps the starting ratio.
 */
public record BlindSchedule(int smallBlind, int bigBlind, int handsPerLevel, int increasePercent) {

    /** Nice amounts from 10 up: these mantissas times a power of ten. Below 10, every integer. */
    private static final double[] NICE_MANTISSAS = {1, 1.5, 2, 2.5, 3, 4, 5, 6, 8};

    public BlindSchedule {
        if (smallBlind <= 0 || bigBlind < smallBlind) {
            throw new IllegalArgumentException("Invalid blinds: " + smallBlind + "/" + bigBlind);
        }
        if (handsPerLevel < 0) {
            throw new IllegalArgumentException("Invalid hands per level: " + handsPerLevel);
        }
        if (handsPerLevel > 0 && increasePercent <= 0) {
            throw new IllegalArgumentException("Invalid increase: " + increasePercent + " %");
        }
    }

    public static BlindSchedule fixed(int smallBlind, int bigBlind) {
        return new BlindSchedule(smallBlind, bigBlind, 0, 0);
    }

    public boolean increases() {
        return handsPerLevel > 0;
    }

    /** Level of a hand, from 1: hands 1 to {@code handsPerLevel} are level 1. */
    public int level(int handNumber) {
        return increases() ? (Math.max(1, handNumber) - 1) / handsPerLevel + 1 : 1;
    }

    public Blinds blindsAt(int level) {
        int small = smallBlind;
        for (int i = 1; i < level && increases(); i++) {
            // Each level grows from the previous rounded amount, and always goes up.
            small = Math.max(nearestNice(small * (1 + increasePercent / 100.0)), nextNiceAbove(small));
        }
        int big = (int) Math.round((double) small * bigBlind / smallBlind);
        return new Blinds(small, big);
    }

    private static int nearestNice(double amount) {
        int best = 1;
        for (int nice : niceValuesUpTo(amount * 2)) {
            // On a tie, the higher value wins.
            if (Math.abs(nice - amount) <= Math.abs(best - amount)) {
                best = nice;
            }
        }
        return best;
    }

    /** The nice value just above {@code amount}, so that a level never repeats the previous one. */
    private static int nextNiceAbove(int amount) {
        return niceValuesUpTo(amount * 2 + 10).stream().filter(nice -> nice > amount).findFirst().orElseThrow();
    }

    private static List<Integer> niceValuesUpTo(double limit) {
        List<Integer> values = new ArrayList<>();
        for (int i = 1; i < 10; i++) {
            values.add(i);
        }
        for (long power = 10; power <= limit * 10; power *= 10) {
            for (double mantissa : NICE_MANTISSAS) {
                values.add((int) (mantissa * power));
            }
        }
        return values;
    }
}
