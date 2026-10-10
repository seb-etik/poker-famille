package fr.famille.poker.server;

import java.util.random.RandomGenerator;

/** Four capital letters, without I and O that read like 1 and 0. */
final class TableCodes {

    static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    static final int LENGTH = 4;

    private final RandomGenerator random;

    TableCodes(RandomGenerator random) {
        this.random = random;
    }

    String next() {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
