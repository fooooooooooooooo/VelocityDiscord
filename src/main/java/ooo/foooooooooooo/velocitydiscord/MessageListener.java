package ooo.foooooooooooo.velocitydiscord;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class MessageListener extends ListenerAdapter {
    public final String discordColor = "#7289da";
    public final String attachmentColor = "#4abdff";
    private final ProxyServer server;
    private final Logger logger;
    private final Config config;
    private JDA jda;

    public MessageListener(ProxyServer server, Logger logger, Config config) {
        this.server = server;
        this.logger = logger;
        this.config = config;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromType(ChannelType.TEXT)) {
            logger.finest("ignoring non text channel message");
            return;
        }

        if (jda == null) {
            jda = event.getJDA();
        }

        TextChannel channel = event.getTextChannel();
        if (!channel.getId().equals(config.CHANNEL_ID)) return;

        User author = event.getAuthor();
        if (!config.SHOW_BOT_MESSAGES && author.isBot()) {
            logger.finer("ignoring bot message");
            return;
        }

        if (author.getId().equals(jda.getSelfUser().getId())) {
            logger.finer("ignoring own message");
            return;
        }

        Message message = event.getMessage();
        Guild guild = event.getGuild();

        Member member = guild.getMember(author);
        if (member == null) {
            logger.warning("failed to get member: " + author.getId());
            return;
        }

        Color color = member.getColor();
        if (color == null) color = Color.white;
        String hex = "#" + Integer.toHexString(color.getRGB()).substring(2);

        // parse configured message formats
        String discord_chunk = config.DISCORD_CHUNK
                .replace("{discord_color}", discordColor);

        String username_chunk = config.USERNAME_CHUNK
                .replace("{role_color}", hex)
                .replace("{username}", author.getName())
                .replace("{discriminator}", author.getDiscriminator())
                .replace("{nickname}", member.getEffectiveName());

        String attachment_chunk = config.ATTACHMENTS;
        String message_chunk = config.MC_CHAT_MESSAGE
                .replace("{discord_chunk}", discord_chunk)
                .replace("{username_chunk}", username_chunk)
                .replace("{message}", message.getContentDisplay());

        ArrayList<String> attachmentChunks = new ArrayList<>();

        List<Message.Attachment> attachments = new ArrayList<>();
        if (config.SHOW_ATTACHMENTS) {
            attachments = message.getAttachments();
        }

        for (Message.Attachment attachment : attachments) {
            attachmentChunks.add(attachment_chunk
                    .replace("{url}", attachment.getUrl())
                    .replace("{attachment_color}", attachmentColor)
            );
        }

        var content = message.getContentDisplay();

        if (content.isBlank()) {
            message_chunk = message_chunk.replace(" {attachments}", "{attachments}");
        }

        message_chunk = message_chunk
                .replace("{message}", content)
                .replace("{attachments}", String.join(" ", attachmentChunks));

        sendMessage(MiniMessage.miniMessage().deserialize(message_chunk).asComponent());
    }

    private void sendMessage(Component msg) {
        for (RegisteredServer server : server.getAllServers())
            server.sendMessage(msg);
    }
}
