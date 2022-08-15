package ooo.foooooooooooo.velocitydiscord.discord;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import ooo.foooooooooooo.velocitydiscord.Config;
import ooo.foooooooooooo.velocitydiscord.MessageListener;
import ooo.foooooooooooo.velocitydiscord.discord.commands.ICommand;
import ooo.foooooooooooo.velocitydiscord.discord.commands.ListCommand;
import ooo.foooooooooooo.velocitydiscord.util.StringTemplate;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public class Discord extends ListenerAdapter {
    private final Logger logger;
    private final Config config;

    private final JDA jda;
    private final Map<String, ICommand> commands = new HashMap<>();
    private TextChannel activeChannel;

    public Discord(ProxyServer server, Logger logger, Config config) {
        this.logger = logger;
        this.config = config;

        commands.put("list", new ListCommand(server, config));

        MessageListener messageListener = new MessageListener(server, logger, config);

        JDABuilder builder = JDABuilder
                .createDefault(config.DISCORD_TOKEN)
                .setChunkingFilter(ChunkingFilter.ALL)
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .addEventListeners(messageListener, this);

        try {
            jda = builder.build();
        } catch (LoginException e) {
            this.logger.severe("Failed to login to discord: " + e);
            throw new RuntimeException("Failed to login to discord: " + e);
        }
    }

    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        logger.info(MessageFormat.format("Bot ready, Guilds: {0} ({1} available)", event.getGuildTotalCount(), event.getGuildAvailableCount()));

        TextChannel channel = jda.getTextChannelById(config.CHANNEL_ID);

        if (channel == null) {
            logger.severe("Could not load channel with id: " + config.CHANNEL_ID);
            throw new RuntimeException("Could not load channel id: " + config.CHANNEL_ID);
        }

        logger.info("Loaded channel: " + channel.getName());

        if (!channel.canTalk()) {
            logger.severe("Cannot talk in configured channel");
            throw new RuntimeException("Cannot talk in configured channel");
        }

        activeChannel = channel;

        var guild = activeChannel.getGuild();

        guild.upsertCommand("list", "list players").queue();
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerChat(PlayerChatEvent event) {
        Optional<ServerConnection> currentServer = event.getPlayer().getCurrentServer();

        if (currentServer.isEmpty()) return;

        String username = event.getPlayer().getUsername();
        String server = currentServer.get().getServerInfo().getName();
        String content = event.getMessage();

        String message = new StringTemplate(config.DISCORD_CHAT_MESSAGE)
                .add("username", username)
                .add("server", server)
                .add("message", content)
                .toString();

        sendMessage(message);
    }

    @Subscribe
    public void onConnect(PlayerChooseInitialServerEvent event) {
        var initialServer = event.getInitialServer();

        if (initialServer.isEmpty()) return;

        var username = event.getPlayer().getUsername();
        var server = initialServer.get().getServerInfo().getName();

        var message = new StringTemplate(config.JOIN_MESSAGE)
                .add("username", username)
                .add("server", server);

        sendMessage(message.toString());
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        var currentServer = event.getPlayer().getCurrentServer();

        if (currentServer.isEmpty()) return;

        var username = event.getPlayer().getUsername();
        var server = currentServer.get().getServerInfo().getName();

        var message = new StringTemplate(config.LEAVE_MESSAGE)
                .add("username", username)
                .add("server", server);

        sendMessage(message.toString());
    }

    @SuppressWarnings("UnstableApiUsage")
    @Subscribe
    public void onServerConnect(ServerPostConnectEvent event) {
        var currentServer = event.getPlayer().getCurrentServer(); // why Optional? true lulw

        if (currentServer.isEmpty()) return;

        var username = event.getPlayer().getUsername();
        var server = currentServer.get().getServerInfo().getName();
        var previousServer = event.getPreviousServer();

        if (previousServer == null) {
            return;
        }

        var previous = previousServer.getServerInfo().getName();

        var message = new StringTemplate(config.SERVER_SWITCH_MESSAGE)
                .add("username", username)
                .add("current", server)
                .add("previous", previous);

        sendMessage(message.toString());
    }

    public void sendMessage(String message) {
        activeChannel.sendMessage(message).queue();
    }

    public void playerDeath(String message) {
        sendMessage("**" + message + "**");
    }

    public void playerAdvancement(String message) {
        sendMessage("**" + message + "**");
    }

    @Override
    public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
        var command = event.getName();

        if (!commands.containsKey(command)) {
            return;
        }

        commands.get(command).handle(event);
    }
}
