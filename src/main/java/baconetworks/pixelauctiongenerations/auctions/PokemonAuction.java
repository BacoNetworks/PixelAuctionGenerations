package baconetworks.pixelauctiongenerations.auctions;

import baconetworks.pixelauctiongenerations.PixelAuctionGenerations;
import baconetworks.pixelauctiongenerations.utils.Utils;
import com.pixelmonmod.pixelmon.config.PixelmonEntityList;
import com.pixelmonmod.pixelmon.entities.pixelmon.EntityPixelmon;
import com.pixelmonmod.pixelmon.storage.PixelmonStorage;
import com.pixelmonmod.pixelmon.storage.PlayerStorage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransferResult;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PokemonAuction {
    PixelAuctionGenerations plugin = PixelAuctionGenerations.getInstance();
    private UUID seller;
    private long creation;
    private int price;
    private NBTTagCompound pokemonForSaleNBT;
    private int auctionTime;
    private UUID highestBidder;
    private int bidIncrement;
    private List<Integer> intervals;

    public PokemonAuction(UUID seller, long creation, int price, int bidIncrement, int duration) {
        this.bidIncrement = 100;
        this.seller = seller;
        this.creation = creation;
        this.price = price;
        this.bidIncrement = bidIncrement;
        this.auctionTime = duration + 1;
        this.intervals = calcIntervals(duration);
    }

    private List<Integer> calcIntervals(int duration) {
        ArrayList<Integer> factors = new ArrayList<>();
        factors.add(duration);
        factors.add(duration / 2);
        factors.add(duration / 3);

        return factors;
    }

    public boolean sellPokemon() {
        if (this.highestBidder == null) {
            cancelAuction();
            return false;
        }

        EconomyService economyService = plugin.getEconomy();

        Optional<UniqueAccount> buyerEcon = economyService.getOrCreateAccount(this.highestBidder);
        Optional<UniqueAccount> sellerEcon = economyService.getOrCreateAccount(this.seller);

        if (buyerEcon.isPresent() && sellerEcon.isPresent()) {
            TransferResult transferResult = buyerEcon.get().transfer(sellerEcon.get(), economyService.getDefaultCurrency(), new BigDecimal(this.price), Cause.builder().append(plugin).build(EventContext.empty()));
            if (transferResult.getResult() == ResultType.SUCCESS) {
                removeAndExecuteAuction(this.highestBidder);
                sendSaleMessage();
                return true;
            }
            cancelAuction();
        }

        return false;
    }

    public boolean incrementBid(UUID bidder, int bid) {
        EconomyService economyService = plugin.getEconomy();
        Optional<UniqueAccount> bidderEcon = economyService.getOrCreateAccount(bidder);
        Optional<Player> playerOptional = Sponge.getServer().getPlayer(bidder);

        BigDecimal fullAmount = BigDecimal.valueOf(bid);
        if (bidderEcon.isPresent() && bidderEcon.get().getBalance(economyService.getDefaultCurrency()).compareTo(fullAmount) >= 0) {
            if (getAuctionTime() < 1)
                return false;
            setPrice(bid);
            if (getAuctionTime() <= 15) {
                setAuctionTime(15);
            }
            setHighestBidder(bidder);
            playerOptional.ifPresent(player -> player.sendMessage(Text.of(TextColors.GREEN, "You are now the new highest bidder!")));
            sendAuctionMessage();
            return true;
        }
        playerOptional.ifPresent(player -> player.sendMessage(Text.of(TextColors.RED, "Unable to bid on the Pokemon, you do not have the funds!")));
        return false;
    }

    private void removeAndExecuteAuction(UUID buyer) {
        PlayerStorage storage = PixelmonStorage.pokeBallManager.getPlayerStorageFromUUID(buyer).get();
        Optional<Player> buyerOptional = Sponge.getServer().getPlayer(buyer);
        Optional<Player> sellerOptional = Sponge.getServer().getPlayer(this.seller);
        storage.addToFirstEmptySpace(getPokemonForSale());
        buyerOptional.ifPresent(player -> player.sendMessage(Text.of(TextColors.GREEN, "You bought " + player.getName() + "'s " + getPokemonForSale().getString("Name") + " for $" + getPrice())));
        sellerOptional.ifPresent(player -> Text.of(new Object[]{TextColors.GREEN, player.getName() + " bought your " + getPokemonForSale().getString("Name") + " for $" + getPrice()}));
        plugin.getCurrentAuctions().remove(this.seller);
    }

    public boolean setupAuction(int pokemonSlot, Player player) {
        pokemonSlot--;
        PlayerStorage playerPartyStorage = PixelmonStorage.pokeBallManager.getPlayerStorageFromUUID(this.seller).get();
        int num = playerPartyStorage.countTeam();
        if (num > 1) {
            EntityPixelmon pokemon = (EntityPixelmon) PixelmonEntityList.createEntityFromNBT(playerPartyStorage.partyPokemon[pokemonSlot], (World) player.getWorld());
            if (pokemon != null) {
                if (pokemon.isEgg && !plugin.getConfigurationNode().getNode(new Object[]{"PixelAuction Configuration", "Allow Egg sales"}).getBoolean()) {
                    return false;
                }
                playerPartyStorage.removeFromPartyPlayer(pokemonSlot);
                NBTTagCompound compound = new NBTTagCompound();
                compound = pokemon.writeToNBT(compound);
                setPokemonForSale(compound);
                plugin.getCurrentAuctions().put(this.seller, this);
                return true;
            }
        }
        return false;
    }

    public void cancelAuction() {
        PlayerStorage playerPartyStorage = PixelmonStorage.pokeBallManager.getPlayerStorageFromUUID(this.seller).get();
        Optional<Player> playerOptional = Sponge.getServer().getPlayer(this.seller);
        Text Cancelled;
        int AmountOfPokemon = 0;

        for (int i = 0; i < playerPartyStorage.partyPokemon.length; i++) {
            if (playerPartyStorage.partyPokemon[i] != null) {
                AmountOfPokemon++;
            }
        }
        if (AmountOfPokemon <= 5) {
            playerPartyStorage.addToFirstEmptySpace(getPokemonForSale());
            Cancelled = Text.of(TextColors.RED, "Unfortunately your auction fell through,", Text.NEW_LINE, "your Pokemon has been returned to your party!");
        } else {
            World world = DimensionManager.getWorld(0);
            EntityPixelmon pokemon = (EntityPixelmon) PixelmonEntityList.createEntityFromNBT(getPokemonForSale(), world);
            Cancelled = Text.of(TextColors.RED, "Unfortunately your auction fell through,", Text.NEW_LINE, "your Pokemon has been returned to your pc due to your party being full");
            playerPartyStorage.addToPC(pokemon);
        }
        if (this.auctionTime < 1) {
            playerOptional.ifPresent(player -> player.sendMessage(Cancelled));
        }
        Utils.auctionTask();
        plugin.getCurrentAuctions().remove(this.seller);
    }

    public void sendAuctionMessage() {
        World world = DimensionManager.getWorld(0);
        EntityPixelmon pokemon = (EntityPixelmon) PixelmonEntityList.createEntityFromNBT(getPokemonForSale(), world);
        Text chatMessage;
        Text chatMessageBidder;
        Text hiddenIVs;
        if (getHighestBidder() != null) {
            chatMessage = Utils.getHoverText(pokemon, getSeller(), this.highestBidder, getPrice(), getAuctionTime(), getBidIncrement(), false, true);
            chatMessageBidder = Utils.getHoverText(pokemon, getSeller(), this.highestBidder, getPrice(), getAuctionTime(), getBidIncrement(), true, true);
            hiddenIVs = Utils.getHoverText(pokemon, getSeller(), this.highestBidder, getPrice(), getAuctionTime(), getBidIncrement(), false, false);
        } else {
            chatMessage = Utils.getHoverText(pokemon, getSeller(), null, getPrice(), getAuctionTime(), getBidIncrement(), false, true);
            chatMessageBidder = Utils.getHoverText(pokemon, getSeller(), null, getPrice(), getAuctionTime(), getBidIncrement(), true, true);
            hiddenIVs = Utils.getHoverText(pokemon, getSeller(), null, getPrice(), getAuctionTime(), getBidIncrement(), false, false);
        }

        for (Player player : Sponge.getServer().getOnlinePlayers()) {
            if (!plugin.getHideMessages().contains(player.getUniqueId())) {
                EntityPlayer ePlayer = (EntityPlayer) player;
                Player spongeplayer = (Player) ePlayer;

                if (spongeplayer.hasPermission("PixelAuction.viewbidder") && !chatMessageBidder.equals(Text.EMPTY)) {
                    spongeplayer.sendMessage(chatMessageBidder);
                    continue;
                }
                if (spongeplayer.getUniqueId().equals(this.seller) && !spongeplayer.hasPermission("PixelAuction.viewivs")) {
                    spongeplayer.sendMessage(hiddenIVs);
                    continue;
                }
                spongeplayer.sendMessage(chatMessage);
            }
        }
    }

    public void sendSaleMessage() {
        Optional<User> sellerOptional = Sponge.getGame().getServiceManager().provide(UserStorageService.class).get().get(this.seller);
        Optional<User> bidderOptional = Sponge.getGame().getServiceManager().provide(UserStorageService.class).get().get(this.highestBidder);
        for (Player player : Sponge.getServer().getOnlinePlayers()) {

            player.sendMessage(Text.of(TextColors.GREEN, sellerOptional.get().getName() + "'s " + getPokemonForSale().getString("Name") + " was sold to " + bidderOptional.get().getName() + " for $" + getPrice()));
        }
    }


    public UUID getSeller() {
        return this.seller;
    }


    public void setSeller(UUID seller) {
        this.seller = seller;
    }


    public long getCreation() {
        return this.creation;
    }


    public void setCreation(long creation) {
        this.creation = creation;
    }


    public int getPrice() {
        return this.price;
    }


    public void setPrice(int price) {
        this.price = price;
    }


    public NBTTagCompound getPokemonForSale() {
        return this.pokemonForSaleNBT;
    }


    public void setPokemonForSale(NBTTagCompound pokemonForSale) {
        this.pokemonForSaleNBT = pokemonForSale;
    }


    public int getAuctionTime() {
        return this.auctionTime;
    }


    public void setAuctionTime(int auctionTime) {
        this.auctionTime = auctionTime;
    }


    public UUID getHighestBidder() {
        return this.highestBidder;
    }


    public void setHighestBidder(UUID highestBidder) {
        this.highestBidder = highestBidder;
    }


    public int getBidIncrement() {
        return this.bidIncrement;
    }


    public void settBidIncrement(int bidIncrement) {
        this.bidIncrement = bidIncrement;
    }


    public List<Integer> getIntervals() {
        return this.intervals;
    }
}
