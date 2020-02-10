package baconetworks.pixelauctiongenerations.commands;

import baconetworks.pixelauctiongenerations.PixelAuctionGenerations;
import baconetworks.pixelauctiongenerations.auctions.PokemonAuction;
import baconetworks.pixelauctiongenerations.utils.Utils;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.Map;
import java.util.Optional;

public class PixAuctionCancelCommand implements CommandExecutor {
    @Override
    public CommandResult execute(CommandSource commandSource, CommandContext commandContext) throws CommandException {
        PixelAuctionGenerations plugin = PixelAuctionGenerations.getInstance();

        if (!(commandSource instanceof Player)) {
            throw new CommandException(Text.of(TextColors.RED, "You need to be a player to run this command"));
        }

        Optional<Player> optTarget = commandContext.getOne("player");
        Player source = (Player) commandSource;

        if (!optTarget.isPresent()) {
            if (!plugin.getCurrentAuctions().containsKey(source.getUniqueId())) {
                throw new CommandException(Text.of(TextColors.RED, "You have no active Pokemon sales to cancel!"));
            }

            PokemonAuction currentAuction = (PokemonAuction) ((Map.Entry) plugin.getCurrentAuctions().entrySet().iterator().next()).getValue();

            if (currentAuction.getSeller().toString().equalsIgnoreCase(source.getUniqueId().toString())) {
                if (source.hasPermission("pixelauction.command.cancelongoing") && currentAuction.getAuctionTime() > 30) {
                    source.sendMessage(Text.of(TextColors.GREEN, "You have cancelled your auction and your Pokemon has been returned."));
                    Utils.returnPlayerPokemon(source);
                    return CommandResult.success();
                }
                throw new CommandException(Text.of(TextColors.RED, "This auction has been ongoing for longer than 30 seconds, you are no longer able to cancel!"));
            }

            Utils.returnPlayerPokemon(source);
            source.sendMessage(Text.of(TextColors.GREEN, "You have cancelled your auction queue and your Pokemon has been returned."));
            return CommandResult.success();
        }

        source.sendMessage(Text.of(TextColors.GREEN, "Auction Cancelled."));
        Player target = optTarget.get();
        Utils.returnPlayerPokemon(target);
        return CommandResult.success();
    }
}

