package fr.famille.poker.server;

import fr.famille.poker.engine.ActionType;
import fr.famille.poker.engine.HandCategory;
import fr.famille.poker.engine.SeatStatus;
import fr.famille.poker.engine.Stage;

/** Engine enums as written in the protocol; switches so that the compiler flags any new value. */
final class ProtocolNames {

    private ProtocolNames() {
    }

    static String of(ActionType type) {
        return switch (type) {
            case FOLD -> "fold";
            case CHECK -> "check";
            case CALL -> "call";
            case BET -> "bet";
            case RAISE -> "raise";
        };
    }

    static ActionType actionType(String name) {
        for (ActionType type : ActionType.values()) {
            if (of(type).equals(name)) {
                return type;
            }
        }
        return null;
    }

    static String of(Stage stage) {
        return switch (stage) {
            case PRE_FLOP -> "preflop";
            case FLOP -> "flop";
            case TURN -> "turn";
            case RIVER -> "river";
            case COMPLETE -> "complete";
        };
    }

    static String of(SeatStatus status) {
        return switch (status) {
            case ACTIVE -> "active";
            case FOLDED -> "folded";
            case ALL_IN -> "allIn";
            case SITTING_OUT -> "sittingOut";
        };
    }

    static String of(HandCategory category) {
        return switch (category) {
            case HIGH_CARD -> "highCard";
            case ONE_PAIR -> "onePair";
            case TWO_PAIR -> "twoPair";
            case THREE_OF_A_KIND -> "threeOfAKind";
            case STRAIGHT -> "straight";
            case FLUSH -> "flush";
            case FULL_HOUSE -> "fullHouse";
            case FOUR_OF_A_KIND -> "fourOfAKind";
            case STRAIGHT_FLUSH -> "straightFlush";
        };
    }
}
