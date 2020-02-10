package baconetworks.pixelauctiongenerations.utils;

import baconetworks.pixelauctiongenerations.PixelAuctionGenerations;
import org.spongepowered.api.Game;
import org.spongepowered.api.service.pagination.PaginationService;

public class Pagination {

    //Required definitions
    final PixelAuctionGenerations plugin = PixelAuctionGenerations.getInstance();
    final Game game = plugin.getGame();

    //Pagination service builder
    public PaginationService getPaginationService() {
        if (game.getServiceManager().provide(PaginationService.class).isPresent()) {
            return game.getServiceManager().provide(PaginationService.class).get();
        } else {
            return null;
        }
    }
}