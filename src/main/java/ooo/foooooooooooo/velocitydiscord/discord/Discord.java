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
import ooo.foooooooooooo.velocitydiscord.config.definitions.WebhookConfig;
import ooo.foooooooooooo.velocitydiscord.discord.commands.ICommand;
import ooo.foooooooooooo.velocitydiscord.discord.commands.ListCommand;
import ooo.foooooooooooo.velocitydiscord.discord.message.IQueuedMessage;
import ooo.foooooooooooo.velocitydiscord.util.StringTemplate;

import javax.annotation.Nonnull;
import java.awt.*;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Discord extends ListenerAdapter {
  private static final Pattern EveryoneAndHerePattern = Pattern.compile("@(?<ping>everyone|here)");
  private static final Pattern RawPingPattern = Pattern.compile("<@(?<ping>[!&]?\\d+)>");

  private final MessageListener messageListener;

  private final Map<String, ICommand> commands = new HashMap<>();
  private final HashMap<String, List<String>> mentionCompletions = new HashMap<>();
  private final HashMap<String, Channels> serverChannels = new HashMap<>();

  private final Queue<IQueuedMessage> preReadyQueue = new ArrayDeque<>();

  private boolean ready = false;

  private JDA jda;

  private String lastToken;

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
    if (VelocityDiscord.CONFIG.global.discord.commands.list.enabled) {
      this.commands.put(ListCommand.COMMAND_NAME, new ListCommand());
    }

    if (!VelocityDiscord.CONFIG.global.discord.token.equals(this.lastToken)) {
      if (this.jda != null) {
        shutdown();
      }

      var builder = JDABuilder.createDefault(VelocityDiscord.CONFIG.global.discord.token)
        // this seems to download all users at bot startup and keep internal cache updated
        // without it, sometimes mentions miss when they shouldn't
        .setChunkingFilter(ChunkingFilter.ALL)
        .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
        // mentions always miss without this
        .setMemberCachePolicy(MemberCachePolicy.ALL)
        .addEventListeners(this.messageListener, this);

      try {
        this.jda = builder.build();
        this.lastToken = VelocityDiscord.CONFIG.global.discord.token;
      } catch (Exception e) {
        VelocityDiscord.LOGGER.error("Failed to login to discord:", e);
      }
    } else {
      // no ready event, just reload channels here
      loadChannels();
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
    var defaultChannelId = VelocityDiscord.CONFIG.local.discord.mainChannelId;
    this.mainChannel = loadChannel(defaultChannelId);
    this.proxyStartChannel = loadChannel(VelocityDiscord.CONFIG.global.discord.chat.proxyStart.channel.orElse(
      defaultChannelId));
    this.proxyStopChannel = loadChannel(VelocityDiscord.CONFIG.global.discord.chat.proxyStop.channel.orElse(
      defaultChannelId));

    this.serverChannels.clear();
    for (var server : VelocityDiscord.SERVER.getAllServers()) {
      var serverName = server.getServerInfo().getName();
      var config = VelocityDiscord.CONFIG.getServerConfig(serverName);
      var defaultChannel = this.jda.getTextChannelById(config.getDiscordConfig().mainChannelId);
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
    var serverBotConfig = serverConfig.getDiscordConfig();
    var serverDiscordConfig = serverConfig.getChatConfig();

    content = filterRawPings(content);

    if (serverBotConfig.enableMentions) {
      content = parseMentions(server, content);
    }

    if (!serverBotConfig.enableEveryoneAndHere) {
      content = filterEveryoneAndHere(content);
    }

    if (serverDiscordConfig.message.format.isPresent()) {
      var message = new StringTemplate(serverDiscordConfig.message.format.get()).add("username", username)
        .add("server", VelocityDiscord.CONFIG.serverName(server))
        .add("message", content)
        .add("prefix", prefix.orElse(""))
        .toString();

      var targetChannel = getServerChannels(server).chatChannel;
      switch (serverDiscordConfig.message.type) {
        case EMBED -> sendEmbedMessage(targetChannel, message, serverDiscordConfig.message.embedColor);
        case TEXT -> sendMessage(targetChannel, message);
        case WEBHOOK -> sendWebhookMessage(uuid, username, server, content, MessageCategory.MESSAGE);
        default -> throw new IllegalArgumentException("Unexpected value: " + serverDiscordConfig.message.type);
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

    var serverDiscordConfig = VelocityDiscord.CONFIG.getServerConfig(server).getChatConfig();

    if (serverDiscordConfig.join.format.isEmpty()) {
      return;
    }

    var message = new StringTemplate(serverDiscordConfig.join.format.get()).add("username", player.getUsername())
      .add("server", VelocityDiscord.CONFIG.serverName(server))
      .add("prefix", prefix.orElse(""))
      .toString();

    var targetChannel = getServerChannels(server).joinChannel;
    switch (serverDiscordConfig.join.type) {
      case EMBED -> sendEmbedMessage(targetChannel, message, serverDiscordConfig.join.embedColor);
      case TEXT -> sendMessage(targetChannel, message);
      case WEBHOOK -> sendWebhookMessage(
        player.getUniqueId().toString(),
        player.getUsername(),
        server,
        message,
        MessageCategory.JOIN
      );
    }
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public void onServerSwitch(String username, String uuid, Optional<String> prefix, String current, String previous) {
    var serverDiscordConfig = VelocityDiscord.CONFIG.getServerConfig(current).getChatConfig();

    if (serverDiscordConfig.serverSwitch.format.isEmpty()) {
      return;
    }

    var message = new StringTemplate(serverDiscordConfig.serverSwitch.format.get()).add("username", username)
      .add("current", VelocityDiscord.CONFIG.serverName(current))
      .add("previous", VelocityDiscord.CONFIG.serverName(previous))
      .add("prefix", prefix.orElse(""))
      .toString();

    // todo: send to current or previous server or both
    var targetChannel = getServerChannels(current).serverSwitchChannel;
    switch (serverDiscordConfig.serverSwitch.type) {
      case EMBED -> sendEmbedMessage(targetChannel, message, serverDiscordConfig.serverSwitch.embedColor);
      case TEXT -> sendMessage(targetChannel, message);
      case WEBHOOK -> sendWebhookMessage(uuid, username, current, message, MessageCategory.SERVER_SWITCH);
    }
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public void onDisconnect(String username, String uuid, Optional<String> prefix, String server) {
    var serverDiscordConfig = VelocityDiscord.CONFIG.getServerConfig(server).getChatConfig();

    if (serverDiscordConfig.disconnect.format.isEmpty()) {
      return;
    }

    var message = new StringTemplate(serverDiscordConfig.disconnect.format.get()).add("username", username)
      .add("prefix", prefix.orElse(""))
      .toString();

    var targetChannel = getServerChannels(server).disconnectChannel;
    switch (serverDiscordConfig.disconnect.type) {
      case EMBED -> sendEmbedMessage(targetChannel, message, serverDiscordConfig.disconnect.embedColor);
      case TEXT -> sendMessage(targetChannel, message);
      case WEBHOOK -> sendWebhookMessage(uuid, username, server, message, MessageCategory.DISCONNECT);
    }
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public void onLeave(String username, String uuid, Optional<String> prefix, String server) {
    var serverDiscordConfig = VelocityDiscord.CONFIG.getServerConfig(server).getChatConfig();

    if (serverDiscordConfig.leave.format.isEmpty()) {
      return;
    }

    var message = new StringTemplate(serverDiscordConfig.leave.format.get()).add("username", username)
      .add("server", VelocityDiscord.CONFIG.serverName(server))
      .add("prefix", prefix.orElse(""))
      .toString();

    var targetChannel = getServerChannels(server).leaveChannel;
    switch (serverDiscordConfig.leave.type) {
      case EMBED -> sendEmbedMessage(targetChannel, message, serverDiscordConfig.leave.embedColor);
      case TEXT -> sendMessage(targetChannel, message);
      case WEBHOOK -> sendWebhookMessage(uuid, username, server, message, MessageCategory.LEAVE);
    }
  }

  public void onPlayerDeath(String username, String uuid, String server, String displayName, String death) {
    var serverDiscordConfig = VelocityDiscord.CONFIG.getServerConfig(server).getChatConfig();

    if (serverDiscordConfig.death.format.isEmpty()) return;

    var message = new StringTemplate(serverDiscordConfig.death.format.get()).add("username", username)
      .add("displayname", displayName)
      .add("death_message", death)
      .toString();

    var targetChannel = getServerChannels(server).deathChannel;
    switch (serverDiscordConfig.death.type) {
      case EMBED -> sendEmbedMessage(targetChannel, message, serverDiscordConfig.death.embedColor);
      case TEXT -> sendMessage(targetChannel, message);
      case WEBHOOK -> sendWebhookMessage(uuid, username, server, message, MessageCategory.DEATH);
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
    var serverDiscordConfig = VelocityDiscord.CONFIG.getServerConfig(server).getChatConfig();

    if (serverDiscordConfig.advancement.format.isEmpty()) return;

    var message = new StringTemplate(serverDiscordConfig.advancement.format.get()).add("username", username)
      .add("displayname", displayname)
      .add("advancement_title", title)
      .add("advancement_description", description)
      .toString();

    var targetChannel = getServerChannels(server).advancementChannel;
    switch (serverDiscordConfig.advancement.type) {
      case EMBED -> sendEmbedMessage(targetChannel, message, serverDiscordConfig.advancement.embedColor);
      case TEXT -> sendMessage(targetChannel, message);
      case WEBHOOK -> sendWebhookMessage(uuid, username, server, message, MessageCategory.ADVANCEMENT);
    }
  }

  public void onProxyInitialize() {
    if (VelocityDiscord.CONFIG.global.discord.chat.proxyStart.format.isPresent()) {
      var message = VelocityDiscord.CONFIG.global.discord.chat.proxyStart.format.get();

      switch (VelocityDiscord.CONFIG.global.discord.chat.proxyStart.type) {
        case EMBED -> sendEmbedMessage(
          this.proxyStartChannel,
          message,
          VelocityDiscord.CONFIG.global.discord.chat.proxyStart.embedColor
        );
        case TEXT -> sendMessage(this.proxyStartChannel, message);
      }
    }
  }

  public void onProxyShutdown() {
    if (VelocityDiscord.CONFIG.global.discord.chat.proxyStop.format.isPresent()) {
      var message = VelocityDiscord.CONFIG.global.discord.chat.proxyStop.format.get();

      switch (VelocityDiscord.CONFIG.global.discord.chat.proxyStop.type) {
        case EMBED -> sendEmbedMessage(
          this.proxyStopChannel,
          message,
          VelocityDiscord.CONFIG.global.discord.chat.proxyStop.embedColor
        );
        case TEXT -> sendMessage(this.proxyStopChannel, message);
      }
    }
  }

  public void onServerStart(String server) {
    var serverDiscordConfig = VelocityDiscord.CONFIG.getServerConfig(server).getChatConfig();

    if (serverDiscordConfig.serverStart.format.isPresent()) {
      var message = new StringTemplate(serverDiscordConfig.serverStart.format.get()).add(
        "server",
        VelocityDiscord.CONFIG.serverName(server)
      ).toString();

      var targetChannel = this.serverChannels.get(server).serverStartChannel;
      switch (serverDiscordConfig.serverStart.type) {
        case EMBED -> sendEmbedMessage(targetChannel, message, serverDiscordConfig.serverStart.embedColor);
        case TEXT -> sendMessage(targetChannel, message);
      }
    }
  }

  public void onServerStop(String server) {
    var serverDiscordConfig = VelocityDiscord.CONFIG.getServerConfig(server).getChatConfig();

    if (serverDiscordConfig.serverStart.format.isPresent()) {
      var message = new StringTemplate(serverDiscordConfig.serverStart.format.get()).add(
        "server",
        VelocityDiscord.CONFIG.serverName(server)
      ).toString();

      var targetChannel = this.serverChannels.get(server).serverStopChannel;
      switch (serverDiscordConfig.serverStart.type) {
        case EMBED -> sendEmbedMessage(targetChannel, message, serverDiscordConfig.serverStart.embedColor);
        case TEXT -> sendMessage(targetChannel, message);
      }
    }
  }

  public void updateActivityPlayerAmount(int count) {
    if (VelocityDiscord.CONFIG.global.discord.activityText.isEmpty() || !this.ready) {
      return;
    }

    if (this.lastPlayerCount != count) {
      var message = new StringTemplate(VelocityDiscord.CONFIG.global.discord.activityText.get()).add("amount", count)
        .toString();

      this.jda.getPresence().setActivity(Activity.playing(message));

      this.lastPlayerCount = count;
    }
  }

  // todo: per server channel overrides for topic
  public String generateChannelTopic() {
    var config = VelocityDiscord.CONFIG.local.discord;
    if (config.channelTopic.format.isEmpty()) return null;

    // Collect additional information
    var playerCount = VelocityDiscord.SERVER.getPlayerCount();

    var playerList = "";

    // only generate player list if it's in the TOPIC_FORMAT
    if (config.channelTopic.format.get().contains("{player_list}")) {
      var players = VelocityDiscord.SERVER.getAllPlayers()
        .stream()
        .limit(config.channelTopic.playerListMaxCount)
        .map(player -> new StringTemplate(config.channelTopic.playerListPlayerFormat).add(
          "username",
          player.getUsername()
        ).add("ping", player.getPing()).toString())
        .reduce("", (a, b) -> a + config.channelTopic.playerListSeparator + b);

      if (!players.isEmpty()) {
        // Remove leading separator and add header if configured
        players = players.substring(config.channelTopic.playerListSeparator.length());
        if (config.channelTopic.playerListHeader.isPresent()) {
          playerList = config.channelTopic.playerListHeader.get() + players;
        } else {
          playerList = players;
        }
      } else {
        playerList = config.channelTopic.playerListNoPlayersHeader.orElse("");
      }
    }

    var s = VelocityDiscord.SERVER;
    var serverCount = s.getAllServers().size();
    var serverList = s.getAllServers()
      .stream()
      .map(registeredServer -> registeredServer.getServerInfo().getName())
      .toList();
    var hostname = s.getBoundAddress().getHostName();
    var port = String.valueOf(s.getBoundAddress().getPort());
    var queryMotd = PlainTextComponentSerializer.plainText().serialize(s.getConfiguration().getMotd());
    var queryPort = s.getConfiguration().getQueryPort();
    var queryMaxPlayers = s.getConfiguration().getShowMaxPlayers();
    var pluginCount = s.getPluginManager().getPlugins().size();
    var pluginList = s.getPluginManager()
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

      var serverDiscordConfig = VelocityDiscord.CONFIG.getServerConfig(name).getDiscordConfig();

      if (serverDiscordConfig.channelTopic.serverFormat.isEmpty()) {
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

          var serverStatus = new StringTemplate(serverDiscordConfig.channelTopic.serverFormat.get()).add(
              "name",
              VelocityDiscord.CONFIG.serverName(name)
            )
            .add("players", online)
            .add("max_players", max)
            .add("version", ver)
            .add("protocol", protocol)
            .add("motd", motd)
            .toString();

          serverStatuses.put(name, serverStatus);
        }).get(5, TimeUnit.SECONDS);
      } catch (Exception e) {
        if (serverDiscordConfig.channelTopic.serverOfflineFormat.isEmpty()) {
          serverStatuses.put(name, "");
          continue;
        }

        var serverStatus = new StringTemplate(serverDiscordConfig.channelTopic.serverOfflineFormat.get()).add(
          "name",
          VelocityDiscord.CONFIG.serverName(name)
        ).toString();

        serverStatuses.put(name, serverStatus);
      }
    }

    // todo: per server channel overrides for topic
    var serverDiscordConfig = VelocityDiscord.CONFIG.local.discord;

    if (serverDiscordConfig.channelTopic.format.isEmpty()) return "";

    // Build the message
    var template = new StringTemplate(serverDiscordConfig.channelTopic.format.get()).add("players", playerCount)
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

  private void sendWebhookMessage(String uuid, String username, String server, String content, MessageCategory type) {
    var serverConfig = VelocityDiscord.CONFIG.getServerConfig(server);
    var webhookConfig = serverConfig.getDiscordConfig().getWebhookConfig(type);

    var avatar = new StringTemplate(webhookConfig.avatarUrl).add("username", username).add("uuid", uuid).toString();

    var discordName = new StringTemplate(webhookConfig.username).add("username", username)
      .add("server", VelocityDiscord.CONFIG.serverName(server))
      .toString();

    var webhookMessage = new MessageCreateBuilder().setContent(content).build();

    if (this.ready) {
      var channels = this.serverChannels.get(server);
      if (channels == null) {
        VelocityDiscord.LOGGER.error("Failed to get webhook client for server `{}`: serverChannels is null", server);
        return;
      }

      var client = channels.getClientForCategory(type);

      if (client == null) {
        VelocityDiscord.LOGGER.error("Failed to get webhook client for server `{}`: client is null", server);
        return;
      }

      VelocityDiscord.LOGGER.info(
        "Sending webhook message to `{}`: avatar={}, username={}",
        server,
        avatar,
        discordName
      );
      client.sendMessage(webhookMessage).setAvatarUrl(avatar).setUsername(discordName).queue();
    } else {
      this.preReadyQueue.add(new QueuedWebhookMessage(server, type, webhookMessage, avatar, discordName));
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
      msg = Pattern.compile(Pattern.quote("@" + member.getUser().getName()), Pattern.CASE_INSENSITIVE)
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

  private record QueuedWebhookMessage(String server, MessageCategory type, MessageCreateData message, String avatar,
                                      String username) implements IQueuedMessage {

    @Override
    public void send(Discord discord) {
      var channels = discord.serverChannels.get(this.server);
      if (channels == null) {
        VelocityDiscord.LOGGER.error(
          "[Queued] Failed to get webhook client for server `{}`: serverChannels is null",
          this.server
        );
        return;
      }

      var client = channels.getClientForCategory(this.type);

      if (client != null) {
        client.sendMessage(this.message).setAvatarUrl(this.avatar).setUsername(this.username).queue();
      } else {
        VelocityDiscord.LOGGER.error(
          "[Queued] Failed to get webhook client for server `{}`: client is null",
          this.server
        );
      }
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

    private IncomingWebhookClient mainWebhook;

    public TextChannel chatChannel;
    public IncomingWebhookClient chatWebhook;
    public TextChannel deathChannel;
    public IncomingWebhookClient deathWebhook;
    public TextChannel advancementChannel;
    public IncomingWebhookClient advancementWebhook;
    public TextChannel joinChannel;
    public IncomingWebhookClient joinWebhook;
    public TextChannel leaveChannel;
    public IncomingWebhookClient leaveWebhook;
    public TextChannel disconnectChannel;
    public IncomingWebhookClient disconnectWebhook;
    public TextChannel serverSwitchChannel;
    public IncomingWebhookClient serverSwitchWebhook;
    public TextChannel serverStartChannel;
    public TextChannel serverStopChannel;

    public Channels(Discord discord, String serverName, ServerConfig config, TextChannel defaultChannel) {
      this.serverName = serverName;

      var chat = config.getDiscordConfig().chat;

      this.chatChannel = getChannel(discord, chat.message.channelId, defaultChannel);
      this.deathChannel = getChannel(discord, chat.death.channelId, defaultChannel);
      this.advancementChannel = getChannel(discord, chat.advancement.channelId, defaultChannel);
      this.joinChannel = getChannel(discord, chat.join.channelId, defaultChannel);
      this.leaveChannel = getChannel(discord, chat.leave.channelId, defaultChannel);
      this.disconnectChannel = getChannel(discord, chat.disconnect.channelId, defaultChannel);
      this.serverSwitchChannel = getChannel(discord, chat.serverSwitch.channelId, defaultChannel);
      this.serverStartChannel = getChannel(discord, chat.serverStart.channel, defaultChannel);
      this.serverStopChannel = getChannel(discord, chat.serverStop.channel, defaultChannel);

      this.mainWebhook = null;

      if (config.getDiscordConfig().isWebhookUsed()) {
        this.chatWebhook = getClient(discord, chat.message.webhook);
        this.deathWebhook = getClient(discord, chat.death.webhook);
        this.advancementWebhook = getClient(discord, chat.advancement.webhook);
        this.joinWebhook = getClient(discord, chat.join.webhook);
        this.leaveWebhook = getClient(discord, chat.leave.webhook);
        this.disconnectWebhook = getClient(discord, chat.disconnect.webhook);
        this.serverSwitchWebhook = getClient(discord, chat.serverSwitch.webhook);
      }
    }

    public IncomingWebhookClient getClientForCategory(MessageCategory category) {
      return switch (category) {
        case MESSAGE -> this.chatWebhook;
        case DEATH -> this.deathWebhook;
        case ADVANCEMENT -> this.advancementWebhook;
        case JOIN -> this.joinWebhook;
        case LEAVE -> this.leaveWebhook;
        case DISCONNECT -> this.disconnectWebhook;
        case SERVER_SWITCH -> this.serverSwitchWebhook;
      };
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static TextChannel getChannel(Discord discord, Optional<String> id, TextChannel defaultChannel) {
      if (id.isEmpty()) {
        return defaultChannel;
      }

      return discord.loadChannel(id.get());
    }

    private IncomingWebhookClient createClient(Discord discord, WebhookConfig config) {
      try {
        return WebhookClient.createClient(discord.jda, config.url);
      } catch (Exception e) {
        VelocityDiscord.LOGGER.error("Failed to create webhook client for server {}", this.serverName, e);
      }
        return null;
    }

    private IncomingWebhookClient getMainWebhook(Discord discord) {
      if (this.mainWebhook == null) {
        var config = VelocityDiscord.CONFIG.getServerConfig(this.serverName).getDiscordConfig().webhook;
        if (config.isInvalid()) {
          this.mainWebhook = createClient(discord, config);
        }
      }

      return this.mainWebhook;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private IncomingWebhookClient getClient(Discord discord, Optional<WebhookConfig> config) {
      if (config.isEmpty() || config.get().isInvalid()) {
        return getMainWebhook(discord);
      }

      return createClient(discord, config.get());
    }
  }
}
