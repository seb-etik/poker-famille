package fr.famille.poker.server.message;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeName;

/** The whole table as one player sees it (see docs/PROTOCOL.md for each field). */
@JsonTypeName("state")
public record StateMessage(
        TableView table,
        YouView you,
        List<SeatView> seats,
        HandView hand,
        AllowedActionsView allowedActions,
        ResultView lastResult) implements ServerMessage {

    public record TableView(String code, int smallBlind, int bigBlind, int startingStack, String status, Integer hostSeat) {
    }

    public record YouView(Integer seat, boolean isHost) {
    }

    public record SeatView(int seat, String nickname, String avatar, int stack, int bet, String status,
            boolean connected, boolean hasCards, List<String> cards) {
    }

    public record HandView(int number, String stage, int buttonSeat, int smallBlindSeat, int bigBlindSeat,
            List<String> board, List<PotView> pots, int currentBet, Integer toActSeat, Long remainingMs) {
    }

    public record PotView(int amount, List<Integer> seats) {
    }

    public record AllowedActionsView(List<String> types, int callAmount, int minAmount, int maxAmount) {
    }

    public record ResultView(int handNumber, List<PayoutView> payouts, Map<Integer, ShownHandView> shownHands) {
    }

    public record PayoutView(int amount, Map<Integer, Integer> winners) {
    }

    public record ShownHandView(String category, List<String> cards) {
    }
}
