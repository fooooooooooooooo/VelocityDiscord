package ooo.foooooooooooo.velocitydiscord.discord;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import ooo.foooooooooooo.velocitydiscord.discord.commands.ICommand;
import ooo.foooooooooooo.velocitydiscord.discord.commands.ListCommand;
import ooo.foooooooooooo.velocitydiscord.util.StringTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.regex.Pattern;

import static ooo.foooooooooooo.velocitydiscord.VelocityDiscord.*;

public class Discord extends Thread {
  private static final Pattern EveryoneAndHerePattern = Pattern.compile("@(?<ping>everyone|here)");
  public static String SelfId;
  private static JDA jda;
  private WebhookClient webhookClient;
  private final Map<String, ICommand> commands = Map.of("list", new ListCommand());
  private TextChannel activeChannel;
  private int lastPlayerCount = -1;

  public Discord() {
    this.setDaemon(true);
    this.setName("VelocityDiscord Thread");
  }

  @Override
  public void run() {
    var builder = JDABuilder.createDefault(CONFIG.DISCORD_TOKEN);

    // this seems to download all users at bot startup and keep internal cache updated
    // without it, sometimes mentions miss when they shouldn't
    builder.setChunkingFilter(ChunkingFilter.ALL);

    // mentions always miss without this
    builder.setMemberCachePolicy(MemberCachePolicy.ALL);

    builder.enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT);
    builder.addEventListeners(new MessageListener(), new DiscordEvents(this));

    try {
      jda = builder.build();
    } catch (Exception e) {
      Logger.error("Failed to login to discord: {}", e.toString());
      throw new RuntimeException("Failed to login to discord: ", e);
    }

    SelfId = jda.getSelfUser().getId();
    webhookClient = CONFIG.DISCORD_USE_WEBHOOK ? new WebhookClientBuilder(CONFIG.WEBHOOK_URL).build() : null;
  }

  public void setActiveChannel(TextChannel newChannel) {
    activeChannel = newChannel;
  }

  public void shutdown() {
    jda.shutdown();
  }

  public void updateActivityPlayerAmount() {
    if (CONFIG.SHOW_ACTIVITY) {
      final int playerCount = SERVER != null ? SERVER.getCurrentPlayerCount() : -1;

      if (this.lastPlayerCount != playerCount) {
        jda
          .getPresence()
          .setActivity(Activity.playing(new StringTemplate(CONFIG.DISCORD_ACTIVITY_TEXT)
            .add("amount", playerCount)
            .toString()));

        this.lastPlayerCount = playerCount;
      }
    }
  }

  public void setupCommands() {
    var guild = activeChannel.getGuild();

    for (var command : commands.entrySet()) {
      guild.upsertCommand(command.getKey(), command.getValue().getDescription()).queue();
    }
  }

  public void onPlayerConnect(String username) {
    var message = new StringTemplate(CONFIG.JOIN_MESSAGE).add("username", username);

    sendMessage(message.toString());
    updateActivityPlayerAmount();
  }

  public void onPlayerDisconnect(String username) {
    var message = new StringTemplate(CONFIG.LEAVE_MESSAGE).add("username", username);

    sendMessage(message.toString());
    updateActivityPlayerAmount();
  }

  public void onPlayerChat(String username, String content, String uuid) {
    if (CONFIG.ENABLE_MENTIONS) {
      content = parseMentions(content);
    }

    if (!CONFIG.ENABLE_EVERYONE_AND_HERE) {
      content = filterEveryoneAndHere(content);
    }

    if (CONFIG.DISCORD_USE_WEBHOOK) {
      var avatar = new StringTemplate(CONFIG.WEBHOOK_AVATAR_URL).add("username", username).add("uuid", uuid).toString();

      var discordName = new StringTemplate(CONFIG.WEBHOOK_USERNAME).add("username", username).toString();

      sendWebhookMessage(avatar, discordName, content);
    } else {
      var message = new StringTemplate(CONFIG.DISCORD_CHAT_MESSAGE)
        .add("username", username)
        .add("message", content)
        .toString();

      sendMessage(message);
    }
  }

  public void onPlayerDeath(String username, String message) {
    sendMessage(new StringTemplate(CONFIG.DEATH_MESSAGE)
      .add("username", username)
      .add("death_message", message)
      .toString());
  }

  public void onPlayerAdvancement(String username, String title, String description) {
    sendMessage(new StringTemplate(CONFIG.ADVANCEMENT_MESSAGE)
      .add("username", username)
      .add("advancement_title", title)
      .add("advancement_description", description)
      .toString());
  }

  private void sendMessage(@NotNull String message) {
    activeChannel.sendMessage(message).queue();
  }

  private String parseMentions(String message) {
    var msg = message;

    for (var member : activeChannel.getMembers()) {
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

  private void sendWebhookMessage(String avatar, String username, String content) {
    var webhookMessage = new WebhookMessageBuilder()
      .setAvatarUrl(avatar)
      .setUsername(username)
      .setContent(content)
      .build();

    webhookClient.send(webhookMessage);
  }

  public void handleSlashCommand(SlashCommandInteractionEvent event) {
    var command = event.getName();

    var handler = commands.get(command);

    if (handler == null) {
      return;
    }

    handler.handle(event);
  }
}
