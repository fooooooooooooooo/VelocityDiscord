package ooo.foooooooooooo.velocitydiscord.discord;

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

  private final Logger logger;
  private final Config config;
  private final JDA jda;
  private final IncomingWebhookClient webhookClient;
  private final Map<String, ICommand> commands = new HashMap<>();

  private TextChannel activeChannel;
  private int lastPlayerCount = -1;

  public boolean ready = false;

  // todo: find a way to abstract away ProxyServer to remove all velocity dependencies
  public Discord(ProxyServer server, Logger logger, Config config) {
    this.logger = logger;
    this.config = config;

    commands.put("list", new ListCommand(server, config));

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
