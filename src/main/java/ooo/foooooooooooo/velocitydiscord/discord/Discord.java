package ooo.foooooooooooo.velocitydiscord.discord;

import com.velocitypowered.api.proxy.Player;
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
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import ooo.foooooooooooo.velocitydiscord.VelocityDiscord;
import ooo.foooooooooooo.velocitydiscord.config.ServerConfig;
import ooo.foooooooooooo.velocitydiscord.discord.commands.ICommand;
import ooo.foooooooooooo.velocitydiscord.discord.commands.ListCommand;
import ooo.foooooooooooo.velocitydiscord.discord.message.IQueuedMessage;
import ooo.foooooooooooo.velocitydiscord.util.StringTemplate;

import javax.annotation.Nonnull;
import java.awt.*;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Discord extends ListenerAdapter {
  private static final Pattern EveryoneAndHerePattern = Pattern.compile("@(?<ping>everyone|here)");
  private static final Pattern RawPingPattern = Pattern.compile("<@(?<ping>[!&]?\\d+)>");

  private final MessageListener messageListener;

  private final Map<String, ICommand> commands = new HashMap<>();
  private final HashMap<String, List<String>> mentionCompletions = new HashMap<>();
  private final HashMap<String, Channels> serverChannels = new HashMap<>();

  // queue of Object because multiple types of messages and
  // cant create a common RestAction object without activeChannel
  private final Queue<IQueuedMessage> preReadyQueue = new ArrayDeque<>();

  private boolean ready = false;

  private JDA jda;

  private String lastToken;

  private IncomingWebhookClient webhookClient;

  private TextChannel mainChannel;
  private TextChannel proxyStartChannel;
  private TextChannel proxyStopChannel;
  private Channels defaultChannels;

  private int lastPlayerCount = -1;

  public Discord() {
    this.messageListener = new MessageListener(this.serverChannels);

    onConfigReload();
  }

  public void onConfigReload() {
    if (VelocityDiscord.CONFIG.bot.listCommand.DISCORD_LIST_ENABLED) {
      this.commands.put(ListCommand.COMMAND_NAME, new ListCommand());
    }

    // update webhook id in case the webhook url changed
    this.messageListener.updateWebhookId();

    if (!VelocityDiscord.CONFIG.bot.DISCORD_TOKEN.equals(this.lastToken)) {
      if (this.jda != null) {
        shutdown();
      }

      var builder = JDABuilder.createDefault(VelocityDiscord.CONFIG.bot.DISCORD_TOKEN)
        // this seems to download all users at bot startup and keep internal cache updated
        // without it, sometimes mentions miss when they shouldn't
        .setChunkingFilter(ChunkingFilter.ALL)
        .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
        // mentions always miss without this
        .setMemberCachePolicy(MemberCachePolicy.ALL)
        .addEventListeners(this.messageListener, this);

      try {
        this.jda = builder.build();
        this.lastToken = VelocityDiscord.CONFIG.bot.DISCORD_TOKEN;
      } catch (Exception e) {
        VelocityDiscord.LOGGER.error("Failed to login to discord:", e);
      }
    } else {
      // no ready event, just reload channels here
      loadChannels();
    }

    if (this.jda == null) return;

    // todo: per server channel overrides for webhook
    if (!VelocityDiscord.CONFIG.bot.WEBHOOK_URL.isEmpty()) {
      if (VelocityDiscord.CONFIG.isAnyWebhookEnabled()) {
        this.webhookClient = WebhookClient.createClient(this.jda, VelocityDiscord.CONFIG.bot.WEBHOOK_URL);
      } else {
        this.webhookClient = null;
      }
    }
  }

  public void shutdown() {
    this.jda.shutdown();
  }

  // region JDA events

  @Override
  public void onReady(@Nonnull ReadyEvent event) {
    VelocityDiscord.LOGGER.info(
      "Bot ready, Guilds: {} ({} available)",
      event.getGuildTotalCount(),
      event.getGuildAvailableCount()
    );

    loadChannels();

    this.ready = true;

    for (var msg : this.preReadyQueue) {
      msg.send(this);
    }
  }

  private void loadChannels() {
    this.mainChannel = loadChannel(VelocityDiscord.CONFIG.bot.MAIN_CHANNEL_ID);
    this.proxyStartChannel =
      loadChannel(VelocityDiscord.CONFIG.discord.PROXY_START_CHANNEL.orElse(VelocityDiscord.CONFIG.bot.MAIN_CHANNEL_ID));
    this.proxyStopChannel =
      loadChannel(VelocityDiscord.CONFIG.discord.PROXY_STOP_CHANNEL.orElse(VelocityDiscord.CONFIG.bot.MAIN_CHANNEL_ID));

    this.serverChannels.clear();
    for (var server : VelocityDiscord.SERVER.getAllServers()) {
      var serverName = server.getServerInfo().getName();
      var config = VelocityDiscord.CONFIG.getServerConfig(serverName);
      var defaultChannel = this.jda.getTextChannelById(config.getBotConfig().MAIN_CHANNEL_ID);
      this.serverChannels.put(serverName, new Channels(this, serverName, config, defaultChannel));
    }

    this.defaultChannels = new Channels(this, "default", VelocityDiscord.CONFIG, this.mainChannel);

    this.messageListener.onServerChannelsUpdated();

    // Load all discord users in the channel and add them to MC client chat suggestions
    this.mentionCompletions.clear();
    for (var channels : this.serverChannels.values()) {
      var members = channels.chatChannel.getMembers();
      this.mentionCompletions.put(
        channels.serverName,
        members.stream().map((m -> "@" + m.getUser().getName())).toList()
      );
    }

    if (!this.commands.isEmpty()) {
      var guild = this.mainChannel.getGuild();

      for (var entry : this.commands.entrySet()) {
        guild.upsertCommand(entry.getKey(), entry.getValue().description()).queue();
      }
    }
  }

  @Override
  public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
    if (!this.ready) return;

    var command = event.getName();

    if (!this.commands.containsKey(command)) {
      return;
    }

    this.commands.get(command).handle(event);
  }

  // endregion

  // region Server events

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public void onPlayerChat(String username, String uuid, Optional<String> prefix, String server, String content) {
    var serverConfig = VelocityDiscord.CONFIG.getServerConfig(server);
    var serverBotConfig = serverConfig.getBotConfig();
    var serverDiscordConfig = serverConfig.getDiscordMessageConfig();

    content = filterRawPings(content);

    if (serverBotConfig.ENABLE_MENTIONS) {
      content = parseMentions(server, content);
    }

    if (!serverBotConfig.ENABLE_EVERYONE_AND_HERE) {
      content = filterEveryoneAndHere(content);
    }

    if (serverDiscordConfig.MESSAGE_FORMAT.isPresent()) {
      var message = new StringTemplate(serverDiscordConfig.MESSAGE_FORMAT.get())
        .add("username", username)
        .add("server", VelocityDiscord.CONFIG.serverName(server))
        .add("message", content)
        .add("prefix", prefix.orElse(""))
        .toString();

      var targetChannel = getServerChannels(server).chatChannel;
      switch (serverDiscordConfig.MESSAGE_TYPE) {
        case EMBED -> sendEmbedMessage(targetChannel, message, serverDiscordConfig.MESSAGE_EMBED_COLOR);
        case TEXT -> sendMessage(targetChannel, message);
        case WEBHOOK -> sendWebhookMessage(targetChannel, uuid, username, server, content);
        default -> throw new IllegalArgumentException("Unexpected value: " + serverDiscordConfig.MESSAGE_TYPE);
      }
    }
  }

  private void sendChatCompletions(String server, Player player) {
    if (!this.ready) {
      this.preReadyQueue.add(new QueuedChatCompletion(server, player));
    } else {
      player.addCustomChatCompletions(this.mentionCompletions.get(server));
    }
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public void onJoin(Player player, Optional<String> prefix, String server) {
    sendChatCompletions(server, player);

    var serverDiscordConfig = VelocityDiscord.CONFIG.getServerConfig(server).getDiscordMessageConfig();

    if (serverDiscordConfig.JOIN_FORMAT.isEmpty()) {
      return;
    }

    var message = new StringTemplate(serverDiscordConfig.JOIN_FORMAT.get())
      .add("username", player.getUsername())
      .add("server", VelocityDiscord.CONFIG.serverName(server))
      .add("prefix", prefix.orElse(""))
      .toString();

    var targetChannel = getServerChannels(server).joinChannel;
    switch (serverDiscordConfig.JOIN_TYPE) {
      case EMBED -> sendEmbedMessage(targetChannel, message, serverDiscordConfig.JOIN_EMBED_COLOR);
      case TEXT -> sendMessage(targetChannel, message);
      case WEBHOOK ->
        sendWebhookMessage(targetChannel, player.getUniqueId().toString(), player.getUsername(), server, message);
    }
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public void onServerSwitch(String username, String uuid, Optional<String> prefix, String current, String previous) {
    var serverDiscordConfig = VelocityDiscord.CONFIG.getServerConfig(current).getDiscordMessageConfig();

    if (serverDiscordConfig.SERVER_SWITCH_FORMAT.isEmpty()) {
      return;
    }

    var message = new StringTemplate(serverDiscordConfig.SERVER_SWITCH_FORMAT.get())
      .add("username", username)
      .add("current", VelocityDiscord.CONFIG.serverName(current))
      .add("previous", VelocityDiscord.CONFIG.serverName(previous))
      .add("prefix", prefix.orElse(""))
      .toString();

    // todo: send to current or previous server or both
    var targetChannel = getServerChannels(current).serverSwitchChannel;
    switch (serverDiscordConfig.SERVER_SWITCH_TYPE) {
      case EMBED -> sendEmbedMessage(targetChannel, message, serverDiscordConfig.SERVER_SWITCH_EMBED_COLOR);
      case TEXT -> sendMessage(targetChannel, message);
      case WEBHOOK -> sendWebhookMessage(targetChannel, uuid, username, current, message);
    }
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public void onDisconnect(String username, String uuid, Optional<String> prefix, String server) {
    var serverDiscordConfig = VelocityDiscord.CONFIG.getServerConfig(server).getDiscordMessageConfig();

    if (serverDiscordConfig.DISCONNECT_FORMAT.isEmpty()) {
      return;
    }

    var message = new StringTemplate(serverDiscordConfig.DISCONNECT_FORMAT.get())
      .add("username", username)
      .add("prefix", prefix.orElse(""))
      .toString();

    var targetChannel = getServerChannels(server).disconnectChannel;
    switch (serverDiscordConfig.DISCONNECT_TYPE) {
      case EMBED -> sendEmbedMessage(targetChannel, message, serverDiscordConfig.DISCONNECT_EMBED_COLOR);
      case TEXT -> sendMessage(targetChannel, message);
      case WEBHOOK -> sendWebhookMessage(targetChannel, uuid, username, server, message);
    }
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public void onLeave(String username, String uuid, Optional<String> prefix, String server) {
    var serverDiscordConfig = VelocityDiscord.CONFIG.getServerConfig(server).getDiscordMessageConfig();

    if (serverDiscordConfig.LEAVE_FORMAT.isEmpty()) {
      return;
    }

    var message = new StringTemplate(serverDiscordConfig.LEAVE_FORMAT.get())
      .add("username", username)
      .add("server", VelocityDiscord.CONFIG.serverName(server))
      .add("prefix", prefix.orElse(""))
      .toString();

    var targetChannel = getServerChannels(server).leaveChannel;
    switch (serverDiscordConfig.LEAVE_TYPE) {
      case EMBED -> sendEmbedMessage(targetChannel, message, serverDiscordConfig.LEAVE_EMBED_COLOR);
      case TEXT -> sendMessage(targetChannel, message);
      case WEBHOOK -> sendWebhookMessage(targetChannel, uuid, username, server, message);
    }
  }

  public void onPlayerDeath(String username, String uuid, String server, String displayName, String death) {
    var serverDiscordConfig = VelocityDiscord.CONFIG.getServerConfig(server).getDiscordMessageConfig();

    if (serverDiscordConfig.DEATH_FORMAT.isEmpty()) return;

    var message = new StringTemplate(serverDiscordConfig.DEATH_FORMAT.get())
      .add("username", username)
      .add("displayname", displayName)
      .add("death_message", death)
      .toString();

    var targetChannel = getServerChannels(server).deathChannel;
    switch (serverDiscordConfig.DEATH_TYPE) {
      case EMBED -> sendEmbedMessage(targetChannel, message, serverDiscordConfig.DEATH_EMBED_COLOR);
      case TEXT -> sendMessage(targetChannel, message);
      case WEBHOOK -> sendWebhookMessage(targetChannel, uuid, username, server, message);
    }
  }

  public void onPlayerAdvancement(
    String username,
    String uuid,
    String server,
    String displayname,
    String title,
    String description
  ) {
    var serverDiscordConfig = VelocityDiscord.CONFIG.getServerConfig(server).getDiscordMessageConfig();

    if (serverDiscordConfig.ADVANCEMENT_FORMAT.isEmpty()) return;

    var message = new StringTemplate(serverDiscordConfig.ADVANCEMENT_FORMAT.get())
      .add("username", username)
      .add("displayname", displayname)
      .add("advancement_title", title)
      .add("advancement_description", description)
      .toString();

    var targetChannel = getServerChannels(server).advancementChannel;
    switch (serverDiscordConfig.ADVANCEMENT_TYPE) {
      case EMBED -> sendEmbedMessage(targetChannel, message, serverDiscordConfig.ADVANCEMENT_EMBED_COLOR);
      case TEXT -> sendMessage(targetChannel, message);
      case WEBHOOK -> sendWebhookMessage(targetChannel, uuid, username, server, message);
    }
  }

  public void onProxyInitialize() {
    if (VelocityDiscord.CONFIG.discord.PROXY_START_FORMAT.isPresent()) {
      var message = VelocityDiscord.CONFIG.discord.PROXY_START_FORMAT.get();

      switch (VelocityDiscord.CONFIG.discord.PROXY_START_TYPE) {
        case EMBED ->
          sendEmbedMessage(this.proxyStartChannel, message, VelocityDiscord.CONFIG.discord.PROXY_START_EMBED_COLOR);
        case TEXT -> sendMessage(this.proxyStartChannel, message);
      }
    }
  }

  public void onProxyShutdown() {
    if (VelocityDiscord.CONFIG.discord.PROXY_STOP_FORMAT.isPresent()) {
      var message = VelocityDiscord.CONFIG.discord.PROXY_STOP_FORMAT.get();

      switch (VelocityDiscord.CONFIG.discord.PROXY_STOP_TYPE) {
        case EMBED ->
          sendEmbedMessage(this.proxyStopChannel, message, VelocityDiscord.CONFIG.discord.PROXY_STOP_EMBED_COLOR);
        case TEXT -> sendMessage(this.proxyStopChannel, message);
      }
    }
  }

  public void onServerStart(String server) {
    var serverDiscordConfig = VelocityDiscord.CONFIG.getServerConfig(server).getDiscordMessageConfig();

    if (serverDiscordConfig.SERVER_START_FORMAT.isPresent()) {
      var message = new StringTemplate(serverDiscordConfig.SERVER_START_FORMAT.get())
        .add("server", VelocityDiscord.CONFIG.serverName(server))
        .toString();

      var targetChannel = this.serverChannels.get(server).serverStartChannel;
      switch (serverDiscordConfig.SERVER_START_TYPE) {
        case EMBED -> sendEmbedMessage(targetChannel, message, serverDiscordConfig.SERVER_START_EMBED_COLOR);
        case TEXT -> sendMessage(targetChannel, message);
      }
    }
  }

  public void onServerStop(String server) {
    var serverDiscordConfig = VelocityDiscord.CONFIG.getServerConfig(server).getDiscordMessageConfig();

    if (serverDiscordConfig.SERVER_STOP_FORMAT.isPresent()) {
      var message = new StringTemplate(serverDiscordConfig.SERVER_STOP_FORMAT.get())
        .add("server", VelocityDiscord.CONFIG.serverName(server))
        .toString();

      var targetChannel = this.serverChannels.get(server).serverStopChannel;
      switch (serverDiscordConfig.SERVER_STOP_TYPE) {
        case EMBED -> sendEmbedMessage(targetChannel, message, serverDiscordConfig.SERVER_STOP_EMBED_COLOR);
        case TEXT -> sendMessage(targetChannel, message);
      }
    }
  }

  public void updateActivityPlayerAmount(int count) {
    if (!VelocityDiscord.CONFIG.bot.SHOW_ACTIVITY || !this.ready) {
      return;
    }

    if (this.lastPlayerCount != count) {
      var message = new StringTemplate(VelocityDiscord.CONFIG.bot.ACTIVITY_FORMAT).add("amount", count).toString();

      this.jda.getPresence().setActivity(Activity.playing(message));

      this.lastPlayerCount = count;
    }
  }

  // todo: per server channel overrides for topic
  public String generateChannelTopic() {
    if (VelocityDiscord.CONFIG.discord.TOPIC_FORMAT.isEmpty()) return null;

    // Collect additional information
    var playerCount = VelocityDiscord.SERVER.getPlayerCount();

    var playerList = "";

    // only generate player list if it's in the TOPIC_FORMAT
    if (VelocityDiscord.CONFIG.discord.TOPIC_FORMAT.get().contains("{player_list}")) {
      var players = VelocityDiscord.SERVER
        .getAllPlayers()
        .stream()
        .limit(VelocityDiscord.CONFIG.discord.TOPIC_PLAYER_LIST_MAX_COUNT)
        .map(player -> new StringTemplate(VelocityDiscord.CONFIG.discord.TOPIC_PLAYER_LIST_FORMAT)
          .add("username", player.getUsername())
          .add("ping", player.getPing())
          .toString())
        .reduce("", (a, b) -> a + VelocityDiscord.CONFIG.discord.TOPIC_PLAYER_LIST_SEPARATOR + b);

      if (!players.isEmpty()) {
        // Remove leading separator and add header if configured
        players = players.substring(VelocityDiscord.CONFIG.discord.TOPIC_PLAYER_LIST_SEPARATOR.length());
        if (VelocityDiscord.CONFIG.discord.TOPIC_PLAYER_LIST_HEADER.isPresent()) {
          playerList = VelocityDiscord.CONFIG.discord.TOPIC_PLAYER_LIST_HEADER.get() + players;
        } else {
          playerList = players;
        }
      } else {
        playerList = VelocityDiscord.CONFIG.discord.TOPIC_PLAYER_LIST_NO_PLAYERS_HEADER.orElse("");
      }
    }

    var s = VelocityDiscord.SERVER;
    var serverCount = s.getAllServers().size();
    var serverList =
      s.getAllServers().stream().map(registeredServer -> registeredServer.getServerInfo().getName()).toList();
    var hostname = s.getBoundAddress().getHostName();
    var port = String.valueOf(s.getBoundAddress().getPort());
    var queryMotd = PlainTextComponentSerializer.plainText().serialize(s.getConfiguration().getMotd());
    var queryPort = s.getConfiguration().getQueryPort();
    var queryMaxPlayers = s.getConfiguration().getShowMaxPlayers();
    var pluginCount = s.getPluginManager().getPlugins().size();
    var pluginList = s
      .getPluginManager()
      .getPlugins()
      .stream()
      .map(plugin -> plugin.getDescription().getName())
      .flatMap(Optional::stream)
      .toList();
    var proxyVersion = s.getVersion().getVersion();
    var proxySoftware = s.getVersion().getName();

    // Calculate average ping
    var averagePing = s.getAllPlayers().stream().mapToLong(Player::getPing).average().orElse(0.0);

    // Get server uptime
    var uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
    var formattedUptime = formatUptime(uptimeMillis);

    // Ping each server and get status
    var serverStatuses = new HashMap<String, String>();
    for (var registeredServer : s.getAllServers()) {
      var name = registeredServer.getServerInfo().getName();

      if (VelocityDiscord.CONFIG.serverDisabled(name)) {
        continue;
      }

      var serverDiscordConfig = VelocityDiscord.CONFIG.getServerConfig(name).getDiscordMessageConfig();

      if (serverDiscordConfig.TOPIC_SERVER_FORMAT.isEmpty()) {
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

          var serverStatus = new StringTemplate(serverDiscordConfig.TOPIC_SERVER_FORMAT.get())
            .add("name", VelocityDiscord.CONFIG.serverName(name))
            .add("players", online)
            .add("max_players", max)
            .add("version", ver)
            .add("protocol", protocol)
            .add("motd", motd)
            .toString();

          serverStatuses.put(name, serverStatus);
        }).get(5, TimeUnit.SECONDS);
      } catch (Exception e) {
        if (serverDiscordConfig.TOPIC_SERVER_OFFLINE_FORMAT.isEmpty()) {
          serverStatuses.put(name, "");
          continue;
        }

        var serverStatus = new StringTemplate(serverDiscordConfig.TOPIC_SERVER_OFFLINE_FORMAT.get())
          .add("name", VelocityDiscord.CONFIG.serverName(name))
          .toString();

        serverStatuses.put(name, serverStatus);
      }
    }

    // todo: per server channel overrides for topic
    var serverDiscordConfig = VelocityDiscord.CONFIG.discord;

    // Build the message
    var template = new StringTemplate(serverDiscordConfig.TOPIC_FORMAT.get())
      .add("players", playerCount)
      .add("player_list", playerList)
      .add("servers", serverCount)
      .add("server_list", String.join(", ", serverList.stream().map(VelocityDiscord.CONFIG::serverName).toList()))
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
    if (!this.ready) return;

    var topic = generateChannelTopic();

    if (topic == null) return;

    this.mainChannel.getManager().setTopic(topic).queue();
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

  private void sendMessage(TextChannel targetChannel, @Nonnull String message) {
    if (this.ready) {
      targetChannel.sendMessage(message).queue();
    } else {
      this.preReadyQueue.add(new QueuedStringMessage(targetChannel, message));
    }
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private void sendEmbedMessage(TextChannel channel, String message, Optional<Color> color) {
    var embed = new EmbedBuilder().setDescription(message);

    color.ifPresent(embed::setColor);

    if (this.ready) {
      channel.sendMessageEmbeds(embed.build()).queue();
    } else {
      this.preReadyQueue.add(new QueuedEmbedMessage(channel, embed));
    }
  }

  // todo: send webhooks to specific channels
  private void sendWebhookMessage(TextChannel channel, String uuid, String username, String server, String content) {
    if (this.webhookClient == null) {
      VelocityDiscord.LOGGER.debug("Webhook client was not created due to configuration error, skipping sending "
        + "message");
      return;
    }

    var avatar = new StringTemplate(VelocityDiscord.CONFIG.bot.WEBHOOK_AVATAR_URL)
      .add("username", username)
      .add("uuid", uuid)
      .toString();

    var discordName = new StringTemplate(VelocityDiscord.CONFIG.bot.WEBHOOK_USERNAME)
      .add("username", username)
      .add("server", VelocityDiscord.CONFIG.serverName(server))
      .toString();

    var webhookMessage = new MessageCreateBuilder().setContent(content).build();

    if (this.ready) {
      this.webhookClient.sendMessage(webhookMessage).setAvatarUrl(avatar).setUsername(discordName).queue();
    } else {
      this.preReadyQueue.add(new QueuedWebhookMessage(webhookMessage, avatar, discordName));
    }
  }

  // endregion

  private String parseMentions(String server, String message) {
    var channels = this.serverChannels.get(server);
    if (channels == null || channels.chatChannel == null || !this.ready) {
      return message;
    }

    var msg = message;

    for (var member : channels.chatChannel.getMembers()) {
      msg = Pattern
        .compile(Pattern.quote("@" + member.getUser().getName()), Pattern.CASE_INSENSITIVE)
        .matcher(msg)
        .replaceAll(member.getAsMention());
    }

    return msg;
  }

  private String filterEveryoneAndHere(String message) {
    return EveryoneAndHerePattern.matcher(message).replaceAll("@\u200B${ping}");
  }

  private String filterRawPings(String message) {
    return RawPingPattern.matcher(message).replaceAll("<@\u200B${ping}>");
  }

  private TextChannel loadChannel(String id) {
    var channel = this.jda.getTextChannelById(id);

    if (channel == null) {
      VelocityDiscord.LOGGER.error("Could not load channel with id: {}", id);
      return null;
    }

    if (!channel.canTalk()) {
      VelocityDiscord.LOGGER.error("Cannot talk in configured channel");
      return null;
    }

    return channel;
  }

  private Channels getServerChannels(String server) {
    return this.serverChannels.getOrDefault(server, this.defaultChannels);
  }

  private record QueuedWebhookMessage(MessageCreateData message, String avatar, String username)
    implements IQueuedMessage {
    @Override
    public void send(Discord discord) {
      discord.webhookClient.sendMessage(this.message).setAvatarUrl(this.avatar).setUsername(this.username).queue();
    }
  }

  private record QueuedEmbedMessage(TextChannel channel, EmbedBuilder embed) implements IQueuedMessage {
    @Override
    public void send(Discord discord) {
      this.channel.sendMessageEmbeds(this.embed.build()).queue();
    }
  }

  private record QueuedStringMessage(TextChannel channel, String message) implements IQueuedMessage {
    @Override
    public void send(Discord discord) {
      this.channel.sendMessage(this.message).queue();
    }
  }

  private record QueuedChatCompletion(String server, Player player) implements IQueuedMessage {
    @Override
    public void send(Discord discord) {
      this.player.addCustomChatCompletions(discord.mentionCompletions.get(this.server));
    }
  }

  public static class Channels {
    public String serverName;

    public TextChannel chatChannel;
    public TextChannel deathChannel;
    public TextChannel advancementChannel;
    public TextChannel joinChannel;
    public TextChannel leaveChannel;
    public TextChannel disconnectChannel;
    public TextChannel serverSwitchChannel;
    public TextChannel serverStartChannel;
    public TextChannel serverStopChannel;

    public Channels(Discord discord, String serverName, ServerConfig config, TextChannel defaultChannel) {
      this.serverName = serverName;

      var msgCfg = config.getDiscordMessageConfig();

      this.chatChannel = getChannel(discord, msgCfg.MESSAGE_CHANNEL, defaultChannel);
      this.deathChannel = getChannel(discord, msgCfg.DEATH_CHANNEL, defaultChannel);
      this.advancementChannel = getChannel(discord, msgCfg.ADVANCEMENT_CHANNEL, defaultChannel);
      this.joinChannel = getChannel(discord, msgCfg.JOIN_CHANNEL, defaultChannel);
      this.leaveChannel = getChannel(discord, msgCfg.LEAVE_CHANNEL, defaultChannel);
      this.disconnectChannel = getChannel(discord, msgCfg.DISCONNECT_CHANNEL, defaultChannel);
      this.serverSwitchChannel = getChannel(discord, msgCfg.SERVER_SWITCH_CHANNEL, defaultChannel);
      this.serverStartChannel = getChannel(discord, msgCfg.SERVER_START_CHANNEL, defaultChannel);
      this.serverStopChannel = getChannel(discord, msgCfg.SERVER_STOP_CHANNEL, defaultChannel);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static TextChannel getChannel(Discord discord, Optional<String> id, TextChannel defaultChannel) {
      if (id.isEmpty()) {
        return defaultChannel;
      }

      return discord.loadChannel(id.get());
    }
  }
}
