package ooo.foooooooooooo.velocitydiscord.discord;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
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
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import ooo.foooooooooooo.velocitydiscord.MessageListener;
import ooo.foooooooooooo.velocitydiscord.discord.commands.ICommand;
import ooo.foooooooooooo.velocitydiscord.discord.commands.ListCommand;
import ooo.foooooooooooo.velocitydiscord.events.AdvancementCallback;
import ooo.foooooooooooo.velocitydiscord.util.StringTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import static ooo.foooooooooooo.velocitydiscord.VelocityDiscord.*;

public class Discord extends ListenerAdapter {
  private static final Pattern EveryoneAndHerePattern = Pattern.compile("@(?<ping>everyone|here)");
  public static String SelfId;
  private static JDA jda;
  private final WebhookClient webhookClient;
  private final Map<String, ICommand> commands = new HashMap<>();
  private TextChannel activeChannel;
  private int lastPlayerCount = -1;

  public Discord() {
    commands.put("list", new ListCommand());

    var messageListener = new MessageListener();

    var builder = JDABuilder.createDefault(CONFIG.DISCORD_TOKEN)
                            // this seems to download all users at bot startup and keep internal cache updated
                            // without it, sometimes mentions miss when they shouldn't
                            .setChunkingFilter(ChunkingFilter.ALL)
                            .enableIntents(GatewayIntent.GUILD_MEMBERS,
                              GatewayIntent.GUILD_MESSAGES,
                              GatewayIntent.MESSAGE_CONTENT
                            )
                            // mentions always miss without this
                            .setMemberCachePolicy(MemberCachePolicy.ALL)
                            .addEventListeners(messageListener, this);

    try {
      jda = builder.build();

      SelfId = jda.getSelfUser().getId();
    } catch (Exception e) {
      Logger.error("Failed to login to discord: {}", e.toString());
      throw new RuntimeException("Failed to login to discord: ", e);
    }

    webhookClient = CONFIG.DISCORD_USE_WEBHOOK ? new WebhookClientBuilder(CONFIG.WEBHOOK_URL).build() : null;
  }

  public void shutdown() {
    jda.shutdown();
  }

  @Override
  public void onReady(@NotNull ReadyEvent event) {
    Logger.info("Bot ready, Guilds: {} ({} available)", event.getGuildTotalCount(), event.getGuildAvailableCount());

    var channel = jda.getTextChannelById(Objects.requireNonNull(CONFIG.CHANNEL_ID));

    if (channel == null) {
      Logger.error("Could not load channel with id: {}", CONFIG.CHANNEL_ID);
      throw new RuntimeException("Could not load channel id: " + CONFIG.CHANNEL_ID);
    }

    Logger.info("Loaded channel: {}", channel.getName());

    if (!channel.canTalk()) {
      Logger.error("Cannot talk in configured channel");
      throw new RuntimeException("Cannot talk in configured channel");
    }

    activeChannel = channel;

    var guild = activeChannel.getGuild();

    guild.upsertCommand("list", "list players").queue();

    updateActivityPlayerAmount();

    ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
      onPlayerConnect(handler.getPlayer());
    });

    ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
      onPlayerDisconnect(handler.getPlayer());
    });

    ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
      if (sender.isPlayer()) {
        onPlayerChat(message, sender);
      }
    });

    ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
      if (entity instanceof ServerPlayerEntity player) {
        var name = player.getDisplayName().getString();
        var message = getComponentText(damageSource.getDeathMessage(player)).replace(name + " ", "");

        onPlayerDeath(name, message);
      }
    });
    AdvancementCallback.EVENT.register((player, advancement) -> {
      var display = advancement.getDisplay();

      if (display == null || display.isHidden()) {
        Logger.trace("Ignoring unsent display");
        return;
      }

      var title = getComponentText(display.getTitle());
      var description = getComponentText(display.getDescription());

      onPlayerAdvancement(player.getName().getString(), title, description);
    });
  }

  @Override
  public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
    var command = event.getName();

    if (!commands.containsKey(command)) {
      return;
    }

    commands.get(command).handle(event);
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

  public void onPlayerConnect(ServerPlayerEntity player) {
    var username = player.getName().getString();
    var message = new StringTemplate(CONFIG.JOIN_MESSAGE).add("username", username);

    sendMessage(message.toString());
    updateActivityPlayerAmount();
  }

  public void onPlayerDisconnect(ServerPlayerEntity player) {
    var username = player.getName().getString();
    var message = new StringTemplate(CONFIG.LEAVE_MESSAGE).add("username", username);

    sendMessage(message.toString());
    updateActivityPlayerAmount();
  }

  public void onPlayerChat(SignedMessage signedMessage, ServerPlayerEntity player) {
    var username = player.getName().getString();
    var content = signedMessage.getContent().getString();

    if (CONFIG.ENABLE_MENTIONS) {
      content = parseMentions(content);
    }

    if (!CONFIG.ENABLE_EVERYONE_AND_HERE) {
      content = filterEveryoneAndHere(content);
    }

    if (CONFIG.DISCORD_USE_WEBHOOK) {
      var uuid = player.getUuid().toString();

      var avatar = new StringTemplate(CONFIG.WEBHOOK_AVATAR_URL).add("username", username).add("uuid", uuid).toString();

      var discordName = new StringTemplate(CONFIG.WEBHOOK_USERNAME).add("username", username).toString();

      sendWebhookMessage(avatar, discordName, content);
    } else {
      var message =
        new StringTemplate(CONFIG.DISCORD_CHAT_MESSAGE).add("username", username).add("message", content).toString();

      sendMessage(message);
    }
  }

  private static String getComponentText(Text component) {
    return component.getString();
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

  public void sendMessage(@NotNull String message) {
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

  public void sendWebhookMessage(String avatar, String username, String content) {
    var webhookMessage =
      new WebhookMessageBuilder().setAvatarUrl(avatar).setUsername(username).setContent(content).build();

    webhookClient.send(webhookMessage);
  }
}
