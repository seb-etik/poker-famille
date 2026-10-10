package fr.famille.poker.server;

public record TableSettings(int smallBlind, int bigBlind, int startingStack) {

    public static final TableSettings DEFAULT = new TableSettings(5, 10, 1000);

    public TableSettings {
        if (smallBlind <= 0 || bigBlind < smallBlind || startingStack < bigBlind) {
            throw new IllegalArgumentException("Invalid table settings: " + smallBlind + "/" + bigBlind + ", " + startingStack);
        }
    }
}
