package fr.famille.poker.engine;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

/** A table: its seats, its blinds and the button, which moves from one hand to the next. */
public final class Table {

    public static final int MAX_SEATS = 8;

    private final String code;
    private final int smallBlind;
    private final int bigBlind;
    private final List<Seat> seats = IntStream.range(0, MAX_SEATS).mapToObj(Seat::new).toList();
    private Integer buttonSeat;
    private Hand currentHand;

    public Table(String code, int smallBlind, int bigBlind) {
        this.code = Objects.requireNonNull(code, "code");
        this.smallBlind = smallBlind;
        this.bigBlind = bigBlind;
    }

    public String code() {
        return code;
    }

    public int smallBlind() {
        return smallBlind;
    }

    public int bigBlind() {
        return bigBlind;
    }

    public List<Seat> seats() {
        return seats;
    }

    public Optional<Hand> currentHand() {
        return Optional.ofNullable(currentHand);
    }

    public void sit(int seatNumber, Player player) {
        if (seatNumber < 0 || seatNumber >= MAX_SEATS) {
            throw new IllegalArgumentException("No seat " + seatNumber);
        }
        boolean alreadySeated = seats.stream()
                .anyMatch(seat -> !seat.isEmpty() && seat.player().id().equals(player.id()));
        if (alreadySeated) {
            throw new IllegalStateException(player.nickname() + " is already seated");
        }
        seats.get(seatNumber).sit(player);
    }

    /** Deals a new hand to every seated player who has chips, the button moving one player on. */
    public Hand startHand(Deck deck) {
        if (currentHand != null && !currentHand.isComplete()) {
            throw new IllegalStateException("A hand is in progress");
        }
        List<Seat> dealtIn = seats.stream()
                .filter(seat -> !seat.isEmpty() && seat.player().stack() > 0)
                .toList();
        if (dealtIn.size() < 2) {
            throw new IllegalStateException("A hand needs at least two players with chips");
        }
        seats.stream().filter(seat -> !dealtIn.contains(seat)).forEach(Seat::sitOut);

        buttonSeat = nextButton(dealtIn);
        currentHand = Hand.start(dealtIn, buttonSeat, smallBlind, bigBlind, deck);
        return currentHand;
    }

    /** First seat dealt in after the previous button, wrapping around; the first one for the first hand. */
    private int nextButton(List<Seat> dealtIn) {
        if (buttonSeat == null) {
            return dealtIn.getFirst().number();
        }
        return dealtIn.stream()
                .map(Seat::number)
                .filter(number -> number > buttonSeat)
                .findFirst()
                .orElse(dealtIn.getFirst().number());
    }
}
