package fr.famille.poker.server;

import java.util.List;
import java.util.Locale;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

/** The family leaderboard: games won to the end, by nickname, for the current month or since the server started. */
@Path("/api/leaderboard")
public class LeaderboardResource {

    @Inject
    ResultStore results;

    @GET
    public List<LeaderboardEntry> leaderboard(@QueryParam("period") @DefaultValue("month") String period) {
        LeaderboardPeriod selected;
        try {
            selected = LeaderboardPeriod.valueOf(period.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Période inconnue : month ou all.");
        }
        return results.leaderboard(selected);
    }
}
