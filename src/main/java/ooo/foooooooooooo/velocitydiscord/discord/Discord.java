package ooo.foooooooooooo.velocitydiscord.discord;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.ProxyServer;
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
import ooo.foooooooooooo.velocitydiscord.MessageListener;
import ooo.foooooooooooo.velocitydiscord.config.Config;
import ooo.foooooooooooo.velocitydiscord.discord.commands.ICommand;
import ooo.foooooooooooo.velocitydiscord.discord.commands.ListCommand;
import ooo.foooooooooooo.velocitydiscord.util.StringTemplate;
import ooo.foooooooooooo.velocitydiscord.yep.AdvancementMessage;
import ooo.foooooooooooo.velocitydiscord.yep.DeathMessage;

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
  private final WebhookClient webhookClient;
  private final Map<String, ICommand> commands = new HashMap<>();

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

    webhookClient = config.bot.USE_WEBHOOKS ? new WebhookClientBuilder(config.bot.WEBHOOK_URL).build() : null;
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
  }

  @Subscribe(order = PostOrder.FIRST)
  public void onPlayerChat(PlayerChatEvent event) {
    var currentServer = event.getPlayer().getCurrentServer();

    if (currentServer.isEmpty()) {
      return;
    }

    var username = event.getPlayer().getUsername();
    var server = currentServer.get().getServerInfo().getName();
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

    var previousServer = event.getPreviousServer();

    StringTemplate message = null;

    if (previousServer.isPresent()) {
      if (config.discord.SERVER_SWITCH_MESSAGE_FORMAT.isPresent()) {

        var previous = previousServer.get().getServerInfo().getName();

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
    var webhookMessage = new WebhookMessageBuilder().setAvatarUrl(avatar).setUsername(username).setContent(content).build();

    webhookClient.send(webhookMessage);
  }

  public void playerDeath(String username, DeathMessage death) {
    if (config.discord.DEATH_MESSAGE_FORMAT.isPresent()) {
      var message = new StringTemplate(config.discord.DEATH_MESSAGE_FORMAT.get())
        .add("username", username)
        .add("death_message", death.message).toString();

      sendMessage(message);
    }
  }

  public void playerAdvancement(String username, AdvancementMessage advancement) {
    if (config.discord.ADVANCEMENT_MESSAGE_FORMAT.isPresent()) {
      var message = new StringTemplate(config.discord.ADVANCEMENT_MESSAGE_FORMAT.get())
        .add("username", username)
        .add("advancement_title", advancement.title)
        .add("advancement_description", advancement.description).toString();

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
}
