package fr.famille.poker.engine;

import java.util.Objects;

/** Someone sitting at the table, identified by the id their phone keeps, with their chips. */
public final class Player {

    private final String id;
    private final String nickname;
    private final String avatar;
    private int stack;

    public Player(String id, String nickname, String avatar, int stack) {
        if (stack < 0) {
            throw new IllegalArgumentException("Negative stack: " + stack);
        }
        this.id = Objects.requireNonNull(id, "id");
        this.nickname = Objects.requireNonNull(nickname, "nickname");
        this.avatar = Objects.requireNonNull(avatar, "avatar");
        this.stack = stack;
    }

    public String id() {
        return id;
    }

    public String nickname() {
        return nickname;
    }

    public String avatar() {
        return avatar;
    }

    public int stack() {
        return stack;
    }

    void addChips(int amount) {
        stack += amount;
    }

    void removeChips(int amount) {
        if (amount > stack) {
            throw new IllegalStateException(nickname + " cannot pay " + amount + " with " + stack);
        }
        stack -= amount;
    }
}
