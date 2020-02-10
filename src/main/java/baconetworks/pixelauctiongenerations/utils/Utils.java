package baconetworks.pixelauctiongenerations.utils;

import baconetworks.pixelauctiongenerations.PixelAuctionGenerations;
import baconetworks.pixelauctiongenerations.auctions.PokemonAuction;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonSpec;
import com.pixelmonmod.pixelmon.config.PixelmonEntityList;
import com.pixelmonmod.pixelmon.entities.pixelmon.EntityPixelmon;
import com.pixelmonmod.pixelmon.storage.PixelmonStorage;
import com.pixelmonmod.pixelmon.storage.PlayerStorage;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.LiteralText;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class Utils {

    public static void returnPlayerPokemon(Player player) {
        if (PixelAuctionGenerations.getInstance().getCurrentAuctions().containsKey(player.getUniqueId())) {
            PokemonAuction pokemonSale = PixelAuctionGenerations.getInstance().getCurrentAuctions().get(player.getUniqueId());
            pokemonSale.cancelAuction();
        }
    }

    public static void handleServerStop() {
        PixelAuctionGenerations.getInstance().getCurrentAuctions().forEach((key, value) -> {
            PixelAuctionGenerations.getInstance().getFinishedAuctions().add(key);
            PlayerStorage storage = PixelmonStorage.pokeBallManager.getPlayerStorageFromUUID(key).get();
            if (storage.partyPokemon.length < 6) {
                storage.addToFirstEmptySpace(value.getPokemonForSale());
            } else {
                World world = DimensionManager.getWorld(0);
                EntityPixelmon pokemon = (EntityPixelmon) PixelmonEntityList.createEntityFromNBT(value.getPokemonForSale(), world);
                storage.addToPC(pokemon);
            }
        });
        PixelAuctionGenerations.getInstance().getFinishedAuctions().forEach(e -> PixelAuctionGenerations.getInstance().getCurrentAuctions().remove(e));
        PixelAuctionGenerations.getInstance().getFinishedAuctions().clear();
    }


    public static void repeatAuctionTask() {
        Task.builder()
                .intervalTicks(20L)
                .execute(Utils::auctionTask)
                .submit(PixelAuctionGenerations.getInstance());
    }


    public static void auctionTask() {
        if (!PixelAuctionGenerations.getInstance().getCurrentAuctions().isEmpty()) {
            final PokemonAuction pokemonAuction = PixelAuctionGenerations.getInstance().getCurrentAuctions().entrySet().iterator().next().getValue();
            if (!PixelAuctionGenerations.getInstance().getFinishedAuctions().contains(pokemonAuction.getSeller())) {
                final int newTime = pokemonAuction.getAuctionTime() - 1;
                pokemonAuction.setAuctionTime(newTime);
                if (newTime < -1) {
                    PixelAuctionGenerations.getInstance().getFinishedAuctions().add(pokemonAuction.getSeller());
                } else if (newTime == -1) {
                    pokemonAuction.sellPokemon();
                    PixelAuctionGenerations.getInstance().getFinishedAuctions().add(pokemonAuction.getSeller());
                } else if (pokemonAuction.getIntervals().contains(newTime) || newTime == 5 || newTime == 2 || newTime == 1 || newTime == 0) {
                    pokemonAuction.sendAuctionMessage();
                }
            }
            if (!PixelAuctionGenerations.getInstance().getFinishedAuctions().isEmpty())
                for (UUID uuid : PixelAuctionGenerations.getInstance().getFinishedAuctions()) {
                    PixelAuctionGenerations.getInstance().getCurrentAuctions().remove(uuid);
                    PixelAuctionGenerations.getInstance().getFinishedAuctions().remove(uuid);
                }
        }
    }

    public static Text getHoverText(EntityPixelmon data, @Nullable UUID seller, UUID bidder, int price, int time, int incrementation, boolean showBidder, boolean showIVs) {
        Text sell = null;
        LiteralText IVs;
        Optional<User> sellerOptional = Sponge.getGame().getServiceManager().provide(UserStorageService.class).flatMap(userStorageService -> userStorageService.get(seller));


        String name = data.getNickname();
        String heldItem = "";
        if (name == null) {
            name = (data.getSpecies()).name;
        }
        if (data.heldItem == null || data.heldItem.getItem().equals(ItemTypes.AIR)) {
            heldItem = "N/A";
        } else {
            heldItem = data.heldItem.getItem().getItemStackDisplayName(data.heldItem);
        }

        String Shiny;
        if (data.isShiny()) {
            Shiny = "True";
        } else {
            Shiny = "False";
        }

        Text hover = Text.of("", TextColors.DARK_GREEN, TextStyles.BOLD, name, TextStyles.RESET, "\n",
                TextColors.AQUA, "Level: ", data.getLvl().getLevel(), "\n",
                TextColors.GOLD, "Shiny: " + Shiny, "\n",
                TextColors.YELLOW, "Nature: ", data.getNature().getLocalizedName(), "\n",
                TextColors.GOLD, "Ability: ", (data.getAbilitySlot() != 2) ? TextColors.GOLD : TextColors.GRAY, data.getAbility().getName(), "\n",
                TextColors.GREEN, "Growth: ", data.getGrowth().toString(), "\n",
                TextColors.RED, "Item: ", heldItem, "\n",
                TextColors.BLUE, "Gender: ", data.getGender().name(), "\n",
                TextColors.LIGHT_PURPLE, "Ball Type: ", data.caughtBall.name(), "\n",
                TextColors.DARK_PURPLE, "Orig. Trainer: ", data.originalTrainer, "\n");

        if (PokemonSpec.from(new String[]{"unbreedable"}).matches(data)) {
            hover = hover.concat(Text.of("\n", TextColors.RED, "Breedable: False"));
        } else {
            hover = hover.concat(Text.of("\n", TextColors.GREEN, "Breedable: True"));
        }


        Text Info = Text.builder("Info").color(TextColors.YELLOW).onHover(TextActions.showText(hover)).build();

        if (showIVs) {
            IVs = Text.builder("IV's").color(TextColors.LIGHT_PURPLE).onHover(TextActions.showText(Text.of("", TextColors.LIGHT_PURPLE, TextStyles.UNDERLINE, "IV's", TextStyles.RESET, "\n", TextColors.DARK_GREEN, "HP: ", data.stats.IVs.HP, "\n", TextColors.RED, "Attack: ", data.stats.IVs.Attack, "\n", TextColors.GOLD, "Defence: ", data.stats.IVs.Defence, "\n", TextColors.LIGHT_PURPLE, "Sp. Attack: ", (data.stats.IVs.SpAtt), "\n", TextColors.YELLOW, "Sp. Defence: ", data.stats.IVs.SpDef, "\n", TextColors.DARK_AQUA, "Speed: ", data.stats.IVs.Speed))).build();
        } else {
            IVs = Text.builder("IV's").color(TextColors.LIGHT_PURPLE).onHover(TextActions.showText(Text.of("", TextColors.LIGHT_PURPLE, TextStyles.UNDERLINE, "IV's", TextStyles.RESET, "\n", TextColors.DARK_GREEN, "HP: ", "..", "\n", TextColors.RED, "Attack: ", "..", "\n", TextColors.GOLD, "Defence: ", "..", "\n", TextColors.LIGHT_PURPLE, "Sp. Attack: ", "..", "\n", TextColors.YELLOW, "Sp. Defence: ", "..", "\n", TextColors.DARK_AQUA, "Speed: ", ".."))).build();
        }
        Text EVS = Text.builder("EV's").color(TextColors.RED).onHover(TextActions.showText(Text.of("", TextColors.GOLD, TextStyles.UNDERLINE, "EV's", TextStyles.RESET, "\n", TextColors.DARK_GREEN, "HP: ", data.stats.EVs.HP, "\n", TextColors.RED, "Attack: ", data.stats.EVs.Attack, "\n", TextColors.GOLD, "Defence: ", data.stats.EVs.Defence, "\n", TextColors.LIGHT_PURPLE, "Sp. Attack: ", data.stats.EVs.SpecialAttack, "\n", TextColors.YELLOW, "Sp. Defence: ", data.stats.EVs.SpecialDefence, "\n", TextColors.DARK_AQUA, "Speed: ", data.stats.EVs.Speed))).build();
        Text Moves = Text.builder("Moves").color(TextColors.BLUE).onHover(TextActions.showText(Text.of("", TextColors.BLUE, TextStyles.UNDERLINE, "Moveset", TextStyles.RESET, "\n", TextColors.DARK_PURPLE, "Move 1: ", (data.getMoveset().attacks[0] != null) ? data.getMoveset().attacks[0].baseAttack.getLocalizedName() : "N/A", "\n", TextColors.LIGHT_PURPLE, "Move 2: ", (data.getMoveset().attacks[1] != null) ? data.getMoveset().attacks[1].baseAttack.getLocalizedName() : "N/A", "\n", TextColors.AQUA, "Move 3: ", (data.getMoveset().attacks[2] != null) ? data.getMoveset().attacks[2].baseAttack.getLocalizedName() : "N/A", "\n", TextColors.DARK_AQUA, "Move 4: ", (data.getMoveset().attacks[3] != null) ? data.getMoveset().attacks[3].baseAttack.getLocalizedName() : "N/A"))).build();

        if (showBidder) {
            if (bidder == null) {
                sell = Text.builder().append(Text.of("", TextColors.GRAY, " [", TextColors.AQUA, "Auc", TextColors.GRAY, ": ", TextColors.GOLD, "$", price, TextColors.GRAY, "]")).onClick(TextActions.runCommand("/pabid " + (price + incrementation))).onHover(TextActions.showText(Text.of(TextColors.RED, "Click to increase bid by $", incrementation))).build();
            } else {
                Optional<User> bidderOptional = Sponge.getGame().getServiceManager().provide(UserStorageService.class).flatMap(provide -> provide.get(bidder));
                if (bidderOptional.isPresent()) {
                    sell = Text.builder().append(Text.of("", TextColors.GRAY, " [", TextColors.AQUA, "Auc", TextColors.GRAY, ": ", TextColors.GOLD, "$", price, TextColors.GRAY, "]")).onClick(TextActions.runCommand("/pabid " + (price + incrementation))).onHover(TextActions.showText(Text.of(TextColors.RED, "Highest bidder: ", TextColors.GOLD, bidderOptional.get().getName(), "\n", TextColors.RED, "Click to increase bid by $", incrementation))).build();
                }
            }
        } else {
            sell = Text.builder().append(Text.of("", TextColors.GRAY, " [", TextColors.AQUA, "Auc", TextColors.GRAY, ": ", TextColors.GOLD, "$", price, TextColors.GRAY, "]")).onClick(TextActions.runCommand("/pabid " + (price + incrementation))).onHover(TextActions.showText(Text.of(TextColors.RED, "Click to increase bid by $", incrementation))).build();
        }

        return Text.of(TextColors.LIGHT_PURPLE, sellerOptional.get().getName(), ":", sell, TextColors.GRAY, "[", TextColors.RED, time, "s", TextColors.GRAY, "] ", TextColors.GREEN,
                data.isShiny() ? TextColors.GOLD : "", (data.getSpecies()).name, TextColors.GRAY, " [", Info, TextColors.GRAY, "-", IVs, TextColors.GRAY, "-", EVS, TextColors.GRAY, "-", Moves, TextColors.GRAY, "]");
    }
}
