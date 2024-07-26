package ooo.foooooooooooo.velocitydiscord.discord;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.IncomingWebhookClient;
import net.dv8tion.jda.api.entities.WebhookClient;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import ooo.foooooooooooo.velocitydiscord.config.Config;
import ooo.foooooooooooo.velocitydiscord.discord.commands.ICommand;
import ooo.foooooooooooo.velocitydiscord.discord.commands.ListCommand;
import ooo.foooooooooooo.velocitydiscord.util.StringTemplate;

import javax.annotation.Nonnull;
import java.awt.*;
import java.lang.management.ManagementFactory;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Discord extends ListenerAdapter {
  private static final Pattern EveryoneAndHerePattern = Pattern.compile("@(?<ping>everyone|here)");

  private final Logger logger;
  private final Config config;
  private JDA jda;
  private final ProxyServer server;

  private String lastToken;
  private IncomingWebhookClient webhookClient;
  private final Map<String, ICommand> commands = new HashMap<>();

  private TextChannel activeChannel;
  private int lastPlayerCount = -1;

  // todo: buffer messages until ready
  public boolean ready = false;

  // todo: find a way to abstract away ProxyServer to remove all velocity dependencies
  public Discord(ProxyServer server, Logger logger, Config config) {
    this.logger = logger;
    this.config = config;
    this.server = server;

    configReloaded();
  }

  public void configReloaded() {
    commands.put("list", new ListCommand(server, config));

    var messageListener = new MessageListener(server, logger, config);

    if (!config.bot.DISCORD_TOKEN.equals(lastToken)) {
      if (jda != null) {
        shutdown();
      }

      var builder = JDABuilder.createDefault(config.bot.DISCORD_TOKEN)
        // this seems to download all users at bot startup and keep internal cache updated
        // without it, sometimes mentions miss when they shouldn't
        .setChunkingFilter(ChunkingFilter.ALL) //
        .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
        // mentions always miss without this
        .setMemberCachePolicy(MemberCachePolicy.ALL) //
        .addEventListeners(messageListener, this);

      try {
        jda = builder.build();
        this.lastToken = config.bot.DISCORD_TOKEN;
      } catch (Exception e) {
        this.logger.severe("Failed to login to discord: " + e);
      }
    }

    if (jda != null && !config.bot.WEBHOOK_URL.isEmpty()) {
      webhookClient = config.discord.isWebhookEnabled() ? WebhookClient.createClient(jda, config.bot.WEBHOOK_URL) : null;
    }
  }

  public void shutdown() {
    jda.shutdown();
  }

  // region JDA events

  @Override
  public void onReady(@Nonnull ReadyEvent event) {
    logger.info(MessageFormat.format("Bot ready, Guilds: {0} ({1} available)", event.getGuildTotalCount(), event.getGuildAvailableCount()));

    var channel = jda.getTextChannelById(Objects.requireNonNull(config.bot.CHANNEL_ID));

    if (channel == null) {
      logger.severe("Could not load channel with id: " + config.bot.CHANNEL_ID);
      return;
    }

    logger.info("Loaded channel: " + channel.getName());

    if (!channel.canTalk()) {
      logger.severe("Cannot talk in configured channel");
      return;
    }

    activeChannel = channel;

    var guild = activeChannel.getGuild();

    guild.upsertCommand("list", "list players").queue();

    this.ready = true;
  }

  @Override
  public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
    if (!ready) return;

    var command = event.getName();

    if (!commands.containsKey(command)) {
      return;
    }

    commands.get(command).handle(event);
  }

  // endregion

  // region Server events

  public void onPlayerChat(String username, String uuid, String server, String content) {
    if (!ready) return;

    if (config.bot.ENABLE_MENTIONS) {
      content = parseMentions(content);
    }

    if (!config.bot.ENABLE_EVERYONE_AND_HERE) {
      content = filterEveryoneAndHere(content);
    }

    if (config.discord.isWebhookEnabled()) {
      sendWebhookMessage(uuid, username, server, content);

      return;
    }

    if (config.discord.MESSAGE_FORMAT.isPresent()) {
      var message = new StringTemplate(config.discord.MESSAGE_FORMAT.get())
        .add("username", username)
        .add("server", server)
        .add("message", content).toString();

      switch (config.discord.MESSAGE_TYPE) {
        case EMBED -> sendEmbedMessage(message, config.discord.MESSAGE_EMBED_COLOR);
        case TEXT -> sendMessage(message);
      }
    }
  }

  public void onJoin(String username, String server) {
    if (!ready) return;

    if (config.discord.JOIN_MESSAGE_FORMAT.isEmpty()) {
      return;
    }

    var message = new StringTemplate(config.discord.JOIN_MESSAGE_FORMAT.get())
      .add("username", username)
      .add("server", server).toString();

    switch (config.discord.JOIN_MESSAGE_TYPE) {
      case EMBED -> sendEmbedMessage(message, config.discord.JOIN_MESSAGE_EMBED_COLOR);
      case TEXT -> sendMessage(message);
    }
  }

  public void onServerSwitch(String username, String current, String previous) {
    if (!ready) return;

    if (config.discord.SERVER_SWITCH_MESSAGE_FORMAT.isEmpty()) {
      return;
    }

    var message = new StringTemplate(config.discord.SERVER_SWITCH_MESSAGE_FORMAT.get())
      .add("username", username)
      .add("current", current)
      .add("previous", previous).toString();

    switch (config.discord.SERVER_SWITCH_MESSAGE_TYPE) {
      case EMBED -> sendEmbedMessage(message, config.discord.SERVER_SWITCH_MESSAGE_EMBED_COLOR);
      case TEXT -> sendMessage(message);
    }
  }

  public void onDisconnect(String username) {
    if (!ready) return;

    if (config.discord.DISCONNECT_MESSAGE_FORMAT.isEmpty()) {
      return;
    }

    var message = new StringTemplate(config.discord.DISCONNECT_MESSAGE_FORMAT.get())
      .add("username", username).toString();

    switch (config.discord.LEAVE_MESSAGE_TYPE) {
      case EMBED -> sendEmbedMessage(message, config.discord.DISCONNECT_MESSAGE_EMBED_COLOR);
      case TEXT -> sendMessage(message);
    }
  }

  public void onLeave(String username, String server) {
    if (!ready) return;

    if (config.discord.LEAVE_MESSAGE_FORMAT.isEmpty()) {
      return;
    }

    var message = new StringTemplate(config.discord.LEAVE_MESSAGE_FORMAT.get())
      .add("username", username)
      .add("server", server).toString();

    switch (config.discord.LEAVE_MESSAGE_TYPE) {
      case EMBED -> sendEmbedMessage(message, config.discord.LEAVE_MESSAGE_EMBED_COLOR);
      case TEXT -> sendMessage(message);
    }
  }

  public void onPlayerDeath(String username, String displayName, String death) {
    if (!ready) return;

    if (config.discord.DEATH_MESSAGE_FORMAT.isPresent()) {
      var message = new StringTemplate(config.discord.DEATH_MESSAGE_FORMAT.get())
        .add("username", username)
        .add("displayname", displayName)
        .add("death_message", death).toString();

      switch (config.discord.DEATH_MESSAGE_TYPE) {
        case EMBED -> sendEmbedMessage(message, config.discord.DEATH_MESSAGE_EMBED_COLOR);
        case TEXT -> sendMessage(message);
      }
    }
  }

  public void onPlayerAdvancement(String username, String displayname, String title, String description) {
    if (!ready) return;

    if (config.discord.ADVANCEMENT_MESSAGE_FORMAT.isPresent()) {
      var message = new StringTemplate(config.discord.ADVANCEMENT_MESSAGE_FORMAT.get())
        .add("username", username)
        .add("displayname", displayname)
        .add("advancement_title", title)
        .add("advancement_description", description).toString();

      switch (config.discord.ADVANCEMENT_MESSAGE_TYPE) {
        case EMBED -> sendEmbedMessage(message, config.discord.ADVANCEMENT_MESSAGE_EMBED_COLOR);
        case TEXT -> sendMessage(message);
      }
    }
  }

  public void onProxyInitialize() {
    if (!ready) return;

    if (config.discord.PROXY_START_MESSAGE_FORMAT.isPresent()) {
      var message = new StringTemplate(config.discord.PROXY_START_MESSAGE_FORMAT.get()).toString();

      switch (config.discord.PROXY_START_MESSAGE_TYPE) {
        case EMBED -> sendEmbedMessage(message, config.discord.PROXY_START_MESSAGE_EMBED_COLOR);
        case TEXT -> sendMessage(message);
      }
    }
  }

  public void onProxyShutdown() {
    if (!ready) return;

    if (config.discord.PROXY_STOP_MESSAGE_FORMAT.isPresent()) {
      var message = new StringTemplate(config.discord.PROXY_STOP_MESSAGE_FORMAT.get()).toString();

      switch (config.discord.PROXY_STOP_MESSAGE_TYPE) {
        case EMBED -> sendEmbedMessage(message, config.discord.PROXY_STOP_MESSAGE_EMBED_COLOR);
        case TEXT -> sendMessage(message);
      }
    }
  }

  public void onServerStart(String server) {
    if (!ready) return;

    if (config.discord.SERVER_START_MESSAGE_FORMAT.isPresent()) {
      var message = new StringTemplate(config.discord.SERVER_START_MESSAGE_FORMAT.get())
        .add("server", server).toString();

      switch (config.discord.SERVER_START_MESSAGE_TYPE) {
        case EMBED -> sendEmbedMessage(message, config.discord.SERVER_START_MESSAGE_EMBED_COLOR);
        case TEXT -> sendMessage(message);
      }
    }
  }

  public void onServerStop(String server) {
    if (!ready) return;

    if (config.discord.SERVER_STOP_MESSAGE_FORMAT.isPresent()) {
      var message = new StringTemplate(config.discord.SERVER_STOP_MESSAGE_FORMAT.get())
        .add("server", server).toString();

      switch (config.discord.SERVER_STOP_MESSAGE_TYPE) {
        case EMBED -> sendEmbedMessage(message, config.discord.SERVER_STOP_MESSAGE_EMBED_COLOR);
        case TEXT -> sendMessage(message);
      }
    }
  }

  public void updateActivityPlayerAmount(int count) {
    if (!config.bot.SHOW_ACTIVITY) {
      return;
    }

    if (this.lastPlayerCount != count) {
      var message = new StringTemplate(config.bot.ACTIVITY_FORMAT)
        .add("amount", count).toString();

      jda.getPresence().setActivity(Activity.playing(message));

      this.lastPlayerCount = count;
    }
  }

  public void updateChannelTopic() {
    if (config.discord.TOPIC_FORMAT.isEmpty() || !ready) {
      return;
    }

    // Collect additional information
    var playerCount = this.server.getPlayerCount();
    var playerList = this.server.getAllPlayers().stream()
      .map(Player::getUsername)
      .toList();
    var playerPingList = this.server.getAllPlayers().stream()
      .map(player -> player.getUsername() + " (" + player.getPing() + "ms)")
      .toList();
    var serverCount = this.server.getAllServers().size();
    var serverList = this.server.getAllServers().stream()
      .map(registeredServer -> registeredServer.getServerInfo().getName())
      .toList();
    var hostname = this.server.getBoundAddress().getHostName();
    var port = String.valueOf(this.server.getBoundAddress().getPort());
    var queryMotd = PlainTextComponentSerializer.plainText().serialize(this.server.getConfiguration().getMotd());
    var queryMap = this.server.getConfiguration().getQueryMap();
    var queryPort = this.server.getConfiguration().getQueryPort();
    var queryMaxPlayers = this.server.getConfiguration().getShowMaxPlayers();
    var pluginCount = this.server.getPluginManager().getPlugins().size();
    var pluginList = this.server.getPluginManager().getPlugins().stream()
      .map(plugin -> plugin.getDescription().getName())
      .flatMap(Optional::stream)
      .toList();
    var version = this.server.getVersion().getVersion();
    var software = this.server.getVersion().getName();

    // Calculate average ping
    var averagePing = this.server.getAllPlayers().stream()
      .mapToLong(Player::getPing)
      .average()
      .orElse(0.0);


    // Get server uptime
    var uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
    var uptimeHours = TimeUnit.MILLISECONDS.toHours(uptimeMillis);
    var uptimeMinutes = TimeUnit.MILLISECONDS.toMinutes(uptimeMillis) % 60;

    // Ping each server and get status
    var serverStatuses = new HashMap<String, String>();
    for (var registeredServer : server.getAllServers()) {
      try {
        var ping = registeredServer.ping();

        ping.thenAccept(serverPing -> {
          if (serverPing.getPlayers().isEmpty()) {
            return;
          }

          var players = serverPing.getPlayers().get();

          var serverStatus = registeredServer.getServerInfo().getName() + " - " +
            players.getOnline() + "/" + players.getMax() + " players," +
            " Version: " + serverPing.getVersion().getName() + " (" + serverPing.getVersion().getProtocol() + ") | " +
            PlainTextComponentSerializer.plainText().serialize(serverPing.getDescriptionComponent());

          serverStatuses.put(registeredServer.getServerInfo().getName(), serverStatus);
        }).get(5, TimeUnit.SECONDS);
      } catch (Exception e) {
        serverStatuses.put(registeredServer.getServerInfo().getName(), registeredServer.getServerInfo().getName() + " - Offline");
      }
    }

    // Build the message
    StringTemplate template = new StringTemplate(config.discord.TOPIC_FORMAT.get())
      .add("playerCount", playerCount)
      .add("playerList", String.join(", ", playerList))
      .add("playerPingList", String.join(", ", playerPingList))
      .add("serverCount", serverCount)
      .add("serverList", String.join(", ", serverList))
      .add("hostname", hostname)
      .add("port", port)
      .add("queryMotd", queryMotd)
      .add("queryMap", queryMap)
      .add("queryPort", queryPort)
      .add("queryMaxPlayers", queryMaxPlayers)
      .add("pluginCount", pluginCount)
      .add("pluginList", String.join(", ", pluginList))
      .add("version", version)
      .add("software", software)
      .add("averagePing", String.format("%.2f ms", averagePing))
      .add("uptime", String.format("%dh %dm", uptimeHours, uptimeMinutes));

    // Add server-specific details with server[SERVERNAME] placeholders
    for (var entry : serverStatuses.entrySet()) {
      template.add("server[" + entry.getKey() + "]", entry.getValue());
    }

    var topic = template.toString();

    if (topic.length() > 1024) {
      topic = topic.substring(0, 1000) + "...";
    }

    activeChannel.getManager().setTopic(topic).queue();
  }

  // endregion

  // region Message sending

  private void sendMessage(@Nonnull String message) {
    activeChannel.sendMessage(message).queue();
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private void sendEmbedMessage(String message, Optional<Color> color) {
    var embed = new EmbedBuilder().setDescription(message);

    color.ifPresent(embed::setColor);

    activeChannel.sendMessageEmbeds(embed.build()).queue();
  }

  private void sendWebhookMessage(String uuid, String username, String server, String content) {
    if (webhookClient == null) {
      logger.fine("Webhook client was not created due to configuration error, skipping sending message");
      return;
    }

    var avatar = new StringTemplate(config.bot.WEBHOOK_AVATAR_URL)
      .add("username", username)
      .add("uuid", uuid).toString();

    var discordName = new StringTemplate(config.bot.WEBHOOK_USERNAME)
      .add("username", username)
      .add("server", server).toString();

    var webhookMessage = new MessageCreateBuilder().setContent(content).build();

    webhookClient.sendMessage(webhookMessage).setAvatarUrl(avatar).setUsername(discordName).queue();
  }

  // endregion

  private String parseMentions(String message) {
    var msg = message;

    for (var member : activeChannel.getMembers()) {
      msg = Pattern.compile(Pattern.quote("@" + member.getUser().getName()), Pattern.CASE_INSENSITIVE).matcher(msg).replaceAll(member.getAsMention());
    }

    return msg;
  }

  private String filterEveryoneAndHere(String message) {
    return EveryoneAndHerePattern.matcher(message).replaceAll("@\u200B${ping}");
  }
}
