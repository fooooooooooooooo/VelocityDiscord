package ooo.foooooooooooo.velocitydiscord.config;

import ooo.foooooooooooo.config.Config;
import ooo.foooooooooooo.config.Key;
import ooo.foooooooooooo.velocitydiscord.config.commands.ListCommandConfig;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class DiscordConfig extends Config {
  private static final String DefaultToken = "TOKEN";
  private static final String DefaultChannelId = "000000000000000000";

  // bot
  @Key(value = "token", overridable = false)
  public String DISCORD_TOKEN = DefaultToken;
  @Key("channel")
  public String MAIN_CHANNEL_ID = DefaultChannelId;

  // pings
  @Key("enable_mentions")
  public Boolean ENABLE_MENTIONS = true;
  @Key("enable_everyone_and_here")
  public Boolean ENABLE_EVERYONE_AND_HERE = false;

  // bot activity
  @Key(value = "activity_format", overridable = false)
  public Optional<String> ACTIVITY_FORMAT = Optional.of("with {amount} players online");

  // update channel topic
  @Key(value = "update_channel_topic_interval", overridable = false)
  public int UPDATE_CHANNEL_TOPIC_INTERVAL_MINUTES = 0;

  // base webhook
  @Key("webhook")
  public WebhookConfig WEBHOOK;

  // list command
  @Key("commands.list")
  public ListCommandConfig COMMANDS_LIST;

  // channel topic
  @Key("channel_topic.format")
  public Optional<String> TOPIC_FORMAT = Optional.of("""
    {players}/{max_players}
    {player_list}
    {hostname}:{port}
    Uptime: {uptime}""");

  @Key("channel_topic.server")
  public Optional<String> TOPIC_SERVER_FORMAT = Optional.of("{name}: {players}/{max_players}");
  @Key("channel_topic.server_offline")
  public Optional<String> TOPIC_SERVER_OFFLINE_FORMAT = Optional.of("{name}: Offline");

  @Key("channel_topic.player_list_no_players_header")
  public Optional<String> TOPIC_PLAYER_LIST_NO_PLAYERS_HEADER = Optional.of("No players online");
  @Key("channel_topic.player_list_header")
  public Optional<String> TOPIC_PLAYER_LIST_HEADER = Optional.of("Players: ");
  @Key("channel_topic.player_list_player")
  public String TOPIC_PLAYER_LIST_FORMAT = "{username}";
  @Key("channel_topic.player_list_separator")
  public String TOPIC_PLAYER_LIST_SEPARATOR = ", ";
  @Key("channel_topic.player_list_max_count")
  public int TOPIC_PLAYER_LIST_MAX_COUNT = 10;

  public boolean updateChannelTopicDisabled() {
    return this.UPDATE_CHANNEL_TOPIC_INTERVAL_MINUTES == 0;
  }

  @SuppressWarnings("unused")
  public DiscordConfig(com.electronwill.nightconfig.core.Config config) {
    super(config);
  }

  @SuppressWarnings("unused")
  public DiscordConfig(com.electronwill.nightconfig.core.Config config, DiscordConfig main) {
    super(config, main);
  }

  public boolean isDefaultValues() {
    return this.DISCORD_TOKEN.equals(DefaultToken) || this.MAIN_CHANNEL_ID.equals(DefaultChannelId);
  }
}
