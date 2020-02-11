package baconetworks.pixelauctiongenerations;

import baconetworks.pixelauctiongenerations.auctions.PokemonAuction;
import baconetworks.pixelauctiongenerations.commands.*;
import baconetworks.pixelauctiongenerations.listeners.PlayerListener;
import baconetworks.pixelauctiongenerations.utils.CommandCooldown;
import baconetworks.pixelauctiongenerations.utils.Utils;
import com.google.inject.Inject;
import net.minecraft.server.MinecraftServer;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.text.Text;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Plugin(
        id = "pixelauctiongenerations",
        name = "PixelAuctionGenerations",
        description = "A port of PixelAuction for Pixelmon Generations",
        authors = {
                "kristi71111"},
        version = "@VERSION@"
)
public class PixelAuctionGenerations {

    private static PixelAuctionGenerations instance;

    @Inject
    private Logger logger;

    @Inject
    private Game game;

    @Inject
    @DefaultConfig(sharedRoot = false)
    private File defaultConfig;

    @Inject
    @DefaultConfig(sharedRoot = false)

    private ConfigurationLoader<CommentedConfigurationNode> configurationLoader;
    private CommentedConfigurationNode configurationNode;
    private EconomyService economyService;
    private LinkedHashMap currentAuctions = new LinkedHashMap();
    private ConcurrentHashMap commandCooldowns = new ConcurrentHashMap();
    private CopyOnWriteArrayList finishedAuctions = new CopyOnWriteArrayList();
    private ArrayList hideMessages = new ArrayList();
    private LinkedHashMap needConfirmation = new LinkedHashMap();


    public static PixelAuctionGenerations getInstance() {
        return instance;
    }


    @Listener
    public void onInit(GameInitializationEvent event) {
        instance = this;

        CommandSpec AucHelp = CommandSpec.builder().executor(new PixAuctionHelpCommand()).permission("pixelauction.command.help").build();

        CommandSpec pixBid = CommandSpec.builder().executor(new PixBidCommand())
                .permission("pixelauction.command.bid")
                .arguments(GenericArguments.onlyOne(GenericArguments.integer(Text.of("bid"))))
                .build();

        CommandSpec pixAuctionCancel = CommandSpec.builder()
                .executor(new PixAuctionCancelCommand())
                .arguments(GenericArguments.optional(GenericArguments.requiringPermission(GenericArguments.player(Text.of("player")), "pixelauction.command.forcecancel"))).permission("pixelauction.command.cancel")
                .build();

        CommandSpec pixAuctionHide = CommandSpec.builder()
                .executor(new PixAuctionSilenceCommand())
                .permission("pixelauction.command.hide").build();

        CommandSpec pixAuctionCreate = CommandSpec.builder()
                .executor(new PixAuctionCommand())
                .arguments(GenericArguments.seq(GenericArguments.optional(GenericArguments.integer(Text.of("slot"))), GenericArguments.optional(GenericArguments.integer(Text.of("price")))), GenericArguments.optional(GenericArguments.integer(Text.of("increment"))), GenericArguments.optional(GenericArguments.integer(Text.of("duration"))))
                .permission("pixelauction.command.auction").build();

        CommandSpec pixAuction = CommandSpec.builder()
                .executor(new PixAuctionHelpCommand())
                .permission("pixelauction.command.help")
                .child(AucHelp, "help", "?")
                .child(pixBid, "bid")
                .child(pixAuctionCancel, "cancel")
                .child(pixAuctionHide, "hide")
                .child(pixAuctionCreate, "create", "new")
                .build();


        Sponge.getCommandManager().register(this, pixAuction, "auc", "auction", "pauc");
        Sponge.getEventManager().registerListeners(this, new PlayerListener());
        try {
            if (!this.defaultConfig.exists()) {
                this.defaultConfig.createNewFile();
                this.configurationNode = this.configurationLoader.load();
                this.configurationNode.getNode(new Object[]{"PixelAuction Configuration", "Command wait time (seconds)"}).setValue(300);
                this.configurationNode.getNode(new Object[]{"PixelAuction Configuration", "Maximum queued auctions"}).setValue(10);
                this.configurationNode.getNode(new Object[]{"PixelAuction Configuration", "Minimum bid increment"}).setValue(50);
                this.configurationNode.getNode(new Object[]{"PixelAuction Configuration", "Maximum bid increment"}).setValue(500);
                this.configurationNode.getNode(new Object[]{"PixelAuction Configuration", "Allow Egg sales"}).setValue(Boolean.FALSE);
                this.configurationNode.getNode(new Object[]{"PixelAuction Configuration", "Duration", "Minimum"}).setValue(30);
                this.configurationNode.getNode(new Object[]{"PixelAuction Configuration", "Duration", "Maximum"}).setValue(60);
                this.configurationNode.getNode("PixelAuction Configuration").setComment("For any issues/suggestions, ask Kristi.");
                this.configurationLoader.save(this.configurationNode);
            }
            this.configurationNode = this.configurationLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Listener
    public void postInit(GamePostInitializationEvent event) {
        this.economyService = Sponge.getServiceManager().provide(EconomyService.class).orElseGet(null);
    }


    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        Utils.repeatAuctionTask();
    }


    @Listener
    public void onServerStop(GameStoppingServerEvent event) {
        Utils.handleServerStop();
    }


    public Logger getLogger() {
        return this.logger;
    }


    public EconomyService getEconomy() {
        return this.economyService;
    }


    public Map<UUID, CommandCooldown> getCommandCooldowns() {
        return this.commandCooldowns;
    }


    public LinkedHashMap<UUID, PokemonAuction> getCurrentAuctions() {
        return this.currentAuctions;
    }


    public Game getGame() {
        return this.game;
    }


    public List<UUID> getFinishedAuctions() {
        return this.finishedAuctions;
    }


    public CommentedConfigurationNode getConfigurationNode() {
        return this.configurationNode;
    }


    public List<UUID> getHideMessages() {
        return this.hideMessages;
    }


    public MinecraftServer getServer() {
        return (MinecraftServer) Sponge.getServer();
    }


    public LinkedHashMap<UUID, List<Integer>> getNeedConfirmation() {
        return this.needConfirmation;
    }
}
