package baconetworns.pixelauctiongenerations;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.plugin.Plugin;

@Plugin(
        id = "pixelauctiongenerations",
        name = "PixelAuctionGenerations",
        description = "A port of PixelAuction for Pixelmon Generations"
)
public class PixelAuctionGenerations {

    @Inject
    private Logger logger;

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
    }
}
