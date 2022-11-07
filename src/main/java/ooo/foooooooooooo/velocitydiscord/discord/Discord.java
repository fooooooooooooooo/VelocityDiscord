package ooo.foooooooooooo.velocitydiscord.discord;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import ooo.foooooooooooo.velocitydiscord.Config;
import ooo.foooooooooooo.velocitydiscord.MessageListener;
import ooo.foooooooooooo.velocitydiscord.yep.AdvancementMessage;
import ooo.foooooooooooo.velocitydiscord.yep.DeathMessage;
import ooo.foooooooooooo.velocitydiscord.discord.commands.ICommand;
import ooo.foooooooooooo.velocitydiscord.discord.commands.ListCommand;
import ooo.foooooooooooo.velocitydiscord.util.StringTemplate;

import javax.annotation.Nonnull;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public class Discord extends ListenerAdapter {
    private final ProxyServer server;
    private final Logger logger;
    private final Config config;

    private final JDA jda;
    private final WebhookClient webhookClient;
    private final Map<String, ICommand> commands = new HashMap<>();
    private TextChannel activeChannel;

    public Discord(ProxyServer server, Logger logger, Config config) {
        this.server = server;
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
        } catch (Exception e) {
            this.logger.severe("Failed to login to discord: " + e);
            throw new RuntimeException("Failed to login to discord: ", e);
        }

        webhookClient = config.DISCORD_USE_WEBHOOK
                ? new WebhookClientBuilder(config.WEBHOOK_URL).build()
                : null;
    }

    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        logger.info(MessageFormat.format("Bot ready, Guilds: {0} ({1} available)", event.getGuildTotalCount(), event.getGuildAvailableCount()));

        TextChannel channel = jda.getTextChannelById(Objects.requireNonNull(config.CHANNEL_ID));

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

        updateActivityPlayerAmount();
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerChat(PlayerChatEvent event) {
        Optional<ServerConnection> currentServer = event.getPlayer().getCurrentServer();

        if (currentServer.isEmpty()) return;

        var username = event.getPlayer().getUsername();
        var server = currentServer.get().getServerInfo().getName();
        var content = event.getMessage();

        if (config.DISCORD_USE_WEBHOOK) {
            var uuid = event.getPlayer().getUniqueId().toString();

            var avatar = new StringTemplate(config.WEBHOOK_AVATAR_URL)
                    .add("username", username)
                    .add("uuid", uuid)
                    .toString();

            var discordName = new StringTemplate(config.WEBHOOK_USERNAME)
                    .add("username", username)
                    .add("server", server)
                    .toString();

            sendWebhookMessage(avatar, discordName, content);
        } else {
            var message = new StringTemplate(config.DISCORD_CHAT_MESSAGE)
                    .add("username", username)
                    .add("server", server)
                    .add("message", content)
                    .toString();

            sendMessage(message);
        }
    }

    @Subscribe
    public void onConnect(ServerConnectedEvent event) {
        var username = event.getPlayer().getUsername();
        var server = event.getServer().getServerInfo().getName();

        Optional<RegisteredServer> previousServer = event.getPreviousServer();

        StringTemplate message;

        if (previousServer.isPresent()) {
            var previous = previousServer.get().getServerInfo().getName();

            message = new StringTemplate(config.SERVER_SWITCH_MESSAGE)
                    .add("username", username)
                    .add("current", server)
                    .add("previous", previous);
        } else {
            message = new StringTemplate(config.JOIN_MESSAGE)
                    .add("username", username)
                    .add("server", server);
        }

        sendMessage(message.toString());

        updateActivityPlayerAmount();
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Optional<ServerConnection> currentServer = event.getPlayer().getCurrentServer();

        if (currentServer.isEmpty()) return;

        var username = event.getPlayer().getUsername();
        var server = currentServer.get().getServerInfo().getName();

        var message = new StringTemplate(config.LEAVE_MESSAGE)
            .add("username", username)
            .add("server", server);

        sendMessage(message.toString());

        updateActivityPlayerAmount();
    }
    public void sendMessage(@Nonnull String message) {
        activeChannel.sendMessage(message).queue();
    }

    public void sendWebhookMessage(String avatar, String username, String content) {
        WebhookMessage webhookMessage = new WebhookMessageBuilder()
                .setAvatarUrl(avatar)
                .setUsername(username)
                .setContent(content)
                .build();
        webhookClient.send(webhookMessage);
    }

    public void playerDeath(String username, DeathMessage message) {
        sendMessage(
            new StringTemplate(config.DEATH_MESSAGE) //
                .add("username", username) //
                .add("death_message", message.message) //
                .toString()
        );
    }

    public void playerAdvancement(String username, AdvancementMessage message) {
        sendMessage(
            new StringTemplate(config.ADVANCEMENT_MESSAGE) //
                .add("username", username) //
                .add("advancement_title", message.title) //
                .add("advancement_description", message.description) //
                .toString()
        );
    }

    @Override
    public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
        var command = event.getName();

        if (!commands.containsKey(command)) {
            return;
        }

        commands.get(command).handle(event);
    }

    public void updateActivityPlayerAmount() {
        jda.getPresence()
                .setActivity(Activity.playing(
                        new StringTemplate(config.DISCORD_ACTIVITY_TEXT)
                                .add("amount", this.server.getPlayerCount())
                                .toString()));
    }
}
