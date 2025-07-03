package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.serde.annotations.SerdeDefault;
import com.electronwill.nightconfig.core.serde.annotations.SerdeKey;
import ooo.foooooooooooo.velocitydiscord.config.commands.ListCommandConfig;
import ooo.foooooooooooo.velocitydiscord.config.commands.ListCommandGlobalConfig;

import java.util.Optional;
import java.util.function.Supplier;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused"})
public class DiscordConfig {
  private static final String DefaultChannelId = "000000000000000000";

  // bot
  @SerdeKey("channel")
  @SerdeDefault(provider = "defaultChannelId")
  public String MAIN_CHANNEL_ID;
  private final transient Supplier<String> defaultChannelId = () -> DefaultChannelId;

  // pings
  @SerdeKey("enable_mentions")
  @SerdeDefault(provider = "defaultEnableMentions")
  public Boolean ENABLE_MENTIONS;
  private final transient Supplier<Boolean> defaultEnableMentions = () -> true;

  @SerdeKey("enable_everyone_and_here")
  @SerdeDefault(provider = "defaultEnableEveryoneAndHere")
  public Boolean ENABLE_EVERYONE_AND_HERE;
  private final transient Supplier<Boolean> defaultEnableEveryoneAndHere = () -> false;

  // base webhook
  @SerdeKey("webhook")
  @SerdeDefault(provider = "defaultWebhookConfig")
  public WebhookConfig WEBHOOK;
  private final transient Supplier<WebhookConfig> defaultWebhookConfig = () -> EmptyConfig.deserialize(new WebhookConfig());

  // list command
  @SerdeKey("commands.list")
  @SerdeDefault(provider = "defaultCommandsListConfig")
  public ListCommandConfig COMMANDS_LIST;
  private final transient Supplier<ListCommandConfig> defaultCommandsListConfig = () -> EmptyConfig.deserialize(new ListCommandConfig());

  @SerdeKey("commands.list.global")
  @SerdeDefault(provider = "defaultCommandsListGlobalConfig")
  public ListCommandGlobalConfig COMMANDS_LIST_GLOBAL;
  private final transient Supplier<ListCommandGlobalConfig> defaultCommandsListGlobalConfig = () -> EmptyConfig.deserialize(new ListCommandGlobalConfig());

  // channel topic
  @SerdeKey("channel_topic.format")
  @SerdeDefault(provider = "defaultTopicFormat")
  public Optional<String> TOPIC_FORMAT;
  private final transient Supplier<Optional<String>> defaultTopicFormat = () -> Optional.of("""
    {players}/{max_players}
    {player_list}
    {hostname}:{port}
    Uptime: {uptime}""");

  @SerdeKey("channel_topic.server")
  @SerdeDefault(provider = "defaultTopicServerFormat")
  public Optional<String> TOPIC_SERVER_FORMAT;
  private final transient Supplier<Optional<String>> defaultTopicServerFormat = () -> Optional.of("{name}: {players}/{max_players}");

  @SerdeKey("channel_topic.server_offline")
  @SerdeDefault(provider = "defaultTopicServerOfflineFormat")
  public Optional<String> TOPIC_SERVER_OFFLINE_FORMAT;
  private final transient Supplier<Optional<String>> defaultTopicServerOfflineFormat = () -> Optional.of("{name}: Offline");

  @SerdeKey("channel_topic.player_list_no_players_header")
  @SerdeDefault(provider = "defaultTopicPlayerListNoPlayersHeader")
  public Optional<String> TOPIC_PLAYER_LIST_NO_PLAYERS_HEADER;
  private final transient Supplier<Optional<String>> defaultTopicPlayerListNoPlayersHeader = () -> Optional.of("No players online");

  @SerdeKey("channel_topic.player_list_header")
  @SerdeDefault(provider = "defaultTopicPlayerListHeader")
  public Optional<String> TOPIC_PLAYER_LIST_HEADER;
  private final transient Supplier<Optional<String>> defaultTopicPlayerListHeader = () -> Optional.of("Players: ");

  @SerdeKey("channel_topic.player_list_player")
  @SerdeDefault(provider = "defaultTopicPlayerListFormat")
  public String TOPIC_PLAYER_LIST_FORMAT;
  private final transient Supplier<String> defaultTopicPlayerListFormat = () -> "{username}";

  @SerdeKey("channel_topic.player_list_separator")
  @SerdeDefault(provider = "defaultTopicPlayerListSeparator")
  public String TOPIC_PLAYER_LIST_SEPARATOR;
  private final transient Supplier<String> defaultTopicPlayerListSeparator = () -> ", ";

  @SerdeKey("channel_topic.player_list_max_count")
  @SerdeDefault(provider = "defaultTopicPlayerListMaxCount")
  public int TOPIC_PLAYER_LIST_MAX_COUNT;
  private final transient Supplier<Integer> defaultTopicPlayerListMaxCount = () -> 10;

  public boolean isDefaultValues() {
    return this.MAIN_CHANNEL_ID.equals(DefaultChannelId);
  }

  public String debug() {
    var sb = new StringBuilder();

    sb.append("channel: ").append(ConfigConstants.debugString(this.MAIN_CHANNEL_ID)).append("\n");
    sb.append("enable_mentions: ").append(this.ENABLE_MENTIONS).append("\n");
    sb.append("enable_everyone_and_here: ").append(this.ENABLE_EVERYONE_AND_HERE).append("\n");

    sb.append("webhook:\n");
    for (var line : this.WEBHOOK.debug().split("\n")) {
      sb.append("  ").append(line).append("\n");
    }

    sb.append("commands_list:\n");
    for (var line : this.COMMANDS_LIST.debug().split("\n")) {
      sb.append("  ").append(line).append("\n");
    }

    sb.append("commands_list_global:\n");
    for (var line : this.COMMANDS_LIST_GLOBAL.debug().split("\n")) {
      sb.append("  ").append(line).append("\n");
    }

    sb.append("channel_topic:\n");
    sb.append("  format: ").append(this.TOPIC_FORMAT.map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("  server: ").append(this.TOPIC_SERVER_FORMAT.map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("  server_offline: ").append(this.TOPIC_SERVER_OFFLINE_FORMAT.map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("  player_list_no_players_header: ").append(this.TOPIC_PLAYER_LIST_NO_PLAYERS_HEADER.map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("  player_list_header: ").append(this.TOPIC_PLAYER_LIST_HEADER.map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("  player_list_format: ").append(ConfigConstants.debugString(this.TOPIC_PLAYER_LIST_FORMAT)).append("\n");
    sb.append("  player_list_separator: ").append(ConfigConstants.debugString(this.TOPIC_PLAYER_LIST_SEPARATOR)).append("\n");
    sb.append("  player_list_max_count: ").append(this.TOPIC_PLAYER_LIST_MAX_COUNT).append("\n");

    return sb.toString();
  }
}
