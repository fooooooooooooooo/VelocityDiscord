package ooo.foooooooooooo.velocitydiscord;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
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
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.logging.Logger;

public class Discord extends ListenerAdapter {
    private final Logger logger;
    private final Config config;

    private final JDA jda;

    private TextChannel activeChannel;

    public Discord(ProxyServer server, Logger logger, Config config) {
        this.logger = logger;
        this.config = config;

        MessageListener messageListener = new MessageListener(server, logger, config);

        JDABuilder builder = JDABuilder
                .createDefault(config.DISCORD_TOKEN)
                .setChunkingFilter(ChunkingFilter.ALL)
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES)
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
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerChat(PlayerChatEvent event) {
        if (activeChannel == null) {
            logger.warning("Channel not loaded yet");
        }

        Optional<ServerConnection> currentServer = event.getPlayer().getCurrentServer();

        if (currentServer.isEmpty()) return;

        String username = event.getPlayer().getUsername();
        String server = currentServer.get().getServerInfo().getName();
        String content = event.getMessage();

        String message = config.DISCORD_CHAT_MESSAGE
                .replace("{username}", username)
                .replace("{server}", server)
                .replace("{message}", content);

        sendMessage(message);
    }

    @Subscribe
    public void onConnect(PlayerChooseInitialServerEvent event) {
        if (activeChannel == null) {
            logger.warning("Channel not loaded yet");
        }

        Optional<RegisteredServer> initialServer = event.getInitialServer();

        if (initialServer.isEmpty()) return;

        String username = event.getPlayer().getUsername();
        String server = initialServer.get().getServerInfo().getName();

        String message = config.JOIN_MESSAGE
                .replace("{username}", username)
                .replace("{server}", server);

        sendMessage(message);
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        if (activeChannel == null) {
            logger.warning("Channel not loaded yet");
        }

        Optional<ServerConnection> currentServer = event.getPlayer().getCurrentServer();

        if (currentServer.isEmpty()) return;

        String username = event.getPlayer().getUsername();
        String server = currentServer.get().getServerInfo().getName();
        String message = config.LEAVE_MESSAGE
                .replace("{username}", username)
                .replace("{server}", server);

        sendMessage(message);
    }

    @SuppressWarnings("UnstableApiUsage")
    @Subscribe
    public void onServerConnect(ServerPostConnectEvent event) {
        if (activeChannel == null) {
            logger.warning("Channel not loaded yet");
        }

        Optional<ServerConnection> currentServer = event.getPlayer().getCurrentServer(); // why Optional? true lulw

        if (currentServer.isEmpty()) return;

        String username = event.getPlayer().getUsername();
        String server = currentServer.get().getServerInfo().getName();
        RegisteredServer previousServer = event.getPreviousServer();

        if (previousServer == null) {
            return;
        }

        String previous = previousServer.getServerInfo().getName();

        String message = config.SERVER_SWITCH_MESSAGE
                .replace("{username}", username)
                .replace("{current}", server)
                .replace("{previous}", previous);

        sendMessage(message);
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
}
