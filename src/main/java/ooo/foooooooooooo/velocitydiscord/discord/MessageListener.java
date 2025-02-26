package ooo.foooooooooooo.velocitydiscord.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import ooo.foooooooooooo.velocitydiscord.VelocityDiscord;
import ooo.foooooooooooo.velocitydiscord.util.StringTemplate;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageListener extends ListenerAdapter {
  private static final Pattern LINK_REGEX =
    Pattern.compile("[^:/?#\\s]+:(?://)?(?:[^?#\\s]+)?(?:\\?[^#\\s]+)?(?:#\\S+)?");
  private static final Pattern WEBHOOK_ID_REGEX = Pattern.compile(".*/webhooks/\\d+/([a-zA-Z0-9_-]+)");
  private final HashMap<String, Discord.Channels> serverChannels;
  private final HashMap<Long, List<String>> channelToServersMap = new HashMap<>();

  private JDA jda;

  public MessageListener(HashMap<String, Discord.Channels> serverChannels) {
    this.serverChannels = serverChannels;
    onServerChannelsUpdated();
  }

  public void onServerChannelsUpdated() {
    this.channelToServersMap.clear();

    for (var entry : this.serverChannels.entrySet()) {
      this.channelToServersMap
        .computeIfAbsent(entry.getValue().chatChannel.getIdLong(), (k) -> new ArrayList<>())
        .add(entry.getKey());
    }
  }

  @Override
  public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
    if (!event.isFromType(ChannelType.TEXT)) {
      VelocityDiscord.LOGGER.trace("ignoring non text channel message");
      return;
    }

    if (this.jda == null) {
      this.jda = event.getJDA();
    }

    var channel = event.getChannel().asTextChannel();
    var targetServerNames = this.channelToServersMap.get(channel.getIdLong());

    if (targetServerNames == null) {
      return;
    }

    VelocityDiscord.LOGGER.trace(
      "Received message from Discord channel {} for servers {}",
      channel.getName(),
      targetServerNames
    );

    var messages = new HashMap<String, String>();
    for (var serverName : targetServerNames) {
      messages.put(serverName, serializeMinecraftMessage(event, serverName));
    }

    for (var server : VelocityDiscord.SERVER.getAllServers()) {
      var serverName = server.getServerInfo().getName();
      if (!VelocityDiscord.CONFIG.EXCLUDED_SERVERS_RECEIVE_MESSAGES
        && VelocityDiscord.CONFIG.serverDisabled(serverName)) {
        continue;
      }

      var message = messages.get(serverName);
      if (message == null) continue;

      server.sendMessage(MiniMessage.miniMessage().deserialize(message).asComponent());
    }
  }

  private String serializeMinecraftMessage(MessageReceivedEvent event, String server) {
    var serverConfig = VelocityDiscord.CONFIG.getServerConfig(server);
    var serverMinecraftConfig = serverConfig.getMinecraftConfig();

    var author = event.getAuthor();
    if (!serverConfig.getMinecraftConfig().SHOW_BOT_MESSAGES && author.isBot()) {
      VelocityDiscord.LOGGER.debug("ignoring bot message");
      return null;
    }

    if (author.getId().equals(this.jda.getSelfUser().getId()) || (
      author.getId().equals(serverConfig.getDiscordConfig().WEBHOOK.webhookId))) {
      VelocityDiscord.LOGGER.debug("ignoring own message");
      return null;
    }

    var message = event.getMessage();
    var guild = event.getGuild();

    var color = Color.white;
    var nickname = author.getName(); // Nickname defaults to username
    var rolePrefix = "";

    var member = guild.getMember(author);
    if (member != null) {
      color = member.getColor();
      if (color == null) {
        color = Color.white;
      }
      nickname = member.getEffectiveName();

      // Get the role prefix
      var highestRole = member
        .getRoles()
        .stream()
        .filter(role -> !serverMinecraftConfig.rolePrefixes.getPrefixForRole(role.getId()).isEmpty())
        .findFirst();

      rolePrefix =
        highestRole.map(role -> serverMinecraftConfig.rolePrefixes.getPrefixForRole(role.getId())).orElse("");
    }

    var hex = "#" + Integer.toHexString(color.getRGB()).substring(2);

    // parse configured message formats
    var discord_chunk = new StringTemplate(serverMinecraftConfig.DISCORD_CHUNK_FORMAT)
      .add("discord_color", serverMinecraftConfig.DISCORD_COLOR)
      .toString();

    var display_name = author.getGlobalName();

    if (display_name == null) {
      display_name = author.getName();
    }

    var username_chunk = new StringTemplate(serverMinecraftConfig.USERNAME_CHUNK_FORMAT)
      .add("role_color", hex)
      .add("username", escapeTags(author.getName()))
      .add("display_name", escapeTags(display_name))
      .add("nickname", escapeTags(nickname))
      .toString();

    var attachment_chunk = serverMinecraftConfig.ATTACHMENT_FORMAT;
    var message_chunk = new StringTemplate(serverMinecraftConfig.MESSAGE_FORMAT)
      .add("discord_chunk", discord_chunk)
      .add("role_prefix", escapeTags(rolePrefix))
      .add("username_chunk", username_chunk)
      .add("message", message.getContentDisplay());

    var attachmentChunks = new ArrayList<String>();

    List<Message.Attachment> attachments = new ArrayList<>();
    if (serverMinecraftConfig.SHOW_ATTACHMENTS) {
      attachments = message.getAttachments();
    }

    for (var attachment : attachments) {
      var chunk = new StringTemplate(attachment_chunk)
        .add("url", attachment.getUrl())
        .add("attachment_color", serverMinecraftConfig.ATTACHMENT_COLOR)
        .toString();

      attachmentChunks.add(chunk);
    }

    var content = message.getContentDisplay();

    // Remove leading whitespace from attachments if there's no content
    if (content.isBlank()) {
      message_chunk = message_chunk.replace(" {attachments}", "{attachments}");
    }

    if (serverMinecraftConfig.LINK_FORMAT.isPresent()) {
      // Replace links with the link format
      content = LINK_REGEX.matcher(content).replaceAll(match -> {
        var url = match.group();
        var replacement = new StringTemplate(serverMinecraftConfig.LINK_FORMAT.get())
          .add("url", url)
          .add("link_color", serverMinecraftConfig.LINK_COLOR)
          .toString();

        return Matcher.quoteReplacement(replacement);
      });
    }

    message_chunk.add("message", content);
    message_chunk.add("attachments", String.join(" ", attachmentChunks));

    return message_chunk.toString();
  }

  private String escapeTags(String input) {
    return input.replace("<", "ᐸ").replace(">", "ᐳ");
  }
}
