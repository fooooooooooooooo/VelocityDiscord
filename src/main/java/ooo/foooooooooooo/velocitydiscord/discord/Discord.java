package ooo.foooooooooooo.velocitydiscord.discord;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
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
import ooo.foooooooooooo.velocitydiscord.config.Config;
import ooo.foooooooooooo.velocitydiscord.discord.commands.ICommand;
import ooo.foooooooooooo.velocitydiscord.discord.commands.ListCommand;
import ooo.foooooooooooo.velocitydiscord.util.StringTemplate;

import javax.annotation.Nonnull;
import java.awt.*;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

  private TextChannel activeChannel;
  private int lastPlayerCount = -1;

  public boolean ready = false;

  public Discord(ProxyServer server, Logger logger, Config config) {
    this.server = server;
    this.logger = logger;
    this.config = config;

    commands.put("list", new ListCommand(server, logger, config));

    var messageListener = new MessageListener(server, logger, config);

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
    } catch (Exception e) {
      this.logger.severe("Failed to login to discord: " + e);
      throw new RuntimeException("Failed to login to discord: ", e);
    }

    webhookClient = config.discord.isWebhookEnabled() ? WebhookClient.createClient(jda, config.bot.WEBHOOK_URL) : null;
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

    this.ready = true;
  }

  public void shutdown() {
    jda.shutdown();
  }

  @Subscribe(order = PostOrder.FIRST)
  public void onPlayerChat(PlayerChatEvent event) {
    if (!ready) return;

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


    if (config.discord.isWebhookEnabled()) {
      var uuid = event.getPlayer().getUniqueId().toString();

      sendWebhookMessage(uuid, username, server, content);

      return;
    }

    if (config.discord.MESSAGE_FORMAT.isEmpty()) {
      return;
    }

    var message = new StringTemplate(config.discord.MESSAGE_FORMAT.get())
      .add("username", username)
      .add("server", server)
      .add("message", content).toString();

    switch (config.discord.MESSAGE_TYPE) {
      case EMBED -> sendEmbedMessage(message, config.discord.MESSAGE_EMBED_COLOR);
      case TEXT -> sendMessage(message);
    }
  }

  @Subscribe
  public void onConnect(ServerConnectedEvent event) {
    if (!ready) return;

    var username = event.getPlayer().getUsername();
    var server = event.getServer().getServerInfo().getName();

    if (config.serverDisabled(server)) {
      updateActivityPlayerAmount();

      return;
    }

    var previousServer = event.getPreviousServer();

    String message = null;

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
          .add("previous", previous).toString();
      }
    } else if (config.discord.JOIN_MESSAGE_FORMAT.isPresent()) {
      message = new StringTemplate(config.discord.JOIN_MESSAGE_FORMAT.get())
        .add("username", username)
        .add("server", server).toString();
    }

    if (message != null) {
      switch (config.discord.JOIN_MESSAGE_TYPE) {
        case EMBED -> sendEmbedMessage(message, config.discord.JOIN_MESSAGE_EMBED_COLOR);
        case TEXT -> sendMessage(message);
      }
    }

    updateActivityPlayerAmount();
  }

  @Subscribe
  public void onDisconnect(DisconnectEvent event) {
    if (!ready) return;

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
        .add("server", server).toString();

      switch (config.discord.LEAVE_MESSAGE_TYPE) {
        case EMBED -> sendEmbedMessage(message, config.discord.LEAVE_MESSAGE_EMBED_COLOR);
        case TEXT -> sendMessage(message);
      }
    }

    updateActivityPlayerAmount();
  }

  private void sendMessage(@Nonnull String message) {
    activeChannel.sendMessage(message).queue();
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private void sendEmbedMessage(String message, Optional<Color> color) {
    var embed = new EmbedBuilder().setDescription(message);

    color.ifPresent(embed::setColor);

    activeChannel.sendMessageEmbeds(embed.build()).queue();
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

  private void sendWebhookMessage(String uuid, String username, String server, String content) {
    var avatar = new StringTemplate(config.bot.WEBHOOK_AVATAR_URL)
      .add("username", username)
      .add("uuid", uuid).toString();

    var discordName = new StringTemplate(config.bot.WEBHOOK_USERNAME)
      .add("username", username)
      .add("server", server).toString();

    var webhookMessage = new MessageCreateBuilder().setContent(content).build();

    webhookClient.sendMessage(webhookMessage).setAvatarUrl(avatar).setUsername(discordName).queue();
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

  @Override
  public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
    if (!ready) return;

    var command = event.getName();

    if (!commands.containsKey(command)) {
      return;
    }

    commands.get(command).handle(event);
  }

  private void updateActivityPlayerAmount() {
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
