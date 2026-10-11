package fr.famille.poker.engine;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.IntStream;

/** A table: its seats, its blinds and the button, which moves from one hand to the next. */
public final class Table {

    public static final int MAX_SEATS = 8;

    private final String code;
    private final BlindSchedule blinds;
    private final List<Seat> seats = IntStream.range(0, MAX_SEATS).mapToObj(Seat::new).toList();
    private Integer buttonSeat;
    private Hand currentHand;
    private int handsDealt;

    /** A table with fixed blinds. */
    public Table(String code, int smallBlind, int bigBlind) {
        this(code, BlindSchedule.fixed(smallBlind, bigBlind));
    }

    public Table(String code, BlindSchedule blinds) {
        this.code = Objects.requireNonNull(code, "code");
        this.blinds = Objects.requireNonNull(blinds, "blinds");
    }

    public String code() {
        return code;
    }

    /** Level of the hand in progress or just played; 1 before the first hand. */
    public int blindLevel() {
        return blinds.level(handsDealt);
    }

    public Blinds currentBlinds() {
        return blinds.blindsAt(blindLevel());
    }

    /** Blinds of the next level; empty when the blinds are fixed. */
    public Optional<Blinds> nextBlinds() {
        return blinds.increases() ? Optional.of(blinds.blindsAt(blindLevel() + 1)) : Optional.empty();
    }

    /** Hands still to be dealt at the current blinds (0: the next hand goes up); empty when fixed. */
    public OptionalInt handsUntilNextLevel() {
        return blinds.increases()
                ? OptionalInt.of(blindLevel() * blinds.handsPerLevel() - handsDealt)
                : OptionalInt.empty();
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

    /** Frees a seat and returns its player; not allowed for a seat dealt in the hand in progress. */
    public Player leave(int seatNumber) {
        Seat seat = seats.get(seatNumber);
        boolean dealtIn = currentHand != null && !currentHand.isComplete() && currentHand.seats().contains(seat);
        if (dealtIn) {
            throw new IllegalStateException("Seat " + seatNumber + " is in the hand in progress");
        }
        return seat.vacate();
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
        handsDealt++;
        Blinds handBlinds = currentBlinds();
        currentHand = Hand.start(dealtIn, buttonSeat, handBlinds.small(), handBlinds.big(), deck);
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
