package fr.famille.poker.server;

import org.jboss.resteasy.reactive.RestResponse;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path("/api/tables")
public class TableResource {

    /** Every field is optional; {@code blindIncreaseEveryHands} = 0 keeps the blinds fixed. */
    public record CreateTableRequest(Integer smallBlind, Integer bigBlind, Integer startingStack,
            Integer blindIncreaseEveryHands, Integer blindIncreasePercent) {
    }

    public record CreateTableResponse(String code) {
    }

    @Inject
    TableManager tables;

    @POST
    public RestResponse<CreateTableResponse> create(CreateTableRequest request) {
        TableSettings defaults = TableSettings.DEFAULT;
        CreateTableRequest given = request == null ? new CreateTableRequest(null, null, null, null, null) : request;
        TableSettings settings;
        try {
            settings = new TableSettings(
                    given.smallBlind() == null ? defaults.smallBlind() : given.smallBlind(),
                    given.bigBlind() == null ? defaults.bigBlind() : given.bigBlind(),
                    given.startingStack() == null ? defaults.startingStack() : given.startingStack(),
                    given.blindIncreaseEveryHands() == null ? defaults.handsPerLevel() : given.blindIncreaseEveryHands(),
                    given.blindIncreasePercent() == null ? defaults.increasePercent() : given.blindIncreasePercent());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Réglages de la table invalides.");
        }
        GameTable table = tables.create(settings);
        return RestResponse.status(RestResponse.Status.CREATED, new CreateTableResponse(table.code()));
    }
}
