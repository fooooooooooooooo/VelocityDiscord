package ooo.foooooooooooo.velocitydiscord.discord;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
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
import ooo.foooooooooooo.velocitydiscord.config.Config;
import ooo.foooooooooooo.velocitydiscord.config.ServerConfig;
import ooo.foooooooooooo.velocitydiscord.discord.commands.ICommand;
import ooo.foooooooooooo.velocitydiscord.discord.commands.ListCommand;
import ooo.foooooooooooo.velocitydiscord.util.StringTemplate;

import javax.annotation.Nonnull;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Discord extends ListenerAdapter {
  private static final Pattern EveryoneAndHerePattern = Pattern.compile("@(?<ping>everyone|here)");

  private final ProxyServer server;
  private final Logger logger;
  private final Config config;
  private final JDA jda;
  private final IncomingWebhookClient webhookClient;
  private final Map<String, ICommand> commands = new HashMap<>();
  private final Map<ServerConfig, TextChannel> channels = new HashMap<>();
  private TextChannel activeChannel;
  private int lastPlayerCount = -1;

  public Discord(ProxyServer server, Logger logger, Config config) {
    this.server = server;
    this.logger = logger;
    this.config = config;

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

    for (ServerConfig serverConfig : config.bot.SERVERS) {
      TextChannel channel = getChannel(serverConfig.CHANNEL_ID);
      channels.put(serverConfig, channel);
    }
    activeChannel = getChannel(config.bot.CHANNEL_ID);
    updateActivityPlayerAmount();
  }

  private TextChannel getChannel(String channelId) {
    TextChannel channel = jda.getTextChannelById(Objects.requireNonNull(channelId));
    if (channel == null) {
      logger.severe("Could not load channel with id: " + channelId);
      throw new RuntimeException("Could not load channel id: " + channelId);
    }
    if (!channel.canTalk()) {
      logger.severe("Cannot talk in configured channel");
      throw new RuntimeException("Cannot talk in configured channel");
    }
    logger.info("Loaded channel: " + channel.getName());
    Guild guild = channel.getGuild();
    guild.upsertCommand("list", "list players").queue();
    return channel;
  }

  public void shutdown() {
    jda.shutdown();
  }

  @Subscribe(order = PostOrder.FIRST)
  public void onPlayerChat(PlayerChatEvent event) {
    var currentServer = event.getPlayer().getCurrentServer();

    if (currentServer.isEmpty()) {
      return;
    }

    var server = currentServer.get().getServerInfo().getName();
    logger.info("Server: " + server);
    if (config.serverDisabled(server)) {
      return;
    }
    TextChannel channel = getTextChannel(server);

    var username = event.getPlayer().getUsername();
    var content = event.getMessage();

    if (config.bot.ENABLE_MENTIONS) {
      content = parseMentions(content, channel);
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

      sendMessage(message, channel);
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
      sendMessage(message.toString(), server);
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

      sendMessage(message.toString(), server);
    }

    updateActivityPlayerAmount();
  }
  public void sendMessage(@Nonnull String message, String server) {
    var channel = getTextChannel(server);
    sendMessage(message, channel);
  }
  public void sendMessage(@Nonnull String message, TextChannel channel) {
    channel.sendMessage(message).queue();
  }

  private TextChannel getTextChannel(String server) {
    TextChannel channel = activeChannel;
    ServerConfig serverConfig = config.getServerConfigByName(server);
    if (serverConfig != null) {
      channel = channels.get(serverConfig);
    }
    return channel;
  }

  private String parseMentions(String message, TextChannel channel) {
    var msg = message;

    for (var member : channel.getMembers()) {
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

  public void sendPlayerDeath(String username, String displayname, String death, String serverName) {
    if (config.discord.DEATH_MESSAGE_FORMAT.isPresent()) {
      var message = new StringTemplate(config.discord.DEATH_MESSAGE_FORMAT.get())
        .add("username", username)
        .add("displayname", displayname)
        .add("death_message", death).toString();

      sendMessage(message, serverName);
    }
  }

  public void sendPlayerAdvancement(String username, String displayname, String title, String description, String serverName) {
    if (config.discord.ADVANCEMENT_MESSAGE_FORMAT.isPresent()) {
      var message = new StringTemplate(config.discord.ADVANCEMENT_MESSAGE_FORMAT.get())
        .add("username", username)
        .add("displayname", displayname)
        .add("advancement_title", title)
        .add("advancement_description", description).toString();

      sendMessage(message, serverName);
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
}
