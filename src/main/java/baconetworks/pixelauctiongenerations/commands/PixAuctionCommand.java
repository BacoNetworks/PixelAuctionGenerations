package baconetworks.pixelauctiongenerations.commands;

import baconetworks.pixelauctiongenerations.PixelAuctionGenerations;
import baconetworks.pixelauctiongenerations.auctions.PokemonAuction;
import baconetworks.pixelauctiongenerations.utils.CommandCooldown;
import com.pixelmonmod.pixelmon.storage.PixelmonStorage;
import com.pixelmonmod.pixelmon.storage.PlayerStorage;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.Arrays;
import java.util.List;

public class PixAuctionCommand implements CommandExecutor {
    public CommandResult execute(CommandSource commandSource, CommandContext args) throws CommandException {
        PixelAuctionGenerations plugin = PixelAuctionGenerations.getInstance();

        if (!(commandSource instanceof Player)) {
            throw new CommandException(Text.of(TextColors.RED, "You need to be a player to run this command"));
        }

        Player source = (Player) commandSource;

        if (!(args.getOne("slot").isPresent())) {
            source.sendMessage(Text.of(TextColors.RED, "You are missing arguments:"));
            throw new CommandException(Text.of(TextColors.RED, ("/auc <slot> <price> <increment> <duration>")));
        }

        int slot = (Integer) args.getOne("slot").get();

        if (!(args.getOne("price").isPresent())) {
            source.sendMessage(Text.of(TextColors.RED, "You are missing arguments:"));
            throw new CommandException(Text.of(TextColors.RED, ("/auc " + slot + " <price> <increment> <duration>")));
        }

        int price = (Integer) args.getOne("price").get();

        if (price < 25000) {
            throw new CommandException(Text.of(TextColors.RED, ("The minimum auction price per pokemon is 25k")));
        }

        if (!(args.getOne("increment").isPresent())) {
            source.sendMessage(Text.of(TextColors.RED, "You are missing arguments:"));
            throw new CommandException(Text.of(TextColors.RED, ("/auc " + slot + " " + price + " <increment> <duration>")));
        }

        int incrementation = (Integer) args.getOne("increment").get();

        if (!(args.getOne("duration").isPresent())) {
            source.sendMessage(Text.of(TextColors.RED, "You are an argument:"));
            throw new CommandException(Text.of(TextColors.RED, ("/auc " + slot + " " + price + " " + incrementation + " <duration>")));
        }

        int duration = (Integer) args.getOne("duration").get();

        PlayerStorage playerPartyStorage = PixelmonStorage.pokeBallManager.getPlayerStorageFromUUID(source.getUniqueId()).get();

        if (playerPartyStorage.partyPokemon[slot - 1] == null || playerPartyStorage.partyPokemon[slot - 1].isEmpty()) {
            throw new CommandException(Text.of(TextColors.RED, "There is no pokemon in the slot you're trying to auction!"));
        }

        final List<Integer> cmdArgs = Arrays.asList(slot, price, incrementation, duration);


        if (!plugin.getNeedConfirmation().containsKey(source.getUniqueId())) {
            source.sendMessage(Text.of(TextColors.RED, "Type the command again to confirm your auction"));
            plugin.getNeedConfirmation().put(source.getUniqueId(), cmdArgs);
            return CommandResult.empty();
        }
        if (!plugin.getNeedConfirmation().get(source.getUniqueId()).equals(cmdArgs)) {
            source.sendMessage(Text.of(TextColors.RED, "Type the command again to confirm your auction"));
            plugin.getNeedConfirmation().replace(source.getUniqueId(), cmdArgs);
            return CommandResult.empty();
        }
        plugin.getNeedConfirmation().remove(source.getUniqueId());

        if (slot < 1 || slot > 6) {
            throw new CommandException(Text.of(TextColors.RED, "Invalid slot number, must be 1-6!"));
        }

        if (price < 0) {
            throw new CommandException(Text.of(TextColors.RED, "Invalid price, must be above $0!"));
        }

        if (incrementation < plugin.getConfigurationNode().getNode(new Object[]{"PixelAuction Configuration", "Minimum bid increment"}).getInt()) {
            throw new CommandException(Text.of(TextColors.RED, "Invalid bid incrementation, must be above $" + plugin.getConfigurationNode()
                    .getNode(new Object[]{"PixelAuction Configuration", "Minimum bid increment"}).getInt() + "!"));
        }

        if (incrementation > plugin.getConfigurationNode().getNode(new Object[]{"PixelAuction Configuration", "Maximum bid increment"}).getInt()) {
            throw new CommandException(Text.of(TextColors.RED, "Invalid bid incrementation, must be below $" + plugin.getConfigurationNode()
                    .getNode(new Object[]{"PixelAuction Configuration", "Maximum bid increment"}).getInt() + "!"));
        }

        if (duration < plugin.getConfigurationNode().getNode(new Object[]{"PixelAuction Configuration", "Duration", "Minimum"}).getInt() || duration >
                plugin.getConfigurationNode().getNode(new Object[]{"PixelAuction Configuration", "Duration", "Maximum"}).getInt()) {
            throw new CommandException(Text.of(TextColors.RED, "Invalid auction duration, must be between " + plugin.getConfigurationNode()
                    .getNode(new Object[]{"PixelAuction Configuration", "Duration", "Minimum"}).getInt() + " and " + plugin.getConfigurationNode()
                    .getNode(new Object[]{"PixelAuction Configuration", "Duration", "Maximum"}).getInt() + " seconds!"));
        }

        CommandCooldown cooldown = null;
        if (!source.hasPermission("pixelauction.exemptCD")) {
            if (plugin.getCommandCooldowns().containsKey(source.getUniqueId())) {
                cooldown = plugin.getCommandCooldowns().get(source.getUniqueId());

                int time = (int) (System.currentTimeMillis() / 1000L - cooldown.getUnixTime());
                if (time < plugin.getConfigurationNode().getNode(new Object[]{"PixelAuction Configuration", "Command wait time (seconds)"}).getInt()) {
                    throw new CommandException(Text.of(TextColors.RED, "You cannot use this command so frequently. Please wait " + (plugin.getConfigurationNode()
                            .getNode(new Object[]{"PixelAuction Configuration", "Command wait time (seconds)"}).getInt() - time) + " seconds to reuse it!"));
                }
            } else {
                cooldown = new CommandCooldown(source.getUniqueId(), 0L);
                plugin.getCommandCooldowns().put(source.getUniqueId(), cooldown);
            }
        }
        if (!plugin.getCurrentAuctions().isEmpty()) {
            source.sendMessage(Text.of(TextColors.RED, "Another auction is already in place, you will be added to the queue!"));

            if (plugin.getCurrentAuctions().size() >= plugin.getConfigurationNode().getNode(new Object[]{"PixelAuction Configuration", "Maximum queued auctions"}).getInt()) {
                throw new CommandException(Text.of(TextColors.RED, "The queue is currently full, please wait and try again later!"));
            }

            if (plugin.getCurrentAuctions().containsKey(source.getUniqueId())) {
                throw new CommandException(Text.of(TextColors.RED, "You already have an Auction queued, please cancel your current auction to list another!"));
            }
        }

        PokemonAuction pokemonAuction = new PokemonAuction(source.getUniqueId(), System.currentTimeMillis() / 1000L, price, incrementation, duration);

        if (pokemonAuction.setupAuction(slot, (Player) commandSource)) {
            if (cooldown != null) {
                cooldown.setUnixTime(System.currentTimeMillis() / 1000L);
            }
            commandSource.sendMessage(Text.of(TextColors.GREEN, "You have successfully listed the Pokemon for auction!"));
            return CommandResult.success();
        }
        commandSource.sendMessage(Text.of(TextColors.RED, "Unable to list the Pokemon in that slot!"));
        return CommandResult.empty();
    }
}
