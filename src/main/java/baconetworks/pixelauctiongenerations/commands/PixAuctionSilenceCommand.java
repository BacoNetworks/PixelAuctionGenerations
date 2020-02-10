package baconetworks.pixelauctiongenerations.commands;

import baconetworks.pixelauctiongenerations.PixelAuctionGenerations;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

public class PixAuctionSilenceCommand implements CommandExecutor {
    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        PixelAuctionGenerations plugin = PixelAuctionGenerations.getInstance();
        if (!(src instanceof Player)) {
            throw new CommandException(Text.of(TextColors.RED, "You need to be a player to run this command"));
        }
        Player player = (Player) src;
        if (plugin.getHideMessages().contains(player.getUniqueId())) {
            plugin.getHideMessages().remove(player.getUniqueId());
            player.sendMessage(Text.of(TextColors.YELLOW, "Auction Messages are now SHOWING"));
        } else {
            plugin.getHideMessages().add(player.getUniqueId());
            player.sendMessage(Text.of(TextColors.YELLOW, "Auction Messages are now HIDDEN"));
        }
        return CommandResult.success();
    }
}
