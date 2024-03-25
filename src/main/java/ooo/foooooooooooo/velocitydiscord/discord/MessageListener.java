package ooo.foooooooooooo.velocitydiscord.discord;

import com.velocitypowered.api.proxy.ProxyServer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import ooo.foooooooooooo.velocitydiscord.config.Config;
import ooo.foooooooooooo.velocitydiscord.util.StringTemplate;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class MessageListener extends ListenerAdapter {
  private static final Pattern WEBHOOK_ID_REGEX = Pattern.compile("^https://discord\\.com/api/webhooks/(\\d+)/.+$");
  private final String webhookId;
  private final ProxyServer server;
  private final Logger logger;
  private final Config config;
  private JDA jda;

  public MessageListener(ProxyServer server, Logger logger, Config config) {
    this.server = server;
    this.logger = logger;
    this.config = config;

    final var matcher = WEBHOOK_ID_REGEX.matcher(config.bot.WEBHOOK_URL);
    this.webhookId = matcher.find() ? matcher.group(1) : null;
    logger.log(Level.FINER, "Found webhook id: {0}", webhookId);
  }

  @Override
  public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
    if (!event.isFromType(ChannelType.TEXT)) {
      logger.finest("ignoring non text channel message");
      return;
    }

    if (jda == null) {
      jda = event.getJDA();
    }

    var channel = event.getChannel().asTextChannel();
    if (!channel.getId().equals(config.bot.CHANNEL_ID)) {
      return;
    }

    var author = event.getAuthor();
    if (!config.minecraft.SHOW_BOT_MESSAGES && author.isBot()) {
      logger.finer("ignoring bot message");
      return;
    }

    if (author.getId().equals(jda.getSelfUser().getId()) || (Objects.nonNull(this.webhookId) && author.getId().equals(this.webhookId))) {
      logger.finer("ignoring own message");
      return;
    }

    var message = event.getMessage();
    var guild = event.getGuild();

    var color = Color.white;
    var nickname = author.getName(); // Nickname defaults to username

    var member = guild.getMember(author);
    if (member != null) {
      color = member.getColor();
      if (color == null) {
        color = Color.white;
      }
      nickname = member.getEffectiveName();
    }

    var hex = "#" + Integer.toHexString(color.getRGB()).substring(2);

    // parse configured message formats
    var discord_chunk = new StringTemplate(config.minecraft.DISCORD_CHUNK_FORMAT)
      .add("discord_color", config.minecraft.DISCORD_COLOR).toString();

    var username_chunk = new StringTemplate(config.minecraft.USERNAME_CHUNK_FORMAT)
      .add("role_color", hex)
      .add("username", author.getName())
      .add("nickname", nickname).toString();

    var attachment_chunk = config.minecraft.ATTACHMENT_FORMAT;
    var message_chunk = new StringTemplate(config.minecraft.MESSAGE_FORMAT)
      .add("discord_chunk", discord_chunk)
      .add("username_chunk", username_chunk)
      .add("message", message.getContentDisplay());

    var attachmentChunks = new ArrayList<String>();

    List<Message.Attachment> attachments = new ArrayList<>();
    if (config.minecraft.SHOW_ATTACHMENTS) {
      attachments = message.getAttachments();
    }

    for (var attachment : attachments) {
      var chunk = new StringTemplate(attachment_chunk)
        .add("url", attachment.getUrl())
        .add("attachment_color", config.minecraft.ATTACHMENT_COLOR).toString();

      attachmentChunks.add(chunk);
    }

    var content = message.getContentDisplay();

    // Remove leading whitespace from attachments if there's no content
    if (content.isBlank()) {
      message_chunk = message_chunk.replace(" {attachments}", "{attachments}");
    }

    message_chunk.add("message", content);
    message_chunk.add("attachments", String.join(" ", attachmentChunks));

    sendMessage(MiniMessage.miniMessage().deserialize(message_chunk.toString()).asComponent());
  }

  private void sendMessage(Component msg) {
    for (var server : server.getAllServers()) {
      if (!config.EXCLUDED_SERVERS_RECEIVE_MESSAGES && config.serverDisabled(server.getServerInfo().getName())) {
        continue;
      }

      server.sendMessage(msg);
    }
  }
}
