package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.Config;

import java.awt.*;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class DiscordMessageConfig extends BaseConfig {
  public DiscordMessageConfig(Config config) {
    super(config);
    loadConfig();
  }

  public DiscordMessageConfig(Config config, DiscordMessageConfig main) {
    super(config, main);
    loadConfig();
  }

  private static final Color RED = new Color(0xbf4040);
  private static final Color GREEN = new Color(0x40bf4f);

  // chat
  @Key("discord.chat.message.format")
  public Optional<String> MESSAGE_FORMAT = Optional.of("{username}: {message}");
  @Key("discord.chat.message.type")
  public UserMessageType MESSAGE_TYPE = UserMessageType.TEXT;
  @Key("discord.chat.message.embed_color")
  public Optional<Color> MESSAGE_EMBED_COLOR = Optional.empty();
  @Key("discord.chat.message.channel")
  public Optional<String> MESSAGE_CHANNEL = Optional.empty();

  // death
  @Key("discord.chat.death_message.format")
  public Optional<String> DEATH_FORMAT = Optional.of("**{username} {death_message}**");
  @Key("discord.chat.death_message.type")
  public UserMessageType DEATH_TYPE = UserMessageType.TEXT;
  @Key("discord.chat.death_message.embed_color")
  public Optional<Color> DEATH_EMBED_COLOR = Optional.of(RED);
  @Key("discord.chat.death_message.channel")
  public Optional<String> DEATH_CHANNEL = Optional.empty();

  // advancement
  @Key("discord.chat.advancement.format")
  public Optional<String> ADVANCEMENT_FORMAT =
    Optional.of("**{username} has made the advancement __{advancement_title}__**\n_{advancement_description}_");
  @Key("discord.chat.advancement.type")
  public UserMessageType ADVANCEMENT_TYPE = UserMessageType.TEXT;
  @Key("discord.chat.advancement.embed_color")
  public Optional<Color> ADVANCEMENT_EMBED_COLOR = Optional.of(GREEN);
  @Key("discord.chat.advancement.channel")
  public Optional<String> ADVANCEMENT_CHANNEL = Optional.empty();

  // join
  @Key("discord.chat.join.format")
  public Optional<String> JOIN_FORMAT = Optional.of("**{username} joined the game**");
  @Key("discord.chat.join.type")
  public UserMessageType JOIN_TYPE = UserMessageType.TEXT;
  @Key("discord.chat.join.embed_color")
  public Optional<Color> JOIN_EMBED_COLOR = Optional.of(GREEN);
  @Key("discord.chat.join.channel")
  public Optional<String> JOIN_CHANNEL = Optional.empty();

  // leave
  @Key("discord.chat.leave.format")
  public Optional<String> LEAVE_FORMAT = Optional.of("**{username} left the game**");
  @Key("discord.chat.leave.type")
  public UserMessageType LEAVE_TYPE = UserMessageType.TEXT;
  @Key("discord.chat.leave.embed_color")
  public Optional<Color> LEAVE_EMBED_COLOR = Optional.of(RED);
  @Key("discord.chat.leave.channel")
  public Optional<String> LEAVE_CHANNEL = Optional.empty();

  // disconnect
  @Key("discord.chat.disconnect.format")
  public Optional<String> DISCONNECT_FORMAT = Optional.of("**{username} disconnected**");
  @Key("discord.chat.disconnect.type")
  public UserMessageType DISCONNECT_TYPE = UserMessageType.TEXT;
  @Key("discord.chat.disconnect.embed_color")
  public Optional<Color> DISCONNECT_EMBED_COLOR = Optional.of(RED);
  @Key("discord.chat.disconnect.channel")
  public Optional<String> DISCONNECT_CHANNEL = Optional.empty();

  // server switch
  @Key("discord.chat.server_switch.format")
  public Optional<String> SERVER_SWITCH_FORMAT = Optional.of("**{username} moved to {current} from {previous}**");
  @Key("discord.chat.server_switch.type")
  public UserMessageType SERVER_SWITCH_TYPE = UserMessageType.TEXT;
  @Key("discord.chat.server_switch.embed_color")
  public Optional<Color> SERVER_SWITCH_EMBED_COLOR = Optional.of(GREEN);
  @Key("discord.chat.server_switch.channel")
  public Optional<String> SERVER_SWITCH_CHANNEL = Optional.empty();

  // proxy start
  @Key("discord.chat.proxy_start.format")
  public Optional<String> PROXY_START_FORMAT = Optional.of("**Proxy started**");
  @Key("discord.chat.proxy_start.type")
  public MessageType PROXY_START_TYPE = MessageType.TEXT;
  @Key("discord.chat.proxy_start.embed_color")
  public Optional<Color> PROXY_START_EMBED_COLOR = Optional.of(GREEN);
  @Key("discord.chat.proxy_start.channel")
  public Optional<String> PROXY_START_CHANNEL = Optional.empty();

  // proxy stop
  @Key("discord.chat.proxy_stop.format")
  public Optional<String> PROXY_STOP_FORMAT = Optional.of("**Proxy stopped**");
  @Key("discord.chat.proxy_stop.type")
  public MessageType PROXY_STOP_TYPE = MessageType.TEXT;
  @Key("discord.chat.proxy_stop.embed_color")
  public Optional<Color> PROXY_STOP_EMBED_COLOR = Optional.of(RED);
  @Key("discord.chat.proxy_stop.channel")
  public Optional<String> PROXY_STOP_CHANNEL = Optional.empty();

  // server start
  @Key("discord.chat.server_start.format")
  public Optional<String> SERVER_START_FORMAT = Optional.of("**{server} has started**");
  @Key("discord.chat.server_start.type")
  public MessageType SERVER_START_TYPE = MessageType.TEXT;
  @Key("discord.chat.server_start.embed_color")
  public Optional<Color> SERVER_START_EMBED_COLOR = Optional.of(GREEN);
  @Key("discord.chat.server_start.channel")
  public Optional<String> SERVER_START_CHANNEL = Optional.empty();

  // server stop
  @Key("discord.chat.server_stop.format")
  public Optional<String> SERVER_STOP_FORMAT = Optional.of("**{server} has stopped**");
  @Key("discord.chat.server_stop.type")
  public MessageType SERVER_STOP_TYPE = MessageType.TEXT;
  @Key("discord.chat.server_stop.embed_color")
  public Optional<Color> SERVER_STOP_EMBED_COLOR = Optional.of(RED);
  @Key("discord.chat.server_stop.channel")
  public Optional<String> SERVER_STOP_CHANNEL = Optional.empty();

  // channel topic
  @Key("discord.channel_topic.format")
  public Optional<String> TOPIC_FORMAT = Optional.of("""
    {players}/{max_players}
    {player_list}
    {hostname}:{port}
    Uptime: {uptime}""");

  @Key("discord.channel_topic.server")
  public Optional<String> TOPIC_SERVER_FORMAT = Optional.of("{name}: {players}/{max_players}");
  @Key("discord.channel_topic.server_offline")
  public Optional<String> TOPIC_SERVER_OFFLINE_FORMAT = Optional.of("{name}: Offline");

  @Key("discord.channel_topic.player_list_no_players_header")
  public Optional<String> TOPIC_PLAYER_LIST_NO_PLAYERS_HEADER = Optional.of("No players online");
  @Key("discord.channel_topic.player_list_header")
  public Optional<String> TOPIC_PLAYER_LIST_HEADER = Optional.of("Players: ");
  @Key("discord.channel_topic.player_list_player")
  public String TOPIC_PLAYER_LIST_FORMAT = "{username}";
  @Key("discord.channel_topic.player_list_separator")
  public String TOPIC_PLAYER_LIST_SEPARATOR = ", ";
  @Key("discord.channel_topic.player_list_max_count")
  public int TOPIC_PLAYER_LIST_MAX_COUNT = 10;

  public boolean isWebhookEnabled() {
    return this.MESSAGE_TYPE == UserMessageType.WEBHOOK;
  }
}
