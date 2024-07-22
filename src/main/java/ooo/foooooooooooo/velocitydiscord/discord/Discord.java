package ooo.foooooooooooo.velocitydiscord.discord;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.TaskStatus;
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
import ooo.foooooooooooo.velocitydiscord.VelocityDiscord;
import ooo.foooooooooooo.velocitydiscord.config.Config;
import ooo.foooooooooooo.velocitydiscord.discord.commands.ICommand;
import ooo.foooooooooooo.velocitydiscord.discord.commands.ListCommand;
import ooo.foooooooooooo.velocitydiscord.util.StringTemplate;

import javax.annotation.Nonnull;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Discord extends ListenerAdapter {
  private static final Pattern EveryoneAndHerePattern = Pattern.compile("@(?<ping>everyone|here)");

  private final ProxyServer server;
  private final Logger logger;
  private final Config config;
  private final VelocityDiscord plugin;
  private final JDA jda;
  private final IncomingWebhookClient webhookClient;
  private final Map<String, ICommand> commands = new HashMap<>();

  private TextChannel activeChannel;
  private int lastPlayerCount = -1;
  private ScheduledTask updateTask;

  public Discord(ProxyServer server, Logger logger, Config config, VelocityDiscord plugin) {
    this.server = server;
    this.logger = logger;
    this.config = config;
    this.plugin = plugin;

    commands.put("list", new ListCommand(server, logger, config));

    var messageListener = new MessageListener(server, logger, config);

    var builder = JDABuilder.createDefault(config.bot.DISCORD_TOKEN)
      // this seems to download all users at bot startup and keep internal cache updated
      // without it, sometimes mentions miss when they shouldn't
      .setChunkingFilter(ChunkingFilter.ALL).enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
      // mentions always miss without this
      .setMemberCachePolicy(MemberCachePolicy.ALL).addEventListeners(messageListener, this);

    try {
      jda = builder.build();
    } catch (Exception e) {
      this.logger.severe("Failed to login to discord: " + e);
      throw new RuntimeException("Failed to login to discord: ", e);
    }

    webhookClient = config.bot.USE_WEBHOOKS ? WebhookClient.createClient(jda, config.bot.WEBHOOK_URL) : null;
  }

  @Override
  public void onReady(@Nonnull ReadyEvent event) {
    logger.info(MessageFormat.format("Bot ready, Guilds: {0} ({1} available)", event.getGuildTotalCount(), event.getGuildAvailableCount()));

    var channel = jda.getTextChannelById(Objects.requireNonNull(config.bot.CHANNEL_ID));

    if (channel == null) {
      logger.severe("Could not load channel with id: " + config.bot.CHANNEL_ID);
      throw new RuntimeException("Could not load channel id: " + config.bot.CHANNEL_ID);
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

    
    if (config.bot.UPDATE_CHANNEL_TOPIC_INTERVAL >= 10) {
          // Schedule the task to update the channel topic at the specified interval
          this.updateTask = server.getScheduler().buildTask(plugin, this::updateChannelTopic)
              .repeat(config.bot.UPDATE_CHANNEL_TOPIC_INTERVAL, TimeUnit.MINUTES)
              .schedule();
          logger.info("Scheduled task to update channel topic every " + config.bot.UPDATE_CHANNEL_TOPIC_INTERVAL + " minutes");
        }
  }

  public void shutdown() {
    jda.shutdown();
    if (updateTask != null && !updateTask.status().equals(TaskStatus.CANCELLED)) {
      updateTask.cancel();
    }
  }

  @Subscribe(order = PostOrder.FIRST)
  public void onPlayerChat(PlayerChatEvent event) {
    var currentServer = event.getPlayer().getCurrentServer();

    if (currentServer.isEmpty()) {
      return;
    }

    var server = currentServer.get().getServerInfo().getName();

    if (config.serverDisabled(server)) {
      return;
    }

    var username = event.getPlayer().getUsername();
    var content = event.getMessage();

    if (config.bot.ENABLE_MENTIONS) {
      content = parseMentions(content);
    }

    if (!config.bot.ENABLE_EVERYONE_AND_HERE) {
      content = filterEveryoneAndHere(content);
    }

    if (config.bot.USE_WEBHOOKS) {
      var uuid = event.getPlayer().getUniqueId().toString();

      var avatar = new StringTemplate(config.bot.WEBHOOK_AVATAR_URL)
        .add("username", username)
        .add("uuid", uuid).toString();

      var discordName = new StringTemplate(config.bot.WEBHOOK_USERNAME)
        .add("username", username)
        .add("server", server).toString();

      sendWebhookMessage(avatar, discordName, content);
    } else {
      if (config.discord.MESSAGE_FORMAT.isEmpty()) {
        return;
      }

      var message = new StringTemplate(config.discord.MESSAGE_FORMAT.get())
        .add("username", username)
        .add("server", server)
        .add("message", content).toString();

      sendMessage(message);
    }
  }

  @Subscribe
  public void onConnect(ServerConnectedEvent event) {
    var username = event.getPlayer().getUsername();
    var server = event.getServer().getServerInfo().getName();

    if (config.serverDisabled(server)) {
      updateActivityPlayerAmount();

      return;
    }

    var previousServer = event.getPreviousServer();

    StringTemplate message = null;

    if (previousServer.isPresent()) {
      if (config.discord.SERVER_SWITCH_MESSAGE_FORMAT.isPresent()) {
        var previous = previousServer.get().getServerInfo().getName();

        if (config.serverDisabled(previous)) {
          updateActivityPlayerAmount();

          return;
        }

        message = new StringTemplate(config.discord.SERVER_SWITCH_MESSAGE_FORMAT.get())
          .add("username", username)
          .add("current", server)
          .add("previous", previous);
      }
    } else if (config.discord.JOIN_MESSAGE_FORMAT.isPresent()) {
      message = new StringTemplate(config.discord.JOIN_MESSAGE_FORMAT.get())
        .add("username", username)
        .add("server", server);
    }

    if (message != null) {
      sendMessage(message.toString());
    }

    updateActivityPlayerAmount();
  }

  @Subscribe
  public void onDisconnect(DisconnectEvent event) {
    var currentServer = event.getPlayer().getCurrentServer();

    var username = event.getPlayer().getUsername();
    var server = currentServer.map(serverConnection -> serverConnection.getServerInfo().getName()).orElse("null");

    if (config.serverDisabled(server)) {
      updateActivityPlayerAmount();

      return;
    }

    String template = currentServer.isPresent() ? config.discord.LEAVE_MESSAGE_FORMAT.orElse(null) : config.discord.DISCONNECT_MESSAGE_FORMAT.orElse(null);

    if (template != null) {
      var message = new StringTemplate(template)
        .add("username", username)
        .add("server", server);

      sendMessage(message.toString());
    }

    updateActivityPlayerAmount();
  }

  public void sendMessage(@Nonnull String message) {
    activeChannel.sendMessage(message).queue();
  }

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

  public void sendWebhookMessage(String avatar, String username, String content) {
    var webhookMessage = new MessageCreateBuilder().setContent(content).build();

    webhookClient.sendMessage(webhookMessage).setAvatarUrl(avatar).setUsername(username).queue();
  }

  public void sendPlayerDeath(String username, String displayname, String death) {
    if (config.discord.DEATH_MESSAGE_FORMAT.isPresent()) {
      var message = new StringTemplate(config.discord.DEATH_MESSAGE_FORMAT.get())
        .add("username", username)
        .add("displayname", displayname)
        .add("death_message", death).toString();

      sendMessage(message);
    }
  }

  public void sendPlayerAdvancement(String username, String displayname, String title, String description) {
    if (config.discord.ADVANCEMENT_MESSAGE_FORMAT.isPresent()) {
      var message = new StringTemplate(config.discord.ADVANCEMENT_MESSAGE_FORMAT.get())
        .add("username", username)
        .add("displayname", displayname)
        .add("advancement_title", title)
        .add("advancement_description", description).toString();

      sendMessage(message);
    }
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
    if (!config.bot.SHOW_ACTIVITY) {
      return;
    }

    final var playerCount = this.server.getPlayerCount();

    if (this.lastPlayerCount != playerCount) {
      var message = new StringTemplate(config.bot.ACTIVITY_FORMAT)
        .add("amount", playerCount).toString();

      jda.getPresence().setActivity(Activity.playing(message));

      this.lastPlayerCount = playerCount;
    }
  }
  
  public void updateChannelTopic() {
    if (config.discord.TOPIC_FORMAT.isPresent()) {
        // Collect additional information
        int playerCount = this.server.getPlayerCount();
        List<String> playerList = this.server.getAllPlayers().stream()
            .map(player -> player.getUsername())
            .collect(Collectors.toList());
        List<String> playerPingList = this.server.getAllPlayers().stream()
            .map(player -> String.valueOf(player.getUsername()+ " (" + player.getPing() + "ms)"))
            .collect(Collectors.toList());
        int serverCount = this.server.getAllServers().size();
        List<String> serverList = this.server.getAllServers().stream()
            .map(registeredServer -> registeredServer.getServerInfo().getName())
            .collect(Collectors.toList());
        List<String> serverOnlineList = this.server.getAllServers().stream()
            .map(registeredServer -> String.valueOf(registeredServer.getServerInfo().getName() + " (" + registeredServer.getPlayersConnected().size() + " players)"))
            .collect(Collectors.toList());
        String hostname = this.server.getBoundAddress().getHostName();
        String port = String.valueOf(this.server.getBoundAddress().getPort());
        String queryMotd = PlainTextComponentSerializer.plainText().serialize(this.server.getConfiguration().getMotd());
        String queryMap = this.server.getConfiguration().getQueryMap();
        int queryPort = this.server.getConfiguration().getQueryPort();
        int queryMaxPlayers = this.server.getConfiguration().getShowMaxPlayers();
        int pluginCount = this.server.getPluginManager().getPlugins().size();
        List<String> pluginList = this.server.getPluginManager().getPlugins().stream()
            .map(plugin -> plugin.getDescription().getName())
            .flatMap(Optional::stream)
            .collect(Collectors.toList());
        String version = this.server.getVersion().getVersion();
        String software = this.server.getVersion().getName();

        // Build the message
        var message = new StringTemplate(config.discord.TOPIC_FORMAT.get())
            .add("playerCount", playerCount)
            .add("playerList", String.join(", ", playerList))
            .add("playerPingList", String.join(", ", playerPingList))
            .add("serverCount", serverCount)
            .add("serverList", String.join(", ", serverList))
            .add("serverOnlineList", String.join(", ", serverOnlineList))
            .add("hostname", hostname)
            .add("port", port)
            .add("queryMotd", queryMotd)
            .add("queryMap", queryMap.toString())
            .add("queryPort", queryPort)
            .add("queryMaxPlayers", queryMaxPlayers)
            .add("pluginCount", pluginCount)
            .add("pluginList", String.join(", ", pluginList))
            .add("version", version)
            .add("software", software)
            
            .toString();

        // Update the channel topic
        activeChannel.getManager().setTopic(message).queue();
    }
  }
}
