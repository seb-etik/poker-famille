package fr.famille.poker.engine;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * One hand of No-Limit Hold'em, from the blinds to the payment of the pots.
 *
 * <p>Seats are given in table order (clockwise). Cards are dealt one at a time starting left of
 * the button, without burn cards: they add nothing with a {@link java.security.SecureRandom} shuffle.
 * When no more betting is possible (everyone all in), the board is dealt at once to the showdown,
 * where every remaining player shows their cards.
 */
public final class Hand {

    private final List<Seat> seats;
    private final int buttonIndex;
    private final int bigBlind;
    private final Deck deck;
    private final List<Card> board = new ArrayList<>(5);
    private Stage stage = Stage.PRE_FLOP;
    private BettingRound round;
    private int toActIndex = -1;
    private int smallBlindIndex;
    private int bigBlindIndex;
    private HandResult result;

    private Hand(List<Seat> seats, int buttonIndex, int bigBlind, Deck deck) {
        this.seats = seats;
        this.buttonIndex = buttonIndex;
        this.bigBlind = bigBlind;
        this.deck = deck;
    }

    /**
     * Posts the blinds, deals the hole cards and waits for the first player to act.
     *
     * @param seats the seats dealt in, in table order, each with a player who has chips
     */
    public static Hand start(List<Seat> seats, int buttonSeat, int smallBlind, int bigBlind, Deck deck) {
        if (seats.size() < 2) {
            throw new IllegalArgumentException("A hand needs at least two players");
        }
        if (seats.stream().anyMatch(seat -> seat.isEmpty() || seat.player().stack() == 0)) {
            throw new IllegalArgumentException("Every seat dealt in needs a player with chips");
        }
        if (smallBlind <= 0 || bigBlind < smallBlind) {
            throw new IllegalArgumentException("Invalid blinds: " + smallBlind + "/" + bigBlind);
        }
        int buttonIndex = indexOf(seats, buttonSeat);
        Hand hand = new Hand(List.copyOf(seats), buttonIndex, bigBlind, deck);
        hand.begin(smallBlind);
        return hand;
    }

    private void begin(int smallBlind) {
        seats.forEach(Seat::startHand);
        // Heads-up, the button posts the small blind.
        smallBlindIndex = seats.size() == 2 ? buttonIndex : next(buttonIndex);
        bigBlindIndex = next(smallBlindIndex);
        seats.get(smallBlindIndex).pay(smallBlind);
        seats.get(bigBlindIndex).pay(bigBlind);
        // Even when the big blind is short, the others must match a full big blind.
        round = new BettingRound(bigBlind, bigBlind);

        for (int card = 0; card < 2; card++) {
            for (int offset = 1; offset <= seats.size(); offset++) {
                seats.get((buttonIndex + offset) % seats.size()).receive(deck.draw());
            }
        }
        proceedAfter(bigBlindIndex);
    }

    public Stage stage() {
        return stage;
    }

    public boolean isComplete() {
        return stage == Stage.COMPLETE;
    }

    public List<Seat> seats() {
        return seats;
    }

    public int buttonSeat() {
        return seats.get(buttonIndex).number();
    }

    public int smallBlindSeat() {
        return seats.get(smallBlindIndex).number();
    }

    public int bigBlindSeat() {
        return seats.get(bigBlindIndex).number();
    }

    public List<Card> board() {
        return List.copyOf(board);
    }

    /** Seat number of the player who must act, empty once the hand is over. */
    public OptionalInt toAct() {
        return isComplete() ? OptionalInt.empty() : OptionalInt.of(seats.get(toActIndex).number());
    }

    /** Bet to match in the current round. */
    public int currentBet() {
        return isComplete() ? 0 : round.currentBet();
    }

    /** Pots from the previous rounds; bets of the current round are on {@link Seat#bet()}. */
    public List<Pot> pots() {
        Map<Integer, Integer> contributions = new LinkedHashMap<>();
        for (Seat seat : seats) {
            contributions.put(seat.number(), seat.totalBet() - seat.bet());
        }
        Set<Integer> contenders = seats.stream()
                .filter(Seat::isInHand)
                .map(Seat::number)
                .collect(Collectors.toSet());
        return Pot.split(contributions, contenders);
    }

    public Optional<HandResult> result() {
        return Optional.ofNullable(result);
    }

    public AllowedActions allowedActions() {
        if (isComplete()) {
            return AllowedActions.none();
        }
        Seat seat = seats.get(toActIndex);
        int currentBet = round.currentBet();
        int toCall = currentBet - seat.bet();
        int callAmount = Math.min(toCall, seat.player().stack());
        int allInAmount = seat.bet() + seat.player().stack();

        Set<ActionType> types = EnumSet.noneOf(ActionType.class);
        if (toCall > 0) {
            types.add(ActionType.FOLD);
            types.add(ActionType.CALL);
        } else {
            types.add(ActionType.CHECK);
        }
        boolean opponentCanBet = seats.stream()
                .anyMatch(other -> other != seat && other.status() == SeatStatus.ACTIVE);
        if (opponentCanBet && allInAmount > currentBet && round.mayRaise(seat)) {
            types.add(currentBet == 0 ? ActionType.BET : ActionType.RAISE);
            // Going all in is always allowed, even below the minimum.
            int minAmount = Math.min(currentBet + round.minRaise(), allInAmount);
            return new AllowedActions(types, callAmount, minAmount, allInAmount);
        }
        return new AllowedActions(types, callAmount, 0, 0);
    }

    /** Plays an action for the given seat, or throws {@link IllegalActionException} without changing anything. */
    public void act(int seatNumber, Action action) {
        if (isComplete()) {
            throw new IllegalActionException("La main est terminée.");
        }
        Seat seat = seats.get(toActIndex);
        if (seat.number() != seatNumber) {
            throw new IllegalActionException("Ce n'est pas ton tour.");
        }
        AllowedActions allowed = allowedActions();
        if (!allowed.types().contains(action.type())) {
            throw new IllegalActionException("Action impossible : " + describe(action.type()) + ".");
        }

        switch (action.type()) {
            case FOLD -> seat.fold();
            case CHECK -> { }
            case CALL -> seat.pay(allowed.callAmount());
            case BET, RAISE -> {
                if (action.amount() < allowed.minAmount() || action.amount() > allowed.maxAmount()) {
                    throw new IllegalActionException("Le montant doit être entre "
                            + allowed.minAmount() + " et " + allowed.maxAmount() + ".");
                }
                seat.pay(action.amount() - seat.bet());
                round.recordRaise(action.amount());
            }
        }
        round.recordAction(seat);
        proceedAfter(toActIndex);
    }

    private void proceedAfter(int lastIndex) {
        if (contenders().size() == 1) {
            closeRound();
            payPots(Map.of());
            return;
        }
        if (!isRoundOver()) {
            toActIndex = nextToAct(lastIndex);
            return;
        }
        closeRound();
        if (stage == Stage.RIVER) {
            showdown();
            return;
        }
        dealNextStreet();
        // Starts the new round left of the button, or deals on if nobody can bet any more.
        proceedAfter(buttonIndex);
    }

    private boolean isRoundOver() {
        List<Seat> active = seats.stream().filter(seat -> seat.status() == SeatStatus.ACTIVE).toList();
        for (Seat seat : active) {
            if (seat.bet() < round.currentBet()) {
                return false;
            }
            // A lone active player facing only all-ins has nobody left to bet against.
            if (!round.hasActed(seat) && active.size() > 1) {
                return false;
            }
        }
        return true;
    }

    private int nextToAct(int fromIndex) {
        for (int offset = 1; offset <= seats.size(); offset++) {
            int index = (fromIndex + offset) % seats.size();
            Seat seat = seats.get(index);
            if (seat.status() == SeatStatus.ACTIVE
                    && (seat.bet() < round.currentBet() || !round.hasActed(seat))) {
                return index;
            }
        }
        throw new IllegalStateException("Nobody left to act");
    }

    /** Gives back the part of the highest bet nobody matched, then clears the bets. */
    private void closeRound() {
        Seat highest = null;
        int secondHighestBet = 0;
        for (Seat seat : seats) {
            if (highest == null || seat.bet() > highest.bet()) {
                if (highest != null) {
                    secondHighestBet = Math.max(secondHighestBet, highest.bet());
                }
                highest = seat;
            } else {
                secondHighestBet = Math.max(secondHighestBet, seat.bet());
            }
        }
        if (highest.bet() > secondHighestBet) {
            highest.refund(highest.bet() - secondHighestBet);
        }
        seats.forEach(Seat::endBettingRound);
    }

    private void dealNextStreet() {
        stage = switch (stage) {
            case PRE_FLOP -> Stage.FLOP;
            case FLOP -> Stage.TURN;
            case TURN -> Stage.RIVER;
            case RIVER, COMPLETE -> throw new IllegalStateException("No street after " + stage);
        };
        int count = stage == Stage.FLOP ? 3 : 1;
        for (int i = 0; i < count; i++) {
            board.add(deck.draw());
        }
        round = new BettingRound(0, bigBlind);
    }

    private void showdown() {
        Map<Integer, HandRank> shown = new LinkedHashMap<>();
        for (Seat seat : contenders()) {
            List<Card> cards = new ArrayList<>(seat.holeCards());
            cards.addAll(board);
            shown.put(seat.number(), HandEvaluator.evaluate(cards));
        }
        payPots(shown);
    }

    /**
     * Pays each pot to its best eligible hand. Tied winners split it; odd chips go one by one to
     * the winners closest to the left of the button. Without a showdown, the last player left wins.
     */
    private void payPots(Map<Integer, HandRank> shown) {
        List<Payout> payouts = new ArrayList<>();
        for (Pot pot : pots()) {
            List<Seat> winners = winners(pot, shown);
            Map<Integer, Integer> shares = new LinkedHashMap<>();
            int share = pot.amount() / winners.size();
            int oddChips = pot.amount() % winners.size();
            for (int i = 0; i < winners.size(); i++) {
                int amount = share + (i < oddChips ? 1 : 0);
                winners.get(i).player().addChips(amount);
                shares.put(winners.get(i).number(), amount);
            }
            payouts.add(new Payout(pot, shares));
        }
        result = new HandResult(payouts, shown);
        stage = Stage.COMPLETE;
        toActIndex = -1;
    }

    /** Best eligible hands of a pot, ordered from the left of the button. */
    private List<Seat> winners(Pot pot, Map<Integer, HandRank> shown) {
        List<Seat> eligible = new ArrayList<>();
        for (int offset = 1; offset <= seats.size(); offset++) {
            Seat seat = seats.get((buttonIndex + offset) % seats.size());
            if (pot.eligibleSeats().contains(seat.number())) {
                eligible.add(seat);
            }
        }
        if (shown.isEmpty()) {
            return eligible;
        }
        HandRank best = eligible.stream().map(seat -> shown.get(seat.number())).max(HandRank::compareTo).orElseThrow();
        return eligible.stream().filter(seat -> shown.get(seat.number()).compareTo(best) == 0).toList();
    }

    private List<Seat> contenders() {
        return seats.stream().filter(Seat::isInHand).toList();
    }

    private int next(int index) {
        return (index + 1) % seats.size();
    }

    private static int indexOf(List<Seat> seats, int seatNumber) {
        for (int i = 0; i < seats.size(); i++) {
            if (seats.get(i).number() == seatNumber) {
                return i;
            }
        }
        throw new IllegalArgumentException("The button must be on a seat dealt in: " + seatNumber);
    }

    private static String describe(ActionType type) {
        return switch (type) {
            case FOLD -> "se coucher";
            case CHECK -> "checker";
            case CALL -> "suivre";
            case BET -> "miser";
            case RAISE -> "relancer";
        };
    }
}
