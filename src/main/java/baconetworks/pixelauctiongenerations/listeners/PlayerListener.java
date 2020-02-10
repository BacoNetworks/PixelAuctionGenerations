package baconetworks.pixelauctiongenerations.listeners;

import baconetworks.pixelauctiongenerations.PixelAuctionGenerations;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;

public class PlayerListener {
    @Listener
    public void onPlayerQuit(ClientConnectionEvent.Disconnect event) {
        PixelAuctionGenerations.getInstance().getHideMessages().remove(event.getTargetEntity().getUniqueId());
    }
}
