package fr.famille.poker.server;

/** Where finished games go: a file in production, memory in the tests. */
@FunctionalInterface
public interface ResultBook {

    /** Records a game won to the end and returns the winner's place in the leaderboard of the month. */
    Standing recordWin(GameRecord game);
}
