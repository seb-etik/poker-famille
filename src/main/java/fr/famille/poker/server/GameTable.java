package fr.famille.poker.server;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;

import fr.famille.poker.engine.Action;
import fr.famille.poker.engine.ActionType;
import fr.famille.poker.engine.AllowedActions;
import fr.famille.poker.engine.Blinds;
import fr.famille.poker.engine.Card;
import fr.famille.poker.engine.Deck;
import fr.famille.poker.engine.Hand;
import fr.famille.poker.engine.HandResult;
import fr.famille.poker.engine.IllegalActionException;
import fr.famille.poker.engine.Player;
import fr.famille.poker.engine.Seat;
import fr.famille.poker.engine.SeatStatus;
import fr.famille.poker.engine.Table;
import fr.famille.poker.server.message.ErrorCode;
import fr.famille.poker.server.message.EventMessage;
import fr.famille.poker.server.message.StateMessage;
import fr.famille.poker.server.message.StateMessage.AllowedActionsView;
import fr.famille.poker.server.message.StateMessage.BlindsView;
import fr.famille.poker.server.message.StateMessage.HandView;
import fr.famille.poker.server.message.StateMessage.PayoutView;
import fr.famille.poker.server.message.StateMessage.PotView;
import fr.famille.poker.server.message.StateMessage.ResultView;
import fr.famille.poker.server.message.StateMessage.SeatView;
import fr.famille.poker.server.message.StateMessage.ShownHandView;
import fr.famille.poker.server.message.StateMessage.TableView;
import fr.famille.poker.server.message.StateMessage.YouView;

/**
 * A table being played: who is in the room, the host, the chain of hands and the action timer.
 *
 * <p>Every public method is {@code synchronized}: requests and timers of one table are handled one
 * at a time. After each change, every connected player receives the events, then the state as they
 * see it. Plain Java on purpose: the WebSocket and the Vert.x timers come in through {@link Outbox}
 * and {@link Scheduler}.
 */
public final class GameTable {

    private static final int MAX_NICKNAME_LENGTH = 20;
    private static final int MAX_ID_LENGTH = 64;

    private final String code;
    private final TableSettings settings;
    private final Timings timings;
    private final Outbox outbox;
    private final Scheduler scheduler;
    private final Supplier<Deck> decks;
    private final Clock clock;
    private final Table table;
    /** Everyone who joined, seated or not, in order of arrival. Kept after leaving a seat, so is the stack. */
    private final Map<String, Participant> participants = new LinkedHashMap<>();

    private String hostId;
    private boolean playing;
    /** Seat of the player who won the game (the only one left with chips), until a new game starts. */
    private Integer winnerSeat;
    private int handNumber;
    private HandResult lastResult;
    private Scheduler.Cancellable timer;
    private long turnId;
    private Instant actionDeadline;
    private Instant lastActivity;

    private static final class Participant {
        final Player player;
        boolean connected;
        /** Left their seat during a hand they are dealt in: played for until the hand ends. */
        boolean leaving;

        Participant(Player player) {
            this.player = player;
        }
    }

    public GameTable(String code, TableSettings settings, Timings timings, Outbox outbox, Scheduler scheduler,
            Supplier<Deck> decks, Clock clock) {
        this.code = code;
        this.settings = settings;
        this.timings = timings;
        this.outbox = outbox;
        this.scheduler = scheduler;
        this.decks = decks;
        this.clock = clock;
        this.table = new Table(code, settings.blindSchedule());
        this.lastActivity = clock.instant();
    }

    public String code() {
        return code;
    }

    public synchronized void join(String playerId, String nickname, String avatar) {
        String name = nickname == null ? "" : nickname.strip();
        if (playerId == null || playerId.isBlank() || playerId.length() > MAX_ID_LENGTH) {
            throw new GameException(ErrorCode.INVALID_MESSAGE, "Identifiant de joueur invalide.");
        }
        if (name.isEmpty() || name.length() > MAX_NICKNAME_LENGTH) {
            throw new GameException(ErrorCode.INVALID_MESSAGE, "Le surnom doit faire de 1 à 20 caractères.");
        }
        if (avatar == null || avatar.length() > MAX_ID_LENGTH) {
            throw new GameException(ErrorCode.INVALID_MESSAGE, "Avatar invalide.");
        }
        Participant participant = participants.computeIfAbsent(playerId,
                id -> new Participant(new Player(id, name, avatar, settings.startingStack())));
        participant.connected = true;
        if (hostId == null) {
            hostId = playerId;
        }
        if (isToAct(participant)) {
            scheduleTurnTimer();
        }
        publish(List.of());
    }

    public synchronized void disconnect(String playerId) {
        Participant participant = participants.get(playerId);
        if (participant == null) {
            return;
        }
        participant.connected = false;
        if (isToAct(participant)) {
            scheduleTurnTimer();
        }
        publish(List.of());
    }

    public synchronized void sit(String playerId, Integer seatNumber) {
        Participant participant = joined(playerId);
        if (seatOf(participant).isPresent()) {
            throw new GameException(ErrorCode.ALREADY_SEATED, "Tu es déjà assis.");
        }
        if (seatNumber == null || seatNumber < 0 || seatNumber >= Table.MAX_SEATS
                || !table.seats().get(seatNumber).isEmpty()) {
            throw new GameException(ErrorCode.SEAT_TAKEN, "Ce siège n'est pas libre.");
        }
        table.sit(seatNumber, participant.player);
        publish(List.of());
    }

    public synchronized void leave(String playerId) {
        Participant participant = joined(playerId);
        Seat seat = seatOf(participant)
                .orElseThrow(() -> new GameException(ErrorCode.ILLEGAL_ACTION, "Tu n'es pas assis."));
        if (isDealtIn(seat)) {
            participant.leaving = true;
            if (isToAct(participant)) {
                scheduleTurnTimer();
            }
        } else {
            table.leave(seat.number());
        }
        if (playerId.equals(hostId)) {
            passHostAfter(seat.number());
        }
        publish(List.of());
    }

    public synchronized void start(String playerId) {
        joined(playerId);
        if (!playerId.equals(hostId)) {
            throw new GameException(ErrorCode.NOT_HOST, "Seul l'hôte peut lancer la partie.");
        }
        if (playing) {
            throw new GameException(ErrorCode.ILLEGAL_ACTION, "La partie est déjà lancée.");
        }
        if (playersWithChips() < 2) {
            throw new GameException(ErrorCode.NOT_ENOUGH_PLAYERS, "Il faut au moins deux joueurs assis.");
        }
        playing = true;
        winnerSeat = null;
        startHand();
    }

    /** After a victory, the host starts a new game with the same table: fresh stacks, blinds back to level 1. */
    public synchronized void restart(String playerId) {
        joined(playerId);
        if (!playerId.equals(hostId)) {
            throw new GameException(ErrorCode.NOT_HOST, "Seul l'hôte peut relancer une partie.");
        }
        if (playing || winnerSeat == null) {
            throw new GameException(ErrorCode.ILLEGAL_ACTION, "La partie n'est pas terminée.");
        }
        long seated = table.seats().stream().filter(seat -> !seat.isEmpty()).count();
        if (seated < 2) {
            throw new GameException(ErrorCode.NOT_ENOUGH_PLAYERS, "Il faut au moins deux joueurs assis.");
        }
        table.newGame(settings.startingStack());
        // Players standing aside get a fresh stack too, for when they sit down again.
        participants.replaceAll((id, participant) -> {
            if (seatOf(participant).isPresent()) {
                return participant;
            }
            Player player = participant.player;
            Participant fresh = new Participant(
                    new Player(player.id(), player.nickname(), player.avatar(), settings.startingStack()));
            fresh.connected = participant.connected;
            return fresh;
        });
        winnerSeat = null;
        lastResult = null;
        handNumber = 0;
        playing = true;
        startHand();
    }

    public synchronized void act(String playerId, String actionName, Integer amount) {
        Participant participant = joined(playerId);
        ActionType type = ProtocolNames.actionType(actionName);
        if (type == null) {
            throw new GameException(ErrorCode.INVALID_MESSAGE, "Action inconnue : " + actionName + ".");
        }
        boolean needsAmount = type == ActionType.BET || type == ActionType.RAISE;
        if (needsAmount && amount == null) {
            throw new GameException(ErrorCode.INVALID_MESSAGE, "Il manque le montant.");
        }
        if (!isToAct(participant)) {
            throw new GameException(ErrorCode.ILLEGAL_ACTION, "Ce n'est pas ton tour.");
        }
        int seatNumber = seatOf(participant).orElseThrow().number();
        play(seatNumber, new Action(type, needsAmount ? amount : 0));
    }

    /** True when nobody is connected and nothing happened for {@code idle}: the table can be dropped. */
    public synchronized boolean isAbandoned(Duration idle) {
        boolean someoneConnected = participants.values().stream().anyMatch(participant -> participant.connected);
        return !someoneConnected && lastActivity.plus(idle).isBefore(clock.instant());
    }

    public synchronized void close() {
        cancelTimer();
        playing = false;
    }

    private void startHand() {
        cancelTimer();
        lastResult = null;
        handNumber++;
        int levelBefore = table.blindLevel();
        Hand hand = table.startHand(decks.get());
        List<EventMessage> events = new ArrayList<>();
        if (table.blindLevel() > levelBefore) {
            Blinds blinds = table.currentBlinds();
            events.add(new EventMessage.BlindsUp(table.blindLevel(), blinds.small(), blinds.big()));
        }
        events.add(new EventMessage.HandStarted(handNumber, hand.buttonSeat()));
        // Blinds can put everyone all in: the board may already be dealt.
        events.addAll(transitions(hand, 0));
        afterChange(hand, events);
    }

    private void play(int seatNumber, Action action) {
        Hand hand = currentHand().orElseThrow();
        Seat seat = table.seats().get(seatNumber);
        AllowedActions allowed = hand.allowedActions();
        int totalBet = switch (action.type()) {
            case CALL -> seat.bet() + allowed.callAmount();
            case BET, RAISE -> action.amount();
            case FOLD, CHECK -> seat.bet();
        };
        int boardBefore = hand.board().size();
        try {
            hand.act(seatNumber, action);
        } catch (IllegalActionException e) {
            throw new GameException(ErrorCode.ILLEGAL_ACTION, e.getMessage());
        }
        List<EventMessage> events = new ArrayList<>();
        events.add(new EventMessage.Action(seatNumber, ProtocolNames.of(action.type()), totalBet));
        events.addAll(transitions(hand, boardBefore));
        afterChange(hand, events);
    }

    /** Board cards dealt since {@code boardBefore}, street by street, then the end of the hand. */
    private List<EventMessage> transitions(Hand hand, int boardBefore) {
        List<EventMessage> events = new ArrayList<>();
        List<Card> board = hand.board();
        int[] streetEnds = {3, 4, 5};
        String[] streets = {"flop", "turn", "river"};
        int dealtUpTo = boardBefore;
        for (int i = 0; i < streetEnds.length; i++) {
            if (dealtUpTo < streetEnds[i] && board.size() >= streetEnds[i]) {
                events.add(new EventMessage.Board(streets[i], codes(board.subList(dealtUpTo, streetEnds[i]))));
                dealtUpTo = streetEnds[i];
            }
        }
        hand.result().ifPresent(result -> {
            if (!result.shownHands().isEmpty()) {
                events.add(new EventMessage.Showdown());
            }
            events.add(new EventMessage.Win(payouts(result)));
        });
        return events;
    }

    private void afterChange(Hand hand, List<EventMessage> events) {
        if (hand.isComplete()) {
            endHand(hand, events);
        } else {
            scheduleTurnTimer();
        }
        publish(events);
    }

    private void endHand(Hand hand, List<EventMessage> events) {
        cancelTimer();
        HandResult result = hand.result().orElseThrow();
        lastResult = result;
        for (Participant participant : participants.values()) {
            if (participant.leaving) {
                participant.leaving = false;
                seatOf(participant).ifPresent(seat -> table.leave(seat.number()));
            }
        }
        if (playersWithChips() < 2) {
            playing = false;
            declareWinner(events);
            return;
        }
        Duration pause = result.shownHands().isEmpty() ? timings.pauseAfterFold() : timings.pauseAfterShowdown();
        int endedHand = handNumber;
        timer = scheduler.schedule(pause, () -> nextHand(endedHand));
    }

    /** The game is over: if one player is left with chips, they won it. */
    private void declareWinner(List<EventMessage> events) {
        table.seats().stream()
                .filter(seat -> !seat.isEmpty() && seat.player().stack() > 0)
                .map(Seat::number)
                .findFirst()
                .ifPresent(seat -> {
                    winnerSeat = seat;
                    events.add(new EventMessage.GameWon(seat));
                });
    }

    private synchronized void nextHand(int endedHand) {
        if (!playing || handNumber != endedHand) {
            return;
        }
        if (playersWithChips() < 2) {
            playing = false;
            publish(List.of());
            return;
        }
        startHand();
    }

    private void scheduleTurnTimer() {
        cancelTimer();
        Hand hand = currentHand().orElseThrow();
        int seatNumber = hand.toAct().orElseThrow();
        Participant participant = participantAt(table.seats().get(seatNumber));
        boolean present = participant.connected && !participant.leaving;
        Duration delay = present ? timings.actionTimeout() : timings.absentPlayerDelay();
        actionDeadline = clock.instant().plus(delay);
        long turn = ++turnId;
        timer = scheduler.schedule(delay, () -> timeout(turn));
    }

    /** The player took too long, or is away: check when possible, otherwise fold. */
    private synchronized void timeout(long turn) {
        if (turn != turnId || currentHand().map(Hand::isComplete).orElse(true)) {
            return;
        }
        Hand hand = currentHand().orElseThrow();
        boolean canCheck = hand.allowedActions().types().contains(ActionType.CHECK);
        play(hand.toAct().orElseThrow(), canCheck ? Action.check() : Action.fold());
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        turnId++;
    }

    private void publish(List<EventMessage> events) {
        lastActivity = clock.instant();
        for (Map.Entry<String, Participant> entry : participants.entrySet()) {
            if (!entry.getValue().connected) {
                continue;
            }
            events.forEach(event -> outbox.send(entry.getKey(), event));
            outbox.send(entry.getKey(), stateFor(entry.getValue()));
        }
    }

    private StateMessage stateFor(Participant viewer) {
        Optional<Seat> viewerSeat = seatOf(viewer);
        Integer hostSeat = Optional.ofNullable(hostId).map(participants::get).flatMap(this::seatOf)
                .map(Seat::number).orElse(null);
        Blinds blinds = table.currentBlinds();
        TableView tableView = new TableView(code, blinds.small(), blinds.big(), settings.startingStack(),
                playing ? "playing" : "waiting", hostSeat, table.blindLevel(),
                table.handsUntilNextLevel().isPresent() ? table.handsUntilNextLevel().getAsInt() : null,
                table.nextBlinds().map(next -> new BlindsView(next.small(), next.big())).orElse(null),
                winnerSeat);
        YouView you = new YouView(viewerSeat.map(Seat::number).orElse(null), viewer.player.id().equals(hostId));

        List<SeatView> seats = new ArrayList<>();
        for (Seat seat : table.seats()) {
            seats.add(seat.isEmpty() ? null : seatView(seat, viewerSeat.orElse(null)));
        }

        Optional<Hand> hand = currentHand();
        HandView handView = hand.map(this::handView).orElse(null);
        AllowedActionsView allowed = hand
                .filter(h -> viewerSeat.isPresent() && h.toAct().equals(OptionalInt.of(viewerSeat.get().number())))
                .map(h -> allowedView(h.allowedActions()))
                .orElse(null);
        return new StateMessage(tableView, you, seats, handView, allowed, resultView());
    }

    private SeatView seatView(Seat seat, Seat viewerSeat) {
        Participant participant = participantAt(seat);
        List<Card> holeCards = seat.holeCards();
        boolean inHand = seat.status() == SeatStatus.ACTIVE || seat.status() == SeatStatus.ALL_IN;
        // Private cards: only to their owner, or to everyone once shown at the showdown.
        boolean shown = lastResult != null && lastResult.shownHands().containsKey(seat.number());
        boolean visible = !holeCards.isEmpty() && (seat == viewerSeat || shown);
        return new SeatView(seat.number(), participant.player.nickname(), participant.player.avatar(),
                participant.player.stack(), seat.bet(), ProtocolNames.of(seat.status()), participant.connected,
                inHand && !holeCards.isEmpty(), visible ? codes(holeCards) : null);
    }

    private HandView handView(Hand hand) {
        List<PotView> pots = hand.pots().stream().map(pot -> new PotView(pot.amount(), pot.eligibleSeats())).toList();
        Integer toAct = hand.toAct().isPresent() ? hand.toAct().getAsInt() : null;
        Long remainingMs = toAct == null ? null
                : Math.max(0, Duration.between(clock.instant(), actionDeadline).toMillis());
        return new HandView(handNumber, ProtocolNames.of(hand.stage()), hand.buttonSeat(), hand.smallBlindSeat(),
                hand.bigBlindSeat(), codes(hand.board()), pots, hand.currentBet(), toAct, remainingMs);
    }

    private static AllowedActionsView allowedView(AllowedActions allowed) {
        List<String> types = allowed.types().stream().sorted(Comparator.naturalOrder()).map(ProtocolNames::of).toList();
        return new AllowedActionsView(types, allowed.callAmount(), allowed.minAmount(), allowed.maxAmount());
    }

    private ResultView resultView() {
        if (lastResult == null) {
            return null;
        }
        Map<Integer, ShownHandView> shown = new LinkedHashMap<>();
        lastResult.shownHands().forEach((seat, rank) ->
                shown.put(seat, new ShownHandView(ProtocolNames.of(rank.category()), codes(rank.bestCards()))));
        return new ResultView(handNumber, payouts(lastResult), shown);
    }

    private static List<PayoutView> payouts(HandResult result) {
        return result.payouts().stream()
                .map(payout -> new PayoutView(payout.pot().amount(), payout.shares()))
                .toList();
    }

    private void passHostAfter(int seatNumber) {
        for (int offset = 1; offset < Table.MAX_SEATS; offset++) {
            Seat seat = table.seats().get((seatNumber + offset) % Table.MAX_SEATS);
            if (!seat.isEmpty()) {
                hostId = seat.player().id();
                return;
            }
        }
    }

    private Participant joined(String playerId) {
        Participant participant = participants.get(playerId);
        if (participant == null) {
            throw new GameException(ErrorCode.NOT_JOINED, "Il faut d'abord rejoindre la table.");
        }
        return participant;
    }

    private Optional<Hand> currentHand() {
        return table.currentHand();
    }

    private Optional<Seat> seatOf(Participant participant) {
        return table.seats().stream()
                .filter(seat -> !seat.isEmpty() && seat.player() == participant.player)
                .findFirst();
    }

    private Participant participantAt(Seat seat) {
        return participants.get(seat.player().id());
    }

    private boolean isDealtIn(Seat seat) {
        return currentHand().filter(hand -> !hand.isComplete() && hand.seats().contains(seat)).isPresent();
    }

    private boolean isToAct(Participant participant) {
        Optional<Seat> seat = seatOf(participant);
        return seat.isPresent() && currentHand()
                .map(hand -> hand.toAct().equals(OptionalInt.of(seat.get().number())))
                .orElse(false);
    }

    private long playersWithChips() {
        return table.seats().stream().filter(seat -> !seat.isEmpty() && seat.player().stack() > 0).count();
    }

    private static List<String> codes(List<Card> cards) {
        return cards.stream().map(Card::code).toList();
    }
}
