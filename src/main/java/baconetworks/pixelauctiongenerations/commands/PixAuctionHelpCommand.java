package baconetworks.pixelauctiongenerations.commands;

import baconetworks.pixelauctiongenerations.utils.Pagination;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextTemplate;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import java.util.ArrayList;
import java.util.List;

public class PixAuctionHelpCommand implements CommandExecutor {
    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        String[] Commands = {"/pauc <slot> <price> <increment> <duration>", "/pabid <amount>", "/pacancel", "/pahide"};
        String[] Descriptions = {"Creates an auction selling the Pokemon from the player's chosen party slot, with the initial price and the bid increment defined by the player.", "Places a bid on the currently active auction, increasing the bid by the specified amount.", "Cancels the player's pending auction.", "Hide the auction announcements"};
        //Some definitions
        List<Text> texts = new ArrayList<>();
        //We defined the builder here
        for (int i = 0; i < Commands.length; i++) {
            texts.add(Text.builder()
                    .append(Text.builder()
                            .color(TextColors.GOLD)
                            .style(TextStyles.BOLD)
                            .append(Text.of(Commands[i]))
                            .onClick(TextActions.suggestCommand(Commands[i]))
                            .onHover(TextActions.showText(Text.of(Descriptions[i])))
                            .build())
                    .build());
        }
        texts.add(Text.builder()
                .append(Text.builder()
                        .color(TextColors.DARK_RED)
                        .style(TextStyles.BOLD)
                        .append(Text.of("Minimum price per auction is at 25k!"))
                        .build())
                .build());

        TextTemplate Title = TextTemplate.of(TextColors.RED, TextStyles.BOLD, "PixelAuction");
        PaginationList paginationlist = new Pagination().getPaginationService().builder().footer(Text.of(TextColors.GRAY, TextStyles.BOLD, "Ported to Pixelmon Generations by kristi71111")).padding(Text.of("-")).title(Title.apply().build()).contents(texts).build();
        paginationlist.sendTo(src);
        return CommandResult.success();
    }
}
