package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.Config;

import java.awt.*;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class DiscordMessageConfig extends BaseConfig {
  public DiscordMessageConfig(Config config) {
    loadConfig(config);
  }

  // chat
  public Optional<String> MESSAGE_FORMAT = Optional.of("{username}: {message}");
  public UserMessageType MESSAGE_TYPE = UserMessageType.TEXT;
  public Optional<Color> MESSAGE_EMBED_COLOR = Optional.empty();

  public Optional<String> DEATH_MESSAGE_FORMAT = Optional.of("**{username} {death_message}**");
  public MessageType DEATH_MESSAGE_TYPE = MessageType.TEXT;
  public Optional<Color> DEATH_MESSAGE_EMBED_COLOR = Optional.of(new Color(0xbf4040));

  public Optional<String> ADVANCEMENT_MESSAGE_FORMAT = Optional.of("**{username} has made the advancement __{advancement_title}__**\n_{advancement_description}_");
  public MessageType ADVANCEMENT_MESSAGE_TYPE = MessageType.TEXT;
  public Optional<Color> ADVANCEMENT_MESSAGE_EMBED_COLOR = Optional.of(new Color(0x40bf4f));

  // join/leave
  public Optional<String> JOIN_MESSAGE_FORMAT = Optional.of("**{username} joined the game**");
  public MessageType JOIN_MESSAGE_TYPE = MessageType.TEXT;
  public Optional<Color> JOIN_MESSAGE_EMBED_COLOR = Optional.of(new Color(0x40bf4f));

  public Optional<String> LEAVE_MESSAGE_FORMAT = Optional.of("**{username} left the game**");
  public MessageType LEAVE_MESSAGE_TYPE = MessageType.TEXT;
  public Optional<Color> LEAVE_MESSAGE_EMBED_COLOR = Optional.of(new Color(0xbf4040));

  public Optional<String> DISCONNECT_MESSAGE_FORMAT = Optional.of("**{username} disconnected**");
  public MessageType DISCONNECT_MESSAGE_TYPE = MessageType.TEXT;
  public Optional<Color> DISCONNECT_MESSAGE_EMBED_COLOR = Optional.of(new Color(0xbf4040));

  public Optional<String> SERVER_SWITCH_MESSAGE_FORMAT = Optional.of("**{username} moved to {current} from {previous}**");
  public MessageType SERVER_SWITCH_MESSAGE_TYPE = MessageType.TEXT;
  public Optional<Color> SERVER_SWITCH_MESSAGE_EMBED_COLOR = Optional.of(new Color(0x40bf4f));

  // proxy start/stop, server start/stop
  public Optional<String> PROXY_START_MESSAGE_FORMAT = Optional.of("**Proxy started**");
  public MessageType PROXY_START_MESSAGE_TYPE = MessageType.TEXT;
  public Optional<Color> PROXY_START_MESSAGE_EMBED_COLOR = Optional.of(new Color(0x40bf4f));

  public Optional<String> PROXY_STOP_MESSAGE_FORMAT = Optional.of("**Proxy stopped**");
  public MessageType PROXY_STOP_MESSAGE_TYPE = MessageType.TEXT;
  public Optional<Color> PROXY_STOP_MESSAGE_EMBED_COLOR = Optional.of(new Color(0xbf4040));

  public Optional<String> SERVER_START_MESSAGE_FORMAT = Optional.of("**{server} has started**");
  public MessageType SERVER_START_MESSAGE_TYPE = MessageType.TEXT;
  public Optional<Color> SERVER_START_MESSAGE_EMBED_COLOR = Optional.of(new Color(0x40bf4f));

  public Optional<String> SERVER_STOP_MESSAGE_FORMAT = Optional.of("**{server} has stopped**");
  public MessageType SERVER_STOP_MESSAGE_TYPE = MessageType.TEXT;
  public Optional<Color> SERVER_STOP_MESSAGE_EMBED_COLOR = Optional.of(new Color(0xbf4040));

  // channel topic
  public Optional<String> TOPIC_FORMAT = Optional.of("Player count: {playerCount} | Players: {playerList} | Pings: {playerPingList} | "
    + "Server count: {serverCount} | Servers: {serverList} | Hostname: {hostname} | Port: {port} | Query MOTD: {queryMotd} | "
    + "Query map: {queryMap} | Query port: {queryPort} | Max players: {queryMaxPlayers} | "
    + "Plugin count: {pluginCount} | Plugins: {pluginList} | Version: {version}");


    @Override
  protected void loadConfig(Config config) {
    MESSAGE_FORMAT = getOptional(config, "discord.chat.message", MESSAGE_FORMAT);

    // old config value not actually present in example config
    var useWebhooks = get(config, "discord.use_webhook", false);

    // if useWebhooks is true, force MESSAGE_TYPE to be WEBHOOK to not
    // break old behavior if someone didn't update their config
    if (useWebhooks) {
      MESSAGE_TYPE = UserMessageType.WEBHOOK;
    } else {
      var message_type = get(config, "discord.chat.message_type", "text");
      switch (message_type) {
        case "text", "":
          MESSAGE_TYPE = UserMessageType.TEXT;
          break;
        case "webhook":
          MESSAGE_TYPE = UserMessageType.WEBHOOK;
          break;
        case "embed":
          MESSAGE_TYPE = UserMessageType.EMBED;
          break;
        default:
          throw new RuntimeException("Invalid message type: " + message_type);
      }
    }
    MESSAGE_EMBED_COLOR = getColor(config, "discord.chat.message_embed_color", MESSAGE_EMBED_COLOR);

    DEATH_MESSAGE_FORMAT = getOptional(config, "discord.chat.death_message", DEATH_MESSAGE_FORMAT);
    DEATH_MESSAGE_TYPE = getMessageType(config, "discord.chat.death_message_type", DEATH_MESSAGE_TYPE);
    DEATH_MESSAGE_EMBED_COLOR = getColor(config, "discord.chat.death_message_embed_color", DEATH_MESSAGE_EMBED_COLOR);

    ADVANCEMENT_MESSAGE_FORMAT = getOptional(config, "discord.chat.advancement_message", ADVANCEMENT_MESSAGE_FORMAT);
    ADVANCEMENT_MESSAGE_TYPE = getMessageType(config, "discord.chat.advancement_message_type", ADVANCEMENT_MESSAGE_TYPE);
    ADVANCEMENT_MESSAGE_EMBED_COLOR = getColor(config, "discord.chat.advancement_message_embed_color", ADVANCEMENT_MESSAGE_EMBED_COLOR);

    JOIN_MESSAGE_FORMAT = getOptional(config, "discord.chat.join_message", JOIN_MESSAGE_FORMAT);
    JOIN_MESSAGE_TYPE = getMessageType(config, "discord.chat.join_message_type", JOIN_MESSAGE_TYPE);
    JOIN_MESSAGE_EMBED_COLOR = getColor(config, "discord.chat.join_message_embed_color", JOIN_MESSAGE_EMBED_COLOR);

    LEAVE_MESSAGE_FORMAT = getOptional(config, "discord.chat.leave_message", LEAVE_MESSAGE_FORMAT);
    LEAVE_MESSAGE_TYPE = getMessageType(config, "discord.chat.leave_message_type", LEAVE_MESSAGE_TYPE);
    LEAVE_MESSAGE_EMBED_COLOR = getColor(config, "discord.chat.leave_message_embed_color", LEAVE_MESSAGE_EMBED_COLOR);

    DISCONNECT_MESSAGE_FORMAT = getOptional(config, "discord.chat.disconnect_message", DISCONNECT_MESSAGE_FORMAT);
    DISCONNECT_MESSAGE_TYPE = getMessageType(config, "discord.chat.disconnect_message_type", DISCONNECT_MESSAGE_TYPE);
    DISCONNECT_MESSAGE_EMBED_COLOR = getColor(config, "discord.chat.disconnect_message_embed_color", DISCONNECT_MESSAGE_EMBED_COLOR);

    SERVER_SWITCH_MESSAGE_FORMAT = getOptional(config, "discord.chat.server_switch_message", SERVER_SWITCH_MESSAGE_FORMAT);
    SERVER_SWITCH_MESSAGE_TYPE = getMessageType(config, "discord.chat.server_switch_message_type", SERVER_SWITCH_MESSAGE_TYPE);
    SERVER_SWITCH_MESSAGE_EMBED_COLOR = getColor(config, "discord.chat.server_switch_message_embed_color", SERVER_SWITCH_MESSAGE_EMBED_COLOR);

    TOPIC_FORMAT = getOptional(config, "discord.topic_format", TOPIC_FORMAT);
  }

  private Optional<Color> getColor(Config config, String key, Optional<Color> defaultValue) {
    Optional<String> defaultHex = defaultValue.map((c) -> String.format("#%06X", (0xFFFFFF & c.getRGB())));
    return getOptional(config, key, defaultHex).map(Color::decode);
  }

  private MessageType getMessageType(Config config, String key, MessageType defaultValue) {
    var type = get(config, key, defaultValue.toString().toLowerCase());
    return switch (type) {
      case "text" -> MessageType.TEXT;
      case "embed" -> MessageType.EMBED;
      case "" -> defaultValue;
      default -> throw new RuntimeException("Invalid message type: " + type);
    };
  }

  public boolean isWebhookEnabled() {
    return MESSAGE_TYPE == UserMessageType.WEBHOOK;
  }

  public enum UserMessageType {
    TEXT,
    WEBHOOK,
    EMBED
  }

  public enum MessageType {
    TEXT,
    EMBED
  }
}
