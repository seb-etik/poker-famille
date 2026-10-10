package fr.famille.poker.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** A place at the table and what its player does in the current hand. */
public final class Seat {

    private final int number;
    private Player player;
    private final List<Card> holeCards = new ArrayList<>(2);
    private int bet;
    private int totalBet;
    private SeatStatus status = SeatStatus.SITTING_OUT;

    public Seat(int number) {
        this.number = number;
    }

    public int number() {
        return number;
    }

    public boolean isEmpty() {
        return player == null;
    }

    /** The seated player; fails on an empty seat. */
    public Player player() {
        return Optional.ofNullable(player).orElseThrow(() -> new IllegalStateException("Seat " + number + " is empty"));
    }

    public List<Card> holeCards() {
        return List.copyOf(holeCards);
    }

    /** Chips put in during the current betting round. */
    public int bet() {
        return bet;
    }

    /** Chips put in during the whole hand. */
    public int totalBet() {
        return totalBet;
    }

    public SeatStatus status() {
        return status;
    }

    void sit(Player player) {
        if (this.player != null) {
            throw new IllegalStateException("Seat " + number + " is taken");
        }
        this.player = Objects.requireNonNull(player, "player");
    }

    void startHand() {
        holeCards.clear();
        bet = 0;
        totalBet = 0;
        status = SeatStatus.ACTIVE;
    }

    void sitOut() {
        holeCards.clear();
        bet = 0;
        totalBet = 0;
        status = SeatStatus.SITTING_OUT;
    }

    void receive(Card card) {
        holeCards.add(card);
    }

    /** Puts up to {@code amount} chips in front of the seat; a player short of chips goes all in. */
    int pay(int amount) {
        int paid = Math.min(amount, player.stack());
        player.removeChips(paid);
        bet += paid;
        totalBet += paid;
        if (player.stack() == 0) {
            status = SeatStatus.ALL_IN;
        }
        return paid;
    }

    /** Gives back chips that nobody matched. */
    void refund(int amount) {
        player.addChips(amount);
        bet -= amount;
        totalBet -= amount;
        if (status == SeatStatus.ALL_IN) {
            status = SeatStatus.ACTIVE;
        }
    }

    void endBettingRound() {
        bet = 0;
    }

    void fold() {
        status = SeatStatus.FOLDED;
    }

    boolean isInHand() {
        return status == SeatStatus.ACTIVE || status == SeatStatus.ALL_IN;
    }
}
