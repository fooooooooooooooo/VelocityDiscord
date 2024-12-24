package ooo.foooooooooooo.velocitydiscord.discord;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import ooo.foooooooooooo.velocitydiscord.config.Config;
import ooo.foooooooooooo.velocitydiscord.discord.commands.ICommand;
import ooo.foooooooooooo.velocitydiscord.discord.commands.ListCommand;
import ooo.foooooooooooo.velocitydiscord.util.StringTemplate;

import javax.annotation.Nonnull;
import java.awt.*;
import java.lang.management.ManagementFactory;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Discord extends ListenerAdapter {
  private static final Pattern EveryoneAndHerePattern = Pattern.compile("@(?<ping>everyone|here)");

  private final Logger logger;
  private final Config config;
  private JDA jda;
  private final ProxyServer server;

  private String lastToken;
  private IncomingWebhookClient webhookClient;
  private final Map<String, ICommand> commands = new HashMap<>();

  private final MessageListener messageListener;

  private TextChannel activeChannel;
  private int lastPlayerCount = -1;

  private List<String> mentionCompletions;

  public boolean ready = false;

  // queue of Object because multiple types of messages and
  // cant create a common RestAction object without activeChannel
  private final Queue<Object> preReadyQueue = new ArrayDeque<>();

  // todo: find a way to abstract away ProxyServer to remove all velocity dependencies
  public Discord(ProxyServer server, Logger logger, Config config) {
    this.logger = logger;
    this.config = config;
    this.server = server;
    this.messageListener = new MessageListener(server, logger, config);

    configReloaded();
  }

  public void configReloaded() {
    commands.put("list", new ListCommand(server, config));

    // update webhook id in case the webhook url changed
    this.messageListener.updateWebhookId();

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
        .addEventListeners(this.messageListener, this);

      try {
        jda = builder.build();
        this.lastToken = config.bot.DISCORD_TOKEN;
      } catch (Exception e) {
        this.logger.severe("Failed to login to discord: " + e);
      }
    }

    if (jda != null && !config.bot.WEBHOOK_URL.isEmpty()) {
      if (config.discord.isWebhookEnabled()) {
        webhookClient = WebhookClient.createClient(jda, config.bot.WEBHOOK_URL);
      } else {
        webhookClient = null;
      }
    }
  }

  public void shutdown() {
    jda.shutdown();
  }

  // region JDA events

  @Override
  public void onReady(@Nonnull ReadyEvent event) {
    logger.info(MessageFormat.format(
      "Bot ready, Guilds: {0} ({1} available)",
      event.getGuildTotalCount(),
      event.getGuildAvailableCount()
    ));

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

    // Load all discord users in the channel and add them to MC client chat suggestions
    var members = channel.getMembers();
    mentionCompletions = members.stream().map((m -> "@"+m.getUser().getName())).toList();
    for (var mention : mentionCompletions) {
      System.out.println(mention);
    }

    activeChannel = channel;

    var guild = activeChannel.getGuild();

    guild.upsertCommand("list", "list players").queue();

    this.ready = true;

    for (var msg : preReadyQueue) {
      if (msg instanceof String message) {
        activeChannel.sendMessage(message).queue();
      } else if (msg instanceof QueuedWebhookMessage webhookMessage) {
        if (this.webhookClient != null) {
          this.webhookClient.sendMessage(webhookMessage.message)
            .setAvatarUrl(webhookMessage.avatar)
            .setUsername(webhookMessage.username)
            .queue();
        }
      } else if (msg instanceof MessageEmbed embed) {
        activeChannel.sendMessageEmbeds(embed).queue();
      } else if (msg instanceof Player player) {
        player.addCustomChatCompletions(mentionCompletions);
      } else {
        logger.warning("Unknown message type in preReadyQueue: " + msg);
      }
    }
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

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public void onPlayerChat(String username, String uuid, Optional<String> prefix, String server, String content) {
    if (config.bot.ENABLE_MENTIONS) {
      content = parseMentions(content);
    }

    if (!config.bot.ENABLE_EVERYONE_AND_HERE) {
      content = filterEveryoneAndHere(content);
    }

    if (config.discord.MESSAGE_FORMAT.isPresent()) {
      var message = new StringTemplate(config.discord.MESSAGE_FORMAT.get())
        .add("username", username)
        .add("server", config.serverName(server))
        .add("message", content)
        .add("prefix", prefix.orElse(""))
        .toString();

      switch (config.discord.MESSAGE_TYPE) {
        case EMBED -> sendEmbedMessage(message, config.discord.MESSAGE_EMBED_COLOR);
        case TEXT -> sendMessage(message);
        case WEBHOOK -> sendWebhookMessage(uuid, username, server, content);
        default -> throw new IllegalArgumentException("Unexpected value: " + config.discord.MESSAGE_TYPE);
      }
    }
  }

  private void sendChatCompletions(Player player) {
    if (mentionCompletions == null) {
      preReadyQueue.add(player);
    } else {
      player.addCustomChatCompletions(mentionCompletions);
    }
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public void onJoin(Player player, Optional<String> prefix, String server) {
    sendChatCompletions(player);

    if (config.discord.JOIN_MESSAGE_FORMAT.isEmpty()) {
      return;
    }

    var message = new StringTemplate(config.discord.JOIN_MESSAGE_FORMAT.get())
      .add("username", player.getUsername())
      .add("server", config.serverName(server))
      .add("prefix", prefix.orElse(""))
      .toString();

    switch (config.discord.JOIN_MESSAGE_TYPE) {
      case EMBED -> sendEmbedMessage(message, config.discord.JOIN_MESSAGE_EMBED_COLOR);
      case TEXT -> sendMessage(message);
      case WEBHOOK -> sendWebhookMessage(player.getUniqueId().toString(), player.getUsername(), server, message);
    }
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public void onServerSwitch(String username, String uuid, Optional<String> prefix, String current, String previous) {
    if (config.discord.SERVER_SWITCH_MESSAGE_FORMAT.isEmpty()) {
      return;
    }

    var message = new StringTemplate(config.discord.SERVER_SWITCH_MESSAGE_FORMAT.get())
      .add("username", username)
      .add("current", config.serverName(current))
      .add("previous", config.serverName(previous))
      .add("prefix", prefix.orElse(""))
      .toString();

    switch (config.discord.SERVER_SWITCH_MESSAGE_TYPE) {
      case EMBED -> sendEmbedMessage(message, config.discord.SERVER_SWITCH_MESSAGE_EMBED_COLOR);
      case TEXT -> sendMessage(message);
      case WEBHOOK -> sendWebhookMessage(uuid, username, current, message);
    }
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public void onDisconnect(String username, String uuid, Optional<String> prefix, String server) {
    if (config.discord.DISCONNECT_MESSAGE_FORMAT.isEmpty()) {
      return;
    }

    var message = new StringTemplate(config.discord.DISCONNECT_MESSAGE_FORMAT.get())
      .add("username", username)
      .add("prefix", prefix.orElse(""))
      .toString();

    switch (config.discord.LEAVE_MESSAGE_TYPE) {
      case EMBED -> sendEmbedMessage(message, config.discord.DISCONNECT_MESSAGE_EMBED_COLOR);
      case TEXT -> sendMessage(message);
      case WEBHOOK -> sendWebhookMessage(uuid, username, server, message);
    }
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public void onLeave(String username, String uuid, Optional<String> prefix, String server) {
    if (config.discord.LEAVE_MESSAGE_FORMAT.isEmpty()) {
      return;
    }

    var message = new StringTemplate(config.discord.LEAVE_MESSAGE_FORMAT.get())
      .add("username", username)
      .add("server", config.serverName(server))
      .add("prefix", prefix.orElse(""))
      .toString();

    switch (config.discord.LEAVE_MESSAGE_TYPE) {
      case EMBED -> sendEmbedMessage(message, config.discord.LEAVE_MESSAGE_EMBED_COLOR);
      case TEXT -> sendMessage(message);
      case WEBHOOK -> sendWebhookMessage(uuid, username, server, message);
    }
  }

  public void onPlayerDeath(String username, String uuid, String server, String displayName, String death) {
    if (config.discord.DEATH_MESSAGE_FORMAT.isPresent()) {
      var message = new StringTemplate(config.discord.DEATH_MESSAGE_FORMAT.get())
        .add("username", username)
        .add("displayname", displayName)
        .add("death_message", death)
        .toString();

      switch (config.discord.DEATH_MESSAGE_TYPE) {
        case EMBED -> sendEmbedMessage(message, config.discord.DEATH_MESSAGE_EMBED_COLOR);
        case TEXT -> sendMessage(message);
        case WEBHOOK -> sendWebhookMessage(uuid, username, server, message);
      }
    }
  }

  public void onPlayerAdvancement(String username, String uuid, String server, String displayname, String title, String description) {
    if (config.discord.ADVANCEMENT_MESSAGE_FORMAT.isPresent()) {
      var message = new StringTemplate(config.discord.ADVANCEMENT_MESSAGE_FORMAT.get())
        .add("username", username)
        .add("displayname", displayname)
        .add("advancement_title", title)
        .add("advancement_description", description)
        .toString();

      switch (config.discord.ADVANCEMENT_MESSAGE_TYPE) {
        case EMBED -> sendEmbedMessage(message, config.discord.ADVANCEMENT_MESSAGE_EMBED_COLOR);
        case TEXT -> sendMessage(message);
        case WEBHOOK -> sendWebhookMessage(uuid, username, server, message);
      }
    }
  }

  public void onProxyInitialize() {
    if (config.discord.PROXY_START_MESSAGE_FORMAT.isPresent()) {
      var message = config.discord.PROXY_START_MESSAGE_FORMAT.get();

      switch (config.discord.PROXY_START_MESSAGE_TYPE) {
        case EMBED -> sendEmbedMessage(message, config.discord.PROXY_START_MESSAGE_EMBED_COLOR);
        case TEXT -> sendMessage(message);
      }
    }
  }

  public void onProxyShutdown() {
    if (config.discord.PROXY_STOP_MESSAGE_FORMAT.isPresent()) {
      var message = config.discord.PROXY_STOP_MESSAGE_FORMAT.get();

      switch (config.discord.PROXY_STOP_MESSAGE_TYPE) {
        case EMBED -> sendEmbedMessage(message, config.discord.PROXY_STOP_MESSAGE_EMBED_COLOR);
        case TEXT -> sendMessage(message);
      }
    }
  }

  public void onServerStart(String server) {
    if (config.discord.SERVER_START_MESSAGE_FORMAT.isPresent()) {
      var message = new StringTemplate(config.discord.SERVER_START_MESSAGE_FORMAT.get())
        .add("server", config.serverName(server))
        .toString();

      switch (config.discord.SERVER_START_MESSAGE_TYPE) {
        case EMBED -> sendEmbedMessage(message, config.discord.SERVER_START_MESSAGE_EMBED_COLOR);
        case TEXT -> sendMessage(message);
      }
    }
  }

  public void onServerStop(String server) {
    if (config.discord.SERVER_STOP_MESSAGE_FORMAT.isPresent()) {
      var message = new StringTemplate(config.discord.SERVER_STOP_MESSAGE_FORMAT.get())
        .add("server", config.serverName(server))
        .toString();

      switch (config.discord.SERVER_STOP_MESSAGE_TYPE) {
        case EMBED -> sendEmbedMessage(message, config.discord.SERVER_STOP_MESSAGE_EMBED_COLOR);
        case TEXT -> sendMessage(message);
      }
    }
  }

  public void updateActivityPlayerAmount(int count) {
    if (!config.bot.SHOW_ACTIVITY || !ready) {
      return;
    }

    if (this.lastPlayerCount != count) {
      var message = new StringTemplate(config.bot.ACTIVITY_FORMAT)
        .add("amount", count)
        .toString();

      jda.getPresence().setActivity(Activity.playing(message));

      this.lastPlayerCount = count;
    }
  }

  public String generateChannelTopic() {
    if (config.discord.TOPIC_FORMAT.isEmpty()) return null;

    // Collect additional information
    var playerCount = this.server.getPlayerCount();

    var playerList = "";

    // only generate player list if it's in the TOPIC_FORMAT
    if (config.discord.TOPIC_FORMAT.get().contains("{player_list}")) {
      playerList = this.server.getAllPlayers().stream()
        .limit(config.discord.TOPIC_PLAYER_LIST_MAX_COUNT)
        .map(player -> new StringTemplate(config.discord.TOPIC_PLAYER_LIST_FORMAT)
          .add("username", player.getUsername())
          .add("ping", player.getPing())
          .toString()
        )
        .reduce("", (a, b) -> a + config.discord.TOPIC_PLAYER_LIST_SEPARATOR + b);

      if (!playerList.isEmpty()) {
        playerList = playerList.substring(config.discord.TOPIC_PLAYER_LIST_SEPARATOR.length());
      } else {
        playerList = config.discord.TOPIC_PLAYER_LIST_NO_PLAYERS_HEADER.orElse("");
      }
    }

    var serverCount = this.server.getAllServers().size();
    var serverList = this.server.getAllServers().stream()
      .map(registeredServer -> registeredServer.getServerInfo().getName())
      .toList();
    var hostname = this.server.getBoundAddress().getHostName();
    var port = String.valueOf(this.server.getBoundAddress().getPort());
    var queryMotd = PlainTextComponentSerializer.plainText().serialize(this.server.getConfiguration().getMotd());
    var queryPort = this.server.getConfiguration().getQueryPort();
    var queryMaxPlayers = this.server.getConfiguration().getShowMaxPlayers();
    var pluginCount = this.server.getPluginManager().getPlugins().size();
    var pluginList = this.server.getPluginManager().getPlugins().stream()
      .map(plugin -> plugin.getDescription().getName())
      .flatMap(Optional::stream)
      .toList();
    var proxyVersion = this.server.getVersion().getVersion();
    var proxySoftware = this.server.getVersion().getName();

    // Calculate average ping
    var averagePing = this.server.getAllPlayers().stream()
      .mapToLong(Player::getPing)
      .average()
      .orElse(0.0);

    // Get server uptime
    var uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
    var formattedUptime = formatUptime(uptimeMillis);

    // Ping each server and get status
    var serverStatuses = new HashMap<String, String>();
    for (var registeredServer : server.getAllServers()) {
      var name = registeredServer.getServerInfo().getName();

      if (config.serverDisabled(name)) {
        continue;
      }

      if (config.discord.TOPIC_SERVER_FORMAT.isEmpty()) {
        serverStatuses.put(name, "");
      }

      try {
        var ping = registeredServer.ping();

        ping.thenAccept(serverPing -> {
          if (serverPing.getPlayers().isEmpty()) {
            return;
          }

          var players = serverPing.getPlayers().get();

          var online = players.getOnline();
          var max = players.getMax();
          var ver = serverPing.getVersion().getName();
          var protocol = serverPing.getVersion().getProtocol();
          var motd = PlainTextComponentSerializer.plainText().serialize(serverPing.getDescriptionComponent());

          var serverStatus = new StringTemplate(config.discord.TOPIC_SERVER_FORMAT.get())
            .add("name", config.serverName(name))
            .add("players", online)
            .add("max_players", max)
            .add("version", ver)
            .add("protocol", protocol)
            .add("motd", motd)
            .toString();

          serverStatuses.put(name, serverStatus);
        }).get(5, TimeUnit.SECONDS);
      } catch (Exception e) {
        if (config.discord.TOPIC_SERVER_OFFLINE_FORMAT.isEmpty()) {
          serverStatuses.put(name, "");
          continue;
        }

        var serverStatus = new StringTemplate(config.discord.TOPIC_SERVER_OFFLINE_FORMAT.get())
          .add("name", config.serverName(name))
          .toString();

        serverStatuses.put(name, serverStatus);
      }
    }

    // Build the message
    var template = new StringTemplate(config.discord.TOPIC_FORMAT.get())
      .add("players", playerCount)
      .add("player_list", playerList)
      .add("servers", serverCount)
      .add("server_list", String.join(", ", serverList.stream().map(config::serverName).toList()))
      .add("hostname", hostname)
      .add("port", port)
      .add("motd", queryMotd)
      .add("query_port", queryPort)
      .add("max_players", queryMaxPlayers)
      .add("plugins", pluginCount)
      .add("plugin_list", String.join(", ", pluginList))
      .add("version", proxyVersion)
      .add("software", proxySoftware)
      .add("average_ping", String.format("%.2f ms", averagePing))
      .add("uptime", formattedUptime);

    // Add server-specific details with server[SERVERNAME] placeholders
    for (var entry : serverStatuses.entrySet()) {
      template.add("server[" + entry.getKey() + "]", entry.getValue());
    }

    var topic = template.toString();

    if (topic.length() > 1024) {
      topic = topic.substring(0, 1000) + "...";
    }

    return topic;
  }

  public void updateChannelTopic() {
    if (!ready) return;

    var topic = generateChannelTopic();

    if (topic == null) return;

    activeChannel.getManager().setTopic(topic).queue();
  }

  private String formatUptime(long uptimeMillis) {
    var seconds = TimeUnit.MILLISECONDS.toSeconds(uptimeMillis);
    var minutes = TimeUnit.MILLISECONDS.toMinutes(uptimeMillis);
    var hours = TimeUnit.MILLISECONDS.toHours(uptimeMillis);
    var days = TimeUnit.MILLISECONDS.toDays(uptimeMillis);

    if (seconds < 60) {
      return seconds + "s";
    } else if (minutes < 60) {
      var remainingSeconds = seconds % 60;
      return minutes + "m " + remainingSeconds + "s";
    } else if (hours < 24) {
      var remainingMinutes = minutes % 60;
      return hours + "h " + remainingMinutes + "m";
    } else if (days < 7) {
      var remainingHours = hours % 24;
      return days + "d " + remainingHours + "h";
    } else {
      var weeks = days / 7;
      var remainingDays = days % 7;
      return weeks + "w " + remainingDays + "d";
    }
  }

  // endregion

  // region Message sending

  private void sendMessage(@Nonnull String message) {
    if (ready) {
      activeChannel.sendMessage(message).queue();
    } else {
      preReadyQueue.add(message);
    }
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private void sendEmbedMessage(String message, Optional<Color> color) {
    var embed = new EmbedBuilder().setDescription(message);

    color.ifPresent(embed::setColor);

    if (ready) {
      activeChannel.sendMessageEmbeds(embed.build()).queue();
    } else {
      preReadyQueue.add(embed.build());
    }
  }

  private void sendWebhookMessage(String uuid, String username, String server, String content) {
    if (webhookClient == null) {
      logger.fine("Webhook client was not created due to configuration error, skipping sending message");
      return;
    }

    var avatar = new StringTemplate(config.bot.WEBHOOK_AVATAR_URL)
      .add("username", username)
      .add("uuid", uuid)
      .toString();

    var discordName = new StringTemplate(config.bot.WEBHOOK_USERNAME)
      .add("username", username)
      .add("server", config.serverName(server))
      .toString();

    var webhookMessage = new MessageCreateBuilder().setContent(content).build();

    if (ready) {
      webhookClient.sendMessage(webhookMessage).setAvatarUrl(avatar).setUsername(discordName).queue();
    } else {
      preReadyQueue.add(new QueuedWebhookMessage(webhookMessage, avatar, discordName));
    }
  }

  // endregion

  private String parseMentions(String message) {
    if (activeChannel == null || !ready) {
      return message;
    }

    var msg = message;

    for (var member : activeChannel.getMembers()) {
      msg = Pattern.compile(Pattern.quote("@" + member.getUser().getName()), Pattern.CASE_INSENSITIVE)
        .matcher(msg)
        .replaceAll(member.getAsMention());
    }

    return msg;
  }

  private String filterEveryoneAndHere(String message) {
    return EveryoneAndHerePattern.matcher(message).replaceAll("@\u200B${ping}");
  }

  private record QueuedWebhookMessage(MessageCreateData message, String avatar, String username) {}
}
