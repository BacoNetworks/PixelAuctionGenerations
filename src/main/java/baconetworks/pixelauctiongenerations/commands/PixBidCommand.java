package baconetworks.pixelauctiongenerations.commands;

import baconetworks.pixelauctiongenerations.PixelAuctionGenerations;
import baconetworks.pixelauctiongenerations.auctions.PokemonAuction;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.UUID;

public class PixBidCommand implements CommandExecutor {
    public CommandResult execute(CommandSource commandSource, CommandContext commandContext) throws CommandException {
        PixelAuctionGenerations plugin = PixelAuctionGenerations.getInstance();

        if (!(commandSource instanceof Player)) {
            throw new CommandException(Text.of(TextColors.RED, "You need to be a player to run this command"));
        }
        if (plugin.getCurrentAuctions().isEmpty()) {
            throw new CommandException(Text.of(TextColors.RED, "There are no auctions in place currently!"));
        }
        PokemonAuction currentAuction = plugin.getCurrentAuctions().entrySet().iterator().next().getValue();

        UUID seller = currentAuction.getSeller();
        Player source = (Player) commandSource;

        if (currentAuction.getAuctionTime() < 1) {
            throw new CommandException(Text.of(TextColors.RED, "The auction has just ended."));
        }

        if (seller.toString().equalsIgnoreCase(source.getUniqueId().toString())) {
            throw new CommandException(Text.of(TextColors.RED, "You cannot bid on your own Pokemon!"));
        }

        if (currentAuction.getHighestBidder() != null && currentAuction.getHighestBidder().toString().equalsIgnoreCase(source.getUniqueId().toString())) {
            throw new CommandException(Text.of(TextColors.RED, "You are already the highest bidder!"));
        }

        if (!commandContext.getOne("bid").isPresent()) {
            throw new CommandException(Text.of(TextColors.RED, "You need to actually tell us how much you want to bid."));
        }

        int bid = (Integer) commandContext.getOne("bid").get();

        if (bid < currentAuction.getPrice() + currentAuction.getBidIncrement()) {
            throw new CommandException(Text.of(TextColors.RED, "This auction has a minimum bid increment of $" + currentAuction.getBidIncrement() + " please increase your bid!"));
        }

        if (currentAuction.getAuctionTime() < 1)
            throw new CommandException(Text.of(TextColors.RED, "The auction has just ended."));

        if (currentAuction.incrementBid(source.getUniqueId(), bid)) {
            return CommandResult.success();
        }
        return CommandResult.empty();
    }
}

