package ooo.foooooooooooo.velocitydiscord;


import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.text.Text;
import ooo.foooooooooooo.velocitydiscord.util.StringTemplate;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ooo.foooooooooooo.velocitydiscord.VelocityDiscord.*;

public class MessageListener extends ListenerAdapter {
  private static final Pattern WEBHOOK_ID_REGEX = Pattern.compile("^https://discord\\.com/api/webhooks/(\\d+)/.+$");
  private final String webhookId;
  private JDA jda;

  public MessageListener() {
    final Matcher matcher = WEBHOOK_ID_REGEX.matcher(CONFIG.WEBHOOK_URL);
    this.webhookId = matcher.find() ? matcher.group(1) : null;
    LOGGER.trace("Found webhook id: {}", webhookId);
  }

  @Override
  public void onMessageReceived(
    @NotNull MessageReceivedEvent event
  ) {
    if (!event.isFromType(ChannelType.TEXT)) {
      LOGGER.trace("ignoring non text channel message");
      return;
    }

    if (jda == null) {
      jda = event.getJDA();
    }

    TextChannel channel = event.getChannel().asTextChannel();
    if (!channel.getId().equals(CONFIG.CHANNEL_ID)) { return; }

    User author = event.getAuthor();
    if (!CONFIG.SHOW_BOT_MESSAGES && author.isBot()) {
      LOGGER.trace("ignoring bot message");
      return;
    }

    if (author.getId().equals(jda.getSelfUser().getId()) ||
        (Objects.nonNull(this.webhookId) && author.getId().equals(this.webhookId))) {
      LOGGER.trace("ignoring own message");
      return;
    }

    Message message = event.getMessage();
    Guild guild = event.getGuild();

    Member member = guild.getMember(author);
    if (member == null) {
      LOGGER.warn("failed to get member: {}", author.getId());
      return;
    }

    Color color = member.getColor();
    if (color == null) { color = Color.white; }
    String hex = "#" + Integer.toHexString(color.getRGB()).substring(2);

    // parse configured message formats
    String discord_chunk =
      new StringTemplate(CONFIG.DISCORD_CHUNK).add("discord_color", CONFIG.DISCORD_COLOR).toString();

    String username_chunk = new StringTemplate(CONFIG.USERNAME_CHUNK)
      .add("role_color", hex)
      .add("username", author.getName())
      .add("discriminator", author.getDiscriminator())
      .add("nickname", member.getEffectiveName())
      .toString();

    String attachment_chunk = CONFIG.ATTACHMENTS;
    StringTemplate message_chunk = new StringTemplate(CONFIG.MC_CHAT_MESSAGE)
      .add("discord_chunk", discord_chunk)
      .add("username_chunk", username_chunk)
      .add("message", message.getContentDisplay());

    ArrayList<String> attachmentChunks = new ArrayList<>();

    List<Message.Attachment> attachments = new ArrayList<>();
    if (CONFIG.SHOW_ATTACHMENTS) {
      attachments = message.getAttachments();
    }

    for (Message.Attachment attachment : attachments) {
      attachmentChunks.add(new StringTemplate(attachment_chunk)
        .add("url", attachment.getUrl())
        .add("attachment_color", CONFIG.ATTACHMENT_COLOR)
        .toString());
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
    if (SERVER == null) {
      LOGGER.trace("No server to send message to");

      return;
    }

    SERVER.getPlayerManager().broadcast((Text) msg, false);
  }
}
